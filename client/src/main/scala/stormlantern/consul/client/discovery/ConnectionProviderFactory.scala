package stormlantern.consul.client.discovery

trait ConnectionProviderFactory {
  def create(host: String, port: Int): ConnectionProvider
}
