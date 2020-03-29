import java.net.URLEncoder

import cats.effect.{Blocker, ContextShift, IO}
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.{Response, Uri}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.jsoup.Jsoup

import scala.jdk.CollectionConverters._
import scala.util.Try

trait SearchHandler {
  def apply(query: String): IO[Response[IO]]
}

object SearchHandler {
  def apply()(implicit dsl: Http4sDsl[IO], blocker: Blocker, cs: ContextShift[IO]): SearchHandler = {
    import dsl._
    import encoders._

    query =>
      for {
        html <- searchRutor(query)
        results <- IO.fromTry(extractResults(html))
        response <- Ok(SearchResults(query, results))
      }
      yield
        response
  }

  private def searchRutor(query: String)(implicit blocker: Blocker, contextShift: ContextShift[IO]): IO[String] = {
    val queryEncoded = URLEncoder.encode(query, "UTF-8")
    val io = IO {
      requests.get(s"http://rutor.info:80/search/$queryEncoded").text()
    }
    blocker.blockOn(io)
  }

  private def extractResults(html: String): Try[List[SearchResult]] = Try {
    val document = Jsoup.parse(html)
    val searchResults = document.body.getElementById("index")
    searchResults
      .getElementsByTag("tr").iterator.asScala.drop(1)
      .map { element =>
        val links = element.child(1)
        val title = links.child(2).text
        val magnet = links.child(1).attr("href").trim
        SearchResult(title, magnet)
      }
      .toList
  }

  case class SearchResults(query: String, results: List[SearchResult])

  case class SearchResult(title: String, magnet: String)

  object encoders {
    implicit val encoder0: Encoder[SearchResults] = deriveEncoder
    implicit val encoder1: Encoder[SearchResult] = deriveEncoder
  }
}
