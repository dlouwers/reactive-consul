package stormlantern.consul.client

import java.net.URL

import org.scalatest._
import org.scalatest.concurrent.PatienceConfiguration.{ Interval, Timeout }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds, Span }
import retry.Success
import stormlantern.consul.client.dao.{TTLCheck, ServiceRegistration, SprayConsulHttpClient, ConsulHttpClient}
import stormlantern.consul.client.util.{Logging, RetryPolicy, TestActorSystem, ConsulDockerContainer}
import xebia.consul.client.dao.TTLCheck
import xebia.consul.client.util.TestActorSystem

class SprayCatalogHttpClientIntegrationTest extends FlatSpec with Matchers with ScalaFutures with ConsulDockerContainer with TestActorSystem with RetryPolicy with Logging {

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  // TODO: Remove the dead letter messages caused by the testing procedure
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
        subject.findServiceChange("consul", Some(result.index), Some("500ms")).map { secondResult =>
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
}
