package stormlantern.consul.client

import org.apache.pekko.actor.Status.Failure
import org.apache.pekko.actor._
import org.apache.pekko.testkit.{TestActorRef, TestProbe}
import stormlantern.consul.client.dao.{ConsulHttpClient, ServiceInstance}
import stormlantern.consul.client.discovery.ServiceAvailabilityActor.{Start, Started}
import stormlantern.consul.client.discovery._
import stormlantern.consul.client.helpers.ModelHelpers
import stormlantern.consul.client.loadbalancers.LoadBalancerActor

class ServiceBrokerActorTest extends ClientSpec {

  trait TestScope {
    val httpClient: ConsulHttpClient = mock[ConsulHttpClient]

    val serviceAvailabilityActorFactory: (ActorRefFactory, ServiceDefinition, ActorRef, Boolean) => ActorRef =
      mock[(ActorRefFactory, ServiceDefinition, ActorRef, Boolean) => ActorRef]
    val connectionProviderFactory: ConnectionProviderFactory = mock[ConnectionProviderFactory]
    val connectionProvider: ConnectionProvider               = mock[ConnectionProvider]
    val connectionHolder: ConnectionHolder                   = mock[ConnectionHolder]
    val service1                                             = ServiceDefinition("service1Id", "service1")
    val service2                                             = ServiceDefinition("service2Key", "service2")
    val loadBalancerProbeForService1                         = TestProbe("LoadBalancerActorForService1")
    val loadBalancerProbeForService2                         = TestProbe("LoadBalancerActorForService2")

    val connectionStrategyForService1 = ConnectionStrategy(service1,
                                                           connectionProviderFactory,
                                                           ctx => loadBalancerProbeForService1.ref,
                                                           onlyHealthyServices = true)

    val connectionStrategyForService2 = ConnectionStrategy(service2,
                                                           connectionProviderFactory,
                                                           ctx => loadBalancerProbeForService2.ref,
                                                           onlyHealthyServices = false)
  }

  "The ServiceBrokerActor" should "create a child actor per service" in new TestScope {
    val serviceAvailabilityProbe = TestProbe("ServiceAvailabilityActor")
    (serviceAvailabilityActorFactory.apply _).expects(*, service1, *, *).returns(serviceAvailabilityProbe.ref)

    val sut: TestActorRef[ServiceBrokerActor] = TestActorRef[ServiceBrokerActor](
      ServiceBrokerActor.props(Set(connectionStrategyForService1), serviceAvailabilityActorFactory)
    )
    serviceAvailabilityProbe.expectMsg(Start)
    sut.underlyingActor.loadbalancers.keys should contain(service1.key)
    sut.stop()
  }

  it should "create a load balancer for each new service" in new TestScope {
    val serviceAvailabilityProbe = TestProbe("ServiceAvailabilityActor")
    (serviceAvailabilityActorFactory.apply _).expects(*, service1, *, *).returns(serviceAvailabilityProbe.ref)

    val sut: TestActorRef[ServiceBrokerActor] = TestActorRef[ServiceBrokerActor](
      ServiceBrokerActor.props(Set(connectionStrategyForService1), serviceAvailabilityActorFactory)
    )
    serviceAvailabilityProbe.expectMsg(Start)
    val service: ServiceInstance = ModelHelpers.createService("service1:Id", "service1")
    (connectionProviderFactory.create _)
      .expects(service.serviceAddress, service.servicePort)
      .returns(connectionProvider)
    serviceAvailabilityProbe.send(
      sut,
      ServiceAvailabilityActor.ServiceAvailabilityUpdate(service1.key, added = Set(service), removed = Set.empty)
    )
    loadBalancerProbeForService1.expectMsg(
      LoadBalancerActor.AddConnectionProvider(service.serviceId, connectionProvider)
    )
    sut.stop()
  }

  it should "remove the load balancer for each old service" in new TestScope {
    val serviceAvailabilityProbe = TestProbe("ServiceAvailabilityActor")
    (serviceAvailabilityActorFactory.apply _).expects(*, service1, *, *).returns(serviceAvailabilityProbe.ref)

    val sut: TestActorRef[ServiceBrokerActor] = TestActorRef[ServiceBrokerActor](
      ServiceBrokerActor.props(Set(connectionStrategyForService1), serviceAvailabilityActorFactory)
    )
    serviceAvailabilityProbe.expectMsg(Start)
    val service: ServiceInstance = ModelHelpers.createService("service1:Id", "service1")
    serviceAvailabilityProbe.send(
      sut,
      ServiceAvailabilityActor.ServiceAvailabilityUpdate(service1.key, added = Set.empty, removed = Set(service))
    )
    loadBalancerProbeForService1.expectMsg(LoadBalancerActor.RemoveConnectionProvider(service.serviceId))
    sut.stop()
  }

  it should "initialize after all services have been seen" in new TestScope {
    val serviceAvailabilityProbe = TestProbe("ServiceAvailabilityActor")
    (serviceAvailabilityActorFactory.apply _).expects(*, service1, *, *).returns(serviceAvailabilityProbe.ref)

    val sut: TestActorRef[ServiceBrokerActor] = TestActorRef[ServiceBrokerActor](
      ServiceBrokerActor.props(Set(connectionStrategyForService1), serviceAvailabilityActorFactory)
    )
    serviceAvailabilityProbe.expectMsg(Start)
    val service: ServiceInstance = ModelHelpers.createService(service1)
  }

  it should "request a connection from a loadbalancer" in new TestScope {
    val serviceAvailabilityProbe = TestProbe("ServiceAvailabilityActor")
    (serviceAvailabilityActorFactory.apply _).expects(*, service1, *, *).returns(serviceAvailabilityProbe.ref)

    val sut: TestActorRef[ServiceBrokerActor] = TestActorRef[ServiceBrokerActor](
      ServiceBrokerActor.props(Set(connectionStrategyForService1), serviceAvailabilityActorFactory)
    )
    serviceAvailabilityProbe.expectMsg(Start)
    val service: ServiceInstance = ModelHelpers.createService(service1)
    sut ! ServiceBrokerActor.GetServiceConnection(service.serviceId)
    serviceAvailabilityProbe.send(sut, Started)
    loadBalancerProbeForService1.expectMsg(LoadBalancerActor.GetConnection)
    sut.stop()
  }

  it should "return a failure if a service name cannot be found" in new TestScope {

    val sut: TestActorRef[ServiceBrokerActor] =
      TestActorRef[ServiceBrokerActor](ServiceBrokerActor.props(Set.empty, serviceAvailabilityActorFactory))
    val service: ServiceInstance = ModelHelpers.createService(service1)
    sut ! ServiceBrokerActor.GetServiceConnection(service.serviceId)
    expectMsg(Failure(ServiceUnavailableException(service.serviceId)))
    sut.stop()
  }

  it should "forward a query for connection provider availability" in new TestScope {
    val serviceAvailabilityProbe = TestProbe("ServiceAvailabilityActor")
    (serviceAvailabilityActorFactory.apply _).expects(*, service1, *, *).returns(serviceAvailabilityProbe.ref)

    val sut: TestActorRef[ServiceBrokerActor] = TestActorRef[ServiceBrokerActor](
      ServiceBrokerActor.props(Set(connectionStrategyForService1), serviceAvailabilityActorFactory)
    )
    serviceAvailabilityProbe.expectMsg(Start)
    sut ! ServiceBrokerActor.HasAvailableConnectionProviderFor(service1.key)
    loadBalancerProbeForService1.expectMsgPF() {
      case LoadBalancerActor.HasAvailableConnectionProvider => loadBalancerProbeForService1.sender() ! true
    }
    expectMsg(true)
    sut.stop()
  }

  it should "return false when every service doesn't have at least one connection provider available" in new TestScope {
    val service1AvailabilityProbe = TestProbe("Service1AvailabilityActor")
    val service2AvailabilityProbe = TestProbe("Service2AvailabilityActor")
    (serviceAvailabilityActorFactory.apply _).expects(*, service1, *, *).returns(service1AvailabilityProbe.ref)
    (serviceAvailabilityActorFactory.apply _).expects(*, service2, *, *).returns(service2AvailabilityProbe.ref)

    val sut: TestActorRef[ServiceBrokerActor] = TestActorRef[ServiceBrokerActor](
      ServiceBrokerActor.props(Set(connectionStrategyForService1, connectionStrategyForService2),
                               serviceAvailabilityActorFactory)
    )
    service1AvailabilityProbe.expectMsg(Start)
    service2AvailabilityProbe.expectMsg(Start)
    sut ! ServiceBrokerActor.AllConnectionProvidersAvailable
    loadBalancerProbeForService1.expectMsgPF() {
      case LoadBalancerActor.HasAvailableConnectionProvider => loadBalancerProbeForService1.sender() ! true
    }
    loadBalancerProbeForService2.expectMsgPF() {
      case LoadBalancerActor.HasAvailableConnectionProvider => loadBalancerProbeForService2.sender() ! false
    }
    expectMsg(false)
    sut.stop()
  }

  it should "return true when every service has at least one connection provider avaiable" in new TestScope {
    val service1AvailabilityProbe = TestProbe("Service1AvailabilityActor")
    val service2AvailabilityProbe = TestProbe("Service2AvailabilityActor")
    (serviceAvailabilityActorFactory.apply _).expects(*, service1, *, *).returns(service1AvailabilityProbe.ref)
    (serviceAvailabilityActorFactory.apply _).expects(*, service2, *, *).returns(service2AvailabilityProbe.ref)

    val sut: TestActorRef[ServiceBrokerActor] = TestActorRef[ServiceBrokerActor](
      ServiceBrokerActor.props(Set(connectionStrategyForService1, connectionStrategyForService2),
                               serviceAvailabilityActorFactory)
    )
    service1AvailabilityProbe.expectMsg(Start)
    service2AvailabilityProbe.expectMsg(Start)
    sut ! ServiceBrokerActor.AllConnectionProvidersAvailable
    loadBalancerProbeForService1.expectMsgPF() {
      case LoadBalancerActor.HasAvailableConnectionProvider => loadBalancerProbeForService1.sender() ! true
    }
    loadBalancerProbeForService2.expectMsgPF() {
      case LoadBalancerActor.HasAvailableConnectionProvider => loadBalancerProbeForService2.sender() ! true
    }
    expectMsg(true)
    sut.stop()
  }
}
