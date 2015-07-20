package stormlantern.consul.client.dao

import java.util.UUID

import org.parboiled.common.Base64
import spray.json._

import scala.util.control.NonFatal

trait ConsulHttpProtocol extends DefaultJsonProtocol {

  implicit val uuidFormat = new JsonFormat[UUID] {
    override def read(json: JsValue): UUID = json match {
      case JsString(uuid) => try {
        UUID.fromString(uuid)
      } catch {
        case NonFatal(e) => deserializationError("Expected UUID, but got " + uuid)
      }
      case x => deserializationError("Expected UUID as JsString, but got " + x)
    }

    override def write(obj: UUID): JsValue = JsString(obj.toString)
  }

  implicit val binaryDataFormat = new JsonFormat[BinaryData] {
    override def read(json: JsValue): BinaryData = json match {
      case JsString(data) => try {
        BinaryData(Base64.rfc2045().decode(data))
      } catch {
        case NonFatal(e) => deserializationError("Expected base64 encoded binary data, but got " + data)
      }
      case x => deserializationError("Expected base64 encoded binary data as JsString, but got " + x)
    }

    override def write(obj: BinaryData): JsValue = JsString(Base64.rfc2045().encodeToString(obj.data, false))
  }

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
  implicit val sessionCreationFormat = jsonFormat(SessionCreation, "LockDelay", "Name", "Node", "Checks", "Behavior", "TTL")
  implicit val keyDataFormat = jsonFormat(KeyData, "Key", "CreateIndex", "ModifyIndex", "LockIndex", "Flags", "Value", "Session")
}
