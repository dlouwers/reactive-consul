package stormlantern.consul.client.dao

import java.util.UUID

import spray.json._

import scala.util.control.NonFatal
import java.util.Base64

trait ConsulHttpProtocol extends DefaultJsonProtocol {

  implicit val uuidFormat = new JsonFormat[UUID] {
    override def read(json: JsValue): UUID = json match {
      case JsString(uuid) ⇒ try {
        UUID.fromString(uuid)
      } catch {
        case NonFatal(e) ⇒ deserializationError("Expected UUID, but got " + uuid)
      }
      case x ⇒ deserializationError("Expected UUID as JsString, but got " + x)
    }

    override def write(obj: UUID): JsValue = JsString(obj.toString)
  }

  implicit val binaryDataFormat = new JsonFormat[BinaryData] {
    override def read(json: JsValue): BinaryData = json match {
      case JsString(data) ⇒ try {
        BinaryData(Base64.getMimeDecoder.decode(data))
      } catch {
        case NonFatal(e) ⇒ deserializationError("Expected base64 encoded binary data, but got " + data)
      }
      case x ⇒ deserializationError("Expected base64 encoded binary data as JsString, but got " + x)
    }

    override def write(obj: BinaryData): JsValue = JsString(Base64.getMimeEncoder.encodeToString(obj.data))
  }

  implicit val serviceFormat = jsonFormat(
    (node: String, address: String, serviceId: String, serviceName: String, serviceTags: Option[Set[String]], serviceAddress: String, servicePort: Int) ⇒
      ServiceInstance(node, address, serviceId, serviceName, serviceTags.getOrElse(Set.empty), serviceAddress, servicePort),
    "Node", "Address", "ServiceID", "ServiceName", "ServiceTags", "ServiceAddress", "ServicePort"
  )
  implicit val httpCheckFormat = jsonFormat(HttpHealthCheck, "HTTP", "Interval")
  implicit val scriptCheckFormat = jsonFormat(ScriptHealthCheck, "Script", "Interval")
  implicit val ttlCheckFormat = jsonFormat(TTLHealthCheck, "TTL")
  implicit val checkWriter = lift {
    new JsonWriter[HealthCheck] {
      override def write(obj: HealthCheck): JsValue = obj match {
        case obj: ScriptHealthCheck ⇒ obj.toJson
        case obj: HttpHealthCheck   ⇒ obj.toJson
        case obj: TTLHealthCheck    ⇒ obj.toJson
      }
    }
  }
  implicit val serviceRegistrationFormat = jsonFormat(ServiceRegistration, "Name", "ID", "Tags", "Address", "Port", "Check")
  implicit val sessionCreationFormat = jsonFormat(SessionCreation, "LockDelay", "Name", "Node", "Checks", "Behavior", "TTL")
  implicit val keyDataFormat = jsonFormat(KeyData, "Key", "CreateIndex", "ModifyIndex", "LockIndex", "Flags", "Value", "Session")
  implicit val sessionInfoFormat = jsonFormat(SessionInfo, "LockDelay", "Checks", "Node", "ID", "CreateIndex", "Name", "Behavior", "TTL")
}
