package cf.jrozen.faker.handler

import cats.effect._
import cats.implicits._
import fs2.Stream
import org.http4s.HttpApp
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

object HandlerApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    serverStream[IO].compile.drain.as(ExitCode.Success)
  }


  def serverStream[F[_] : Effect : Sync : ConcurrentEffect : Timer: ContextShift]: Stream[F, ExitCode] = {
    for {
      configs <- Stream.eval(HandlerConfig.load[F])
      service = HandlerService[F](configs)
      app = createApp[F](service)
      exitCode <- server(app)
    } yield exitCode
  }

  def server[F[_] : Sync : ConcurrentEffect : Timer](httpApp: HttpApp[F]): Stream[F, ExitCode] = {
    import scala.concurrent.duration._

    BlazeServerBuilder[F]
//      .bindHttp(8810, "localhost")
      .bindHttp(8810, "localhost")
      .withIdleTimeout(3 second)
      .withHttpApp(httpApp)
      .serve
  }

  def createApp[F[_] : Sync](handlerService: HandlerService[F]) = {
    Router {
      "/endpoint" -> HandlerEndpoints.endpoints[F](handlerService)
      //      "/service" -> //todo: create common service mapping for diagnostics
    }.orNotFound
  }
}