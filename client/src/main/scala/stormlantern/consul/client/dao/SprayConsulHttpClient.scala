package stormlantern.consul.client.dao

import java.io.IOException
import java.net.URL
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import retry.Success
import spray.json._
import stormlantern.consul.client.util.{Logging, RetryPolicy}

import scala.concurrent.Future
import scala.util.Try

class SprayConsulHttpClient(host: URL)(implicit actorSystem: ActorSystem) extends ConsulHttpClient
    with ConsulHttpProtocol with RetryPolicy with Logging with SprayJsonSupport {

  type ConnectionFlow = Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]]

  implicit val executionContext = actorSystem.dispatcher
  implicit val materializer: Materializer = ActorMaterializer()

  implicit val format = new RootJsonReader[Option[Set[ServiceInstance]]] {
    override def read(json: JsValue): Option[Set[ServiceInstance]] = json.convertTo[Option[Set[ServiceInstance]]]
  }

  def createConnection(): Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] = {
    Http().outgoingConnection(host.getHost, host.getPort)
  }

  def sendRequest(request: HttpRequest)(implicit flow: ConnectionFlow): Future[HttpResponse] = {
    Source.single(request)
      .via(flow)
      .runWith(Sink.head)
  }

  def extractIndex[T](response: HttpResponse)(f: Long => T): T = {
    response.headers.find(h => h.name == "X-Consul-Index").map { idx =>
      f(Try(idx.value().toLong).getOrElse(throw new RuntimeException("X-Consul-Index header needs to be numerical")))
    }.getOrElse(throw new RuntimeException("X-Consul-Index header not found"))
  }

  def unmarshall[T: RootJsonReader](response: HttpResponse): Future[T] = Unmarshal(response.entity).to[T]

  def getService(
    service: String,
    tag: Option[String] = None,
    index: Option[Long] = None,
    wait: Option[String] = None,
    dataCenter: Option[String] = None): Future[IndexedServiceInstances] = {
    implicit val connectionFlow = createConnection()
    val dcParameter = dataCenter.map(dc => s"dc=$dc")
    val waitParameter = wait.map(w => s"wait=$w")
    val indexParameter = index.map(i => s"index=$i")
    val tagParameter = tag.map(t => s"tag=$t")
    val parameters = Seq(dcParameter, tagParameter, waitParameter, indexParameter).flatten.mkString("&")
    implicit val success = Success[IndexedServiceInstances](r => true)
    retry {
      sendRequest(RequestBuilding.Get(s"$host/v1/catalog/service/$service?$parameters")).flatMap { response =>
        response.status match {
          case StatusCodes.OK => extractIndex(response) { idx =>
            unmarshall[Option[Set[ServiceInstance]]](response).map {
              _.map { v =>
                IndexedServiceInstances(idx, v)
              }.getOrElse(IndexedServiceInstances(idx, Set.empty[ServiceInstance]))
            }
          }
          case _ => Unmarshal(response.entity).to[String].flatMap(s => Future.failed(new IOException(s)))
        }
      }
    }
  }

  override def putService(registration: ServiceRegistration): Future[String] = {
    val request = RequestBuilding.Put(s"$host/v1/agent/service/register", registration.toJson.asJsObject())
    implicit val connectionFlow = createConnection()
    implicit val success = Success[HttpResponse](r => r.status.isSuccess())
    retry {
      sendRequest(request)
    }.map(r => registration.id.getOrElse(registration.name))
  }

  override def deleteService(serviceId: String): Future[Unit] = {
    implicit val connectionFlow = createConnection()
    val request = RequestBuilding.Delete(s"$host/v1/agent/service/deregister/$serviceId")
    implicit val success = Success[HttpResponse](r => r.status.isSuccess())
    retry {
      sendRequest(request)
    }.map(r => ())
  }

  override def putSession(sessionCreation: Option[SessionCreation] = None, dataCenter: Option[String] = None): Future[UUID] = {
    implicit val connectionFlow = createConnection()
    val dcParameter = dataCenter.map(dc => s"dc=$dc")
    val parameters = Seq(dcParameter).flatten.mkString("&")
    val request = RequestBuilding.Put(s"$host/v1/session/create?$parameters", sessionCreation.map(_.toJson.asJsObject))
    implicit val success = Success[HttpResponse](r => r.status.isSuccess())
    retry {
      sendRequest(request)
    }.flatMap(r => Unmarshal(r.entity).to[String].map(_.parseJson.asJsObject.fields("ID").convertTo[UUID]))
  }

  override def getSessionInfo(sessionId: UUID, index: Option[Long], dataCenter: Option[String] = None): Future[Option[SessionInfo]] = {
    implicit val connectionFlow = createConnection()
    val dcParameter = dataCenter.map(dc => s"dc=$dc")
    val indexParameter = index.map(i => s"index=$i")
    val parameters = Seq(dcParameter, indexParameter).flatten.mkString("&")
    val request = RequestBuilding.Get(s"$host/v1/session/info/$sessionId?$parameters")
    implicit val success = Success[HttpResponse](r => r.status.isSuccess())
    retry {
      sendRequest(request)
    }.flatMap(r => Unmarshal(r.entity).to[String].map(_.parseJson.convertTo[Option[Set[SessionInfo]]].getOrElse(Set.empty).headOption))
  }

  override def putKeyValuePair(key: String, value: Array[Byte], sessionOp: Option[SessionOp] = None): Future[Boolean] = {
    implicit val connectionFlow = createConnection()
    val opParameter = sessionOp.map {
      case AcquireSession(id) => s"acquire=$id"
      case ReleaseSession(id) => s"release=$id"
    }
    val parameters = Seq(opParameter).flatten.mkString("&")
    val request = RequestBuilding.Put(s"$host/v1/kv/$key?$parameters", HttpEntity(value))
    implicit val success = Success[HttpResponse] { r => true }
    retry {
      sendRequest(request)
    }.flatMap { r =>
      Unmarshal(r.entity).to[String].map { s =>
        if (r.status.isSuccess()) {
          s.toBoolean
        } else if (r.status == StatusCodes.InternalServerError && s == "Invalid session") {
          false
        } else {
          throw new IllegalArgumentException(s)
        }
      }
    }
  }

  override def getKeyValuePair(
    key: String,
    index: Option[Long] = None,
    wait: Option[String] = None,
    recurse: Boolean = false,
    keysOnly: Boolean = false): Future[Seq[KeyData]] = {
    implicit val connectionFlow = createConnection()
    val waitParameter = wait.map(p => s"wait=$p")
    val indexParameter = index.map(p => s"index=$p")
    val recurseParameter = if (recurse) Some("recurse") else None
    val keysOnlyParameter = if (keysOnly) Some("keys") else None
    val parameters = Seq(indexParameter, waitParameter, recurseParameter, keysOnlyParameter).flatten.mkString("&")
    val request = RequestBuilding.Get(s"$host/v1/kv/$key?$parameters")
    implicit val success = Success[Seq[KeyData]](r => true)
    retry {
      sendRequest(request).flatMap(unmarshall[Seq[KeyData]])
    }
  }
}
