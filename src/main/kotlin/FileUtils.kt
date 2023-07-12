import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileUtils {
    companion object {
        fun getSingleFileInputStream(content: String): InputStream {
            return File("single-zipped-file.zip").also {
                ZipOutputStream(FileOutputStream(it)).apply {
                    putNextEntry(ZipEntry("only-file-in-zip.txt"))
                    val updatedContent = "Single File: $content"
                    write(updatedContent.toByteArray(), 0, updatedContent.toByteArray().size)
                    closeEntry()
                    close()
                }
            }.inputStream()
        }

        fun getMultipleFilesInputStream(content: String, size: Int): InputStream {
            return File("multiple-zipped-files.zip").also {
                ZipOutputStream(FileOutputStream(it)).apply {
                    for (i in 1..size) {
                        val sb = StringBuilder("Zip File: $content $i")
                        val data = sb.toString().toByteArray()
                        putNextEntry(ZipEntry("zipped-file-$i.txt"))
                        write(data, 0, data.size)
                    }
                    closeEntry()
                    close()
                }
            }.inputStream()
        }
    }
}