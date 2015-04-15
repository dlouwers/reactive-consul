package xebia.consul.client.helpers

import xebia.consul.client.Service

object ModelHelpers {
  def createService(name: String, port: Int = 666, node: String = "node", tags: Seq[String] = Seq.empty) = Service(
    node = node,
    address = s"${node}Address",
    serviceId = s"${name}Id",
    serviceName = name,
    serviceTags = tags,
    serviceAddress = s"${name}Address",
    servicePort = port
  )
}
