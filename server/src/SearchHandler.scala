import java.net.URLEncoder

import cats.syntax.all._
import cats.effect.{Blocker, ContextShift, IO}
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.Response
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.jsoup.Jsoup

import scala.jdk.CollectionConverters._
import scala.util.Try

trait SearchHandler {
  def apply(query: String): IO[Response[IO]]
}

object SearchHandler {

  val dsl: Http4sDsl[IO] = org.http4s.dsl.io

  def apply()(implicit blocker: Blocker, cs: ContextShift[IO]): SearchHandler = {
    import dsl._
    import encoders._

    query =>
      for {
        torrents <- searchOnRutor(query)
        response <- Ok(SearchResults(query, torrents))
      }
      yield
        response
  }

  private def searchOnRutor(query: String)(implicit blocker: Blocker, contextShift: ContextShift[IO]): IO[List[Torrent]] = {
    import RutorApi._
    for {
      html <- blocker.delay[IO, String] {
        requests.get(searchUrl(query)).text()
      }
      torrents <- extractResults(html).liftTo[IO]
    } yield torrents
  }

  case class SearchResults(query: String, results: List[Torrent])

  case class Torrent(title: String, magnet: String)

  object encoders {
    implicit val encoder0: Encoder[SearchResults] = deriveEncoder
    implicit val encoder1: Encoder[Torrent] = deriveEncoder
  }

  object RutorApi {

    private val baseUrl = "http://rutor.info:80/search"

    def searchUrl(query: String): String = {
      val queryEncoded = URLEncoder.encode(query, "UTF-8")
      s"$baseUrl/$queryEncoded"
    }

    def extractResults(html: String): Try[List[Torrent]] = Try {
      val document = Jsoup.parse(html)
      val searchResults = document.body.getElementById("index")
      searchResults
        .getElementsByTag("tr").iterator.asScala.drop(1)
        .map { element =>
          val links = element.child(1)
          val title = links.child(2).text
          val magnet = links.child(1).attr("href").trim
          Torrent(title, magnet)
        }
        .toList
    }
  }
}
