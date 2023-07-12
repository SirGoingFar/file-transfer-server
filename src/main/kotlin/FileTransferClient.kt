import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


internal class FileTransferClient(private val socketAddress: InetSocketAddress) {

    private lateinit var socket: Socket
    private var hostConnectionRetryCount = 0

    init {
        do {
            {
//                socket = Socket(socketAddress.address, socketAddress.port)
//                socket.co
                socket = Socket()
                socket.connect(socketAddress, 6000)
                hostConnectionRetryCount++
            }.multiCatch(
                UnknownHostException::class,
                ConnectException::class
            ) {
                hostConnectionRetryCount++
                println("ERROR - [File Transfer Client] Unable to connect to host [${socketAddress}]. [Reason: $it]")
                throw it
            }
        } while (hostConnectionRetryCount < 3)
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

}