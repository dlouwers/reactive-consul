package stormlantern.consul.client.dao

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
  tags: Set[String] = Set.empty,
  address: Option[String] = None,
  port: Option[Long] = None,
  check: Option[HealthCheck] = None)