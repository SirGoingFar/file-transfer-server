import FileUtils.Companion.getMultipleFilesInputStream
import FileUtils.Companion.getSingleFileInputStream
import org.junit.jupiter.api.assertThrows
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.SocketException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileTransferClientTest {

    @Test
    fun testConnectToHost_hostIsNotReachable_throwsConnectException() {
        val socketAddress = InetSocketAddress("127.0.0.1", 12002)
        assertThrows<ConnectException> { val fileTransferClient = FileTransferClient(socketAddress) }
    }

    @Test
    fun testSendData_validDataIsSent_validDataIsDelivered() {
        val testFileContent = "Here is the file content"
        val socketAddress = InetSocketAddress("127.0.0.1", 12002)
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

    @Test
    fun testSendData_clientAlreadyClosedConnection_throwsSocketException() {
        val testFileContent = "Here is the file content"
        val socketAddress = InetSocketAddress("127.0.0.1", 12002)
        val server = FileTransferServer(socketAddress, 10_000, object : FileTransferServer.FileHandler {
            override fun handle(fileContent: String): Boolean {
                return true
            }
        })
        startServer(server)

        val client = FileTransferClient(socketAddress)
        client.close()

        Thread.sleep(3000) //allowance for components whose shutdown may take time

        assertThrows<SocketException> { client.send(getMultipleFilesInputStream(testFileContent, 3)) }

        server.shutdown()
    }

    @Test
    fun testReconnect_clientAlreadyDisconnected_clientEventuallyReconnect() {
        val socketAddress = InetSocketAddress("127.0.0.1", 12002)
        val server = FileTransferServer(socketAddress, 10_000, object : FileTransferServer.FileHandler {
            override fun handle(fileContent: String): Boolean {
                return true
            }
        })
        startServer(server)

        val client = FileTransferClient(socketAddress)
        client.close()
        assertFalse { client.isConnected() }

        client.reconnect()
        assertTrue { client.isConnected() }

        server.shutdown()
    }

    @Test
    fun testReconnect_clientStillConnected_throwsSocketException() {
        val socketAddress = InetSocketAddress("127.0.0.1", 12002)
        val server = FileTransferServer(socketAddress, 10_000, object : FileTransferServer.FileHandler {
            override fun handle(fileContent: String): Boolean {
                return true
            }
        })
        startServer(server)

        val client = FileTransferClient(socketAddress)
        assertTrue { client.isConnected() }

        assertThrows<SocketException> { client.reconnect() }

        assertTrue { client.isConnected() }

        server.shutdown()
    }

    private fun startServer(server: FileTransferServer) {
        Thread { server.start() }.start()
    }

}