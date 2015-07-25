package stormlantern.consul.client

import akka.actor.{ PoisonPill, ActorSystem }
import akka.actor.SupervisorStrategy.Stop
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, Matchers }
import stormlantern.consul.client.dao.{ ConsulHttpClient, IndexedServiceInstances }
import stormlantern.consul.client.discovery.ServiceAvailabilityActor.Start
import stormlantern.consul.client.discovery.{ ServiceAvailabilityActor, ServiceDefinition }
import stormlantern.consul.client.helpers.ModelHelpers
import stormlantern.consul.client.util.Logging

import scala.concurrent.Future
import scala.concurrent.duration._

class ServiceAvailabilityActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with FlatSpecLike
    with Matchers with BeforeAndAfterAll with MockFactory with Logging {

  implicit val ec = system.dispatcher
  def this() = this(ActorSystem("ServiceAvailabilityActorSpec"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "The ServiceAvailabilityActor" should "receive one service update when there are no changes" in {
    val httpClient: ConsulHttpClient = mock[ConsulHttpClient]
    val sut = TestActorRef(ServiceAvailabilityActor.props(httpClient, ServiceDefinition("bogus"), self))
    (httpClient.findServiceChange _).expects("bogus", None, Some(0L), Some("1s"), None).returns(Future.successful(IndexedServiceInstances(1, Set.empty)))
    (httpClient.findServiceChange _).expects("bogus", None, Some(1L), Some("1s"), None).onCall { p =>
      sut.stop()
      Future.successful(IndexedServiceInstances(1, Set.empty))
    }
    sut ! Start
    expectMsg(1.second, ServiceAvailabilityActor.ServiceAvailabilityUpdate.empty)
    expectNoMsg(1.second)
  }

  it should "receive two service updates when there is a change" in {
    val httpClient: ConsulHttpClient = mock[ConsulHttpClient]
    lazy val sut = TestActorRef(ServiceAvailabilityActor.props(httpClient, ServiceDefinition("bogus"), self))
    val service = ModelHelpers.createService("bogus")
    (httpClient.findServiceChange _).expects("bogus", None, Some(0L), Some("1s"), None).returns(Future.successful(IndexedServiceInstances(1, Set.empty)))
    (httpClient.findServiceChange _).expects("bogus", None, Some(1L), Some("1s"), None).returns(Future.successful(IndexedServiceInstances(2, Set(service))))
    (httpClient.findServiceChange _).expects("bogus", None, Some(2L), Some("1s"), None).onCall { p =>
      sut.stop()
      Future.successful(IndexedServiceInstances(2, Set(service)))
    }
    sut ! Start
    expectMsg(Duration(1, "s"), ServiceAvailabilityActor.ServiceAvailabilityUpdate.empty)
    expectMsg(Duration(1, "s"), ServiceAvailabilityActor.ServiceAvailabilityUpdate(Set(service), Set.empty))
    expectNoMsg(Duration(1, "s"))
  }
}
