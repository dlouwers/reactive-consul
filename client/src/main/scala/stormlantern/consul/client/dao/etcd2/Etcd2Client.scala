package stormlantern.consul.client.dao.etcd2

import me.maciejb.etcd.client.EtcdClient
import stormlantern.consul.client.dao.{IndexedServiceInstances, ServiceDiscoveryClient, ServiceRegistration}

import scala.concurrent.Future

class Etcd2Client(client: EtcdClient) extends ServiceDiscoveryClient {
  override def getService(service: String, tag: Option[String], index: Option[Long], wait: Option[String], dataCenter: Option[String]): Future[IndexedServiceInstances] = {

  }

  override def putService(registration: ServiceRegistration): Future[String] = ???

  override def deleteService(serviceId: String): Future[Unit] = ???
}
