package stormlantern.consul.client.dao

import spray.json._

trait ConsulHttpProtocol extends DefaultJsonProtocol {
  implicit val serviceFormat = jsonFormat(
    (node: String, address: String, serviceId: String, serviceName: String, serviceTags: Option[Set[String]], serviceAddress: String, servicePort: Int) =>
      ServiceInstance(node, address, serviceId, serviceName, serviceTags.getOrElse(Set.empty), serviceAddress, servicePort),
    "Node", "Address", "ServiceID", "ServiceName", "ServiceTags", "ServiceAddress", "ServicePort")
  implicit val httpCheckFormat = jsonFormat(HttpCheck, "HTTP", "Interval")
  implicit val scriptCheckFormat = jsonFormat(ScriptCheck, "Script", "Interval")
  implicit val ttlCheckFormat = jsonFormat(TTLCheck, "TTL")
  implicit val checkWriter = lift {
    new JsonWriter[Check] {
      override def write(obj: Check): JsValue = obj match {
        case obj: ScriptCheck => obj.toJson
        case obj: HttpCheck => obj.toJson
        case obj: TTLCheck => obj.toJson
      }
    }
  }
  implicit val serviceRegistrationFormat = jsonFormat(ServiceRegistration, "Name", "ID", "Tags", "Address", "Port", "Check")
}
