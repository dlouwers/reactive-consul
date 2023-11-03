package com.crobox.reactiveconsul.client.loadbalancers

import com.crobox.reactiveconsul.client.discovery.{ConnectionHolder, ConnectionProvider}
import com.crobox.reactiveconsul.client.{ClientSpec, ServiceUnavailableException}
import org.apache.pekko.actor.Status.Failure
import org.apache.pekko.testkit.TestActorRef

import scala.concurrent.Future

class LoadBalancerActorTest extends ClientSpec {

  trait TestScope {
    val connectionHolder   = mock[ConnectionHolder]
    val connectionProvider = mock[ConnectionProvider]
    val loadBalancer       = mock[LoadBalancer]
  }

  "The LoadBalancerActor" should "hand out a connection holder when requested" in new TestScope {
    val instanceKey = "instanceKey"
    (() => loadBalancer.selectConnection).expects().returns(Some(instanceKey))
    val sut = TestActorRef(new LoadBalancerActor(loadBalancer, "service1"))
    (connectionProvider.getConnectionHolder _).expects(instanceKey, sut).returns(Future.successful(connectionHolder))
    (connectionProvider.destroy _).expects()
    sut.underlyingActor.connectionProviders.put(instanceKey, connectionProvider)
    sut ! LoadBalancerActor.GetConnection
    expectMsg(connectionHolder)
    sut.stop()
  }

  it should "return an error when a connectionprovider fails to provide a connection" in new TestScope {
    val instanceKey       = "instanceKey"
    val expectedException = new ServiceUnavailableException("service1")
    (() => loadBalancer.selectConnection).expects().returns(Some(instanceKey))
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
    (() => connectionHolder.id).expects().returns(instanceKey)
    (connectionProvider.returnConnection _).expects(connectionHolder)
    (() => connectionHolder.id).expects().returns(instanceKey)
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
