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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.excilys.ebi.gatling.recorder.configuration.Configuration;
import com.excilys.ebi.gatling.recorder.configuration.ConfigurationHelper;
import com.excilys.ebi.gatling.recorder.http.event.ShowConfigurationFrameEvent;
import com.excilys.ebi.gatling.recorder.http.event.ShowRunningFrameEvent;
import com.excilys.ebi.gatling.recorder.ui.enumeration.Filter;
import com.excilys.ebi.gatling.recorder.ui.enumeration.FilterType;
import com.excilys.ebi.gatling.recorder.ui.enumeration.ResultType;
import com.google.common.eventbus.Subscribe;

@SuppressWarnings("serial")
public class ConfigurationFrame extends JFrame {

	public final JTextField txtPort = new JTextField(4);
	public final JTextField txtProxyHost = new JTextField(10);
	public final JTextField txtProxyPort = new JTextField(4);
	public final JComboBox cbFilter = new JComboBox();
	public final JComboBox cbFilterType = new JComboBox();
	public final DefaultListModel listElements = new DefaultListModel();
	public final JList listFilters = new JList(listElements);
	public final JTextField txtResultPath = new JTextField(15);
	public final List<JCheckBox> listResultsType = new ArrayList<JCheckBox>();
	public final JCheckBox cbSavePref = new JCheckBox("Save preferences");

	public ConfigurationFrame() {

		/* Initialization of the frame */
		setTitle("Proxy configuration");
		setLayout(new GridBagLayout());
		setMinimumSize(new Dimension(550, 400));
		setResizable(false);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		/* Declaration of components */
		JScrollPane panelFilters = new JScrollPane(listFilters);
		panelFilters.setPreferredSize(new Dimension(180, 80));
		final JTextField txtFilters = new JTextField(10);
		JButton btnFiltersAdd = new JButton("+");
		JButton btnFiltersDel = new JButton("-");
		JButton btnPathResults = new JButton("Browse");
		for (Filter f : Filter.values())
			cbFilter.addItem(f);
		for (FilterType ft : FilterType.values())
			cbFilterType.addItem(ft);
		for (ResultType rt : ResultType.values())
			listResultsType.add(new JCheckBox(rt.getLabel()));
		cbSavePref.setHorizontalTextPosition(SwingConstants.LEFT);
		JButton btnClear = new JButton("Clear");
		JButton btnStart = new JButton("Start !");

		createPopupMenu();

		/* Initialization of components */
		Configuration config = ConfigurationHelper.readFromDisk();
		if (config != null)
			populateItemsFromConfiguration(config);

		/* Layout */
		GridBagConstraints gbc = new GridBagConstraints();

		// 1st column
		gbc.gridx = gbc.gridy = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.insets = new Insets(10, 5, 0, 0);
		add(new JLabel("<html><u><i>Configuration</i></u></html>"), gbc);

		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.LINE_END;
		add(new JLabel("Proxy port :"), gbc);

		gbc.gridy = 2;
		add(new JLabel("Outgoing proxy :"), gbc);

		gbc.gridy = 3;
		add(new JLabel("URL filters :"), gbc);

		gbc.gridy = 4;
		gbc.gridwidth = 1;
		add(txtFilters, gbc);

		gbc.gridy = 6;
		gbc.gridwidth = 2;
		add(new JLabel("Results :"), gbc);

		gbc.gridy = 7;
		add(new JLabel("Results type: "), gbc);

		gbc.gridy = 8;
		add(cbSavePref, gbc);

		// 2nd column
		gbc.gridx = 1;
		gbc.gridy = 4;
		gbc.gridwidth = 1;
		add(btnFiltersAdd, gbc);

		// 3rd column
		gbc.gridx = 2;
		gbc.gridy = 2;
		add(new JLabel("host:"), gbc);

		gbc.gridy = 4;
		gbc.gridwidth = 3;
		gbc.gridheight = 2;
		gbc.anchor = GridBagConstraints.CENTER;
		add(panelFilters, gbc);

		// 4th column
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.gridx = 3;
		gbc.gridy = 2;
		add(txtProxyHost, gbc);

		gbc.gridy = 3;
		add(cbFilter, gbc);

		gbc.gridy = 6;
		add(txtResultPath, gbc);

		gbc.gridy = 7;
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.anchor = GridBagConstraints.LINE_END;
		for (JCheckBox cb : listResultsType)
			add(cb, gbc);

		gbc.gridy = 8;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.CENTER;
		add(btnClear, gbc);

		// 5th column
		gbc.gridwidth = 1;
		gbc.gridx = 4;
		gbc.gridy = 2;
		gbc.anchor = GridBagConstraints.LINE_END;
		add(new JLabel("port:"), gbc);

		// 6th column
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.gridx = 5;
		gbc.gridy = 1;
		add(txtPort, gbc);

		gbc.gridy = 2;
		add(txtProxyPort, gbc);

		gbc.gridy = 3;
		add(cbFilterType, gbc);

		gbc.gridy = 6;
		add(btnPathResults, gbc);

		gbc.gridy = 8;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.CENTER;
		add(btnStart, gbc);

		// 6th column
		gbc.gridx = 5;
		gbc.gridy = 4;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.LINE_START;
		add(btnFiltersDel, gbc);

		/* Listeners */
		cbFilterType.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				// Switch to '1' when goes active state and '2' for passive
				// state.
				if (e.getStateChange() == 1
						&& e.getItem().equals(FilterType.All))
					listFilters.setBackground(Color.LIGHT_GRAY);
				else
					listFilters.setBackground(Color.WHITE);
			}
		});

		btnFiltersAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!listElements.contains(txtFilters.getText())) {
					listElements.addElement(txtFilters.getText());
					listFilters.ensureIndexIsVisible(listElements.getSize() - 1);
				}
				txtFilters.setText(EMPTY);
			}
		});

		btnFiltersDel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i : listFilters.getSelectedIndices())
					listElements.remove(i);
				txtFilters.setText(EMPTY);
			}
		});

		listFilters.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (listFilters.getSelectedIndex() >= 0)
					txtFilters.setText((String) listElements.get(listFilters
							.getSelectedIndex()));
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
				txtProxyHost.setText(EMPTY);
				txtProxyPort.setText(EMPTY);
				txtFilters.setText(EMPTY);
				listElements.removeAllElements();
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

	private void populateItemsFromConfiguration(Configuration config) {
		txtPort.setText(String.valueOf(config.getProxyPort()));
		txtProxyHost.setText(config.getOutgoingProxyHost());
		txtProxyPort.setText(String.valueOf(config.getOutgoingProxyPort()));
		cbFilter.setSelectedItem(config.getFilter());
		cbFilterType.setSelectedItem(config.getFilterType());
		for (String filter : config.getFilters())
			listElements.addElement(filter);
		txtResultPath.setText(config.getResultPath());
		for (JCheckBox cb : listResultsType)
			for (ResultType resultType : config.getResultTypes())
				if (cb.getText().equals(resultType.name()))
					cb.setSelected(true);
	}

	private void createPopupMenu() {
		// Create the popup menu
		final JPopupMenu popup = new JPopupMenu();
		JMenuItem menuItem = new JMenuItem("delete");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i : listFilters.getSelectedIndices())
					listElements.remove(i);
			}
		});
		popup.add(menuItem);

		// Add listener so the popup menu can come up
		listFilters.addMouseListener(new MouseAdapter() {

			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}

			private void maybeShowPopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					// Change the selected item if many items aren't selected
					if (listFilters.getSelectedIndices().length < 1)
						listFilters.setSelectedIndex(listFilters
								.locationToIndex(e.getPoint()));
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
	}
}
