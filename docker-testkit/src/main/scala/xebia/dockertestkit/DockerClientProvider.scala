package xebia.dockertestkit

import java.net.URI

import com.spotify.docker.client.{ DefaultDockerClient, DockerClient }

object DockerClientProvider {
  val client: DockerClient = DefaultDockerClient.fromEnv().build()
  val hostname: String = {
    val uri = new URI(System.getenv("DOCKER_HOST"))
    uri.getScheme match {
      case "tcp" ⇒ uri.getHost
      case "unix" ⇒ "localhost"
    }
  }

}
