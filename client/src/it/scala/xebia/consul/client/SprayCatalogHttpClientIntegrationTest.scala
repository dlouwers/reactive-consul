package xebia.consul.client

import java.net.URL

import akka.actor.ActorSystem
import org.specs2.mutable.Specification
import xebia.consul.client.util.{ ConsulDockerContainer, DockerContainer, Logging }

import scala.concurrent.duration.Duration

class SprayCatalogHttpClientIntegrationTest extends Specification with ConsulDockerContainer with Logging {

  implicit val actorSystem = ActorSystem("test")
  import java.util.concurrent.TimeUnit._
  import scala.concurrent.ExecutionContext.Implicits.global

  // TODO: Remove the dead letter messages caused by the testing procedure
  "The SprayCatalogHttpClient" should {
    "Retrieve a single Consul service from a freshly started Consul instance" in withConsulHost { (host, port) =>
      val subject: CatalogHttpClient = new SprayCatalogHttpClient(new URL(s"http://$host:$port"))
      subject.findServiceChange("consul").map { result =>
        result.instances must have size 1
        result.instances.head.serviceName mustEqual "consul"
      }.await(retries = 0, timeout = Duration(10, SECONDS))
    }

    "Retrieve no unknown service from a freshly started Consul instance" in withConsulHost { (host, port) =>
      val subject: CatalogHttpClient = new SprayCatalogHttpClient(new URL(s"http://$host:$port"))
      subject.findServiceChange("bogus").map { result =>
        logger.info(s"Index is ${result.index}")
        result.instances must have size 0
      }.await(retries = 0, timeout = Duration(10, SECONDS))
    }

    "Retrieve a single Consul service from a freshly started Consul instance and timeout after the second request if nothing changes" in withConsulHost { (host, port) =>
      val subject: CatalogHttpClient = new SprayCatalogHttpClient(new URL(s"http://$host:$port"))
      subject.findServiceChange("consul").flatMap { result =>
        result.instances must have size 1
        result.instances.head.serviceName mustEqual "consul"
        subject.findServiceChange("consul", Some(result.index), Some("500ms")).map { secondResult =>
          secondResult.instances must have size 1
          secondResult.index mustEqual result.index
        }
      }.await(retries = 0, timeout = Duration(10, SECONDS))
    }
  }
}
