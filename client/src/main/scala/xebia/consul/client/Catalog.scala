package xebia.consul.client

import scala.concurrent.Future

trait Catalog {
  def withService[A, B](name: String)(f: A => Future[B]): Future[B]
}

case class ServiceMapping(name: String, connectionObjectProvider: ConnectionObjectProvider)