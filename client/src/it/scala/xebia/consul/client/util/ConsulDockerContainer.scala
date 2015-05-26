package xebia.consul.client.util

import com.spotify.docker.client.messages.ContainerConfig
import xebia.dockertestkit.{ DockerClientProvider, DockerContainer }

import scala.collection.JavaConversions._

trait ConsulDockerContainer extends DockerContainer {

  def image: String = "progrium/consul"
  def command: Seq[String] = Seq("-server", "-bootstrap", "-advertise", DockerClientProvider.hostname)
  override def containerConfig = ContainerConfig.builder().image(image).cmd(command).build()

  def withConsulHost[T](f: (String, Int) => T): T = super.withDockerHost("8500/tcp")(f)
}