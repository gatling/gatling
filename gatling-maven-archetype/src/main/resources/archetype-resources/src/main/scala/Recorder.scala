import scala.tools.nsc.io.File

import com.excilys.ebi.gatling.recorder.ui.GatlingHttpProxyUI
import com.excilys.ebi.gatling.core.util.PathHelper.path2string

object Recorder extends App {

	val url = getClass.getClassLoader.getResource("gatling.conf").getPath
	val projectDir = File(url).parents(2)

	val outputFolder = projectDir / "src/main/scala" / "${package}".replace(".", "/")

	GatlingHttpProxyUI.main(Array("-scala", "-of", outputFolder, "-run", "-eclipse", "${package}"))
}