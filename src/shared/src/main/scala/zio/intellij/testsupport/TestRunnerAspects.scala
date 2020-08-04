package zio.intellij.testsupport

import zio.ZIO
import zio.duration._
import zio.test.TestAnnotationRenderer.LeafRenderer
import zio.test._
import zio.test.environment.Live

private[testsupport] object TestRunnerAspects {

  /**
   * Annotates tests with their execution times.
   */
  val timedReport: TestAspectAtLeastR[Live with Annotations] =
    new TestAspect.PerTest.AtLeastR[Live with Annotations] {
      def perTest[R <: Live with Annotations, E](
        test: ZIO[R, TestFailure[E], TestSuccess]
      ): ZIO[R, TestFailure[E], TestSuccess] =
        Live.withLive(test)(_.either.timed).flatMap {
          case (duration, result) =>
            ZIO.fromEither(result) <*
              Annotations.get(TestAnnotation.timing).flatMap { current =>
                val actualDuration = if (current.isZero) duration else current
                Annotations.annotate(timingTestReport, actualDuration)
              }
        }
    }

  /**
   * An annotation for timing the test suite (note: this is different from the 'timing' annotation
   * to prevent clashing)
   */
  private val timingTestReport: TestAnnotation[Duration] =
    TestAnnotation("intellij-timing", Duration.Zero, _ + _)

  /**
   * A test annotation renderer that renders the time taken to execute each
   * test or suite both in absolute duration and as a percentage of total
   * execution time.
   */
  val renderTiming: TestAnnotationRenderer =
    LeafRenderer(timingTestReport) {
      case child :: _ =>
        if (child.isZero) None
        else Some(s"${child.toMillis}")
    }

}
