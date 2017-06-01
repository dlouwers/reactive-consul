package stormlantern.consul.client

import java.net.URL
import com.spotify.dns.DnsSrvResolvers
import collection.JavaConversions._

object DNS {
  def lookup(consulAddress: String): URL = {
    val resolver = DnsSrvResolvers.newBuilder().build()
    val lookupResult = resolver.resolve(consulAddress).headOption.getOrElse(throw new RuntimeException(s"No record found for ${consulAddress}"))
    new URL(s"http://${lookupResult.host()}:${lookupResult.port()}")
  }
}