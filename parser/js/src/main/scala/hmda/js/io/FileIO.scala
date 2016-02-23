package hmda.js.io

import scala.concurrent.{ Promise, Future }
import scala.scalajs.js
import scala.scalajs.js.Dynamic._

trait FileIO {
  val fs = global.require("fs")

  // Wrapper around nodejs fs.readFileSync
  // https://nodejs.org/api/fs.html#fs_fs_readfilesync_file_options
  def readFileSync(path: String): String = {
    fs.readFileSync(path, "UTF-8").asInstanceOf[String]
  }

  // Wrapper around nodejs fs.readFile
  // https://nodejs.org/api/fs.html#fs_fs_readfile_file_options_callback
  def readFile(path: String): Future[String] = {
    val promise: Promise[String] = Promise()
    fs.readFile(path, "UTF-8", { (err: js.Dynamic, data: String) =>
      if (err != null)
        promise.failure(new RuntimeException("could not read file"))
      else
        promise.success(data)
    })
    promise.future
  }
}
