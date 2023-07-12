import java.io.IOException
import java.io.InputStream
import java.net.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


internal class FileTransferClient(private val socketAddress: InetSocketAddress) {

    private lateinit var socket: Socket
    private var hostConnectionRetryCount = 0

    init {
        do {
            {
                socket = Socket().apply {
                    connect(socketAddress, 5000)
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

    fun send(inputStream: InputStream): Boolean {
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

    fun isConnected() = socket.isConnected

    companion object {
        private const val MAX_HOST_RETRY_COUNT = 3
    }

}