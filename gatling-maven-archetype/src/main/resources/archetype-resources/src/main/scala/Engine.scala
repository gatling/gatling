import com.excilys.ebi.gatling.app.Gatling
import scala.tools.nsc.io.File
import com.excilys.ebi.gatling.core.util.PathHelper.path2string

object Engine extends App {
	
	val packageName = "${package}"

	val url = getClass.getClassLoader.getResource("gatling.conf").getPath
	val projectDir = File(url).parents(2)

	val dataFolder = projectDir / "src/main/resources/data"
	val resultsFolder = projectDir / "target/gatling-results"
	val requestBodiesFolder = projectDir / "src/main/resources/request-bodies"
	val eclipseAssetsFolder = projectDir / "src/main/resources/assets"
	val eclipseSimulationFolder = projectDir / "src/main/scala" / packageName.replace(".", "/")

	Gatling(dataFolder, resultsFolder, requestBodiesFolder, eclipseAssetsFolder, eclipseSimulationFolder, packageName)
}