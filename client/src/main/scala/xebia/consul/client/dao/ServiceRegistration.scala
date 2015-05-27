package xebia.consul.client.dao

//{
//  "ID": "redis1",
//  "Name": "redis",
//  "Tags": [
//    "master",
//    "v1"
//  ],
//  "Address": "127.0.0.1",
//  "Port": 8000,
//  "Check": {
//    "Script": "/usr/local/bin/check_redis.py",
//    "HTTP": "http://localhost:5000/health",
//    "Interval": "10s",
//    "TTL": "15s"
//  }
//}
case class ServiceRegistration(
  name: String,
  id: Option[String] = None,
  tags: Seq[String] = Seq.empty,
  address: Option[String] = None,
  port: Option[Long] = None,
  check: Option[Check] = None)

sealed trait Check
case class ScriptCheck(script: String, interval: String) extends Check
case class HttpCheck(http: String, interval: String) extends Check
case class TTLCheck(ttl: String) extends Check