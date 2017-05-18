import akka.stream._
import akka.stream.scaladsl._

import java.io.File
import java.io.PrintWriter

import scala.io.Source


class TitleSearcherExecutor extends Actor {

  def receive = {
    case searchTitle(title) =>
      val result = searchTitle(title)
      sender ! result

  }
}

def searchTitle(title: String): String = {
  val db1 = io.Source.fromFile((new java.io.File(".").getCanonicalPath) + "/data/db1.txt").getLines()
  val db2 = io.Source.fromFile((new java.io.File(".").getCanonicalPath) + "/data/db2.txt").getLines()
  var line = db1.filter(_ contains title) // moze zawierac inny krotszy tutl -> BUG
  try {
    if (! line) {
      line = db2.filter(_ contains title)
    }
    return line.split(" ").lastOption
  }
  catch Exception NotFound{
    return "Title not found."
  }

}

class OrderExecutor extends Actor {

  def receive = {
    case order(title) =>
      orderBook(title)
      val result = "Book ordered for client " + sender
      sender ! result

  }
}

def order(title: String): String = {
  val output = new PrintWriter(new File((new java.io.File(".").getCanonicalPath) + "/data/orders.txt"))
  output.write(title)
  output.close()

}

class StreamTextExecutor extends Actor {

  def receive = {
    // TODO: dodac obsluge wyjatku not found
    case stream(title) =>
      val source = io.Source.fromFile((new java.io.File(".").getCanonicalPath) + "/data/lorem.txt").getLines()
      for (line <- source.getLines()) {
        sender ! line
        // TODO: sleep dodac
      }
      val result = stream(title)
      sender ! result
  }

}
