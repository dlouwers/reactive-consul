package stormlantern.consul.client.loadbalancers

class RoundRobinLoadBalancer extends LoadBalancer {

  val list = new CircularLinkedHashSet[String]
  var iterator: Iterator[String] = list.iterator

  override def connectionProviderAdded(key: String): Unit = {
    list.add(key)
    iterator = list.iterator
  }

  override def connectionProviderRemoved(key: String): Unit = {
    list.remove(key)
    iterator = list.iterator
  }

  override def selectConnection: Option[String] = {
    if (iterator.hasNext) Some(iterator.next())
    else None
  }
}

