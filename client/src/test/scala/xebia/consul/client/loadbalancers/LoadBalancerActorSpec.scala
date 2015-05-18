package xebia.consul.client.loadbalancers

import akka.actor.ActorSystem
import akka.actor.Status.Failure
import akka.testkit.{ TestActorRef, ImplicitSender, TestKit }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification
import xebia.consul.client.{ ServiceUnavailableException, ConnectionProvider, ConnectionHolder }

import scala.concurrent.Future

class LoadBalancerActorSpec extends Specification with Mockito {

  abstract class ActorScope extends TestKit(ActorSystem("TestSystem")) with specification.After with ImplicitSender {
    override def after: Any = TestKit.shutdownActorSystem(system)
    val connectionHolder = mock[ConnectionHolder]
    val connectionProvider = mock[ConnectionProvider]
  }

  "The LoadBalancerActor" should {

    "hand out a connection holder when requested" in new ActorScope {
      val sut = TestActorRef(new LoadBalancerActor {
        override def serviceName: String = "service1"
        override def selectConnection: Option[Future[ConnectionHolder]] = Some(Future.successful(connectionHolder))
      })
      sut ! LoadBalancerActor.GetConnection
      expectMsg(connectionHolder)
    }

    "return an error when a connection cannot be provided" in new ActorScope {
      val expectedException = new ServiceUnavailableException("service1")
      val sut = TestActorRef(new LoadBalancerActor {
        override def serviceName: String = "service1"
        override def selectConnection: Option[Future[ConnectionHolder]] = Some(Future.failed(expectedException))
      })
      sut ! LoadBalancerActor.GetConnection
      expectMsg(Failure(expectedException))
    }

    "return a connection holder when requested" in new ActorScope {
      val sut = TestActorRef(new LoadBalancerActor {
        override def serviceName: String = "service1"
        override def selectConnection: Option[Future[ConnectionHolder]] = Some(Future.successful(connectionHolder))
      })
      sut.underlyingActor.connectionProviders.put("key", connectionProvider)
      connectionHolder.key returns "key"
      sut ! LoadBalancerActor.ReturnConnection(connectionHolder)
      there was one(connectionProvider).returnConnection(connectionHolder)
    }

    "add a connection provider when requested" in new ActorScope {
      val sut = TestActorRef(new LoadBalancerActor {
        override def serviceName: String = "service1"
        override def selectConnection: Option[Future[ConnectionHolder]] = Some(Future.successful(connectionHolder))
      })
      sut ! LoadBalancerActor.AddConnectionProvider("key", connectionProvider)
      sut.underlyingActor.connectionProviders should havePair("key" -> connectionProvider)
    }

    "remove a connection provider when requested and tell it to destroy itself" in new ActorScope {
      val sut = TestActorRef(new LoadBalancerActor {
        override def serviceName: String = "service1"
        override def selectConnection: Option[Future[ConnectionHolder]] = Some(Future.successful(connectionHolder))
      })
      sut.underlyingActor.connectionProviders.put("key", connectionProvider)
      sut ! LoadBalancerActor.RemoveConnectionProvider("key")
      sut.underlyingActor.connectionProviders should not havePair ("key" -> connectionProvider)
      there was one(connectionProvider).destroy()
    }

    "return true when it has at least one available connection provider for the service" in new ActorScope {
      val sut = TestActorRef(new LoadBalancerActor {
        override def serviceName: String = "service1"
        override def selectConnection: Option[Future[ConnectionHolder]] = Some(Future.successful(connectionHolder))
      })
      sut.underlyingActor.connectionProviders.put("key", connectionProvider)
      sut ! LoadBalancerActor.HasAvailableConnectionProvider
      expectMsg(true)
    }

    "return false when it has no available connection providers for the service" in new ActorScope {
      val sut = TestActorRef(new LoadBalancerActor {
        override def serviceName: String = "service1"
        override def selectConnection: Option[Future[ConnectionHolder]] = Some(Future.successful(connectionHolder))
      })
      sut ! LoadBalancerActor.HasAvailableConnectionProvider
      expectMsg(false)
    }
  }
}
