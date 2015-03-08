package stormlantern.consul

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

trait Catalog {
  def findService(service: String, dataCenter: Option[String] = None): Seq[Service]
}


