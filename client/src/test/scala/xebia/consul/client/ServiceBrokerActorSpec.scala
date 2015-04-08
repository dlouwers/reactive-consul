package xebia.consul.client

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification
import xebia.consul.client.util.Logging

class ServiceBrokerActorSpec extends Specification with Mockito with Logging {

  abstract class ActorScope extends TestKit(ActorSystem("TestSystem")) with specification.After with ImplicitSender {

    override def after: Any = TestKit.shutdownActorSystem(system)
    val httpClient = mock[CatalogHttpClient]
    val serviceVerifier = mock[Props => Unit]
    val connectionProviderFactory = mock[ConnectionProviderFactory]
    val loadBalancer = mock[LoadBalancer]
    val connectionStrategy = ConnectionStrategy(connectionProviderFactory, loadBalancer)
  }

  "The ServiceBrokerActor" should {
    "create a child actor per service" in new ActorScope {
      val sut = TestActorRef(Props(new ServiceBrokerActor(Map("service1" -> connectionStrategy), httpClient) {
        override def createChild(props: Props) = {
          serviceVerifier(props)
          self
        }
      }))
      there were one(serviceVerifier).apply(any[Props])
    }
  }
}
