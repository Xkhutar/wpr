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

class NetworkImposter(private val activity: AppCompatActivity, val context: Context, private val hostAddress: InetAddress, private val commonPort: Int) {
    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null

    private var transmission: TransmissionTask? = null
    private var transmissionThread: Thread? = null
    private var reception: ReceptionTask? = null
    private var receptionThread: Thread? = null

    public fun initiateConnection(isServer: Boolean)
    {
        if (isServer) {
            socket = Socket()
            socket!!.reuseAddress = true
            socket!!.bind(null)
            socket!!.connect(InetSocketAddress(hostAddress, 8989), 10000)
        } else {
            serverSocket = ServerSocket(8989)
            socket = serverSocket!!.accept()
        }

        transmission = TransmissionTask(context, hostAddress, commonPort, socket!!)
        transmissionThread = Thread(transmission)
        reception = ReceptionTask(commonPort, socket!!)
        receptionThread = Thread(reception)

        transmissionThread!!.priority = 10
        transmissionThread!!.start()
        Thread.sleep(1000)

        receptionThread!!.priority = 9
        receptionThread!!.start()
    }

    public fun setRecording(value: Boolean) {
        transmission!!.setRecording(value)
    }


    class ServerHandshake(private val port: Int, private val receptor: (InetAddress) -> Unit, private val onTerminate: (Boolean) -> Unit) : AsyncTask<Void?, Void?, String?>()
    {
        protected override fun doInBackground(vararg p0: Void?): String? {
            Log.d("HANDSHAKE","SERVER")
            val serverSocket = ServerSocket(port)
            val client = serverSocket.accept()
            receptor(client.inetAddress)
            serverSocket.close()
            return "YIPEE"
        }

        protected override fun onPostExecute(result: String?) {
            onTerminate(true)
        }
    }

    class ClientHandshake(private val port: Int, private val hostAddress: InetAddress, private val onTerminate: (Boolean) -> Unit) : AsyncTask<Void?, Void?, String?>() {
        protected override fun doInBackground(vararg p0: Void?): String? {
            Log.d("HANDSHAKE","CLIENT")

            while(true) {
                try{
                    Log.d(CL_TAG,"CTRY "+hostAddress);
                    val socket = Socket()
                    socket.reuseAddress = true
                    socket.bind(null)
                    socket.connect(InetSocketAddress(hostAddress, port), 10000)
                    socket.close()
                    break
                }
                catch (e: Exception)
                {
                    Log.d(CL_TAG,"CFAIl");
                    Thread.sleep(250);
                }

            }

            return "YIPEE"
        }

        protected override fun onPostExecute(result: String?) {
            onTerminate(false)
        }
    }


    class ReceptionTask(private val port: Int, private val socket: Socket) : Runnable {

        override fun run() {
            while (true) {
                Log.d(S_TAG, "Audio receiver started.")
                try {
                    val buffer = ByteArray(NetworkSus.PACKET_SIZE)
                    val inputStream = socket.getInputStream()
                    val audioPlayer = AudioTrack(
                        AudioManager.STREAM_SYSTEM,
                        8192,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        NetworkSus.PACKET_SIZE *10,
                        AudioTrack.MODE_STREAM
                    )

                    var bytesRead = 0

                    while (true) {
                        var numberBytes = inputStream.read(buffer, bytesRead, NetworkSus.PACKET_SIZE - bytesRead)
                        if (numberBytes > 0) {
                            if (bytesRead + numberBytes == NetworkSus.PACKET_SIZE) {
                                audioPlayer!!.write(buffer, 0, NetworkSus.PACKET_SIZE)
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
    }

    class TransmissionTask(private val context: Context, private val hostAddress: InetAddress, private val commonPort: Int, private val socket: Socket) : Runnable {
        @Volatile private var currentlyRecording: Boolean = false
        public fun setRecording(value: Boolean) {
            currentlyRecording = value
        }

        private var buffer: ByteArray = ByteArray(NetworkSus.PACKET_SIZE)

        override fun run() {
            Log.d(CL_TAG, "Attempting to begin transmission...")

            try {
                Log.d("", "Opening client socket - "+hostAddress.toString() +":"+commonPort)

                Log.d(CL_TAG, "Client socket - " + socket!!.isConnected)
                val outputStream = socket!!.getOutputStream()

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    return
                }

                val audioRecorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    8192,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    NetworkSus.PACKET_SIZE
                )

                audioRecorder!!.startRecording()

                try {
                    while (true) {
                        if (currentlyRecording) {
                            val amountToRead = NetworkSus.PACKET_SIZE
                            audioRecorder!!.read(buffer, 0, NetworkSus.PACKET_SIZE)
                            outputStream.write(buffer)
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
    }

    companion object {
        private const val CL_TAG = "CLTASK"
        private const val S_TAG = "S-TASK"
    }
}