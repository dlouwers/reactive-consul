package stormlantern.consul.client.loadbalancers

import akka.actor.ActorSystem
import akka.actor.Status.Failure
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification
import stormlantern.consul.client.{ ConnectionHolder, ConnectionProvider, ServiceUnavailableException }

import scala.concurrent.Future

class LoadBalancerActorSpec extends Specification with Mockito {

  abstract class ActorScope extends TestKit(ActorSystem("TestSystem")) with specification.After with ImplicitSender {
    override def after: Any = TestKit.shutdownActorSystem(system)
    val connectionHolder = mock[ConnectionHolder]
    val connectionProvider = mock[ConnectionProvider]
    val loadBalancer = mock[LoadBalancer]
  }

  "The LoadBalancerActor" should {

    "hand out a connection holder when requested" in new ActorScope {
      val instanceKey = "instanceKey"
      loadBalancer.selectConnection returns Some(instanceKey)
      val sut = TestActorRef(new LoadBalancerActor(loadBalancer, "service1"))
      connectionProvider.getConnectionHolder(instanceKey, sut) returns Future.successful(connectionHolder)
      sut.underlyingActor.connectionProviders.put(instanceKey, connectionProvider)
      sut ! LoadBalancerActor.GetConnection
      expectMsg(connectionHolder)
      there was one(loadBalancer).selectConnection
      there was one(connectionProvider).getConnectionHolder(instanceKey, sut)
    }

    "return an error when a connectionprovider fails to provide a connection" in new ActorScope {
      val instanceKey = "instanceKey"
      val expectedException = new ServiceUnavailableException("service1")
      loadBalancer.selectConnection returns Some(instanceKey)
      val sut = TestActorRef(new LoadBalancerActor(loadBalancer, "service1"))
      connectionProvider.getConnectionHolder(instanceKey, sut) returns Future.failed(expectedException)
      sut.underlyingActor.connectionProviders.put(instanceKey, connectionProvider)
      sut ! LoadBalancerActor.GetConnection
      expectMsg(Failure(expectedException))
      there was one(loadBalancer).selectConnection
      there was one(connectionProvider).getConnectionHolder(instanceKey, sut)
    }

    "return a connection holder when requested" in new ActorScope {
      val instanceKey = "instanceKey"
      connectionHolder.key returns instanceKey
      val sut = TestActorRef(new LoadBalancerActor(loadBalancer, "service1"))
      sut.underlyingActor.connectionProviders.put(instanceKey, connectionProvider)
      sut ! LoadBalancerActor.ReturnConnection(connectionHolder)
      there was one(connectionProvider).returnConnection(connectionHolder)
      there was one(loadBalancer).connectionReturned(connectionHolder.key)
    }

    "add a connection provider when requested" in new ActorScope {
      val instanceKey = "instanceKey"
      val sut = TestActorRef(new LoadBalancerActor(loadBalancer, "service1"))
      sut ! LoadBalancerActor.AddConnectionProvider(instanceKey, connectionProvider)
      sut.underlyingActor.connectionProviders should havePair(instanceKey -> connectionProvider)
      there was one(loadBalancer).connectionProviderAdded(instanceKey)
    }

    "remove a connection provider when requested and tell it to destroy itself" in new ActorScope {
      val instanceKey = "instanceKey"
      val sut = TestActorRef(new LoadBalancerActor(loadBalancer, "service1"))
      sut.underlyingActor.connectionProviders.put(instanceKey, connectionProvider)
      sut ! LoadBalancerActor.RemoveConnectionProvider(instanceKey)
      sut.underlyingActor.connectionProviders should not havePair (instanceKey -> connectionProvider)
      there was one(connectionProvider).destroy()
      there was one(loadBalancer).connectionProviderRemoved(instanceKey)
    }

    "return true when it has at least one available connection provider for the service" in new ActorScope {
      val sut = TestActorRef(new LoadBalancerActor(loadBalancer, "service1"))
      sut.underlyingActor.connectionProviders.put("key", connectionProvider)
      sut ! LoadBalancerActor.HasAvailableConnectionProvider
      expectMsg(true)
    }

    "return false when it has no available connection providers for the service" in new ActorScope {
      val sut = TestActorRef(new LoadBalancerActor(loadBalancer, "service1"))
      sut ! LoadBalancerActor.HasAvailableConnectionProvider
      expectMsg(false)
    }
  }
}
