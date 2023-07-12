import FileUtils.Companion.getSingleFileInputStream
import org.junit.jupiter.api.assertThrows
import java.net.ConnectException
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertTrue

class FileTransferServerTest {

    @Test
    fun testServerProvisioning_connectToValidSocketAddress_serverIsReadyForConnection() {
        val socketAddress = InetSocketAddress("0.0.0.0", 12002)
        val fileTransferServer = FileTransferServer(socketAddress, 10_000, object : FileTransferServer.FileHandler {
            override fun handle(fileContent: String): Boolean {
                return true
            }
        })
        assertTrue { fileTransferServer.isReadyForConnection() }
    }

    @Test
    fun testServerConnection_clientAttemptConnection_connectionIsSuccessful() {
        val socketAddress = InetSocketAddress("0.0.0.0", 12002)
        val server = FileTransferServer(socketAddress, 10_000, object : FileTransferServer.FileHandler {
            override fun handle(fileContent: String): Boolean {
                return true
            }
        })
        startServer(server)

        val client = FileTransferClient(socketAddress)
        Thread.sleep(3000) //Added to allow the client to connect to the server
        assertTrue { server.hasConnectedClient() }
        client.close()

        server.shutdown()
    }

    @Test
    fun testServerConnection_clientAttemptsConnectionWhenServerAlreadyShutdown_throwsConnectException() {
        val socketAddress = InetSocketAddress("0.0.0.0", 12002)
        val server = FileTransferServer(socketAddress, 10_000, object : FileTransferServer.FileHandler {
            override fun handle(fileContent: String): Boolean {
                return true
            }
        })
        startServer(server)
        Thread.sleep(3000) //wait a bit to spin up server
        server.shutdown()

        assertThrows<ConnectException> { FileTransferClient(socketAddress) }
    }

    @Test
    fun testServerConnection_clientSendsData_correctDataIsDelivered() {
        val testFileContent = "Here is the file content"
        val socketAddress = InetSocketAddress("0.0.0.0", 12002)
        val server = FileTransferServer(socketAddress, 10_000, object : FileTransferServer.FileHandler {
            override fun handle(fileContent: String): Boolean {
                assertTrue { fileContent.contains(testFileContent) }
                return true
            }
        })
        startServer(server)

        val client = FileTransferClient(socketAddress)
        client.send(getSingleFileInputStream(testFileContent))
        client.close()

        Thread.sleep(3000)//wait for data to be dispatched before shutting down server
        server.shutdown()
    }

    private fun startServer(server: FileTransferServer) {
        Thread { server.start() }.start()
    }

}