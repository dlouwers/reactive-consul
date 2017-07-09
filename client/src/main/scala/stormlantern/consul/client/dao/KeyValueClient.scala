package stormlantern.consul.client.dao

import java.util.UUID

import scala.concurrent.Future

trait KeyValueClient {
  def putSession(
    sessionCreation: Option[SessionCreation] = None,
    dataCenter: Option[String] = None
  ): Future[UUID]
  def getSessionInfo(sessionId: UUID, index: Option[Long] = None, dataCenter: Option[String] = None): Future[Option[SessionInfo]]
  def putKeyValuePair(key: String, value: Array[Byte], sessionOp: Option[SessionOp] = None): Future[Boolean]
  def getKeyValuePair(
    key: String,
    index: Option[Long] = None,
    wait: Option[String] = None,
    recurse: Boolean = false,
    keysOnly: Boolean = false
  ): Future[Seq[KeyData]]
}
