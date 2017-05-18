import akka.actor.{ ActorRef, ActorSystem, Props, Actor, Inbox }
import scala.concurrent.duration._

object TitleSearcher {

  def props(title: String):
    Props = Props(new TitleSearcher(title))

}

class TitleSearcher(title: String) extends Actor {

  def receive = {
    case GiveTitleToSearch(title) => TitleSearcherExecutor ! searchTitle(title)
  }
}


// A simple actor that prints whatever it receives
class PrintActor extends Actor {
  def receive = {
    case x ⇒ println(x)
  }
}

val printer = system.actorOf(Props[PrintActor])
// The throttler for this example, setting the rate
val throttler = system.actorOf(Props(
  classOf[TimerBasedThrottler],
  3 msgsPer 1.second))
// Set the target
throttler ! SetTarget(Some(printer))
// These three messages will be sent to the target immediately
throttler ! "1"
throttler ! "2"
throttler ! "3"
// These two will wait until a second has passed
throttler ! "4"
throttler ! "5"



//object Library extends App {
//
//  val system = ActorSystem("library")
//
//  val greeter = system.actorOf(Props[Greeter], "greeter")
//
//  // Create an "actor-in-a-box"
//  val inbox = Inbox.create(system)
//
//  // Tell the 'greeter' to change its 'greeting' message
//  greeter.tell(WhoToGreet("akka"), ActorRef.noSender)
//
//  // Ask the 'greeter for the latest 'greeting'
//  // Reply should go to the "actor-in-a-box"
//  inbox.send(greeter, Greet)
//
//  // Wait 5 seconds for the reply with the 'greeting' message
//  val Greeting(message1) = inbox.receive(5.seconds)
//  println(s"Greeting: $message1")
//
//  // Change the greeting and ask for it again
//  greeter.tell(WhoToGreet("typesafe"), ActorRef.noSender)
//  inbox.send(greeter, Greet)
//  val Greeting(message2) = inbox.receive(5.seconds)
//  println(s"Greeting: $message2")
//
//  val greetPrinter = system.actorOf(Props[GreetPrinter])
//  // after zero seconds, send a Greet message every second to the greeter with a sender of the greetPrinter
//  system.scheduler.schedule(0.seconds, 1.second, greeter, Greet)(system.dispatcher, greetPrinter)
//
//}
//}