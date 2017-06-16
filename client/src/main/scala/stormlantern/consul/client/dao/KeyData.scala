package stormlantern.consul.client.dao

import java.util.UUID

/**
 * [
 * {
 * "CreateIndex": 100,
 * "ModifyIndex": 200,
 * "LockIndex": 200,
 * "Key": "zip",
 * "Flags": 0,
 * "Value": "dGVzdA==",
 * "Session": "adf4238a-882b-9ddc-4a9d-5b6758e4159e"
 * }
 * ]
 */
case class KeyData(
  key: String,
  createIndex: Long,
  modifyIndex: Long,
  lockIndex: Long,
  flags: Long,
  value: BinaryData,
  session: Option[UUID]
)
