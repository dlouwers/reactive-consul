package xebia.consul.client.loadbalancers

import akka.actor.ActorSystem
import akka.testkit.{ TestActorRef, ImplicitSender, TestKit }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification
import xebia.consul.client.{ ConnectionProvider, ConnectionHolder }

import scala.concurrent.Future

class LoadBalancerActorSpec extends Specification with Mockito {

  abstract class ActorScope extends TestKit(ActorSystem("TestSystem")) with specification.After with ImplicitSender {
    override def after: Any = TestKit.shutdownActorSystem(system)
    val connectionHolder = mock[ConnectionHolder]
    val connectionProvider = mock[ConnectionProvider]
  }

  "The BaseLoadBalancerActor" should {
    "hand out a connection holder when requested" in new ActorScope {
      val expectedConnectionHolder = Some(connectionHolder)
      val sut = TestActorRef(new LoadBalancerActor {
        override def selectConnection: Future[Option[ConnectionHolder]] = Future.successful(expectedConnectionHolder)
      })
      sut ! LoadBalancerActor.GetConnection
      expectMsg(expectedConnectionHolder)
    }
    "return a connection holder when requested" in new ActorScope {
      val sut = TestActorRef(new LoadBalancerActor {
        override def selectConnection: Future[Option[ConnectionHolder]] = Future.successful(Some(connectionHolder))
      })
      sut.underlyingActor.connectionProviders.put("key", connectionProvider)
      connectionHolder.key returns "key"
      sut ! LoadBalancerActor.ReturnConnection(connectionHolder)
      there was one(connectionProvider).returnConnection(connectionHolder)
    }
    "add a connection provider when requested" in new ActorScope {
      val sut = TestActorRef(new LoadBalancerActor {
        override def selectConnection: Future[Option[ConnectionHolder]] = Future.successful(Some(connectionHolder))
      })
      sut ! LoadBalancerActor.AddConnectionProvider("key", connectionProvider)
      sut.underlyingActor.connectionProviders should havePair("key" -> connectionProvider)
    }
    "remove a connection provider when requested and tell it to destroy itself" in new ActorScope {
      val sut = TestActorRef(new LoadBalancerActor {
        override def selectConnection: Future[Option[ConnectionHolder]] = Future.successful(Some(connectionHolder))
      })
      sut.underlyingActor.connectionProviders.put("key", connectionProvider)
      sut ! LoadBalancerActor.RemoveConnectionProvider("key")
      sut.underlyingActor.connectionProviders should not havePair ("key" -> connectionProvider)
      there was one(connectionProvider).destroy()
    }
  }
}
