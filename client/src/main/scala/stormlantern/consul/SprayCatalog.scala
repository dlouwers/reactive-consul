package stormlantern.consul

class SprayCatalog extends Catalog {
  override def findService(service: String, dataCenter: Option[String]): Seq[Service] = ???
}
