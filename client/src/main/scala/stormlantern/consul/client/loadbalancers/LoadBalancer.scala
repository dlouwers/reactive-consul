package stormlantern.consul.client.loadbalancers

trait LoadBalancer {

  def selectConnection: Option[String]
  def connectionReturned(key: String): Unit = ()
  def connectionProviderAdded(key: String): Unit = ()
  def connectionProviderRemoved(key: String): Unit = ()
}
