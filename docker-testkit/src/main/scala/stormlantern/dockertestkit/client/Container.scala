package stormlantern.dockertestkit.client

import com.spotify.docker.client.messages._
import stormlantern.dockertestkit.DockerClientProvider

import scala.collection.JavaConversions._

class Container(config: ContainerConfig) {

  private val docker = DockerClientProvider.client
  private lazy val container: ContainerCreation = docker.createContainer(config)
  private def id: String = container.id()

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
    Option(docker.inspectContainer(id).networkSettings().ports()).flatMap(pts => Option(pts.get(port))).map(_.toSeq).getOrElse(Seq.empty)
  }
}

