import sbt._
import sbt.Keys._

import com.typesafe.sbt.SbtPgp.PgpKeys._
import sbtrelease.ReleaseStep
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._

object Release {

	// Index of the 'publishArtifact' release step in release process Seq
	private val releaseStepPublishIndex = 7

	lazy val settings = releaseSettings ++ Seq(
		crossBuild := false,
		releaseProcess := updateReleaseStep(releaseProcess.value, releaseStepPublishIndex, usePublishSigned)
	)

	private def updateReleaseStep(releaseProcess: Seq[ReleaseStep], releaseStepIdx : Int, update: ReleaseStep => ReleaseStep) = {
		releaseProcess.updated(releaseStepIdx, update(releaseProcess(releaseStepIdx)))
	}

	private def usePublishSigned(step: ReleaseStep) = {
		lazy val publishSignedArtifactsAction = { st: State =>
			val extracted = Project.extract(st)
			val ref = extracted.get(thisProjectRef)
			extracted.runAggregated(publishSigned in Global in ref, st)
		}

		step.copy(action = publishSignedArtifactsAction)
	}
}