package cf.jrozen.faker.handler

import cats.effect._
import cats.implicits._
import cf.jrozen.faker.commons.web.{ServiceInfo, ServiceInfoEndpoints}
import cf.jrozen.faker.kafka.KafkaConfiguration
import cf.jrozen.faker.model.messages.Event
import cf.jrozen.faker.mongo.MongoConfig
import cf.jrozen.faker.mongo.MongoConnection.connection
import cf.jrozen.faker.mongo.repository.EndpointRepository
import cf.jrozen.faker.mongo.MongoConnection._
import fs2.Stream
import fs2.kafka.{KafkaProducer, ProducerSettings, producerStream}
import org.http4s.HttpApp
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.log4s.getLogger

object HandlerApp extends IOApp {

  private[this] val logger = getLogger

  override def run(args: List[String]): IO[ExitCode] = {
    type F[X] = IO[X]
    for {
      conf <- Stream.eval(HandlerConfig.load[F])
      _ <- Stream.eval(Sync[F].delay(logger.info(s"Config loaded: $conf")))

      producer <- kafkaProducer[F](conf)
      service = HandlerNotificationsService[F](producer, conf)

      mongoConnection <- connection[F](MongoConfig.localDefault)
      workspacesCol = mongoConnection.faker.workspaces
      endpointRepo <- Stream.eval(Sync[F].delay(EndpointRepository[F](workspacesCol)))

      app = Router(
        "/handle" -> HandlerEndpoints[F](service, endpointRepo),
        "/service" -> ServiceInfoEndpoints[F](ServiceInfo("handler"))
      ).orNotFound

      exitCode <- server[F](app)
    } yield exitCode
  }.compile.drain.as(ExitCode.Success)

  def server[F[_] : Sync : ConcurrentEffect : Timer](httpApp: HttpApp[F]): Stream[F, ExitCode] = {
    BlazeServerBuilder[F]
      .bindHttp(8888)
      .withHttpApp(httpApp)
      .serve
  }

  def kafkaProducer[F[_] : ConcurrentEffect](handlerConfig: HandlerConfig): Stream[F, KafkaProducer[F, String, Event]] = {
    val kafkaProducerSettings: ProducerSettings[String, Event] =
      KafkaConfiguration.producerSettings[Event](handlerConfig.kafka).withClientId("handler")
    producerStream[F].using(kafkaProducerSettings)
  }

}
