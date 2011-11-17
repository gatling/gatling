package com.excilys.ebi.gatling.recorder.ui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.border.Border;

import org.apache.commons.lang.StringUtils;

import com.excilys.ebi.gatling.recorder.configuration.Configuration;
import com.excilys.ebi.gatling.recorder.configuration.ConfigurationHelper;
import com.excilys.ebi.gatling.recorder.ui.component.ConfigurationFrame;
import com.excilys.ebi.gatling.recorder.ui.component.RunningFrame;
import com.excilys.ebi.gatling.recorder.ui.enumeration.Filter;
import com.excilys.ebi.gatling.recorder.ui.enumeration.FilterType;
import com.excilys.ebi.gatling.recorder.ui.enumeration.ResultType;

public class ConfigurationValidatorListener implements ActionListener {

	private ConfigurationFrame frame;

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
				config.getResultTypes().add(ResultType.valueOf(cb.getText()));
			}
		}
		// If nothing was selected we add by default 'text'
		if (!tmp)
			config.getResultTypes().add(ResultType.Text);

		if (hasError)
			return;

		if (frame.cbSavePref.isSelected())
			ConfigurationHelper.saveToDisk(config);

		displayConfiguration(config);

		/* Hide the configuration frame and display the running frame */
		frame.setVisible(false);
		JFrame runningFrame = new RunningFrame(config);
		runningFrame.setVisible(true);
	}

	public void displayConfiguration(Configuration conf) {
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
