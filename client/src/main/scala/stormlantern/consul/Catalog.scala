package stormlantern.consul

case class Node(dataCenter: Option[String], node: String, address: String, service: Option[Service])
case class Service(id: Option[String], name: String, tags: Iterable[String], address: Option[String], port: Option[Int])

trait Catalog {
  def register(node: Node)
}


