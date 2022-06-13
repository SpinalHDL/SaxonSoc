package surabi.tools

import spinal.core._
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

object Tools {

  def readmemh(path: String): Array[BigInt] = {
    val buffer = new ArrayBuffer[BigInt]
    for (line <- Source.fromFile(path).getLines) {
      val tokens: Array[String] = line.split("(//)").map(_.trim)
      if (tokens.length > 0 && tokens(0) != "") {
        val i = Integer.parseInt(tokens(0), 16)
        buffer.append(i)
      }
    }
    //println(buffer.toString())
    buffer.toArray
  }
}