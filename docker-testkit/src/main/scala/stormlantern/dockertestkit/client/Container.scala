package stormlantern.dockertestkit.client

import java.util

import com.spotify.docker.client.messages._
import stormlantern.dockertestkit.DockerClientProvider

import scala.collection.JavaConversions._

class Container(config: ContainerConfig) {

  private val docker = DockerClientProvider.client
  private lazy val container: ContainerCreation = docker.createContainer(config)
  private def id: String = container.id()

  def start(): Unit = {
    docker.startContainer(id)
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
    val ports: util.Map[String, util.List[PortBinding]] = Option(docker.inspectContainer(id).networkSettings().ports())
      .getOrElse(throw new IllegalStateException(s"No ports found for on container with id $id"))
    Option(ports.get(port)).getOrElse(throw new IllegalStateException(s"Port $port not found on caintainer with id $id"))
  }
}

