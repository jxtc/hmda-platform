package hmda.api.http.public

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import hmda.parser.filing.ts.TsCsvParser
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import hmda.api.http.model.public.TsValidateRequest
import hmda.api.http.codec.filing.TsCodec._
import hmda.api.http.directives.HmdaTimeDirectives
import io.circe.generic.auto._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import hmda.model.filing.ts.TransmittalSheet
import hmda.validation.engine.TsEngine._

import scala.concurrent.ExecutionContext

trait TsValidationHttpApi
    extends HmdaTimeDirectives
    with FilingValidationHttpApi {

  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  val log: LoggingAdapter
  implicit val ec: ExecutionContext
  implicit val timeout: Timeout

  //ts/parse
  val parseTsRoute =
    path("parse") {
      timedPost { _ =>
        entity(as[TsValidateRequest]) { req =>
          TsCsvParser(req.ts) match {
            case Right(ts) => complete(ToResponseMarshallable(ts))
            case Left(errors) =>
              completeWithParsingErrors(errors)
          }
        }
      } ~
        timedOptions { _ =>
          complete("OPTIONS")
        }
    }

  //ts/validate
  val validateTsRoute =
    path("validate") {
      parameters('check.as[String] ? "all") { checkType =>
        timedPost { _ =>
          entity(as[TsValidateRequest]) { req =>
            TsCsvParser(req.ts) match {
              case Right(ts) => validate(ts, checkType)
              case Left(errors) =>
                completeWithParsingErrors(errors)
            }
          }
        }
      }
    }

  private def validate(ts: TransmittalSheet, chekType: String): Route = {
    val validation: HmdaValidation[TransmittalSheet] = chekType match {
      case "all"         => checkAll(ts, ts.LEI)
      case "syntactical" => checkSyntactical(ts, ts.LEI)
      case "validity"    => checkValidity(ts, ts.LEI)
    }

    val maybeErrors = validation.leftMap(xs => xs.toList).toEither

    maybeErrors match {
      case Right(t) => complete(t)
      case Left(errors) =>
        complete(ToResponseMarshallable(aggregateErrors(errors)))
    }
  }

  def tsRoutes: Route = {
    handleRejections(corsRejectionHandler) {
      cors() {
        encodeResponse {
          pathPrefix("ts") {
            parseTsRoute ~ validateTsRoute
          }
        }
      }
    }
  }

}
