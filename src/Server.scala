import java.io.File
import java.io.PrintWriter

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}

import scala.io.Source


class Server extends App {
  val config: Config = ConfigFactory.load("server.conf")
  val system = ActorSystem("server", config.getConfig("server").withFallback(config))

  val synchroOrders = new SynchroOrders()
  val titleSearcherExecutor = system.actorOf(Props(classOf[TitleSearcherExecutor]), "titleSearcherExecutor")
  val titleOrdererExecutor = system.actorOf(Props(classOf[TitleOrdererExecutor], synchroOrders), "titleOrdererExecutor")
  val titleStreamerExecutor = system.actorOf(Props(classOf[TitleStreamerExecutor]), "titleStreamerExecutor")
}


class TitleSearcherExecutor extends Actor {
  def receive = {
    case SearchTitle(title) =>
      val result = searchTitle(title)
      sender ! SearchedTitle(result)
  }
}

def searchTitle(title: String): String = {
  val db1 = io.Source.fromFile((new java.io.File(".").getCanonicalPath) + "/data/db1.txt").getLines() //TODO: fix path
  val db2 = io.Source.fromFile((new java.io.File(".").getCanonicalPath) + "/data/db2.txt").getLines()
  var line = db1.filter(_ contains title) // TODO: moze zawierac inny krotszy tutl -> BUG
  try {
    if (!(line)) {
      line = db2.filter(_ contains title)
    }
    return line.split(" ").lastOption
  }
  catch Exception NotFound{
    return "Title not found."
  }

}




class TitleOrdererExecutor(synchroOrders: SynchroOrders) extends Actor {
  def receive = {
    case OrderTitle(title) =>
      synchroOrders.orderTitle(title)
      val result = "Book ordered for client " + sender
      sender ! OrderedTitle(result)

  }
}

class SynchroOrders {
  def orderTitle(title: String) = {
    this.synchronized {
      val output = new PrintWriter(new File((new java.io.File(".").getCanonicalPath) + "/data/orders.txt")) //TODO: fix path
      output.write(title)
      output.close()
    }
  }
}




class TitleStreamerExecutor extends Actor {
  def receive = {
    // TODO: dodac obsluge wyjatku not found
    case StreamTitle(title) =>
      val source = io.Source.fromFile((new java.io.File(".").getCanonicalPath) + "/data/lorem.txt").getLines()
      for (line <- source) {
        sender ! line
        // TODO: sleep dodac
      }
      val result = stream(title)
      sender ! StreamedTitle(result)
  }

}


case class SearchedTitle(title: String)
case class OrderedTitle(title: String)
case class StreamedTitle(title: String)