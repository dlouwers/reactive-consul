package xebia.consul.client.util

import com.spotify.docker.client.{DefaultDockerClient, DockerClient}

object DockerClientProvider {
  val client: DockerClient = DefaultDockerClient.fromEnv().build()
}
