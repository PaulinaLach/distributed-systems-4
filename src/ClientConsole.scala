import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Created by paulina on 5/20/17.
  */

class ClientConsole extends App {

  val config: Config = ConfigFactory.load("client.conf")
  val system = ActorSystem("client", config.getConfig("client").withFallback(config))

  val titleSearcher = system.actorOf(Props(classOf[TitleSearcher]), "titleSearcher")
  val titleOrderer = system.actorOf(Props(classOf[TitleOrderer]), "titleOrderer")
  val titleStreamer = system.actorOf(Props(classOf[TitleStreamer]), "titleStreamer")

  val titleSearcherExecutor = system.actorSelection("akka.tcp://server@172.0.0.1:3552/user/titleSearcherExecutor")
  val titleOrdererExecutor = system.actorSelection("akka.tcp://server@172.0.0.1:3552/user/titleOrdererExecutor")
  val titleStreamerExecutor = system.actorSelection("akka.tcp://server@172.0.0.1:3552/user/titleStreamerExecutor")

  while (true) {
    try {
      println("Options: ")
      println(" ")

      println("q - leave")
      println(" ")

      println("search title - search for a title")
      println(" ")

      println("order title - order a book")
      println(" ")

      println("stream title - stream a book")
      println(" ")

      var line = readLine()

      if (line.matches("q")) {
        System.exit(1)
      }
      else if (line.startsWith("search")) {
        var splittedLine = line.split(" ")
        var title = splittedLine(1)

        titleSearcherExecutor.tell(SearchTitle(title), titleSearcher)
      }
      else if (line.startsWith("order")) {
        var splittedLine = line.split(" ")
        var title = splittedLine(1)

        titleOrdererExecutor.tell(OrderTitle(title), titleOrderer)
      }
      else if (line.startsWith("stream")) {
        var splittedLine = line.split(" ")
        var title = splittedLine(1)

        titleStreamerExecutor.tell(StreamTitle(title), titleStreamer)
      }
    }
    catch {
      case e: Throwable => e.printStackTrace
    }
  }
}

class TitleSearcher extends Actor {
  override def receive: Receive = {
    case SearchedTitle(title, price) => println(title + ", " + price)
  }
}

class TitleOrderer extends Actor {
  override def receive: Receive = {
    case OrderedTitle(title) => println(title)
  }
}

class TitleStreamer extends Actor {
  override def receive: Receive = {
    case StreamedTitle(title) => println(title)
  }
}

case class SearchTitle(title: String)
case class OrderTitle(title: String)
case class StreamTitle(title: String)
