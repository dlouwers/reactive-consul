package xebia.consul.client

import java.net.URL

import akka.actor.ActorSystem
import org.specs2.mutable.Specification
import xebia.consul.client.util.{ DockerContainer, Logging }

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SprayCatalogHttpClientIntegrationTest extends Specification with DockerContainer with Logging {

  override def image: String = "progrium/consul"
  override def command: Seq[String] = Seq("-server", "-bootstrap")
  implicit val actorSystem = ActorSystem("test")

  "The SprayCatalogHttpClient" should {
    "Retrieve a single Consul service from a freshly started Consul instance" in withDockerHost("8500/tcp") { (host, port) =>
      val subject: CatalogHttpClient = new SprayCatalogHttpClient(new URL(s"http://$host:$port"))
      val result = Await.result(subject.findServiceChange("consul"), Duration(10, "s"))
      result.instances must have size 1
      result.instances.head.serviceName mustEqual "consul"
    }
    "Retrieve no unknown service from a freshly started Consul instance" in withDockerHost("8500/tcp") { (host, port) =>
      val subject: CatalogHttpClient = new SprayCatalogHttpClient(new URL(s"http://$host:$port"))
      val result = Await.result(subject.findServiceChange("bogus"), Duration(10, "s"))
      logger.info(s"Index is ${result.index}")
      result.instances must have size 0
    }
    "Retrieve a single Consul service from a freshly started Consul instance and timeout after the second request if nothing changes" in withDockerHost("8500/tcp") { (host, port) =>
      val subject: CatalogHttpClient = new SprayCatalogHttpClient(new URL(s"http://$host:$port"))
      val result = Await.result(subject.findServiceChange("consul"), Duration(10, "s"))
      result.instances must have size 1
      result.instances.head.serviceName mustEqual "consul"
      val timoutResult = Await.result(subject.findServiceChange("consul", Some(result.index), Some("500ms")), Duration(10, "s"))
      timoutResult.instances must have size 1
      timoutResult.index mustEqual result.index
    }
  }
}
