package xebia.consul.client

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification
import xebia.consul.client.util.Logging

class ServiceBrokerActorSpec extends Specification with Mockito with Logging {

  abstract class ActorScope extends TestKit(ActorSystem("TestSystem")) with specification.After with ImplicitSender {

    class ForwardingActor(next: ActorRef) extends Actor {
      def receive = {
        case msg => next ! msg
      }
    }

    override def after: Any = TestKit.shutdownActorSystem(system)
    val serviceAvailabilityActorFactory = (s: String, p: ActorRef) => Props(new ForwardingActor(self))
  }

  "The ServiceBrokerActor" should {
    "receive one service update when there are no changes" in new ActorScope {
      val sut = TestActorRef(ServiceBrokerActor.props(serviceAvailabilityActorFactory, Set("service1")))
    }
  }
}
