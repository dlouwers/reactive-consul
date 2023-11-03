package com.crobox.reactiveconsul.client.helpers

import com.crobox.reactiveconsul.client.dao.ServiceInstance
import com.crobox.reactiveconsul.client.discovery.ServiceDefinition

object ModelHelpers {

  def createService(id: String, name: String, port: Int = 666, node: String = "node", tags: Set[String] = Set.empty) =
    ServiceInstance(
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
