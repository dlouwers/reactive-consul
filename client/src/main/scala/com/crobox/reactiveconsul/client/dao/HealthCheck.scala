package com.crobox.reactiveconsul.client.dao

sealed trait HealthCheck
case class ScriptHealthCheck(script: String, interval: String) extends HealthCheck
case class HttpHealthCheck(http: String, interval: String) extends HealthCheck
case class TTLHealthCheck(ttl: String) extends HealthCheck
