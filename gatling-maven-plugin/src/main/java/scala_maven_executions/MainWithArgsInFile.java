package scala_maven_executions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Use a file and reflection to start a main class with arguments define in a file.
 * This class should run without other dependencies than jre.
 * This class is used as a workaround to the windows command line size limitation.
 *
 * @author David Bernard
 */
public class MainWithArgsInFile {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			String mainClassName = args[0];
			List<String> argsFromFile = new ArrayList<String>();
			if (args.length > 0) {
				argsFromFile = MainHelper.readArgFile(new File(args[1]));
			}
			MainHelper.runMain(mainClassName, argsFromFile, null);
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-10000 /*Integer.MIN_VALUE*/);
		}
	}

}
