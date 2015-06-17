package xebia.consul.client

import java.net.URL

import org.scalatest._
import org.scalatest.concurrent.PatienceConfiguration.{ Interval, Timeout }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds, Span }
import retry.Success
import xebia.consul.client.dao.{ SprayConsulHttpClient, TTLCheck, ServiceRegistration, ConsulHttpClient }
import xebia.consul.client.util.{ RetryPolicy, TestActorSystem, ConsulDockerContainer, Logging }

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

  it should "register a new service with Consul" in withConsulHost { (host, port) =>
    withActorSystem { implicit actorSystem =>
      val subject: ConsulHttpClient = new SprayConsulHttpClient(new URL(s"http://$host:$port"))
      subject.registerService(ServiceRegistration("newservice", Some("newservice-1"))).map { result =>
        ()
      }.futureValue(Timeout(Span(10, Seconds)), Interval(Span(0, Seconds)))
      subject.registerService(ServiceRegistration("newservice", Some("newservice-2"), check = Some(TTLCheck("2s")))).map { result =>
        ()
      }.futureValue(Timeout(Span(10, Seconds)), Interval(Span(0, Seconds)))
      retry { () =>
        subject.findServiceChange("newservice").map { result =>
          result.resource should have size 2
          result.resource.head.serviceName shouldEqual "newservice"
        }
      }(Success[Unit](r => true), actorSystem.dispatcher)
    }
  }
}
