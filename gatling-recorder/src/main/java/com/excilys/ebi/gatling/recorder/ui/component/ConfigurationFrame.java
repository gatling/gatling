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

import static org.apache.commons.lang.StringUtils.EMPTY;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.ebi.gatling.recorder.configuration.Configuration;
import com.excilys.ebi.gatling.recorder.configuration.Pattern;
import com.excilys.ebi.gatling.recorder.http.event.ShowConfigurationFrameEvent;
import com.excilys.ebi.gatling.recorder.http.event.ShowRunningFrameEvent;
import com.excilys.ebi.gatling.recorder.ui.enumeration.FilterType;
import com.excilys.ebi.gatling.recorder.ui.enumeration.PatternType;
import com.excilys.ebi.gatling.recorder.ui.enumeration.ResultType;
import com.google.common.eventbus.Subscribe;

@SuppressWarnings("serial")
public class ConfigurationFrame extends JFrame {
	
	public static final Logger logger = LoggerFactory.getLogger(ConfigurationFrame.class);

	public final JTextField txtPort = new JTextField("8000", 4);
	public final JTextField txtSslPort = new JTextField("8001", 4);

	public final JTextField txtProxyHost = new JTextField("address", 10);
	public final JTextField txtProxyPort = new JTextField("port", 4);
	public final JTextField txtProxySslPort = new JTextField("port", 4);

	public final JComboBox cbFilter = new JComboBox();
	public final JComboBox cbFilterType = new JComboBox();
	public final JTextField txtResultPath = new JTextField(40);
	public final List<JCheckBox> listResultsType = new ArrayList<JCheckBox>();
	public final JCheckBox cbSavePref = new JCheckBox("Save preferences");
	public final FilterTable panelFilters = new FilterTable();

	public ConfigurationFrame() {

		/* Initialization of the frame */
		setTitle("Proxy configuration");
		setLayout(new BorderLayout());
		setMinimumSize(new Dimension(800, 640));
		setResizable(true);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		/* Declaration of components */

		txtProxyHost.setName("address");
		txtProxyPort.setName("port");
		txtProxySslPort.setName("port");
		JButton btnFiltersAdd = new JButton("+");
		JButton btnFiltersDel = new JButton("-");
		JButton btnPathResults = new JButton("Browse");
		final List<JRadioButton> listBtnFilters = new ArrayList<JRadioButton>();
		ButtonGroup group = new ButtonGroup();
		JRadioButton rdBtn = null;
		for (PatternType f : PatternType.values()) {
			rdBtn = new JRadioButton(f.name());
			listBtnFilters.add(rdBtn);
			group.add(rdBtn);
		}
		listBtnFilters.get(0).setSelected(true);
		for (FilterType ft : FilterType.values())
			cbFilterType.addItem(ft);
		for (ResultType rt : ResultType.values())
			listResultsType.add(new JCheckBox(rt.getLabel()));
		cbSavePref.setHorizontalTextPosition(SwingConstants.LEFT);
		JButton btnClear = new JButton("Clear");
		JButton btnStart = new JButton("Start !");

		/* Initialization of components */
		populateItemsFromConfiguration();

		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel centerPanel = new JPanel();
		JPanel bottomPanel = new JPanel();

		JPanel centerBottomPanel = new JPanel();

		this.add(topPanel, BorderLayout.NORTH);
		this.add(centerPanel, BorderLayout.CENTER);
		this.add(bottomPanel, BorderLayout.SOUTH);

		topPanel.setBorder(BorderFactory.createTitledBorder("Network"));
		bottomPanel.setBorder(BorderFactory.createTitledBorder("Results"));

		topPanel.setLayout(new GridLayout(2, 3));
		bottomPanel.setLayout(new GridLayout(3, 1));
		centerPanel.setLayout(new BorderLayout());

		topPanel.add(new JLabel("Listening port *"));
		topPanel.add(new JLabel("Localhost"));

		JPanel top1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top1.add(new JLabel("HTTP"));
		top1.add(txtPort);
		top1.add(new JLabel("HTTPS"));
		top1.add(txtSslPort);
		topPanel.add(top1);

		topPanel.add(new JLabel("Outgoing proxy"));

		JPanel top3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top3.add(new JLabel("Host"));
		top3.add(txtProxyHost);
		topPanel.add(top3);

		JPanel top4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top4.add(new JLabel("HTTP"));
		top4.add(txtProxyPort);
		top4.add(new JLabel("HTTPS"));
		top4.add(txtProxySslPort);
		topPanel.add(top4);

		centerBottomPanel.add(new JLabel("Apply method"));
		centerBottomPanel.add(cbFilterType);
		centerBottomPanel.add(btnFiltersAdd);
		centerBottomPanel.add(btnFiltersDel);
		centerBottomPanel.add(btnClear);

		centerPanel.add(panelFilters, BorderLayout.CENTER);
		centerPanel.add(centerBottomPanel, BorderLayout.PAGE_END);

		JPanel top5 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top5.add(new JLabel("Directory *  "));
		top5.add(txtResultPath);
		top5.add(btnPathResults);
		bottomPanel.add(top5);

		JPanel top6 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top6.add(new JLabel("Type"));
		for (JCheckBox cb : listResultsType) {
			top6.add(cb);
		}
		bottomPanel.add(top6);

		JPanel top7 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		top7.add(cbSavePref);
		top7.add(btnStart);
		bottomPanel.add(top7);

		/* Listeners */

		txtProxyHost.addFocusListener(new CustomFocusListener());
		txtProxyPort.addFocusListener(new CustomFocusListener());
		txtProxySslPort.addFocusListener(new CustomFocusListener());

		cbFilterType.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				// Switch to '1' when goes active state and '2' for passive
				// state.
				if (e.getStateChange() == 1 && e.getItem().equals(FilterType.ALL))
					panelFilters.setEnabled(false);
				else
					panelFilters.setEnabled(true);
			}
		});

		btnFiltersAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panelFilters.addRow();
			}
		});

		btnFiltersDel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panelFilters.removeSelectedRow();
			}
		});

		btnPathResults.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

				int choice = fc.showOpenDialog(null);

				if (choice != JFileChooser.APPROVE_OPTION)
					return;

				File chosenFile = fc.getSelectedFile();
				txtResultPath.setText(chosenFile.getPath());
			}
		});

		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				txtPort.setText(EMPTY);
				txtSslPort.setText(EMPTY);
				txtProxyHost.setText(txtProxyHost.getName());
				txtProxyPort.setText(txtProxyPort.getName());
				txtProxySslPort.setText(txtProxySslPort.getName());
				panelFilters.removeAllElements();
				txtResultPath.setText(EMPTY);
				for (JCheckBox cb : listResultsType)
					cb.setSelected(false);
			}
		});

		btnStart.addActionListener(new ConfigurationValidatorListener(this));
	}

	@Subscribe
	public void onShowConfigurationFrameEvent(ShowConfigurationFrameEvent event) {
		setVisible(true);
	}

	@Subscribe
	public void onShowRunningFrameEvent(ShowRunningFrameEvent event) {
		setVisible(false);
	}

	private void populateItemsFromConfiguration() {
		logger.debug("Configuration: {}", Configuration.getInstance());
		txtPort.setText(String.valueOf(Configuration.getInstance().getPort()));
		txtSslPort.setText(String.valueOf(Configuration.getInstance().getSslPort()));
		txtProxyHost.setText(Configuration.getInstance().getProxy().getHost());
		txtProxyPort.setText(String.valueOf(Configuration.getInstance().getProxy().getPort()));
		txtProxySslPort.setText(String.valueOf(Configuration.getInstance().getProxy().getSslPort()));
		cbFilterType.setSelectedItem(Configuration.getInstance().getFilterType());
		for (Pattern pattern : Configuration.getInstance().getPatterns())
			panelFilters.addRow(pattern);
		txtResultPath.setText(Configuration.getInstance().getResultPath());
		cbSavePref.setSelected(Configuration.getInstance().isSaveConfiguration());
		for (JCheckBox cb : listResultsType)
			for (ResultType resultType : Configuration.getInstance().getResultTypes())
				if (cb.getText().equals(resultType.getLabel()))
					cb.setSelected(true);
	}
}

class CustomFocusListener implements FocusListener {

	@Override
	public void focusGained(FocusEvent e) {
		JTextField src = (JTextField) e.getSource();
		if (src.getText().equals(src.getName()))
			src.setText(EMPTY);
	}

	@Override
	public void focusLost(FocusEvent e) {
		JTextField src = (JTextField) e.getSource();
		if (src.getText().equals(EMPTY))
			src.setText(src.getName());
	}
}
