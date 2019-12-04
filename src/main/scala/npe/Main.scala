package npe

import zio._
import zio.console._

trait Json

object Main extends App {
  def parse(s: String): Json = new Json {}

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    getStrLn.map(parse _ andThen (_.toString))
      .orDie.as(0)
}
