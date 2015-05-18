package xebia.consul.client

import akka.actor.Status.Failure
import akka.actor._
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification
import xebia.consul.client.helpers.ModelHelpers
import xebia.consul.client.loadbalancers.LoadBalancerActor
import xebia.consul.client.util.Logging

class ServiceBrokerActorSpec extends Specification with Mockito with Logging {

  abstract class ActorScope extends TestKit(ActorSystem("TestSystem")) with specification.After with ImplicitSender {

    override def after: Any = TestKit.shutdownActorSystem(system)
    implicit val ec = system.dispatcher
    val httpClient = mock[CatalogHttpClient]
    val serviceAvailabilityActorFactory = mock[(ActorRefFactory, String, ActorRef) => ActorRef]
    val connectionProviderFactory = mock[ConnectionProviderFactory]
    val connectionProvider = mock[ConnectionProvider]
    val connectionHolder = mock[ConnectionHolder]
    val connectionStrategy = ConnectionStrategy(connectionProviderFactory, ctx => self)
  }

  "The ServiceBrokerActor" should {

    "create a child actor per service" in new ActorScope {
      serviceAvailabilityActorFactory.apply(any[ActorRefFactory], be("service1"), any[ActorRef]) returns self
      val sut = TestActorRef[ServiceBrokerActor](ServiceBrokerActor.props(Map("service1" -> connectionStrategy), serviceAvailabilityActorFactory), "ServiceBroker")
      there was one(serviceAvailabilityActorFactory).apply(sut.underlyingActor.context, "service1", sut)
      sut.underlyingActor.loadbalancers must haveKey("service1")
    }

    "create a load balancer for each new service" in new ActorScope {
      serviceAvailabilityActorFactory.apply(any[ActorRefFactory], be("service1"), any[ActorRef]) returns self
      val sut = TestActorRef[ServiceBrokerActor](ServiceBrokerActor.props(Map("service1" -> connectionStrategy), serviceAvailabilityActorFactory), "ServiceBroker")
      there was one(serviceAvailabilityActorFactory).apply(sut.underlyingActor.context, "service1", sut)
      val service = ModelHelpers.createService("service1")
      connectionProviderFactory.create(service.serviceAddress, service.servicePort) returns connectionProvider
      sut ! ServiceAvailabilityActor.ServiceAvailabilityUpdate(added = Set(service), removed = Set.empty)
      there was one(connectionProviderFactory).create(service.serviceAddress, service.servicePort)
      expectMsg(LoadBalancerActor.AddConnectionProvider(service.serviceId, connectionProvider))
    }

    "request a connection from a loadbalancer" in new ActorScope {
      serviceAvailabilityActorFactory.apply(any[ActorRefFactory], be("service1"), any[ActorRef]) returns self
      val sut = TestActorRef[ServiceBrokerActor](ServiceBrokerActor.props(Map("service1" -> connectionStrategy), serviceAvailabilityActorFactory), "ServiceBroker")
      there was one(serviceAvailabilityActorFactory).apply(sut.underlyingActor.context, "service1", sut)
      val service = ModelHelpers.createService("service1")
      sut ! ServiceBrokerActor.GetServiceConnection(service.serviceName)
      expectMsg(LoadBalancerActor.GetConnection)
    }

    "return a failure if a service name cannot be found" in new ActorScope {
      val sut = TestActorRef[ServiceBrokerActor](ServiceBrokerActor.props(Map.empty, serviceAvailabilityActorFactory), "ServiceBroker")
      val service = ModelHelpers.createService("service1")
      sut ! ServiceBrokerActor.GetServiceConnection(service.serviceName)
      expectMsg(Failure(ServiceUnavailableException(service.serviceName)))
    }

    "forward a query for connection provider availability" in new ActorScope {
      serviceAvailabilityActorFactory.apply(any[ActorRefFactory], be("service1"), any[ActorRef]) returns self
      val sut = TestActorRef[ServiceBrokerActor](ServiceBrokerActor.props(Map("service1" -> connectionStrategy), serviceAvailabilityActorFactory), "ServiceBroker")
      there was one(serviceAvailabilityActorFactory).apply(sut.underlyingActor.context, "service1", sut)
      sut ! ServiceBrokerActor.HasAvailableConnectionProviderFor("service1")
      expectMsg(LoadBalancerActor.HasAvailableConnectionProvider)
    }

    "return false when every service doesn't have at least one connection provider avaiable" in new ActorScope {
      serviceAvailabilityActorFactory.apply(any[ActorRefFactory], be("service1"), any[ActorRef]) returns self
      serviceAvailabilityActorFactory.apply(any[ActorRefFactory], be("service2"), any[ActorRef]) returns self
      val sut = TestActorRef[ServiceBrokerActor](ServiceBrokerActor.props(Map("service1" -> connectionStrategy, "service2" -> connectionStrategy), serviceAvailabilityActorFactory), "ServiceBroker")
      there was one(serviceAvailabilityActorFactory).apply(sut.underlyingActor.context, "service1", sut)
      there was one(serviceAvailabilityActorFactory).apply(sut.underlyingActor.context, "service2", sut)
      sut ! ServiceBrokerActor.AllConnectionProvidersAvailable
      expectMsgPF() {
        case LoadBalancerActor.HasAvailableConnectionProvider => lastSender ! true
      }
      expectMsgPF() {
        case LoadBalancerActor.HasAvailableConnectionProvider => lastSender ! false
      }
      expectMsg(false)
    }

    "return true when every service has at least one connection provider avaiable" in new ActorScope {
      serviceAvailabilityActorFactory.apply(any[ActorRefFactory], be("service1"), any[ActorRef]) returns self
      serviceAvailabilityActorFactory.apply(any[ActorRefFactory], be("service2"), any[ActorRef]) returns self
      val sut = TestActorRef[ServiceBrokerActor](ServiceBrokerActor.props(Map("service1" -> connectionStrategy, "service2" -> connectionStrategy), serviceAvailabilityActorFactory), "ServiceBroker")
      there was one(serviceAvailabilityActorFactory).apply(sut.underlyingActor.context, "service1", sut)
      there was one(serviceAvailabilityActorFactory).apply(sut.underlyingActor.context, "service2", sut)
      sut ! ServiceBrokerActor.AllConnectionProvidersAvailable
      expectMsgPF() {
        case LoadBalancerActor.HasAvailableConnectionProvider => lastSender ! true
      }
      expectMsgPF() {
        case LoadBalancerActor.HasAvailableConnectionProvider => lastSender ! true
      }
      expectMsg(true)
    }
  }
}
