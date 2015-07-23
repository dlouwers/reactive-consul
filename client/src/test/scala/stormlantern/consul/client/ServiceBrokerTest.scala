package stormlantern.consul.client

import akka.actor.ActorSystem
import akka.actor.Status.Failure
import akka.testkit.{ ImplicitSender, TestKit }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification
import stormlantern.consul.client.dao.ConsulHttpClient
import stormlantern.consul.client.discovery.ConnectionHolder
import stormlantern.consul.client.loadbalancers.LoadBalancerActor
import stormlantern.consul.client.util.Logging

import scala.concurrent.Future

class ServiceBrokerTest extends Specification with Mockito with Logging {

  abstract class ActorScope extends TestKit(ActorSystem("TestSystem")) with specification.After with ImplicitSender {

    override def after: Any = TestKit.shutdownActorSystem(system)
    implicit val ec = system.dispatcher
    val connectionHolder = mock[ConnectionHolder]
    val httpClient = mock[ConsulHttpClient]
    val loadBalancer = self
  }

  "The ServiceBroker" should {

    "return a service connection when requested" in new ActorScope {
      connectionHolder.connection returns Future.successful(true)
      connectionHolder.loadBalancer returns loadBalancer
      val sut = new ServiceBroker(self, httpClient)
      val result = sut.withService("service1") { service: Boolean =>
        Future.successful(service)
      }
      expectMsgPF() {
        case ServiceBrokerActor.GetServiceConnection("service1") =>
          lastSender ! connectionHolder
          result.map(_ shouldEqual true).await
      }
      expectMsg(LoadBalancerActor.ReturnConnection(connectionHolder))
    }

    "return the connection when an error occurs" in new ActorScope {
      connectionHolder.connection returns Future.successful(true)
      connectionHolder.loadBalancer returns loadBalancer
      val sut = new ServiceBroker(self, httpClient)
      val result = sut.withService[Boolean, Boolean]("service1") { service: Boolean =>
        throw new RuntimeException()
      }
      expectMsgPF() {
        case ServiceBrokerActor.GetServiceConnection("service1") =>
          lastSender ! connectionHolder
          result.await should throwA[RuntimeException]
      }
      expectMsg(LoadBalancerActor.ReturnConnection(connectionHolder))
    }

    "throw an error when an excpetion is returned" in new ActorScope {
      val sut = new ServiceBroker(self, httpClient)
      val result = sut.withService("service1") { service: Boolean =>
        Future.successful(service)
      }
      expectMsgPF() {
        case ServiceBrokerActor.GetServiceConnection("service1") =>
          lastSender ! Failure(new RuntimeException())
          result.await should throwA[RuntimeException]
      }
    }
  }
}
