import cats.data.Kleisli
import cats.effect._
import org.http4s._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.dsl.impl.QueryParamDecoderMatcher

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
      implicit val blocker = Blocker.liftExecutionContext(ExecutionContext.global)
      val searchHandler = SearchHandler()
      val routes = Routes(searchHandler).map { response =>
        response.putHeaders(
          Header("Access-Control-Allow-Origin", "*")
        )
      }
      BlazeServerBuilder[IO]
        .withHttpApp(routes)
        .bindHttp(8081, "0.0.0.0")
        .serve
        .compile
        .lastOrError
  }

}

object Routes {

  val dsl = org.http4s.dsl.io

  def apply(searchHandler: SearchHandler): HttpApp[IO] = {
    import dsl._

    Kleisli {
      case GET -> Root => Ok("Success")
      case GET -> Root / "search" :? QueryParameter(query) => searchHandler(query)
      case _ => NotFound()
    }
  }

  object QueryParameter extends QueryParamDecoderMatcher[String]("query")
}
