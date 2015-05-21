package xebia.dockertestkit

import com.spotify.docker.client.messages.ContainerConfig
import org.slf4j.LoggerFactory
import org.specs2.specification.BeforeAfterAll
import xebia.dockertestkit.client.Container

trait DockerContainers extends BeforeAfterAll {

  private val logger = LoggerFactory.getLogger(this.getClass)
  def containerConfigs: Set[ContainerConfig]
  val containers = containerConfigs.map(new Container(_))

  def withDockerHosts[T](ports: Set[String])(f: Map[String, (String, Int)] => T): T = {
    // Find the mapped available ports in the network settings
    f(ports.zip(ports.flatMap(p => containers.map(c => c.mappedPort(p).headOption))).map {
      case (port, Some(binding)) => port -> (DockerClientProvider.hostname, binding.hostPort().toInt)
      case (port, None) => throw new IndexOutOfBoundsException(s"Cannot find mapped port $port")
    }.toMap)
  }

  override def beforeAll(): Unit = containers.foreach(_.start())

  override def afterAll(): Unit = containers.foreach { container =>
    container.stop()
    container.remove()
  }
}

