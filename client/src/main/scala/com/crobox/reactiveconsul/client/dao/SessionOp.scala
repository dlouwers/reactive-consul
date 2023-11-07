package com.crobox.reactiveconsul.client.dao

import java.util.UUID

sealed trait SessionOp
case class AcquireSession(id: UUID) extends SessionOp
case class ReleaseSession(id: UUID) extends SessionOp
