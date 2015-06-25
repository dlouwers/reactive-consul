package stormlantern.consul.client.util

import com.spotify.docker.client.messages.ContainerConfig
import org.scalatest.Suite
import stormlantern.dockertestkit.{ DockerClientProvider, DockerContainer }

import scala.collection.JavaConversions._

trait ConsulDockerContainer extends DockerContainer { this: Suite =>

  def image: String = "progrium/consul"
  def command: Seq[String] = Seq("-server", "-bootstrap", "-advertise", DockerClientProvider.hostname)
  override def containerConfig = ContainerConfig.builder().image(image).cmd(command).build()

  def withConsulHost[T](f: (String, Int) => T): T = super.withDockerHost("8500/tcp")(f)
}