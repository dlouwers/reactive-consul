package xebia.dockertestkit.client

import java.net.URI

import com.spotify.docker.client.messages._
import xebia.dockertestkit.DockerClientProvider

import scala.collection.JavaConversions._

class Container(image: String, command: Seq[String]) {

  private val docker = DockerClientProvider.client
  private val config: ContainerConfig = ContainerConfig.builder().image(image).cmd(command).build()
  private lazy val container: ContainerCreation = docker.createContainer(config)
  private def id: String = container.id()

  def hostname: String = {
    val uri = new URI(System.getenv("DOCKER_HOST"))
    uri.getScheme match {
      case "tcp" ⇒ uri.getHost
      case "unix" ⇒ "localhost"
    }
  }

  def start(): Unit = {
    val hostConfig = HostConfig.builder()
      .publishAllPorts(true)
      .networkMode("bridge")
      .build()
    start(hostConfig)
  }

  def start(config: HostConfig): Unit = {
    docker.startContainer(id, config)
    val info: ContainerInfo = docker.inspectContainer(id)
    if (!info.state().running()) {
      throw new IllegalStateException("Could not start Docker container")
    }
  }

  def stop(): Unit = {
    docker.killContainer(id)
    docker.waitContainer(id)
  }

  def remove(): Unit = {
    docker.removeContainer(id)
  }

  def mappedPort(port: String): Seq[PortBinding] = {
    docker.inspectContainer(id).networkSettings().ports().get(port).toSeq
  }
}

