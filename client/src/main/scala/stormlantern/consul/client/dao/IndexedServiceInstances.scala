package stormlantern.consul.client.dao

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
  serviceTags: Set[String],
  serviceAddress: String,
  servicePort: Int
)

/*
 *   {
    "Node": {
      "ID": "40e4a748-2192-161a-0510-9bf59fe950b5",
      "Node": "foobar",
      "Address": "10.1.10.12",
      "Datacenter": "dc1",
      "TaggedAddresses": {
        "lan": "10.1.10.12",
        "wan": "10.1.10.12"
      },
      "Meta": {
        "instance_type": "t2.medium"
      }
    },
    "Service": {
      "ID": "redis",
      "Service": "redis",
      "Tags": ["primary"],
      "Address": "10.1.10.12",
      "Meta": {
        "redis_version": "4.0"
      },
      "Port": 8000
    },
    "Checks": [
      {
        "Node": "foobar",
        "CheckID": "service:redis",
        "Name": "Service 'redis' check",
        "Status": "passing",
        "Notes": "",
        "Output": "",
        "ServiceID": "redis",
        "ServiceName": "redis",
        "ServiceTags": ["primary"]
      },
      {
        "Node": "foobar",
        "CheckID": "serfHealth",
        "Name": "Serf Health Status",
        "Status": "passing",
        "Notes": "",
        "Output": "",
        "ServiceID": "",
        "ServiceName": "",
        "ServiceTags": []
      }
    ]
  }
 *
 * */
case class Node(node: String, address: String)
case class Service(
  id: String,
  service: String,
  tags: Set[String],
  address: String,
  port: Int)
case class HealthServiceInstance(node: Node, service: Service) {
  def asServiceInstance: ServiceInstance = {
    ServiceInstance(
      node.node,
      node.address,
      service.id,
      service.service,
      service.tags,
      service.address,
      service.port
    )
  }
}

case class IndexedServiceInstances(index: Long, resource: Set[ServiceInstance])
    extends Indexed[Set[ServiceInstance]] {
  def filterForTags(tags: Set[String]): IndexedServiceInstances = {
    this.copy(resource = resource.filter { s =>
      tags.forall(s.serviceTags.contains)
    })
  }
}

object IndexedServiceInstances {
  def empty = IndexedServiceInstances(0, Set.empty)
}
