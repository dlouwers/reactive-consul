package xebia.consul.client

import org.specs2.execute.{Result, AsResult}
import org.specs2.mutable.Specification
import com.spotify.docker.client.{DefaultDockerClient, DockerClient}

class SprayCatalogIntegrationTest extends Specification {

  def withDockerHost[T: AsResult](image: String)(block: (String, Int) => T): Result = {
    try {
      val docker: DockerClient = DefaultDockerClient.fromEnv().build()
      AsResult(block("localhost", 222))
    } finally {
      ???
    }
  }

  "The SprayCatalog" should {
    "Retrieve a single service from a freshly started Consul instance" in withDockerHost("image") { (host, port) =>
      ???
    }
  }
}
