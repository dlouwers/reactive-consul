package stormlantern.consul.client.dao

case class SessionCreation(lockDelay: Option[String] = None,
  name: Option[String] = None,
  node: Option[String] = None,
  checks: Set[Check] = Set.empty,
  behavior: Option[String] = None,
  TTL: Option[String] = None)