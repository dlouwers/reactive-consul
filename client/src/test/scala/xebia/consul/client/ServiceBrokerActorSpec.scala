package xebia.consul.client

import akka.actor.{ ActorLogging, ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification
import xebia.consul.client.helpers.ModelHelpers
import xebia.consul.client.loadbalancers.{ LoadBalancerActor, LoadBalancer }
import xebia.consul.client.util.Logging

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class ServiceBrokerActorSpec extends Specification with Mockito with Logging {

  abstract class ActorScope extends TestKit(ActorSystem("TestSystem")) with specification.After with ImplicitSender {

    override def after: Any = TestKit.shutdownActorSystem(system)
    implicit val ec = system.dispatcher
    val httpClient = mock[CatalogHttpClient]
    val serviceVerifier = mock[Props => Unit]
    val connectionProviderFactory = mock[ConnectionProviderFactory]
    val connectionProvider = mock[ConnectionProvider]
    val connectionHolder = mock[ConnectionHolder]
    val connectionStrategy = ConnectionStrategy(connectionProviderFactory, self)
  }

  "The ServiceBrokerActor" should {
    "create a child actor per service" in new ActorScope {
      val sut = TestActorRef[ServiceBrokerActor](Props(new ServiceBrokerActor(Map("service1" -> connectionStrategy), httpClient) {
        override def createChild(props: Props) = {
          serviceVerifier(props)
          self
        }
      }), "ServiceBroker")
      there was one(serviceVerifier).apply(any[Props])
      sut.underlyingActor.loadbalancers must haveKey("service1")
    }

    "create a load balancer for each new service" in new ActorScope {
      val sut = TestActorRef[ServiceBrokerActor](Props(new ServiceBrokerActor(Map("service1" -> connectionStrategy), httpClient) {
        override def createChild(props: Props) = {
          serviceVerifier(props)
          self
        }
      }), "ServiceBroker")
      val service = ModelHelpers.createService("service1")
      connectionProviderFactory.create(service.serviceAddress, service.servicePort) returns connectionProvider
      sut ! ServiceAvailabilityActor.ServiceAvailabilityUpdate(added = Set(service), removed = Set.empty)
      there was one(connectionProviderFactory).create(service.serviceAddress, service.servicePort)
      expectMsg(LoadBalancerActor.AddConnectionProvider(service.serviceId, connectionProvider))
    }

    "request a connection from a loadbalancer" in new ActorScope {
      val sut = TestActorRef[ServiceBrokerActor](Props(new ServiceBrokerActor(Map("service1" -> connectionStrategy), httpClient) {
        override def createChild(props: Props) = {
          serviceVerifier(props)
          self
        }
      }), "ServiceBroker")
      val service = ModelHelpers.createService("service1")
      sut ! ServiceBrokerActor.GetServiceConnection(service.serviceName)
      expectMsg(LoadBalancerActor.GetConnection)
    }
  }
}
