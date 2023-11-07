package com.crobox.reactiveconsul.client.dao

import java.util.UUID

case class SessionInfo(
  lockDelay: Long,
  checks: Set[String],
  node: String,
  id: UUID,
  createIndex: Long,
  name: Option[String],
  behavior: String,
  TTL: String
)
