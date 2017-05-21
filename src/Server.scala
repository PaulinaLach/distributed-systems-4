import java.io.{File, FileWriter, PrintWriter}

import akka.NotUsed
import akka.actor.{Actor, ActorContext, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import akka.pattern.ask
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy, ThrottleMode}
import com.typesafe.config.{Config, ConfigFactory}

import scala.io.Source.fromFile
import scala.util.{Failure, Success}
import scala.concurrent.duration._


object Server extends App {
  val config: Config = ConfigFactory.load("resources/server.conf")
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
      titleFound = false
      val searchSender = sender
      sender ! searchTitle(context, title, sender)
  }

  def searchTitle(context: ActorContext, title: String, sender: ActorRef) = {
    import context.dispatcher

    val db1 = context.actorOf(Props(classOf[DbActor], "src/data/db1.txt"))
    val db2 = context.actorOf(Props(classOf[DbActor], "src/data/db2.txt"))

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
        if (dbActorRes.line != null) {
          titleFound = true
          val splitted = dbActorRes.line.split(" ")
          val title = splitted(0)
          val price = splitted(1)
          sender ! SearchedTitle(title, price)
        }
      }
    }
  }
}

case class TSearch(title: String)


case class DbActorRes(line: String)

class DbActor(file: String) extends Actor  {
  override def receive: Receive = {
    case TSearch(title) =>
      val record = fromFile(file).getLines.find((s: String) => s.contains(title))
      if (record.isEmpty) {
        var title = "TitleNoFound"
        var price = "None"
        sender ! DbActorRes(title + " " + price)
      } else {
        sender ! DbActorRes(record.get)
      }

  }
}






case class OrderedTitle(title: String)

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
      val output = new FileWriter("src/data/orders.txt", true)
      try {
        output.write(title + "\n")
      }
      finally output.close()
    }
  }
}




case class StreamedTitle(title: String)

class TitleStreamerExecutor extends Actor {
  def receive = {

    case StreamTitle(title) =>
      val materializer = ActorMaterializer.create(context)
      val sink = Source.actorRef(1000, OverflowStrategy.dropNew)
        .throttle(1, 1 second, 1, ThrottleMode.shaping)
        .to(Sink.actorRef(sender, NotUsed))
        .run()(materializer)
      try {
        val lines = fromFile("src/data/" + title + ".txt").getLines
        lines.foreach((line: String) => sink ! StreamedTitle(line))
      }
      catch {
        case e: Exception => sink ! StreamedTitle("Title not found.")
      }
  }
}




