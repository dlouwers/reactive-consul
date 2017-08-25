package stormlantern.consul.client

import akka.actor.Status.Failure
import akka.actor._
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, Matchers }
import stormlantern.consul.client.dao.{ ConsulHttpClient, ServiceInstance }
import stormlantern.consul.client.discovery.ServiceAvailabilityActor.Start
import stormlantern.consul.client.discovery._
import stormlantern.consul.client.helpers.{ CallingThreadExecutionContext, ModelHelpers }
import stormlantern.consul.client.loadbalancers.LoadBalancerActor
import stormlantern.consul.client.util.Logging

class ServiceBrokerActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with FlatSpecLike
    with Matchers with BeforeAndAfterAll with MockFactory with Logging {

  implicit val ec = CallingThreadExecutionContext()
  def this() = this(ActorSystem("ServiceBrokerActorSpec"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  trait TestScope {
    val httpClient: ConsulHttpClient = mock[ConsulHttpClient]
    val serviceAvailabilityActorFactory: (ActorRefFactory, ServiceDefinition, ActorRef) ⇒ ActorRef = mock[(ActorRefFactory, ServiceDefinition, ActorRef) ⇒ ActorRef]
    val connectionProviderFactory: ConnectionProviderFactory = mock[ConnectionProviderFactory]
    val connectionProvider: ConnectionProvider = mock[ConnectionProvider]
    val connectionHolder: ConnectionHolder = mock[ConnectionHolder]
    val service1 = ServiceDefinition("service1Id", "service1")
    val service2 = ServiceDefinition("service2Key", "service2")
    val connectionStrategyForService1 = ConnectionStrategy(service1, connectionProviderFactory, ctx ⇒ self)
    val connectionStrategyForService2 = ConnectionStrategy(service2, connectionProviderFactory, ctx ⇒ self)
  }

  "The ServiceBrokerActor" should "create a child actor per service" in new TestScope {
    (serviceAvailabilityActorFactory.apply _).expects(*, service1, *).returns(self)
    val sut: TestActorRef[ServiceBrokerActor] = TestActorRef[ServiceBrokerActor](ServiceBrokerActor.props(Set(connectionStrategyForService1), serviceAvailabilityActorFactory), "ServiceBroker")
    sut.underlyingActor.loadbalancers.keys should contain(service1.serviceId)
    expectMsg(Start)
    sut.stop()
  }

  it should "create a load balancer for each new service" in new TestScope {
    (serviceAvailabilityActorFactory.apply _).expects(*, service1, *).returns(self)
    val sut: TestActorRef[ServiceBrokerActor] = TestActorRef[ServiceBrokerActor](ServiceBrokerActor.props(Set(connectionStrategyForService1), serviceAvailabilityActorFactory), "ServiceBroker")
    val service: ServiceInstance = ModelHelpers.createService(service1)
    (connectionProviderFactory.create _).expects(service.serviceAddress, service.servicePort).returns(connectionProvider)
    sut ! ServiceAvailabilityActor.ServiceAvailabilityUpdate(added = Set(service), removed = Set.empty)
    expectMsg(Start)
    expectMsg(LoadBalancerActor.AddConnectionProvider(service.serviceId, connectionProvider))
    sut.stop()
  }

  it should "request a connection from a loadbalancer" in new TestScope {
    (serviceAvailabilityActorFactory.apply _).expects(*, service1, *).returns(self)
    val sut: TestActorRef[ServiceBrokerActor] = TestActorRef[ServiceBrokerActor](ServiceBrokerActor.props(Set(connectionStrategyForService1), serviceAvailabilityActorFactory), "ServiceBroker")
    expectMsg(Start)
    val service: ServiceInstance = ModelHelpers.createService(service1)
    sut ! ServiceBrokerActor.GetServiceConnection(service.serviceId)
    expectMsg(LoadBalancerActor.GetConnection)
    sut.stop()
  }

  it should "return a failure if a service name cannot be found" in new TestScope {
    val sut: TestActorRef[ServiceBrokerActor] = TestActorRef[ServiceBrokerActor](ServiceBrokerActor.props(Set.empty, serviceAvailabilityActorFactory), "ServiceBroker")
    val service: ServiceInstance = ModelHelpers.createService(service1)
    sut ! ServiceBrokerActor.GetServiceConnection(service.serviceId)
    expectMsg(Failure(ServiceUnavailableException(service.serviceId)))
    sut.stop()
  }

  it should "forward a query for connection provider availability" in new TestScope {
    (serviceAvailabilityActorFactory.apply _).expects(*, service1, *).returns(self)
    val sut: TestActorRef[ServiceBrokerActor] = TestActorRef[ServiceBrokerActor](ServiceBrokerActor.props(Set(connectionStrategyForService1), serviceAvailabilityActorFactory), "ServiceBroker")
    expectMsg(Start)
    sut ! ServiceBrokerActor.HasAvailableConnectionProviderFor(service1.serviceId)
    expectMsg(LoadBalancerActor.HasAvailableConnectionProvider)
    sut.stop()
  }

  it should "return false when every service doesn't have at least one connection provider avaiable" in new TestScope {
    (serviceAvailabilityActorFactory.apply _).expects(*, service1, *).returns(self)
    (serviceAvailabilityActorFactory.apply _).expects(*, service2, *).returns(self)
    val sut: TestActorRef[ServiceBrokerActor] = TestActorRef[ServiceBrokerActor](ServiceBrokerActor.props(Set(connectionStrategyForService1, connectionStrategyForService2), serviceAvailabilityActorFactory), "ServiceBroker")
    expectMsgAllOf(Start, Start)
    sut ! ServiceBrokerActor.AllConnectionProvidersAvailable
    expectMsgPF() {
      case LoadBalancerActor.HasAvailableConnectionProvider ⇒ lastSender ! true
    }
    expectMsgPF() {
      case LoadBalancerActor.HasAvailableConnectionProvider ⇒ lastSender ! false
    }
    expectMsg(false)
    sut.stop()
  }

  it should "return true when every service has at least one connection provider avaiable" in new TestScope {
    (serviceAvailabilityActorFactory.apply _).expects(*, service1, *).returns(self)
    (serviceAvailabilityActorFactory.apply _).expects(*, service2, *).returns(self)
    val sut: TestActorRef[ServiceBrokerActor] = TestActorRef[ServiceBrokerActor](ServiceBrokerActor.props(Set(connectionStrategyForService1, connectionStrategyForService2), serviceAvailabilityActorFactory), "ServiceBroker")
    expectMsgAllOf(Start, Start)
    sut ! ServiceBrokerActor.AllConnectionProvidersAvailable
    expectMsgPF() {
      case LoadBalancerActor.HasAvailableConnectionProvider ⇒ lastSender ! true
    }
    expectMsgPF() {
      case LoadBalancerActor.HasAvailableConnectionProvider ⇒ lastSender ! true
    }
    expectMsg(true)
    sut.stop()
  }
}
