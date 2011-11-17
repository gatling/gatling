package com.excilys.ebi.gatling.proxy.ui.component;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class TextAreaPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public final JTextArea txt = new JTextArea();

	public TextAreaPanel(String title) {

		JLabel lblTitle = new JLabel(title);
		txt.setEditable(false);
		txt.setPreferredSize(new Dimension(200, 100));

		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		add(lblTitle, gbc);

		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbl.setConstraints(txt, gbc);
		add(txt, gbc);
	}
}
