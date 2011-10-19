package com.excilys.ebi.gatling.app.interpreter

import com.excilys.ebi.gatling.core.util.PathHelper._
import scala.tools.nsc.io.VirtualDirectory
import scala.tools.nsc.interpreter.AbstractFileClassLoader
import java.io.File
import java.io.StringWriter
import java.io.PrintWriter
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import org.joda.time.DateTime

class ScalaScriptInterpreter extends Interpreter {

	val byteCodeDir = new VirtualDirectory("memory", None)
	val classLoader = new AbstractFileClassLoader(byteCodeDir, this.getClass.getClassLoader)

	def run(fileName: String, startDate: DateTime) {
		compile(new File(GATLING_SCENARIOS_FOLDER + "/" + fileName))
		val clazz = classLoader.loadClass("com.excilys.ebi.gatling.script.Simulation")
		val runner = clazz.asInstanceOf[Class[App]].newInstance
		runner.main(Array(startDate.toString));
	}

	def compile(sourceDirectory: File): Unit = {
		// Prepare an object for collecting error messages from the compiler
		val messageCollector = new StringWriter
		val messageCollectorWrapper = new PrintWriter(messageCollector)

		// Initialize the compiler
		val settings = generateSettings()
		val reporter = new ConsoleReporter(settings, Console.in, messageCollectorWrapper)
		val compiler = new Global(settings, reporter)

		// Attempt compilation
		val files =
			if (sourceDirectory.isFile)
				sourceDirectory :: findFiles(new File(sourceDirectory.getAbsolutePath.substring(0, sourceDirectory.getAbsolutePath.length() - 6)))
			else
				findFiles(sourceDirectory)

		(new compiler.Run).compile(files.map(_.toString))

		// Bail out if compilation failed
		if (reporter.hasErrors) {
			reporter.printSummary
			messageCollectorWrapper.close
			throw new RuntimeException("Compilation failed:\n" + messageCollector.toString)
		}
	}

	private def findFiles(root: File): List[File] = {
		if (root.isFile)
			List(root)
		else
			makeList(root.listFiles).flatMap { f => findFiles(f) }
	}

	private def makeList(a: Array[File]): List[File] = {
		if (a == null)
			Nil
		else
			a.toList
	}

	private def generateSettings(): Settings = {
		val settings = new Settings
		settings.usejavacp.value = true
		settings.outputDirs.setSingleOutput(byteCodeDir)
		settings.deprecation.value = true
		settings.unchecked.value = true
		settings
	}
}