package com.example.bprac

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.AsyncTask
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class NetworkImposter(private val activity: AppCompatActivity, val context: Context, var hostAddress: InetAddress, private val commonPort: Int) {
    //private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var isServer: Boolean = false;
    private var hasTransmissionTask = false;

    private var transmissionThread: Thread? = null
    private var receptionThreads = mutableListOf<Thread>()

    private var outputStreams = mutableListOf<OutputStream>()

    @Volatile private var currentlyRecording: Boolean = false
    private var buffer: ByteArray = ByteArray(NetworkImposter.PACKET_SIZE)

    public fun initiateConnection(isServer: Boolean) {
        this.isServer = isServer;
        if(isServer) {
            Thread{ serverHandshake() }.start()
        } else {
            Thread{ clientHandshake() }.start()
        }
    }

    public fun prepareSockets()
    {
        var socket: Socket? = null;
        if (isServer) {
            socket = Socket()
            socket!!.reuseAddress = true
            socket!!.bind(null)
            socket!!.connect(InetSocketAddress(hostAddress, commonPort), 10000)
        } else {
            serverSocket = ServerSocket(commonPort)
            socket = serverSocket!!.accept()
        }

        outputStreams.add(socket!!.getOutputStream())

        if (!hasTransmissionTask) {
            transmissionThread = Thread { transmissionTask() }
            transmissionThread!!.priority = 10
            transmissionThread!!.start()
            hasTransmissionTask = true
        }

        Thread.sleep(1000)

        val receptionThread = Thread { receptionTask(socket) }
        receptionThread!!.priority = 9
        receptionThreads.add(receptionThread)
        receptionThread!!.start()
    }

    fun setRecording(value: Boolean) {
        currentlyRecording = value;
    }



    fun serverHandshake() {
        Log.d("HANDSHAKE","SERVER")
        val serverSocket = ServerSocket(8998)
        val client = serverSocket.accept()
        this.hostAddress = client.inetAddress
        serverSocket.close()
        prepareSockets()
    }


    fun clientHandshake() {
        Log.d("HANDSHAKE","CLIENT")

        while(true) {
            try{
                Log.d(CL_TAG,"CTRY "+hostAddress);
                val socket = Socket()
                socket.reuseAddress = true
                socket.bind(null)
                socket.connect(InetSocketAddress(hostAddress, 8998), 10000)
                socket.close()
                break
            }
            catch (e: Exception)
            {
                Log.d(CL_TAG,"CFAIl");
                Thread.sleep(250);
            }

        }

        prepareSockets()
    }



    fun receptionTask(socket: Socket) {
        while (true) {
            Log.d(S_TAG, "Audio receiver started.")
            try {
                val buffer = ByteArray(NetworkImposter.PACKET_SIZE)
                val inputStream = socket.getInputStream()
                val audioPlayer = AudioTrack(
                    AudioManager.STREAM_SYSTEM,
                    8192,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    NetworkImposter.PACKET_SIZE *10,
                    AudioTrack.MODE_STREAM
                )

                var bytesRead = 0

                while (true) {
                    var numberBytes = inputStream.read(buffer, bytesRead, NetworkImposter.PACKET_SIZE - bytesRead)
                    if (numberBytes > 0) {
                        if (bytesRead + numberBytes == NetworkImposter.PACKET_SIZE) {
                            audioPlayer!!.write(buffer, 0, NetworkImposter.PACKET_SIZE)
                            val myStream = socket.getOutputStream()

                            if(isServer) {
                                for (stream in outputStreams) {
                                    if (stream != myStream)
                                        stream.write(buffer)
                                }
                            }

                            bytesRead = 0
                            numberBytes = 0
                            audioPlayer!!.play()
                        }

                        bytesRead += numberBytes
                    }
                }
            } catch (e: IOException) {
                Log.e(S_TAG, (e.message)!!)
            }
        }
    }


    fun transmissionTask() {
        Log.d(CL_TAG, "Attempting to begin transmission...")

        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            val audioRecorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                8192,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                NetworkImposter.PACKET_SIZE
            )

            audioRecorder!!.startRecording()

            try {
                while (true) {
                    if (currentlyRecording) {
                        val amountToRead = NetworkImposter.PACKET_SIZE
                        audioRecorder!!.read(buffer, 0, NetworkImposter.PACKET_SIZE)
                        for (stream in outputStreams) stream.write(buffer)
                    }
                }
            }
            catch (exception: Exception) {
                exception.printStackTrace()
            }

        } catch (e: IOException) {
            Log.e(CL_TAG, e.stackTraceToString())
        }
    }

    companion object {
        private const val CL_TAG = "CLTASK"
        private const val S_TAG = "S-TASK"
        private const val PACKET_SIZE = 128;
    }
}