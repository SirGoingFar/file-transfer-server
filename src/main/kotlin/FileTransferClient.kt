import java.io.IOException
import java.io.InputStream
import java.net.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


internal class FileTransferClient(
    private val socketAddress: InetSocketAddress,
    private val connectionTimeout: Int = 5000
) {

    private lateinit var socket: Socket
    private var hostConnectionRetryCount = 0

    init {
        do {
            createSocket {
                hostConnectionRetryCount++
                println("ERROR - [File Transfer Client] Unable to connect to host [${socketAddress}]. [Reason: $it]")
                if (hostConnectionRetryCount >= MAX_HOST_RETRY_COUNT) {
                    throw it
                } else {
                    Thread.sleep(2000) //wait a bit before retrying
                }
            }
        } while (hostConnectionRetryCount < MAX_HOST_RETRY_COUNT)
    }

    private fun createSocket(onError: (Throwable) -> Unit) {
        {
            socket = Socket().apply {
                connect(socketAddress, connectionTimeout)
            }
            if (socket.isConnected) {
                println("INFO - [File Transfer Client] Connected successfully to $socketAddress")
            }
            hostConnectionRetryCount++
        }.multiCatch(
            UnknownHostException::class,
            ConnectException::class,
            SocketTimeoutException::class
        ) {
            onError(it)
        }
    }

    fun send(inputStream: InputStream): Boolean {
        if (!isConnected()) {
            throw SocketException("Socket is closed");
        }

        return try {
            val zipInputStream = ZipInputStream(inputStream)
            var entry: ZipEntry?
            while (zipInputStream.nextEntry.also { entry = it } != null) {
                entry?.let {
                    val output = socket.getOutputStream()
                    output.write(zipInputStream.readAllBytes())
                    output.flush()
                    zipInputStream.closeEntry()
                    println("INFO - [File Transfer Client] Content sent")
                }
            }
            true
        } catch (ioException: IOException) {
            println("ERROR - [File Transfer Client] Unable to send file. [Reason: $ioException]")
            false
        }
    }

    fun close() {
        try {
            socket.use { it.close() }
        } catch (ioException: IOException) {
            println("ERROR - [File Transfer Client] Unable to close connection to host [${socketAddress}]. [Reason: $ioException]")
        }
    }

    fun reconnect() {
        if (!isConnected()) {
            createSocket {
                println("ERROR - [File Transfer Client] Unable to reconnect to host [${socketAddress}]. [Reason: $it]")
                throw it
            }
        } else {
            throw Exception("Client already connected")
        }
    }

    fun isConnected() = socket.isBound && !socket.isConnected

    companion object {
        private const val MAX_HOST_RETRY_COUNT = 3
    }

}