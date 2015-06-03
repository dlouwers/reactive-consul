package xebia.consul.client

import akka.actor.{ Actor, ActorSystem }
import akka.testkit.{ TestActorRef, ImplicitSender, TestKit }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification
import xebia.consul.client.ServiceAvailabilityActor.Stop
import xebia.consul.client.dao.{ IndexedServiceInstances, ConsulHttpClient }
import xebia.consul.client.helpers.ModelHelpers
import xebia.consul.client.util.Logging

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class ServiceAvailabilityActorSpec extends Specification with Mockito with Logging {

  abstract class ActorScope extends TestKit(ActorSystem("TestSystem")) with specification.After with ImplicitSender {
    implicit val ec = system.dispatcher
    override def after: Any = system.shutdown()
    val httpClient = mock[ConsulHttpClient]
  }

  "The ServiceAvailabilityActor" should {

    "receive one service update when there are no changes" in new ActorScope {
      httpClient.findServiceChange("bogus", None) returns Future.successful(IndexedServiceInstances(1, Set.empty))
      httpClient.findServiceChange("bogus", Some(1)) returns Future {
        sut ! Stop
        IndexedServiceInstances(1, Set.empty)
      }
      val sut = TestActorRef(ServiceAvailabilityActor.props(httpClient, "bogus", self))
      expectMsg(Duration(1, "s"), ServiceAvailabilityActor.ServiceAvailabilityUpdate.empty)
      expectNoMsg(Duration(1, "s"))
    }

    "receive two service updates when there is a change" in new ActorScope {
      val service = ModelHelpers.createService("bogus")
      httpClient.findServiceChange("bogus", None) returns Future.successful(IndexedServiceInstances(1, Set.empty))
      httpClient.findServiceChange("bogus", Some(1)) returns Future.successful(IndexedServiceInstances(2, Set(service)))
      httpClient.findServiceChange("bogus", Some(2)) returns Future {
        sut ! Stop
        IndexedServiceInstances(2, Set(service))
      }

      val sut = TestActorRef(ServiceAvailabilityActor.props(httpClient, "bogus", self))
      expectMsg(Duration(1, "s"), ServiceAvailabilityActor.ServiceAvailabilityUpdate.empty)
      expectMsg(Duration(1, "s"), ServiceAvailabilityActor.ServiceAvailabilityUpdate(Set(service), Set.empty))
      expectNoMsg(Duration(1, "s"))
    }
  }
}
