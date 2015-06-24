package xebia.dockertestkit

import java.net.URI

import com.spotify.docker.client.DockerClient.ListContainersParam
import com.spotify.docker.client.{ DefaultDockerClient, DockerClient }

import scala.collection.JavaConversions._

object DockerClientProvider {

  lazy val client: DockerClient = DefaultDockerClient.fromEnv().build()

  lazy val hostname: String = {
    val uri = new URI(System.getenv("DOCKER_HOST"))
    uri.getScheme match {
      case "tcp" ⇒ uri.getHost
      case "unix" ⇒ "localhost"
    }
  }

  def cleanUp(): Unit = {
    client.listContainers(ListContainersParam.allContainers()).foreach(c => client.removeContainer(c.id()))
  }
}
