package stormlantern.consul.client.dao

import scala.concurrent.Future

trait ConsulHttpClient {
  def findServiceChange(service: String, tag: Option[String] = None, index: Option[Long] = None, wait: Option[String] = None, dataCenter: Option[String] = None): Future[IndexedServiceInstances]
  def registerService(registration: ServiceRegistration): Future[String]
  def deregisterService(serviceId: String): Future[Unit]
}

