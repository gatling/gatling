/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.recorder.ui.component;

import static com.excilys.ebi.gatling.recorder.http.event.RecorderEventBus.getEventBus;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.trimToNull;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.charset.Charset;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.ebi.gatling.recorder.configuration.Configuration;
import com.excilys.ebi.gatling.recorder.configuration.ConfigurationHelper;
import com.excilys.ebi.gatling.recorder.configuration.Pattern;
import com.excilys.ebi.gatling.recorder.http.event.ShowRunningFrameEvent;
import com.excilys.ebi.gatling.recorder.ui.enumeration.FilterStrategy;

public class ConfigurationValidatorListener implements ActionListener {

	private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidatorListener.class);

	private static final java.util.regex.Pattern PACKAGE_NAME_PATTERN = java.util.regex.Pattern.compile("^([a-z_]{1}[a-z0-9_]*(\\.[a-z_]{1}[a-z0-9_]*)*)$");

	private final ConfigurationFrame frame;

	public ConfigurationValidatorListener(ConfigurationFrame frame) {
		this.frame = frame;
	}

	public void actionPerformed(ActionEvent e) {

		boolean hasError = false;
		Border defaultBorder = frame.txtProxyHost.getBorder();

		Configuration config = Configuration.getInstance();

		if (frame.txtProxyHost.getText().equals(frame.txtProxyHost.getName()))
			frame.txtProxyHost.setText(EMPTY);
		if (frame.txtProxyPort.getText().equals(frame.txtProxyPort.getName()))
			frame.txtProxyPort.setText(EMPTY);
		if (frame.txtProxySslPort.getText().equals(frame.txtProxySslPort.getName()))
			frame.txtProxySslPort.setText(EMPTY);

		frame.tblFilters.validateCells();

		// Parse local proxy port
		try {
			config.setPort(Integer.parseInt(frame.txtPort.getText()));
			frame.txtPort.setBorder(defaultBorder);
		} catch (NumberFormatException nfe) {
			logger.error("Error, while parsing proxy port...");
			frame.txtPort.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.red));
			hasError = true;
		}

		config.getProxy().setHost(StringUtils.trimToNull(frame.txtProxyHost.getText()));

		// Parse local ssl proxy port
		try {
			config.setSslPort(Integer.parseInt(frame.txtSslPort.getText()));
			frame.txtSslPort.setBorder(defaultBorder);
		} catch (NumberFormatException nfe) {
			logger.error("Error, while parsing proxy SSL port...");
			frame.txtSslPort.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.red));
			hasError = true;
		}

		config.getProxy().setHost(StringUtils.trimToNull(frame.txtProxyHost.getText()));

		// Parse outgoing proxy port
		if (!StringUtils.isEmpty(config.getProxy().getHost())) {
			try {
				config.getProxy().setPort(Integer.parseInt(frame.txtProxyPort.getText()));
				frame.txtProxyPort.setBorder(defaultBorder);
			} catch (NumberFormatException nfe) {
				logger.error("Error, while parsing outgoing proxy port... !");
				frame.txtProxyPort.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.red));
				hasError = true;
			}
		}

		// Parse outgoing ssl proxy port
		if (!StringUtils.isEmpty(config.getProxy().getHost())) {
			try {
				config.getProxy().setSslPort(Integer.parseInt(frame.txtProxySslPort.getText()));
				frame.txtProxySslPort.setBorder(defaultBorder);
			} catch (NumberFormatException nfe) {
				logger.error("Error, while parsing outgoing proxy SSL port... !");
				frame.txtProxySslPort.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.red));
				hasError = true;
			}
		}

		config.setFilterStrategy((FilterStrategy) frame.cbFilterStrategies.getSelectedItem());
		// Set urls filters into a list
		config.setPatterns(new ArrayList<Pattern>());
		for (int i = 0; i < frame.tblFilters.getRowCount(); i++)
			config.getPatterns().add((Pattern) frame.tblFilters.getPattern(i));

		// Check if a directory was entered
		config.setOutputFolder(StringUtils.trimToNull(frame.txtOutputFolder.getText()));
		if (config.getOutputFolder() == null) {
			logger.error("Error, no directory selected for results.");
			frame.txtOutputFolder.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.red));
			hasError = true;
		} else
			frame.txtOutputFolder.setBorder(defaultBorder);

		config.setSaveConfiguration(frame.chkSavePref.isSelected());

		// set selected encoding
		config.setEncoding(Charset.class.cast(frame.cbOutputEncoding.getSelectedItem()).name());

		// Set follow redirect
		config.setFollowRedirect(frame.chkFollowRedirect.isSelected());

		// set class name
		String rawClassName = StringUtils.capitalize(frame.txtSimulationClassName.getText().trim());
		frame.txtSimulationClassName.setText(rawClassName);
		if (rawClassName.length() == 0) {
			logger.error("Invalid class name");
			hasError = true;
		} else {
			config.setSimulationClassName(rawClassName);
		}

		// set package
		String rawPackage = frame.txtSimulationPackage.getText().trim().toLowerCase();
		frame.txtSimulationPackage.setText(rawPackage);
		if (!rawPackage.isEmpty() && !PACKAGE_NAME_PATTERN.matcher(rawPackage).matches()) {
			logger.error("Invalid package name");
			hasError = true;
		} else {
			config.setSimulationPackage(trimToNull(rawPackage));
		}

		if (hasError)
			return;

		if (frame.chkSavePref.isSelected())
			ConfigurationHelper.saveToDisk();

		logConfiguration();

		getEventBus().post(new ShowRunningFrameEvent());
	}

	public void logConfiguration() {
		logger.info("Configuration");
		logger.info("-------------");
		logger.info("Proxy port: " + Configuration.getInstance().getPort());
		logger.info("Proxy ssl port: " + Configuration.getInstance().getSslPort());
		if (Configuration.getInstance().getProxy().getHost() != null)
			logger.info("Outgoing proxy: " + Configuration.getInstance().getProxy());
		logger.info("Filters: " + Configuration.getInstance().getFilterStrategy());
		if (!Configuration.getInstance().getFilterStrategy().equals(FilterStrategy.NONE))
			for (Pattern pattern : Configuration.getInstance().getPatterns())
				logger.info("| - " + pattern);
		logger.info("Results: " + Configuration.getInstance().getOutputFolder());
	}
}
