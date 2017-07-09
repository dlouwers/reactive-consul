package stormlantern.consul.client.dao

import scala.concurrent.Future

trait ServiceDiscoveryClient {
  def getService(
    service: String,
    tag: Option[String] = None,
    index: Option[Long] = None,
    wait: Option[String] = None,
    dataCenter: Option[String] = None
  ): Future[IndexedServiceInstances]
  def putService(registration: ServiceRegistration): Future[String]
  def deleteService(serviceId: String): Future[Unit]
}

