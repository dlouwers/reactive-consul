package xebia.consul.client

import java.net.URL

import akka.actor.ActorSystem
import org.specs2.mutable.Specification
import xebia.consul.client.util.{DockerContainer, Logging}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SprayCatalogIntegrationTest extends Specification with DockerContainer with Logging {

  override def image: String = "progrium/consul"
  override def command: Seq[String] = Seq("-server", "-bootstrap")
  implicit val actorSystem = ActorSystem("test")

  "The SprayCatalog" should {
    "Retrieve a single service from a freshly started Consul instance" in withDockerHost("8500/tcp") { (host, port) =>
      val subject: Catalog = new SprayCatalog(new URL(s"http://$host:$port"))
      Thread.sleep(3000)
      val result = Await.result(subject.findService("consul"), Duration(10, "s"))
      logger.info(result.head.toString)
      result must have size 1
      result.head.serviceName == "consul"
    }
  }
}
