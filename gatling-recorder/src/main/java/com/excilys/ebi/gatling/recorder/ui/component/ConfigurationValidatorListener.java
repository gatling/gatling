/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.recorder.ui.component;

import static com.excilys.ebi.gatling.recorder.http.event.RecorderEventBus.getEventBus;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.border.Border;

import org.apache.commons.lang.StringUtils;

import com.excilys.ebi.gatling.recorder.configuration.Configuration;
import com.excilys.ebi.gatling.recorder.configuration.ConfigurationHelper;
import com.excilys.ebi.gatling.recorder.http.event.ShowRunningFrameEvent;
import com.excilys.ebi.gatling.recorder.ui.enumeration.Filter;
import com.excilys.ebi.gatling.recorder.ui.enumeration.FilterType;
import com.excilys.ebi.gatling.recorder.ui.enumeration.ResultType;

public class ConfigurationValidatorListener implements ActionListener {

	private final ConfigurationFrame frame;

	public ConfigurationValidatorListener(ConfigurationFrame frame) {
		this.frame = frame;
	}

	public void actionPerformed(ActionEvent e) {

		boolean hasError = false;
		Border defaultBorder = frame.txtProxyHost.getBorder();

		Configuration config = new Configuration();

		// Parse local proxy port
		try {
			config.setProxyPort(Integer.parseInt(frame.txtPort.getText()));
			frame.txtPort.setBorder(defaultBorder);
		} catch (NumberFormatException nfe) {
			System.err.println("Error, while parsing proxy port...");
			frame.txtPort.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.red));
			hasError = true;
		}

		config.setOutgoingProxyHost(StringUtils.trimToNull(frame.txtProxyHost.getText()));

		// Parse outgoing proxy port
		if (!StringUtils.isEmpty(config.getOutgoingProxyHost())) {
			try {
				config.setOutgoingProxyPort(Integer.parseInt(frame.txtProxyPort.getText()));
				frame.txtProxyPort.setBorder(defaultBorder);
			} catch (NumberFormatException nfe) {
				System.err.println("Error, while parsing outgoing proxy port... !");
				frame.txtProxyPort.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.red));
				hasError = true;
			}
		}

		config.setFilter((Filter) frame.cbFilter.getSelectedItem());
		config.setFilterType((FilterType) frame.cbFilterType.getSelectedItem());
		// Set urls filters into a list
		config.setFilters(new ArrayList<String>());
		for (int i = 0; i < frame.listElements.size(); i++)
			config.getFilters().add((String) frame.listElements.get(i));

		// Check if a directory was entered
		config.setResultPath(StringUtils.trimToNull(frame.txtResultPath.getText()));
		if (config.getResultPath() == null) {
			System.err.println("Error, no directory selected for results.");
			frame.txtResultPath.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.red));
			hasError = true;
		} else
			frame.txtResultPath.setBorder(defaultBorder);

		// Set selected results type into a list
		config.setResultTypes(new ArrayList<ResultType>());
		boolean tmp = false;
		for (JCheckBox cb : frame.listResultsType) {
			if (cb.isSelected()) {
				tmp = true;
				config.getResultTypes().add(ResultType.getByLabel(cb.getText()));
			}
		}

		// If nothing was selected we add by default 'text'
		if (!tmp)
			config.getResultTypes().add(ResultType.TEXT);

		if (hasError)
			return;

		if (frame.cbSavePref.isSelected())
			ConfigurationHelper.saveToDisk(config);

		logConfiguration(config);

		getEventBus().post(new ShowRunningFrameEvent(config));
	}

	public void logConfiguration(Configuration conf) {
		System.out.println("Configuration");
		System.out.println("-------------");
		System.out.println("Proxy port: " + conf.getProxyPort());
		if (conf.getOutgoingProxyHost() != null)
			System.out.println("Outgoing proxy: " + conf.getOutgoingProxyHost() + ":" + conf.getOutgoingProxyPort());
		System.out.println("Filters: " + conf.getFilter() + "(" + conf.getFilterType() + ")");
		if (!conf.getFilterType().equals(FilterType.All))
			for (String f : conf.getFilters())
				System.out.println(" - " + f);
		System.out.println("Results: " + conf.getResultPath());
		System.out.println("Result type:");
		for (ResultType r : conf.getResultTypes())
			System.out.println(" - " + r);
		System.out.println();
	}
}
