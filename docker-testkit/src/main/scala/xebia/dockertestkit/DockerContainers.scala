package xebia.dockertestkit

import com.spotify.docker.client.messages.ContainerConfig
import org.slf4j.LoggerFactory
import org.specs2.specification.BeforeAfterAll
import xebia.dockertestkit.client.Container

trait DockerContainers extends BeforeAfterAll {

  private val logger = LoggerFactory.getLogger(this.getClass)
  def containerConfigs: Set[ContainerConfig]
  val containers = containerConfigs.map(new Container(_))

  def withDockerHosts[T](port: String)(f: (String, Int) => T): T = {
    // Find the mapped available port in the network settings
    val (hostIp, hostPort) = containers.head.mappedPort(port).headOption.map(pb => (containers.head.hostname, pb.hostPort().toInt))
      .getOrElse(throw new IndexOutOfBoundsException(s"Cannot find mapped port $port"))
    f(hostIp, hostPort)
  }

  override def beforeAll(): Unit = containers.foreach(_.start())

  override def afterAll(): Unit = containers.foreach { container =>
    container.stop()
    container.remove()
  }
}

