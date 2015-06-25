package stormlantern.consul.client

import akka.actor.ActorSystem
import akka.actor.SupervisorStrategy.Stop
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import org.mockito.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification
import stormlantern.consul.client.dao.{ ConsulHttpClient, IndexedServiceInstances }
import stormlantern.consul.client.helpers.ModelHelpers
import stormlantern.consul.client.util.Logging

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class ServiceAvailabilityActorSpec extends Specification with Mockito with Logging {

  abstract class ActorScope extends TestKit(ActorSystem("TestSystem")) with specification.After with ImplicitSender {
    implicit val ec = system.dispatcher
    override def after: Any = system.shutdown()
    val httpClient: ConsulHttpClient = smartMock[ConsulHttpClient]
  }

  "The ServiceAvailabilityActor" should {

    "receive one service update when there are no changes" in new ActorScope {
      httpClient.findServiceChange(Matchers.eq("bogus"), Matchers.eq(None), Matchers.any[Option[String]], Matchers.any[Option[String]]) returns Future.successful(IndexedServiceInstances(1, Set.empty))
      httpClient.findServiceChange(Matchers.eq("bogus"), Matchers.eq(Some(1)), Matchers.eq(Some("1s")), Matchers.any[Option[String]]) returns Future {
        sut ! Stop
        IndexedServiceInstances(1, Set.empty)
      }
      val sut = TestActorRef(ServiceAvailabilityActor.props(httpClient, "bogus", self))
      expectMsg(Duration(1, "s"), ServiceAvailabilityActor.ServiceAvailabilityUpdate.empty)
      expectNoMsg(Duration(1, "s"))
    }

    "receive two service updates when there is a change" in new ActorScope {
      val service = ModelHelpers.createService("bogus")
      httpClient.findServiceChange(Matchers.eq("bogus"), Matchers.eq(None), Matchers.any[Option[String]], Matchers.any[Option[String]]) returns Future.successful(IndexedServiceInstances(1, Set.empty))
      httpClient.findServiceChange(Matchers.eq("bogus"), Matchers.eq(Some(1)), Matchers.eq(Some("1s")), Matchers.any[Option[String]]) returns Future.successful(IndexedServiceInstances(2, Set(service)))
      httpClient.findServiceChange(Matchers.eq("bogus"), Matchers.eq(Some(2)), Matchers.eq(Some("1s")), Matchers.any[Option[String]]) returns Future {
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
