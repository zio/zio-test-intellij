package zio.intellij.testsupport

import java.util.concurrent.atomic.AtomicReference

import zio.duration.Duration
import zio.test._
import zio.test.environment.TestEnvironment

object ZTestRunner {

  case class Args private (testClass: String, testMethods: List[String]) {
    def toTestArgs: TestArgs = TestArgs(testMethods, Nil, None)
  }

  object Args {

    /**
     * Read args from file if the command line shortener (Java 9+) is enabled.
     *
     * https://blog.jetbrains.com/idea/2017/10/intellij-idea-2017-3-eap-configurable-command-line-shortener-and-more/
     */
    def readFromFile(args: Array[String]): Option[Array[String]] =
      if (args.length == 1 && args.head.startsWith("@")) {
        val f = scala.io.Source.fromFile(args.head.drop(1))
        try Some(f.getLines().toArray)
        finally f.close()
      } else None

    /**
     * Command line arguments from IntelliJ. Can be either regular-style args:
     * [-s testClassName ...] [-t testMethodName ...]
     * or new-style args file (passed via @filename).
     */
    def parse(args: Array[String]): Args = {
      // TODO: Add a proper command-line parser
      val parsedArgs = readFromFile(args)
        .getOrElse(args)
        .sliding(2, 2)
        .collect {
          case Array("-s", term) => ("testClassTerm", term)
          case Array("-t", term) => ("testMethodTerm", term)
        }
        .toList
        .groupBy(_._1)
        .map { case (k, v) =>
          (k, v.map(_._2))
        }

      val testClass   = parsedArgs
        .getOrElse("testClassTerm", Nil)
        .headOption
        .getOrElse {
          println(
            "Unable to find the spec class name in the command-line args.\n" +
              "Make sure at least one fully-qualified class name was passed with the -s [fqn] parameter.\n" +
              "If you believe this is a bug, please report it to the ZIO IntelliJ plugin maintainers."
          )
          sys.exit(-1)
        }
      val testMethods = parsedArgs.getOrElse("testMethodTerm", Nil)
      Args(testClass, testMethods)
    }
  }

  def createSpec(args: Args): RunnableSpec[TestEnvironment, Any] = {
    import org.portablescala.reflect._
    val fqn = args.testClass.stripSuffix("$") + "$"
    Reflect
      .lookupLoadableModuleClass(fqn)
      .getOrElse(throw new ClassNotFoundException("failed to load object: " + fqn))
      .loadModule()
      .asInstanceOf[RunnableSpec[TestEnvironment, Any]]

  }

  def main(args: Array[String]): Unit = {
    val parsedArgs   = Args.parse(args)
    val testArgs     = parsedArgs.toTestArgs
    val specInstance = createSpec(parsedArgs)
    val spec         = FilteredSpec(specInstance.spec, testArgs)

    val withTiming = spec @@ TestRunnerAspects.timedReport

    val runner = specInstance.runner
      .withReporter(TestRunnerReporter[specInstance.Failure]())

    val _ = runner.unsafeRunSync(withTiming)
  }
}

object TestRunnerReporter {
  def apply[E](): TestReporter[E] =
    (_: Duration, executedSpec: ExecutedSpec[E]) => TestLogger.logLine(render(executedSpec).mkString("\n"))

  def render[E](executedSpec: ExecutedSpec[E]): Seq[String] = {
    val idCounter = new AtomicReference(0)

    def loop(executedSpec: ExecutedSpec[E], pid: Int): Seq[String] =
      executedSpec.caseValue match {
        case ExecutedSpec.SuiteCase(label, specs)              =>
          val id       = idCounter.updateAndGet(_ + 1)
          val started  = suiteStarted(label, id, pid)
          val finished = suiteFinished(label, id)
          val rest     = specs.flatMap(loop(_, id))
          started +: rest :+ finished
        case ExecutedSpec.TestCase(label, result, annotations) =>
          val id       = idCounter.updateAndGet(_ + 1)
          val results  = DefaultTestReporter.render(executedSpec, TestAnnotationRenderer.default)
          val started  = testStarted(label, id, pid)
          val finished = result match {
            case Right(TestSuccess.Succeeded(_)) =>
              val timing = TestRunnerAspects.renderTiming.run(Nil, annotations)
              Seq(testFinished(label, id, timing.headOption))
            case Right(TestSuccess.Ignored)      =>
              Seq(testIgnored(label, id))
            case Left(_)                         =>
              Seq(testFailed(label, id, results.toList))
          }
          started +: finished
      }
    loop(executedSpec, 0)
  }

  private def suiteStarted(label: String, id: Int, parentId: Int) =
    tc(
      s"testSuiteStarted name='${escapeString(label)}' nodeId='$id' parentNodeId='$parentId' " +
        s"captureStandardOutput='false'"
    )

  private def suiteFinished(label: String, id: Int) =
    tc(s"testSuiteFinished name='${escapeString(label)}' nodeId='$id'")

  private def testStarted(label: String, id: Int, parentId: Int) =
    tc(
      s"testStarted name='${escapeString(label)}' nodeId='$id' parentNodeId='$parentId' " +
        s"captureStandardOutput='false'"
    )

  private def testFinished(label: String, id: Int, timing: Option[String]) = {
    val m = s"testFinished name='${escapeString(label)}' nodeId='$id'"
    tc(timing.fold(m)(t => m + s" duration='$t'"))
  }

  private def testIgnored(label: String, id: Int) =
    tc(s"testIgnored name='${escapeString(label)}' nodeId='$id'")

  private def testFailed(label: String, id: Int, res: List[RenderedResult[String]]) = res match {
    case r :: Nil =>
      tc(
        s"testFailed name='${escapeString(label)}' nodeId='$id' message='Assertion failed:' " +
          s"details='${escapeString(r.rendered.drop(1).mkString("\n"))}'"
      )
    case _        => tc(s"testFailed name='${escapeString(label)}' message='Assertion failed' nodeId='$id'")
  }

  def tc(message: String): String =
    s"##teamcity[$message]"

  def escapeString(str: String): String =
    str
      .replaceAll("[|]", "||")
      .replaceAll("[']", "|'")
      .replaceAll("[\n]", "|n")
      .replaceAll("[\r]", "|r")
      .replaceAll("]", "|]")
      .replaceAll("\\[", "|[")
}
