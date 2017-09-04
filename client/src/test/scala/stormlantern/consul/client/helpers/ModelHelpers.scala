package stormlantern.consul.client.helpers

import stormlantern.consul.client.dao.ServiceInstance
import stormlantern.consul.client.discovery.ServiceDefinition

object ModelHelpers {
  def createService(id: String, name: String, port: Int = 666, node: String = "node", tags: Set[String] = Set.empty) = ServiceInstance(
    node = node,
    address = s"${node}Address",
    serviceId = id,
    serviceName = name,
    serviceTags = tags,
    serviceAddress = s"${name}Address",
    servicePort = port
  )
  def createService(service: ServiceDefinition): ServiceInstance = createService(service.key, service.serviceName)
}
