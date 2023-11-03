package stormlantern.consul.client.discovery

import org.apache.pekko.testkit.TestActorRef
import stormlantern.consul.client.ClientSpec
import stormlantern.consul.client.dao.{ConsulHttpClient, IndexedServiceInstances}
import stormlantern.consul.client.discovery.ServiceAvailabilityActor.Start
import stormlantern.consul.client.helpers.ModelHelpers

import scala.concurrent.Future
import scala.concurrent.duration._

class ServiceAvailabilityActorTest extends ClientSpec {

  "The ServiceAvailabilityActor" should "receive one service update when there are no changes" in {
    val httpClient: ConsulHttpClient = mock[ConsulHttpClient]
    val sut = TestActorRef(
      ServiceAvailabilityActor
        .props(httpClient, ServiceDefinition("bogus123", "bogus"), self, onlyHealthyServices = false)
    )
    (httpClient.getService _)
      .expects("bogus", None, Some(0L), Some("1s"), None)
      .returns(Future.successful(IndexedServiceInstances(1, Set.empty)))
    (httpClient.getService _).expects("bogus", None, Some(1L), Some("1s"), None).onCall { p =>
      sut.stop()
      Future.successful(IndexedServiceInstances(1, Set.empty))
    }
    sut ! Start
    expectMsg(1.second, ServiceAvailabilityActor.ServiceAvailabilityUpdate("bogus123"))
    expectMsg(1.second, ServiceAvailabilityActor.Started)
    expectNoMessage(1.second)
  }

  it should "receive two service updates when there is a change" in {
    val httpClient: ConsulHttpClient = mock[ConsulHttpClient]
    lazy val sut = TestActorRef(
      ServiceAvailabilityActor
        .props(httpClient, ServiceDefinition("bogus123", "bogus"), self, onlyHealthyServices = false)
    )
    val service = ModelHelpers.createService("bogus123", "bogus")
    (httpClient.getService _)
      .expects("bogus", None, Some(0L), Some("1s"), None)
      .returns(Future.successful(IndexedServiceInstances(1, Set.empty)))
    (httpClient.getService _)
      .expects("bogus", None, Some(1L), Some("1s"), None)
      .returns(Future.successful(IndexedServiceInstances(2, Set(service))))
    (httpClient.getService _).expects("bogus", None, Some(2L), Some("1s"), None).onCall { p =>
      sut.stop()
      Future.successful(IndexedServiceInstances(2, Set(service)))
    }
    sut ! Start
    expectMsg(1.second, ServiceAvailabilityActor.ServiceAvailabilityUpdate("bogus123"))
    expectMsg(1.second, ServiceAvailabilityActor.Started)
    expectMsg(1.second, ServiceAvailabilityActor.ServiceAvailabilityUpdate("bogus123", Set(service), Set.empty))
    expectNoMessage(1.second)
  }

  it should "receive one service update when there are two with different tags" in {
    val httpClient: ConsulHttpClient = mock[ConsulHttpClient]
    lazy val sut = TestActorRef(
      ServiceAvailabilityActor
        .props(httpClient, ServiceDefinition("bogus123", "bogus", Set("one", "two")), self, onlyHealthyServices = false)
    )
    val nonMatchingservice = ModelHelpers.createService("bogus123", "bogus", tags = Set("one"))
    val matchingService    = nonMatchingservice.copy(serviceTags = Set("one", "two"))
    (httpClient.getService _)
      .expects("bogus", Some("one"), Some(0L), Some("1s"), None)
      .returns(Future.successful(IndexedServiceInstances(1, Set.empty)))
    (httpClient.getService _)
      .expects("bogus", Some("one"), Some(1L), Some("1s"), None)
      .returns(Future.successful(IndexedServiceInstances(2, Set(nonMatchingservice, matchingService))))
    (httpClient.getService _).expects("bogus", Some("one"), Some(2L), Some("1s"), None).onCall { p =>
      sut.stop()
      Future.successful(IndexedServiceInstances(2, Set(nonMatchingservice, matchingService)))
    }
    sut ! Start
    expectMsg(1.second, ServiceAvailabilityActor.ServiceAvailabilityUpdate("bogus123"))
    expectMsg(1.second, ServiceAvailabilityActor.Started)
    expectMsg(1.second, ServiceAvailabilityActor.ServiceAvailabilityUpdate("bogus123", Set(matchingService), Set.empty))
    expectNoMessage(1.second)
  }
}
