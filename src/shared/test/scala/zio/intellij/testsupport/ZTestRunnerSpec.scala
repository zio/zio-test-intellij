package zio.intellij.testsupport

import java.io.File

import zio.test.Assertion._
import zio.test._
import zio.{Has, UIO, ZIO, ZLayer, ZManaged}

object ZTestRunnerSpec extends DefaultRunnableSpec {

  private def tempArgFile: ZLayer[Any, Throwable, Has[File]] = ZManaged.makeEffect {
    val tmpFile = File.createTempFile("ZTestRunnerSpec-", ".tmp")
    val writer = new java.io.PrintWriter(tmpFile)
    try {
      writer.print(
        s"""-testClassTerm
           |class
           |-testMethodTerm
           |method
           |""".stripMargin
      )
    } finally writer.close()
    tmpFile
  } { tmpFile => tmpFile.delete() }.toLayer

  override def spec: ZSpec[zio.test.environment.TestEnvironment, Any] = suite("ZTestRunnerSpec")(
    testM("read arguments from file if command line argument is a file path") {
      (for {
        argFile <- ZIO.access[Has[File]](_.get)
        args <- UIO(ZTestRunner.Args.readFromFile(Array(s"@${argFile.getAbsolutePath}")).get)
      } yield assert(args.toList)(hasSize(equalTo(4)))).provideLayer(tempArgFile)
    },
    test("ignore non-file command line argument"){
      assert(ZTestRunner.Args.readFromFile(Array("abc", "efg")))(isNone)
    }
  )
}
