import FileUtils.Companion.getMultipleFilesInputStream
import FileUtils.Companion.getSingleFileInputStream
import java.net.InetSocketAddress
import java.net.SocketException
import kotlin.system.exitProcess


fun main() {

    //Define connection points
    val socketAddress = InetSocketAddress("127.0.0.1", 12002)

    //Define peers: Start server
    val fileTransferServer =
        FileTransferServer(socketAddress, 10_000, object : FileTransferServer.FileHandler {
            override fun handle(fileContent: String): Boolean {
                println("INFO - [File Consumer] Received content: $fileContent")
                return true
            }
        })

    Thread { fileTransferServer.start() }.start()

    //Define peers: Connect clients
    for (i in 1..10) {

        //Exchange data
        {
            val fileTransferClient = FileTransferClient(socketAddress)

            if (i % 2 != 0) {
                fileTransferClient.send(getSingleFileInputStream("Content - $i"))
            } else {
                fileTransferClient.send(getMultipleFilesInputStream("Content", 2))
            }

            fileTransferClient.close()
        }.multiCatch(SocketException::class) {
            println("ERROR - [File Consumer] Unable to send file: $it")
        }

    }

    //Shutdown
    fileTransferServer.shutdown()

    Thread.sleep(2000) //Wait for other processes to stop

    //Shut down application
    exitProcess(-1)

}
