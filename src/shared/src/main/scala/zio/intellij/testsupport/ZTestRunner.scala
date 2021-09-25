package zio.intellij.testsupport

import zio.duration.Duration
import zio.intellij.testsupport.ZAnnotations.location
import zio.test._
import zio.test.environment.TestEnvironment

import java.util.concurrent.atomic.AtomicReference

object ZTestRunner {

  case class Args private (testClass: String, testMethods: List[String]) {
    def toTestArgs: TestArgs = TestArgs(testMethods, Nil, None)
  }

  object Args {

    /** Read args from file if the command line shortener (Java 9+) is enabled.
      *
      * https://blog.jetbrains.com/idea/2017/10/intellij-idea-2017-3-eap-configurable-command-line-shortener-and-more/
      */
    def readFromFile(args: Array[String]): Option[Array[String]] =
      if (args.length == 1 && args.head.startsWith("@")) {
        val f = scala.io.Source.fromFile(args.head.drop(1))
        try Some(f.getLines().toArray)
        finally f.close()
      } else None

    /** Command line arguments from IntelliJ. Can be either regular-style args: [-s testClassName ...] [-t
      * testMethodName ...] or new-style args file (passed via @filename).
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
      .lookupLoadableModuleClass(fqn, this.getClass.getClassLoader)
      .getOrElse(throw new ClassNotFoundException("failed to load object: " + fqn))
      .loadModule()
      .asInstanceOf[RunnableSpec[TestEnvironment, Any]]

  }

  def main(args: Array[String]): Unit = {
    val parsedArgs   = Args.parse(args)
    val testArgs     = parsedArgs.toTestArgs
    val specInstance = createSpec(parsedArgs)

    val spec = specInstance.aspects.foldLeft(FilteredSpec(specInstance.spec, testArgs))(_ @@ _) @@
      TestAspect.fibers @@
      ZAnnotations.timedReport

    val runner = specInstance.runner
      .withReporter(TestRunnerReporter[specInstance.Failure]())

    val _ = runner.unsafeRunSync(spec)
  }
}

object TestRunnerReporter {
  def apply[E](): TestReporter[E] =
    (_: Duration, executedSpec: ExecutedSpec[E]) => TestLogger.logLine(render(executedSpec).mkString("\n"))

  def render[E](executedSpec: ExecutedSpec[E]): Seq[String]                           = {
    val idCounter = new AtomicReference(0)

    def loop(executedSpec: ExecutedSpec[E], pid: Int, labels: List[String]): Seq[String] =
      executedSpec.caseValue match {
        case ExecutedSpec.LabeledCase(label, spec)      =>
          loop(spec, pid, label :: labels)
        case ExecutedSpec.MultipleCase(specs)           =>
          val id       = idCounter.updateAndGet(_ + 1)
          val label    = labels.reverse.mkString(" - ")
          val started  = onSuiteStarted(label, id, pid)
          val finished = onSuiteFinished(label, id)
          val rest     = specs.flatMap(loop(_, id, List.empty))
          started +: rest :+ finished
        case ExecutedSpec.TestCase(result, annotations) =>
          val id       = idCounter.updateAndGet(_ + 1)
          val results  = DefaultTestReporter.render(executedSpec, TestAnnotationRenderer.default, includeCause = true)
          val loc      = location.run(Nil, annotations).mkString
          val label    = labels.reverse.mkString(" - ")
          val started  = onTestStarted(label, id, loc, pid)
          val finished = result match {
            case Right(TestSuccess.Succeeded(_)) =>
              val timing = ZAnnotations.renderTiming.run(Nil, annotations)
              Seq(onTestFinished(label, id, timing.headOption))
            case Right(TestSuccess.Ignored)      =>
              Seq(onTestIgnored(label, id))
            case Left(_)                         =>
              Seq(onTestFailed(label, id, results.toList))
          }
          started +: finished
      }
    loop(executedSpec, 0, List.empty)
  }

  private def onSuiteStarted(label: String, id: Int, parentId: Int)                   =
    tc(s"testSuiteStarted name='${escapeString(label)}' nodeId='$id' parentNodeId='$parentId'")

  private def onSuiteFinished(label: String, id: Int)                                 =
    tc(s"testSuiteFinished name='${escapeString(label)}' nodeId='$id'")

  private def onTestStarted(label: String, id: Int, loc: String, parentId: Int)       =
    tc(
      s"testStarted name='${escapeString(label)}' nodeId='$id' parentNodeId='$parentId' " +
        s"captureStandardOutput='true' locationHint='${escapeString(loc)}'"
    )

  private def onTestFinished(label: String, id: Int, timing: Option[String])          =
    tc(
      s"testFinished name='${escapeString(label)}' nodeId='$id' duration='${timing.getOrElse("")}'"
    )

  private def onTestIgnored(label: String, id: Int)                                   =
    tc(s"testIgnored name='${escapeString(label)}' nodeId='$id'")

  private def onTestFailed(label: String, id: Int, res: List[RenderedResult[String]]) = res match {
    case r :: Nil =>
      tc(
        s"testFailed name='${escapeString(label)}' nodeId='$id' " +
          s"message='Assertion failed:' details='${escapeString(r.rendered.drop(1).mkString("\n"))}'"
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
