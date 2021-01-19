package zio.intellij.testsupport

import zio.ZIO
import zio.duration._
import zio.test.TestAnnotationRenderer.LeafRenderer
import zio.test._
import zio.test.environment.Live

private[testsupport] object ZAnnotations {

  /**
   * Annotates tests with their execution times.
   */
  val timedReport: TestAspectAtLeastR[Live with Annotations] =
    new TestAspect.PerTest.AtLeastR[Live with Annotations] {
      def perTest[R <: Live with Annotations, E](
        test: ZIO[R, TestFailure[E], TestSuccess]
      ): ZIO[R, TestFailure[E], TestSuccess] =
        Live.withLive(test)(_.either.timed).flatMap { case (duration, result) =>
          ZIO.fromEither(result) <*
            Annotations.get(TestAnnotation.timing).flatMap { current =>
              val actualDuration = if (current.isZero) duration else current
              Annotations.annotate(timingTestReport, actualDuration)
            }
        }
    }

  private val timingTestReport: TestAnnotation[Duration] =
    TestAnnotation("intellij-timing", Duration.Zero, _ + _)

  val renderTiming: TestAnnotationRenderer =
    LeafRenderer(timingTestReport) { case child :: _ =>
      if (child.isZero) None
      else Some(s"${child.toMillis}")
    }

  val location: TestAnnotationRenderer =
    LeafRenderer(TestAnnotation.location) { case child :: _ =>
      if (child.isEmpty) None
      else child.headOption.map(s => s"file://${s.path}:${s.line}")
    }
}
