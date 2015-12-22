package acceptance.util

import java.io.{PrintWriter, File}

object Screencast {
  def storeId() = {
    val file: File = new File(Config.screencastIdFile)
    file.getParentFile().mkdirs()
    new PrintWriter(file) {
      write(Driver.sessionId)
      close
    }
  }
}
