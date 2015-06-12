package xebia.consul.client.dao

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
case class ServiceInstance(
  node: String,
  address: String,
  serviceId: String,
  serviceName: String,
  serviceTags: Option[Set[String]],
  serviceAddress: String,
  servicePort: Int)
case class IndexedServiceInstances(index: Long, resource: Set[ServiceInstance]) extends Indexed[Set[ServiceInstance]]

object IndexedServiceInstances {
  def empty = IndexedServiceInstances(0, Set.empty)
}
