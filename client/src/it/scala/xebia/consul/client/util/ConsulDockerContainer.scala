package xebia.consul.client.util

import org.specs2.execute.{Result, AsResult}

trait ConsulDockerContainer extends DockerContainer {

  override def image: String = "progrium/consul"
  override def command: Seq[String] = Seq("-server", "-bootstrap")

  def withConsulHost[T: AsResult](block: (String, Int) => T): Result = super.withDockerHost("8500/tcp")(block)
}