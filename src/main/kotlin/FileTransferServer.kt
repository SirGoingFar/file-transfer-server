import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger


internal class FileTransferServer(
    private val socketAddress: InetSocketAddress,
    private val connectionTimeout: Int = 60000,
    private val fileHandler: FileHandler
) {
    private val serverSocket: ServerSocket
    private val numberOfOngoingConnection = AtomicInteger()
    private var canReceiveNewClientConnection = true

    init {
        try {
            serverSocket = ServerSocket().apply {
                setPerformancePreferences(0, 1, 1)
                reuseAddress = true
                soTimeout = connectionTimeout
                bind(socketAddress, 20)
            }
            println("INFO - [File Transfer Server] Listening on port [${socketAddress.port}] of host [${socketAddress.address}] and ready for connection.")
        } catch (ioException: IOException) {
            throw RuntimeException("ERROR - [File Transfer Server] Unable to provision server", ioException)
        }
    }

    fun start() {
        serverSocket.use { serverSocket ->

            while (canReceiveNewClientConnection) {
                try {
                    val socket = serverSocket.accept()
                    Thread {
                        socket.use {
                            if (isReadyForConnection()) {
                                numberOfOngoingConnection.incrementAndGet()
                                println("INFO - [File Transfer Server] New client connected.")

                                val reader = BufferedReader(InputStreamReader(it.getInputStream()))
                                val content = reader.readLine()

                                content?.let { c ->
                                    val isHandled = fileHandler.handle(c)
                                    if (!isHandled) {
                                        println("ERROR - [File Transfer Server] Received content not handled")
                                    }
                                }
                                numberOfOngoingConnection.decrementAndGet()
                            }
                        }
                    }.start()
                } catch (socketTimeoutException: SocketTimeoutException) {
                    println("INFO - [File Transfer Server] No client connection detected. [Reason: $socketTimeoutException]")
                } catch (ioException: IOException) {
                    println("ERROR - [File Transfer Server] Something went wrong. [Reason: $ioException]")
                }
            }

        }
    }

    fun shutdown() {
        try {
            serverSocket.close()
            canReceiveNewClientConnection = !serverSocket.isClosed
        } catch (ioException: IOException) {
            println("ERROR - [File Transfer Server] Unable to shutdown the server. [Reason: $ioException]")
        }
    }

    fun isReadyForConnection() = serverSocket.isBound && !serverSocket.isClosed

    fun isUnavailableForConnection() = !canReceiveNewClientConnection || serverSocket.isClosed

    fun hasConnectedClient() = getNumberOfActiveConnections() > 0

    fun getNumberOfActiveConnections() = numberOfOngoingConnection.get()

    interface FileHandler {
        fun handle(fileContent: String): Boolean
    }

}