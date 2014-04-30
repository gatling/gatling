import sbt._
import sbt.Keys._

import com.typesafe.sbt.SbtPgp.PgpKeys._
import sbtrelease._
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._

object Release {

  lazy val settings = releaseSettings ++ Seq(
    crossBuild := false,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishSignedStep,
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )

  private val publishSignedStep = ReleaseStep(
    action = st => {
      val extracted = Project.extract(st)
      val ref = extracted.get(thisProjectRef)
      extracted.runAggregated(publishSigned in Global in ref, st)
    })
}
