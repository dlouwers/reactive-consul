package xebia.dockertestkit

import org.slf4j.LoggerFactory
import org.specs2.specification.BeforeAfterAll
import xebia.dockertestkit.client.Container

trait DockerContainer extends BeforeAfterAll {

  private val logger = LoggerFactory.getLogger(this.getClass)
  def image: String
  def command: Seq[String] = Seq.empty[String]

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

  val container = new Container(image, command)
}

