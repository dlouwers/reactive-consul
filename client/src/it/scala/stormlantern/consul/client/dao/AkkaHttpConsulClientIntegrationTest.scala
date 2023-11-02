package stormlantern.consul.client.dao

import java.net.URL
import java.util.UUID

import org.scalatest._
import org.scalatest.concurrent.{ Eventually, IntegrationPatience, ScalaFutures }
import stormlantern.consul.client.dao.org.apache.pekko.AkkaHttpConsulClient
import stormlantern.consul.client.util.{ ConsulDockerContainer, Logging, RetryPolicy, TestActorSystem }

class AkkaHttpConsulClientIntegrationTest extends FlatSpec with Matchers with ScalaFutures with Eventually with IntegrationPatience with ConsulDockerContainer with TestActorSystem with RetryPolicy with Logging {

  import scala.concurrent.ExecutionContext.Implicits.global

  def withConsulHttpClient[T](f: ConsulHttpClient => T): T = withConsulHost { (host, port) =>
    withActorSystem { implicit actorSystem =>
      val subject: ConsulHttpClient = new AkkaHttpConsulClient(new URL(s"http://$host:$port"))
      f(subject)
    }
  }

  "The AkkaHttpConsulClient" should "retrieve a single Consul service from a freshly started Consul instance" in withConsulHttpClient { subject =>
    eventually {
      subject.getService("consul").map { result =>
        result.resource should have size 1
        result.resource.head.serviceName shouldEqual "consul"
      }.futureValue
    }
  }

  it should "retrieve a single health aware Consul service from a freshly started Consul instance" in withConsulHttpClient {
    subject =>
      eventually {
        subject.getServiceHealthAware("consul").map { result =>
          result.resource should have size 1
          result.resource.head.serviceName shouldEqual "consul"
        }.futureValue
      }
  }

  it should "retrieve no unknown service from a freshly started Consul instance" in withConsulHttpClient { subject =>
    eventually {
      subject.getService("bogus").map { result =>
        logger.info(s"Index is ${result.index}")
        result.resource should have size 0
      }.futureValue
    }
  }

  it should "retrieve a single Consul service from a freshly started Consul instance and timeout after the second request if nothing changes" in withConsulHttpClient { subject =>
    eventually {
      subject.getService("consul").flatMap { result =>
        result.resource should have size 1
        result.resource.head.serviceName shouldEqual "consul"
        subject.getService("consul", None, Some(result.index), Some("500ms")).map { secondResult =>
          secondResult.resource should have size 1
          secondResult.index shouldEqual result.index
        }
      }.futureValue
    }
  }

  it should "register and deregister a new service with Consul" in withConsulHttpClient { subject =>
    subject.putService(ServiceRegistration("newservice", Some("newservice-1")))
      .futureValue should equal("newservice-1")
    subject.putService(ServiceRegistration("newservice", Some("newservice-2"), check = Some(TTLHealthCheck("2s"))))
      .futureValue should equal("newservice-2")
    eventually {
      subject.getService("newservice").map { result =>
        result.resource should have size 2
        result.resource.head.serviceName shouldEqual "newservice"
      }.futureValue
    }
    subject.deleteService("newservice-1").futureValue should equal(())
    eventually {
      subject.getService("newservice").map { result =>
        result.resource should have size 1
        result.resource.head.serviceName shouldEqual "newservice"
      }
    }
  }

  it should "retrieve a service matching tags and leave out others" in withConsulHttpClient { subject =>
    subject.putService(ServiceRegistration("newservice", Some("newservice-1"), Set("tag1", "tag2")))
      .futureValue should equal("newservice-1")
    subject.putService(ServiceRegistration("newservice", Some("newservice-2"), Set("tag2", "tag3")))
      .futureValue should equal("newservice-2")
    eventually {
      subject.getService("newservice").map { result =>
        result.resource should have size 2
        result.resource.head.serviceName shouldEqual "newservice"
      }.futureValue
      subject.getService("newservice", Some("tag2")).map { result =>
        result.resource should have size 2
        result.resource.head.serviceName shouldEqual "newservice"
      }.futureValue
      subject.getService("newservice", Some("tag3")).map { result =>
        result.resource should have size 1
        result.resource.head.serviceName shouldEqual "newservice"
        result.resource.head.serviceId shouldEqual "newservice-2"
      }.futureValue
    }
  }

  it should "register a session and get it's ID then read it back" in withConsulHttpClient { subject =>
    val id: UUID = subject.putSession(Some(SessionCreation(name = Some("MySession")))).futureValue
    subject.getSessionInfo(id).map { sessionInfo =>
      sessionInfo should be('defined)
      sessionInfo.get.id shouldEqual id
    }.futureValue
  }

  it should "get a session lock on a key/value pair and fail to get a second lock" in withConsulHttpClient { subject =>
    val id: UUID = subject.putSession(Some(SessionCreation(name = Some("MySession")))).futureValue
    val payload = """ { "name" : "test" } """.getBytes("UTF-8")
    subject.putKeyValuePair("my/key", payload, Some(AcquireSession(id))).futureValue should be(true)
    subject.putKeyValuePair("my/key", payload, Some(AcquireSession(id))).futureValue should be(false)
    subject.putKeyValuePair("my/key", payload, Some(ReleaseSession(id))).futureValue should be(true)
  }

  it should "get a session lock on a key/value pair and get a second lock after release" in withConsulHttpClient { subject =>
    val id: UUID = subject.putSession(Some(SessionCreation(name = Some("MySession")))).futureValue
    val payload = """ { "name" : "test" } """.getBytes("UTF-8")
    subject.putKeyValuePair("my/key", payload, Some(AcquireSession(id))).futureValue should be(true)
    subject.putKeyValuePair("my/key", payload, Some(ReleaseSession(id))).futureValue should be(true)
    subject.putKeyValuePair("my/key", payload, Some(AcquireSession(id))).futureValue should be(true)
    subject.putKeyValuePair("my/key", payload, Some(ReleaseSession(id))).futureValue should be(true)
  }

  it should "write a key/value pair and read it back" in withConsulHttpClient { subject =>
    val payload = """ { "name" : "test" } """.getBytes("UTF-8")
    subject.putKeyValuePair("my/key", payload).futureValue should be(true)
    val keyDataSeq = subject.getKeyValuePair("my/key").futureValue
    keyDataSeq.head.value should equal(BinaryData(payload))
  }

  it should "fail when aquiring a lock on a key with a non-existent session" in withConsulHttpClient { subject =>
    val payload = """ { "name" : "test" } """.getBytes("UTF-8")
    val nonExistentSessionId = UUID.fromString("9A3BB9C-E2E7-43DF-BFD5-845417146552")
    val result = subject.putKeyValuePair("my/key", payload, Some(AcquireSession(nonExistentSessionId))).futureValue should be(false)
  }
}
