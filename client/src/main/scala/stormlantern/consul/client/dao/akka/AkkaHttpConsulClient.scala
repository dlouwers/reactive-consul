package stormlantern.consul.client.dao.akka

import org.apache.pekko.actor.{ActorSystem, Scheduler}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.util.ByteString
import spray.json._
import stormlantern.consul.client.dao._
import stormlantern.consul.client.util.{Logging, RetryPolicy}

import java.net.URL
import java.util.UUID
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

class AkkaHttpConsulClient(host: URL)(implicit actorSystem: ActorSystem)
    extends ConsulHttpClient
    with ConsulHttpProtocol
    with RetryPolicy
    with Logging {

  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val scheduler: Scheduler                       = actorSystem.scheduler

  private val JsonMediaType = ContentTypes.`application/json`.mediaType
  private val TextMediaType = ContentTypes.`text/plain(UTF-8)`.mediaType

  //
  // Services
  // /////////////////
  def getService(service: String,
                 tag: Option[String] = None,
                 index: Option[Long] = None,
                 wait: Option[String] = None,
                 dataCenter: Option[String] = None): Future[IndexedServiceInstances] = {
    val dcParameter          = dataCenter.map(dc => s"dc=$dc")
    val waitParameter        = wait.map(w => s"wait=$w")
    val indexParameter       = index.map(i => s"index=$i")
    val tagParameter         = tag.map(t => s"tag=$t")
    val parameters           = Seq(dcParameter, tagParameter, waitParameter, indexParameter).flatten.mkString("&")
    val request: HttpRequest = HttpRequest(HttpMethods.GET).withUri(s"$host/v1/catalog/service/$service?$parameters")

    retry[IndexedServiceInstances]() {
      getResponse(request, JsonMediaType).flatMap { response =>
        validIndex(response).map { idx =>
          val services = response.body.parseJson.convertTo[Option[Set[ServiceInstance]]]
          IndexedServiceInstances(idx, services.getOrElse(Set.empty[ServiceInstance]))
        }
      }
    }
  }

  def getServiceHealthAware(service: String,
                            tag: Option[String] = None,
                            index: Option[Long] = None,
                            wait: Option[String] = None,
                            dataCenter: Option[String] = None): Future[IndexedServiceInstances] = {
    val dcParameter      = dataCenter.map(dc => s"dc=$dc")
    val waitParameter    = wait.map(w => s"wait=$w")
    val indexParameter   = index.map(i => s"index=$i")
    val tagParameter     = tag.map(t => s"tag=$t")
    val passingParameter = Some(s"passing=true")
    val parameters =
      Seq(dcParameter, tagParameter, waitParameter, indexParameter, passingParameter).flatten.mkString("&")
    val request: HttpRequest = HttpRequest(HttpMethods.GET).withUri(s"$host/v1/health/service/$service?$parameters")

    retry[IndexedServiceInstances]() {
      getResponse(request, JsonMediaType).flatMap { response =>
        validIndex(response).map { idx =>
          val services = response.body.parseJson.convertTo[Option[Set[HealthServiceInstance]]]
          IndexedServiceInstances(idx, services.getOrElse(Set.empty[HealthServiceInstance]).map(_.asServiceInstance))
        }
      }
    }
  }

  def putService(registration: ServiceRegistration): Future[String] = {
    val request = HttpRequest(HttpMethods.PUT)
      .withUri(s"$host/v1/agent/service/register")
      .withEntity(registration.toJson.asJsObject().toString.getBytes)

    retry[ConsulResponse]() {
      getResponse(request, TextMediaType)
    }.map(r => registration.id.getOrElse(registration.name))
  }

  def deleteService(serviceId: String): Future[Unit] = {
    val request = HttpRequest(HttpMethods.PUT).withUri(s"$host/v1/agent/service/deregister/$serviceId")

    retry[ConsulResponse]() {
      getResponse(request, TextMediaType)
    }.map(r => ())
  }

  //
  // Sessions
  // /////////////////
  def putSession(sessionCreation: Option[SessionCreation], dataCenter: Option[String]): Future[UUID] = {
    val dcParameter = dataCenter.map(dc => s"dc=$dc")
    val parameters  = Seq(dcParameter).flatten.mkString("&")
    val request = sessionCreation.map(_.toJson.asJsObject.toString.getBytes) match {
      case None => HttpRequest(HttpMethods.PUT).withUri(s"$host/v1/session/create?$parameters")
      case Some(entity) =>
        HttpRequest(HttpMethods.PUT).withUri(s"$host/v1/session/create?$parameters").withEntity(entity)
    }

    retry[UUID]() {
      getResponse(request, JsonMediaType).map { response =>
        response.body.parseJson.asJsObject.fields("ID").convertTo[UUID]
      }
    }
  }

  def getSessionInfo(sessionId: UUID, index: Option[Long], dataCenter: Option[String]): Future[Option[SessionInfo]] = {
    val dcParameter    = dataCenter.map(dc => s"dc=$dc")
    val indexParameter = index.map(i => s"index=$i")
    val parameters     = Seq(dcParameter, indexParameter).flatten.mkString("&")
    val request        = HttpRequest(HttpMethods.GET).withUri(s"$host/v1/session/info/$sessionId?$parameters")

    retry[Option[SessionInfo]]() {
      getResponse(request, JsonMediaType).map { response =>
        response.body.parseJson.convertTo[Option[Set[SessionInfo]]].getOrElse(Set.empty).headOption
      }
    }
  }

  //
  // Key Values
  // /////////////////
  def putKeyValuePair(key: String, value: Array[Byte], sessionOp: Option[SessionOp]): Future[Boolean] = {
    import StatusCodes._

    val opParameter = sessionOp.map {
      case AcquireSession(id) => s"acquire=$id"
      case ReleaseSession(id) => s"release=$id"
    }
    val parameters = opParameter.getOrElse("")
    val request    = HttpRequest(HttpMethods.PUT).withUri(s"$host/v1/kv/$key?$parameters").withEntity(value)

    def validator(response: HttpResponse): Boolean =
      response.status.isSuccess() || response.status == InternalServerError

    retry[Boolean]() {
      getResponse(request, JsonMediaType, validator).flatMap {
        case ConsulResponse(OK, _, body)                               => Future successful Option(body.toBoolean).getOrElse(false)
        case ConsulResponse(InternalServerError, _, "Invalid session") => Future successful false
        case ConsulResponse(status, _, body) =>
          Future failed new Exception(s"Request returned status code $status - $body")
      }
    }
  }

  def getKeyValuePair(key: String,
                      index: Option[Long],
                      wait: Option[String],
                      recurse: Boolean,
                      keysOnly: Boolean): Future[Seq[KeyData]] = {

    val waitParameter     = wait.map(p => s"wait=$p")
    val indexParameter    = index.map(p => s"index=$p")
    val recurseParameter  = if (recurse) Some("recurse") else None
    val keysOnlyParameter = if (keysOnly) Some("keys") else None
    val parameters        = Seq(indexParameter, waitParameter, recurseParameter, keysOnlyParameter).flatten.mkString("&")
    val request           = HttpRequest(HttpMethods.GET).withUri(s"$host/v1/kv/$key?$parameters")

    retry[Seq[KeyData]]() {
      getResponse(request, JsonMediaType, _ => true).map { response =>
        if (response.status == StatusCodes.NotFound) {
          Seq.empty
        } else {
          response.body.parseJson.convertTo[Seq[KeyData]]
        }
      }
    }
  }

  //
  // Internal Helpers
  // //////////////////////////
  private def getResponse[T, U](
      request: HttpRequest,
      expectedMediaType: MediaType,
      validator: HttpResponse => Boolean = (in) => in.status.isSuccess()
  ): Future[ConsulResponse] = {

    def validStatus(response: HttpResponse): Future[HttpResponse] =
      if (validator(response)) {
        Future successful response
      } else {
        parseBody(response).flatMap { body =>
          Future failed ConsulException(s"Bad status code: ${response.status.intValue()} with body $body")
        }
      }

    //
    //  Consul does not return the Charset with the Response Content Type, so just MediaType comparison
    //  Furthermore, when an error is returned the content type is text/plain, thank you HashiCorp ...
    // /////////////////////
    def validContentType(resp: HttpResponse): Future[HttpResponse] = {
      val expected = resp.status match {
        case st if st.isSuccess()     => expectedMediaType
        case st if st.isFailure()     => TextMediaType
        case st if st.isRedirection() => TextMediaType // this is a guess
      }

      if (resp.entity.contentType.mediaType == expected) {
        Future successful resp
      } else {
        Future failed ConsulException(
          resp.status,
          s"Unexpected content type: ${resp.entity.contentType}, expected $expectedMediaType"
        )
      }
    }

    def parseBody(response: HttpResponse): Future[String] =
      response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(_.utf8String)

    // make the call
    Http()
      .singleRequest(request)
      .flatMap(validStatus)
      .flatMap(validContenType)
      .flatMap { response: HttpResponse =>
        parseBody(response).map { body: String =>
          ConsulResponse(response.status, response.headers, body)
        }
      }
  }

  private def validIndex(response: ConsulResponse): Future[Long] =
    response.headers.find(_.name() == "X-Consul-Index") match {
      case None => Future failed ConsulException("X-Consul-Index header not found")
      case Some(hdr) =>
        Try(hdr.value.toLong) match {
          case Success(idx) => Future successful idx
          case Failure(ex)  => Future failed ConsulException("X-Consul-Index header was not numeric")
        }
    }
}

//
// Internal Objects
// //////////////////////////
case class ConsulResponse(status: StatusCode, headers: Seq[HttpHeader], body: String)

case class ConsulException(message: String, response: HttpResponse, status: Option[StatusCode] = None)
    extends Exception(message)

object ConsulException {
  def apply(status: StatusCode, msg: String) = new ConsulException(msg, null, Option(status)) // I feel dirty after this
  def apply(msg: String)                     = new ConsulException(msg, null)                 // I feel dirty after this
}
