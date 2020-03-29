import mill._, scalalib._, scalafmt.ScalafmtModule
import mill.eval.Result

object server extends Module with NativeImageModule {
  object Versions {
    val http4s = "0.21.1"
    val logstage = "0.10.2"
  }
  def ivyDeps = Agg(
    ivy"org.http4s::http4s-core:${Versions.http4s}",
    ivy"org.http4s::http4s-dsl:${Versions.http4s}",
    ivy"org.http4s::http4s-circe:${Versions.http4s}",
    ivy"org.http4s::http4s-blaze-server:${Versions.http4s}",
    ivy"com.lihaoyi::requests:0.5.1",
    ivy"io.circe::circe-generic:0.13.0",
    ivy"org.jsoup:jsoup:1.13.1",
    ivy"org.slf4j:slf4j-simple:1.7.21",
  )
}


trait Module extends ScalaModule {
  def scalaVersion = "2.13.1"
}

trait NativeImageModule extends ScalaModule {
  private def javaHome = T.input {
    T.ctx().env.get("JAVA_HOME") match {
      case Some(homePath) => Result.Success(os.Path(homePath))
      case None => Result.Failure("JAVA_HOME env variable is undefined")
    }
  }

  private def nativeImagePath = T.input {
    val path = javaHome()/"bin"/"native-image"
    if (os exists path) Result.Success(path)
    else Result.Failure(
      "native-image is not found in java home directory.\n" +
        "Make sure JAVA_HOME points to GraalVM JDK and " +
        "native-image is set up (https://www.graalvm.org/docs/reference-manual/native-image/)"
    )
  }

  def nativeImage = T {
    import ammonite.ops._
    implicit val workingDirectory = T.ctx().dest
    %%(
      nativeImagePath(),
      "-jar", assembly().path,
      "--no-fallback",
      "--initialize-at-build-time=scala.runtime.Statics$VM",
      "--enable-http",
      "--enable-https",
    )
    finalMainClass()
  }
}

object Versions {
  val `scodec-bits` = "1.1.14"
}

