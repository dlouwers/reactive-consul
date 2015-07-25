package stormlantern.consul.client.loadbalancers

import akka.actor.ActorSystem
import akka.actor.Status.Failure
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfterAll, Matchers, FlatSpecLike }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification
import stormlantern.consul.client.discovery.{ ConnectionProvider, ConnectionHolder }
import stormlantern.consul.client.ServiceUnavailableException
import stormlantern.consul.client.util.Logging

import scala.concurrent.Future

class LoadBalancerActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with FlatSpecLike
    with Matchers with BeforeAndAfterAll with MockFactory with Logging {

  implicit val ec = system.dispatcher
  def this() = this(ActorSystem("LoadBalancerActorSpec"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  trait TestScope {
    val connectionHolder = mock[ConnectionHolder]
    val connectionProvider = mock[ConnectionProvider]
    val loadBalancer = mock[LoadBalancer]
  }

  "The LoadBalancerActor" should "hand out a connection holder when requested" in new TestScope {
    val instanceKey = "instanceKey"
    (loadBalancer.selectConnection _).expects().returns(Some(instanceKey))
    val sut = TestActorRef(new LoadBalancerActor(loadBalancer, "service1"))
    (connectionProvider.getConnectionHolder _).expects(instanceKey, sut).returns(Future.successful(connectionHolder))
    (connectionProvider.destroy _).expects()
    sut.underlyingActor.connectionProviders.put(instanceKey, connectionProvider)
    sut ! LoadBalancerActor.GetConnection
    expectMsg(connectionHolder)
    sut.stop()
  }

  it should "return an error when a connectionprovider fails to provide a connection" in new TestScope {
    val instanceKey = "instanceKey"
    val expectedException = new ServiceUnavailableException("service1")
    (loadBalancer.selectConnection _).expects().returns(Some(instanceKey))
    val sut = TestActorRef(new LoadBalancerActor(loadBalancer, "service1"))
    (connectionProvider.getConnectionHolder _).expects(instanceKey, sut).returns(Future.failed(expectedException))
    (connectionProvider.destroy _).expects()
    sut.underlyingActor.connectionProviders.put(instanceKey, connectionProvider)
    sut ! LoadBalancerActor.GetConnection
    expectMsg(Failure(expectedException))
    sut.stop()
  }

  it should "return a connection holder when requested" in new TestScope {
    val instanceKey = "instanceKey"
    (connectionHolder.key _).expects().returns(instanceKey)
    (connectionProvider.returnConnection _).expects(connectionHolder)
    (connectionHolder.key _).expects().returns(instanceKey)
    (loadBalancer.connectionReturned _).expects(instanceKey)
    (connectionProvider.destroy _).expects()
    val sut = TestActorRef(new LoadBalancerActor(loadBalancer, "service1"))
    sut.underlyingActor.connectionProviders.put(instanceKey, connectionProvider)
    sut ! LoadBalancerActor.ReturnConnection(connectionHolder)
    sut.stop()
  }

  it should "add a connection provider when requested" in new TestScope {
    val instanceKey = "instanceKey"
    (loadBalancer.connectionProviderAdded _).expects(instanceKey)
    (connectionProvider.destroy _).expects()
    val sut = TestActorRef(new LoadBalancerActor(loadBalancer, "service1"))
    sut ! LoadBalancerActor.AddConnectionProvider(instanceKey, connectionProvider)
    sut.underlyingActor.connectionProviders should contain(instanceKey -> connectionProvider)
    sut.stop()
  }

  it should "remove a connection provider when requested and tell it to destroy itself" in new TestScope {
    val instanceKey = "instanceKey"
    (connectionProvider.destroy _).expects()
    (loadBalancer.connectionProviderRemoved _).expects(instanceKey)
    val sut = TestActorRef(new LoadBalancerActor(loadBalancer, "service1"))
    sut.underlyingActor.connectionProviders.put(instanceKey, connectionProvider)
    sut ! LoadBalancerActor.RemoveConnectionProvider(instanceKey)
    sut.underlyingActor.connectionProviders should not contain (instanceKey -> connectionProvider)
  }

  it should "return true when it has at least one available connection provider for the service" in new TestScope {
    (connectionProvider.destroy _).expects()
    val sut = TestActorRef(new LoadBalancerActor(loadBalancer, "service1"))
    sut.underlyingActor.connectionProviders.put("key", connectionProvider)
    sut ! LoadBalancerActor.HasAvailableConnectionProvider
    expectMsg(true)
    sut.stop()
  }

  it should "return false when it has no available connection providers for the service" in new TestScope {
    val sut = TestActorRef(new LoadBalancerActor(loadBalancer, "service1"))
    sut ! LoadBalancerActor.HasAvailableConnectionProvider
    expectMsg(false)
    sut.stop()
  }
}
