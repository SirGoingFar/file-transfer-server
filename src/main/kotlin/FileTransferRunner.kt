import FileUtils.Companion.getMultipleFilesInputStream
import FileUtils.Companion.getSingleFileInputStream
import java.net.InetSocketAddress


fun main() {

    //Define connection points
    val socketAddress = InetSocketAddress("0.0.0.0", 12002)

    //Define peers: Start server
    val fileTransferServer =
        FileTransferServer(socketAddress, 10000, object : FileTransferServer.FileHandler {
            override fun handle(fileContent: String): Boolean {
                println("INFO - [File Consumer] Received content: $fileContent")
                return true
            }
        })

    Thread { fileTransferServer.start() }.start()

    //Define peers: Connect clients
    for (i in 1..10) {

        val fileTransferClient = FileTransferClient(socketAddress)

        //Exchange data
        if (i % 2 != 0) {
            fileTransferClient.send(getSingleFileInputStream("Content - $i"))
        } else {
            fileTransferClient.send(getMultipleFilesInputStream("Content", 2))
        }

        fileTransferClient.close()
    }

    //Shutdown
    fileTransferServer.shutdown()

}
