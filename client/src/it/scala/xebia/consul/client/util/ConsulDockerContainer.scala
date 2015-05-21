package xebia.consul.client.util

import xebia.dockertestkit.DockerContainer

trait ConsulDockerContainer extends DockerContainer {

  override def image: String = "progrium/consul"
  override def command: Seq[String] = Seq("-server", "-bootstrap")

  def withConsulHost[T](f: (String, Int) => T): T = super.withDockerHost("8500/tcp")(f)
}