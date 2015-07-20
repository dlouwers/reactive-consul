package stormlantern.consul.client

import java.net.URL
import java.util.UUID

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds, Span }
import retry.Success
import stormlantern.consul.client.dao._
import stormlantern.consul.client.util.{ ConsulDockerContainer, Logging, RetryPolicy, TestActorSystem }

class SprayConsulHttpClientIntegrationTest extends FlatSpec with Matchers with ScalaFutures with ConsulDockerContainer with TestActorSystem with RetryPolicy with Logging {

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val defaultPatience =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))

  "The SprayCatalogHttpClient" should "retrieve a single Consul service from a freshly started Consul instance" in withConsulHost { (host, port) =>
    withActorSystem { implicit actorSystem =>
      val subject: ConsulHttpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
      subject.findServiceChange("consul").map { result =>
        result.resource should have size 1
        result.resource.head.serviceName shouldEqual "consul"
      }.futureValue
    }
  }

  it should "retrieve no unknown service from a freshly started Consul instance" in withConsulHost { (host, port) =>
    withActorSystem { implicit actorSystem =>
      val subject: ConsulHttpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
      subject.findServiceChange("bogus").map { result =>
        logger.info(s"Index is ${result.index}")
        result.resource should have size 0
      }.futureValue
    }
  }

  it should "retrieve a single Consul service from a freshly started Consul instance and timeout after the second request if nothing changes" in withConsulHost { (host, port) =>
    withActorSystem { implicit actorSystem =>
      val subject: ConsulHttpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
      subject.findServiceChange("consul").flatMap { result =>
        result.resource should have size 1
        result.resource.head.serviceName shouldEqual "consul"
        subject.findServiceChange("consul", None, Some(result.index), Some("500ms")).map { secondResult =>
          secondResult.resource should have size 1
          secondResult.index shouldEqual result.index
        }
      }.futureValue
    }
  }

  it should "register and deregister a new service with Consul" in withConsulHost { (host, port) =>
    withActorSystem { implicit actorSystem =>
      val subject: ConsulHttpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
      subject.registerService(ServiceRegistration("newservice", Some("newservice-1")))
        .futureValue should equal("newservice-1")
      subject.registerService(ServiceRegistration("newservice", Some("newservice-2"), check = Some(TTLCheck("2s"))))
        .futureValue should equal("newservice-2")
      retry { () =>
        subject.findServiceChange("newservice").map { result =>
          result.resource should have size 2
          result.resource.head.serviceName shouldEqual "newservice"
        }
      }(Success[Unit](r => true), actorSystem.dispatcher).futureValue
      subject.deregisterService("newservice-1").futureValue should equal(())
      retry { () =>
        subject.findServiceChange("newservice").map { result =>
          result.resource should have size 1
          result.resource.head.serviceName shouldEqual "newservice"
        }
      }(Success[Unit](r => true), actorSystem.dispatcher).futureValue
    }
  }

  it should "retrieve a service matching tags and leave out others" in withConsulHost { (host, port) =>
    withActorSystem { implicit actorSystem =>
      val subject: ConsulHttpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
      subject.registerService(ServiceRegistration("newservice", Some("newservice-1"), Set("tag1", "tag2")))
        .futureValue should equal("newservice-1")
      subject.registerService(ServiceRegistration("newservice", Some("newservice-2"), Set("tag2", "tag3")))
        .futureValue should equal("newservice-2")
      retry { () =>
        subject.findServiceChange("newservice").map { result =>
          result.resource should have size 2
          result.resource.head.serviceName shouldEqual "newservice"
        }
      }(Success[Unit](r => true), actorSystem.dispatcher).futureValue
      retry { () =>
        subject.findServiceChange("newservice", Some("tag2")).map { result =>
          result.resource should have size 2
          result.resource.head.serviceName shouldEqual "newservice"
        }
      }(Success[Unit](r => true), actorSystem.dispatcher).futureValue
      retry { () =>
        subject.findServiceChange("newservice", Some("tag3")).map { result =>
          result.resource should have size 1
          result.resource.head.serviceName shouldEqual "newservice"
          result.resource.head.serviceId shouldEqual "newservice-2"
        }
      }(Success[Unit](r => true), actorSystem.dispatcher).futureValue
    }
  }

  it should "register a session and get it's ID" in withConsulHost { (host, port) =>
    withActorSystem { implicit actorSystem =>
      val subject: ConsulHttpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
      val id: UUID = subject.createSession(Some(SessionCreation(name = Some("MySession")))).futureValue

    }
  }

  it should "get a session lock on a key/value pair and fail to get a second lock" in withConsulHost { (host, port) =>
    withActorSystem { implicit actorSystem =>
      val subject: ConsulHttpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
      val id: UUID = subject.createSession(Some(SessionCreation(name = Some("MySession")))).futureValue
      val payload = """ { "name" : "test" } """.getBytes("UTF-8")
      subject.putKeyValuePair("my/key", payload, Some(AcquireSession(id))).futureValue should be(true)
      subject.putKeyValuePair("my/key", payload, Some(AcquireSession(id))).futureValue should be(false)
    }
  }
}
