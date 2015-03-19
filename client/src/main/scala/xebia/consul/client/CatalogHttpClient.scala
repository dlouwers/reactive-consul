package xebia.consul.client

import scala.concurrent.Future

trait CatalogHttpClient {
  def findServiceChange(service: String, dataCenter: Option[String] = None): Future[IndexedServices]
}
//[
//  {
//    "Node": "foobar",
//    "Address": "10.1.10.12",
//    "ServiceID": "redis",
//    "ServiceName": "redis",
//    "ServiceTags": null,
//    "ServiceAddress": "",
//    "ServicePort": 8000
//  }
//]
case class Service(node: String, address: String, serviceId: String, serviceName: String, serviceTags: Seq[String], serviceAddress: String, servicePort: Int)
case class IndexedServices(index: String, instances: Seq[Service])

