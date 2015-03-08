package xebia.consul.client

class SprayCatalog extends Catalog {
  override def findService(service: String, dataCenter: Option[String]): Seq[Service] = ???
}
