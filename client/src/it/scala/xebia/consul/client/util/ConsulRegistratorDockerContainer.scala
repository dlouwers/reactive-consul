package xebia.consul.client.util

import com.spotify.docker.client.messages.ContainerConfig
import xebia.dockertestkit.{ DockerClientProvider, DockerContainers, DockerContainer }

import scala.collection.JavaConversions._

trait ConsulRegistratorDockerContainer extends DockerContainers {

  def consulContainerConfig = {
    val image: String = "progrium/consul"
    val command: Seq[String] = Seq("-server", "-bootstrap", "-advertise", DockerClientProvider.hostname)
    ContainerConfig.builder().image(image).cmd(command).build()
  }

  def registratorContainerConfig = {
    val hostname = DockerClientProvider.hostname
    val image: String = "progrium/registrator"
    val command: String = s"consul://$hostname:8500"
    val volume: String = "/var/run/docker.sock:/tmp/docker.sock"
    ContainerConfig.builder().image(image).cmd(command).hostname(hostname).volumes(volume).build()
  }

  override def containerConfigs = Set(consulContainerConfig, registratorContainerConfig)

  def withConsulHost[T](f: (String, Int) => T): T = super.withDockerHosts(Set("8500/tcp")) { pb =>
    val (h, p) = pb("8500/tcp")
    f(h, p)
  }
}