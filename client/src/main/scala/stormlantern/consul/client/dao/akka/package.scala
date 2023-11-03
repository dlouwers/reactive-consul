package stormlantern.consul.client.dao

import org.apache.pekko.http.scaladsl.model.{HttpHeader, HttpResponse, MediaType, StatusCode}

package object akka {
  case class ConsulResponse(status: StatusCode, contentType: MediaType, headers: Seq[HttpHeader], body: String) {}

  case class ConsulException(message: String, response: HttpResponse, status: Option[StatusCode] = None)
      extends Exception(message)

  object ConsulException {
    def apply(status: StatusCode, msg: String) = new ConsulException(msg, null, Option(status))

    def apply(msg: String) = new ConsulException(msg, null)
  }
}
