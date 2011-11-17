package com.excilys.ebi.gatling.recorder.ui.component;

import com.excilys.ebi.gatling.recorder.configuration.Configuration;

public class Controller {

	private static final Controller INSTANCE = new Controller();

	private final ConfigurationFrame configurationFrame = new ConfigurationFrame();

	private final RunningFrame runningFrame = new RunningFrame();

	private Controller() {
		throw new UnsupportedOperationException();
	}

	public static Controller getInstance() {
		return INSTANCE;
	}

	public ConfigurationFrame getConfigurationFrame() {
		return configurationFrame;
	}

	public RunningFrame getRunningFrame() {
		return runningFrame;
	}

	public void onConfigurationValidated(Configuration configuration) {
		configurationFrame.setVisible(false);
		runningFrame.start(configuration);
		runningFrame.setVisible(true);
	}

	public void onStop() {
		configurationFrame.setVisible(false);
		configurationFrame.setVisible(true);
	}
}
