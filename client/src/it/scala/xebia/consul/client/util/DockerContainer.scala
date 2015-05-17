package xebia.consul.client.util

import java.net.URI

import com.spotify.docker.client.messages._
import org.specs2.specification.BeforeAfterAll

import scala.collection.JavaConversions._

trait DockerContainer extends BeforeAfterAll with Logging {

  def image: String
  def command: Seq[String] = Seq.empty[String]

  sealed trait Container {
    def hostname: String
    def start(): Unit
    def start(config: HostConfig): Unit
    def stop(): Unit
    def remove(): Unit
    def mappedPort(port: String): Seq[PortBinding]
  }

  def withDockerHost[T](port: String)(f: (String, Int) => T): T = {
    // Find the random port in the network settings
    val (hostIp, hostPort) = container.mappedPort(port).headOption.map(pb => (container.hostname, pb.hostPort().toInt))
      .getOrElse(throw new IndexOutOfBoundsException(s"Cannot find mapped port $port"))
    f(hostIp, hostPort)
  }

  override def beforeAll(): Unit = container.start()

  override def afterAll(): Unit = {
    container.stop()
    container.remove()
  }

  val container = new Container {

    private val docker = DockerClientProvider.client
    private lazy val config: ContainerConfig = ContainerConfig.builder().image(image).cmd(command).build()
    private lazy val container: ContainerCreation = docker.createContainer(config)
    private def id: String = container.id()

    def hostname: String = {
      val uri = new URI(System.getenv("DOCKER_HOST"))
      uri.getScheme match {
        case "tcp" ⇒ uri.getHost
        case "unix" ⇒ "localhost"
      }
    }

    def start(): Unit = {
      // You can leave the host IP empty for the PortBinding.of first parameter
      val hostConfig = HostConfig.builder()
        .publishAllPorts(true)
        .networkMode("bridge")
        // other host config here
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
      docker.inspectContainer(id).networkSettings().ports().get(port).toSeq
    }
  }
}

