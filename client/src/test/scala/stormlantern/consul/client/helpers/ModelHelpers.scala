package stormlantern.consul.client.helpers

import stormlantern.consul.client.dao.ServiceInstance

object ModelHelpers {
  def createService(name: String, port: Int = 666, node: String = "node", tags: Set[String] = Set.empty) = ServiceInstance(
    node = node,
    address = s"${node}Address",
    serviceId = s"${name}Id",
    serviceName = name,
    serviceTags = tags,
    serviceAddress = s"${name}Address",
    servicePort = port
  )
}
