package scala_maven_executions;

public class LogProcessorUtils {

	public static enum Level {
		ERROR, WARNING, INFO
	}

	public static class LevelState {
		public Level level = Level.INFO;
		public String untilContains = null;
	}

	public static LevelState levelStateOf(String line, LevelState previous) throws Exception {
		LevelState back = new LevelState();
		String lineLowerCase = line.toLowerCase();
		if (lineLowerCase.indexOf("error") > -1) {
			back.level = Level.ERROR;
			if (lineLowerCase.contains(".scala")) {
				back.untilContains = "^";
			}
		} else if (lineLowerCase.indexOf("warn") > -1) {
			back.level = Level.WARNING;
			if (lineLowerCase.contains(".scala")) {
				back.untilContains = "^";
			}
		} else if (previous.untilContains != null) {
			if (!lineLowerCase.contains(previous.untilContains)) {
				back = previous;
			} else {
				back.level = previous.level;
				back.untilContains = null;
			}
		}
		return back;
	}

}
