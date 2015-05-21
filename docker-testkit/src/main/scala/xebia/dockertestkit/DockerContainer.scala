package xebia.dockertestkit

import com.spotify.docker.client.messages.ContainerConfig
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

trait DockerContainer extends DockerContainers {

  private val logger = LoggerFactory.getLogger(this.getClass)
  def image: String
  def command: Seq[String] = Seq.empty[String]
  override def containerConfigs: Set[ContainerConfig] = Set(ContainerConfig.builder().image(image).cmd(command).build())

  def withDockerHost[T](port: String)(f: (String, Int) => T): T = withDockerHosts(Set(port)) { hosts =>
    val (h, p) = hosts(port)
    f(h, p)
  }

}

