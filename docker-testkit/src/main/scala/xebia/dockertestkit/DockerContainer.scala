package xebia.dockertestkit

import com.spotify.docker.client.messages.ContainerConfig

trait DockerContainer extends DockerContainers {

  def containerConfig: ContainerConfig
  override def containerConfigs: Set[ContainerConfig] = Set(containerConfig)

  def withDockerHost[T](port: String)(f: (String, Int) => T): T = withDockerHosts(Set(port)) { hosts =>
    val (h, p) = hosts(port)
    f(h, p)
  }

}

