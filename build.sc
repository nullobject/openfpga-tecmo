import mill._, scalalib._

trait CommonModule extends ScalaModule { m =>
  override def scalaVersion = "2.13.8"
  override def scalacOptions = Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit",
  )
  override def ivyDeps = Agg(
    ivy"org.chipsalliance::chisel:6.2.0",
  )
  override def scalacPluginIvyDeps = Agg(
    ivy"org.chipsalliance:::chisel-plugin:6.2.0",
  )
  object test extends ScalaTests with TestModule.ScalaTest {
    override def ivyDeps = m.ivyDeps() ++ Agg(
      ivy"edu.berkeley.cs::chiseltest:6.0.0"
    )
  }
}

object arcadia extends CommonModule
object tecmo extends CommonModule {
  override def moduleDeps = Seq(arcadia)
}
