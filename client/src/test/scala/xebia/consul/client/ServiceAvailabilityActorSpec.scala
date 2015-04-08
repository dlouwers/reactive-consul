package xebia.consul.client

import akka.actor.ActorSystem
import akka.testkit.{ TestActorRef, ImplicitSender, TestKit }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification
import xebia.consul.client.ServiceAvailabilityActor.Stop
import xebia.consul.client.util.Logging

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class ServiceAvailabilityActorSpec extends Specification with Mockito with Logging {

  abstract class ActorScope extends TestKit(ActorSystem("TestSystem")) with specification.After with ImplicitSender {
    implicit val ec = system.dispatcher
    override def after: Any = system.shutdown()
    val httpClient = mock[CatalogHttpClient]
  }

  "The ServiceAvailabilityActor" should {
    "receive one service update when there are no changes" in new ActorScope {
      httpClient.findServiceChange("bogus", None) returns Future.successful(IndexedServiceInstances(1, Seq.empty))
      httpClient.findServiceChange("bogus", Some(1)) returns Future {
        sut ! Stop
        IndexedServiceInstances(1, Seq.empty)
      }
      val sut = TestActorRef(ServiceAvailabilityActor.props(httpClient, "bogus", self))
      expectMsg(Duration(1, "s"), ServiceAvailabilityActor.ServiceAvailabilityUpdate.empty)
      expectNoMsg(Duration(1, "s"))
    }

    "receive two service updates when there is a change" in new ActorScope {
      val service = Service(
        node = "host",
        address = "host",
        serviceId = "serviceId",
        serviceName = "bogus",
        serviceTags = Seq("tag1", "tag2"),
        serviceAddress = "host",
        servicePort = 666
      )
      httpClient.findServiceChange("bogus", None) returns Future.successful(IndexedServiceInstances(1, Seq.empty))
      httpClient.findServiceChange("bogus", Some(1)) returns Future.successful(IndexedServiceInstances(2, Seq(service)))
      httpClient.findServiceChange("bogus", Some(2)) returns Future {
        sut ! Stop
        IndexedServiceInstances(2, Seq(service))
      }

      val sut = TestActorRef(ServiceAvailabilityActor.props(httpClient, "bogus", self))
      expectMsg(Duration(1, "s"), ServiceAvailabilityActor.ServiceAvailabilityUpdate.empty)
      expectMsg(Duration(1, "s"), ServiceAvailabilityActor.ServiceAvailabilityUpdate(Set(service), Set.empty))
      expectNoMsg(Duration(1, "s"))
    }
  }
}