package xebia.consul.client

import scala.concurrent.Future

trait ConnectionObjectProvider {
  def getObject[T](host: String, port: Int): Future[T]
}

