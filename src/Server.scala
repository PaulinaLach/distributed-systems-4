import java.io.File
import java.io.PrintWriter

import akka.actor.{Actor, ActorContext, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import akka.pattern.ask
import com.typesafe.config.{Config, ConfigFactory}

import scala.io.Source
import scala.util.{Failure, Success}
import scala.concurrent.duration._


object Server extends App {
  val config: Config = ConfigFactory.load("server.conf")
  val system = ActorSystem("server", config.getConfig("server").withFallback(config))

  val synchroOrders = new SynchroOrders()
  val titleSearcherExecutor = system.actorOf(Props(classOf[TitleSearcherExecutor]), "titleSearcherExecutor")
  val titleOrdererExecutor = system.actorOf(Props(classOf[TitleOrdererExecutor], synchroOrders), "titleOrdererExecutor")
  val titleStreamerExecutor = system.actorOf(Props(classOf[TitleStreamerExecutor]), "titleStreamerExecutor")
}


case class SearchedTitle(title: String, price: String)

class TitleSearcherExecutor extends Actor {

  var titleFound = false
  implicit val timeout = Timeout(10 seconds)

  def receive = {
    case SearchTitle(title) =>
      val searchSender = sender
      sender ! searchTitle(context, title, sender)
  }

  def searchTitle(context: ActorContext, title: String, sender: ActorRef) = {
    import context.dispatcher

    val db1 = context.actorOf(Props(classOf[DbActor], "db1.txt"))
    val db2 = context.actorOf(Props(classOf[DbActor], "db2.txt"))

    val db1Future = db1 ? TSearch(title)
    val db2Future = db2 ? TSearch(title)

    db1Future.onComplete {
      case Success(result) => searchCompleted(sender, result)
      case Failure(result) => searchCompleted(sender, result)
    }

    db2Future.onComplete {
      case Success(result) => searchCompleted(sender, result)
      case Failure(result) => searchCompleted(sender, result)
    }
  }


  private def searchCompleted(sender: ActorRef, result: Any) = {
    val dbActorRes: DbActorRes = result.asInstanceOf[DbActorRes]
    //println(s"Input array: ${dbActorRes.lines.mkString(" and ")}")

//    titleFound.synchronized {
//      if (!titleFound) {
//        if (dbActorRes.lines.length > 0) {
//          titleFound = true
//        }
//        var title = String
//        var price = String
//        for (line <- dbActorRes.lines) {
//          println(s"Line $line")
//          val splitted = line.split(";")
//          title = title :+ splitted(0)
//          price = price :+ splitted(1)
//        }
//
//        sender ! SearchedTitle(title, price)
//      }
//    }
    titleFound.synchronized {
      if (!titleFound) {
        var title : String = ""
        var price : String = ""
        if (dbActorRes.line != null) {
          titleFound = true
          println(s"Line ${dbActorRes.line}")
          val splitted = dbActorRes.line.split(";")
          title = splitted(0)
          price = splitted(1)
        }
        else {
          title = "Title not found."
          price = "None"
        }
        sender ! SearchedTitle(title, price)
      }
    }
  }
}

case class TSearch(title: String)


case class DbActorRes(line: String)

class DbActor(file: String) extends Actor  {
  override def receive: Receive = {
    case TSearch(title) =>
      val lines = Source.fromFile(file).getLines
      sender ! DbActorRes(lines.find((s: String) => s.contains(title)).get)
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
//      val result = stream(title)
//      sender ! StreamedTitle(result)
  }

}



case class OrderedTitle(title: String)
case class StreamedTitle(title: String)