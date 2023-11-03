package com.crobox.reactiveconsul.client.dao

import java.util.UUID

import scala.concurrent.Future

trait ConsulHttpClient {
  def getService(
    service: String,
    tag: Option[String] = None,
    index: Option[Long] = None,
    wait: Option[String] = None,
    dataCenter: Option[String] = None
  ): Future[IndexedServiceInstances]
  def getServiceHealthAware(
    service: String,
    tag: Option[String] = None,
    index: Option[Long] = None,
    wait: Option[String] = None,
    dataCenter: Option[String] = None
  ): Future[IndexedServiceInstances]
  def putService(registration: ServiceRegistration): Future[String]
  def deleteService(serviceId: String): Future[Unit]
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