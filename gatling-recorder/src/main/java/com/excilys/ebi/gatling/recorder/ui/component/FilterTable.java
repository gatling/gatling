package com.excilys.ebi.gatling.recorder.ui.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.swing.AbstractCellEditor;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.excilys.ebi.gatling.recorder.configuration.Pattern;
import com.excilys.ebi.gatling.recorder.ui.enumeration.PatternType;

public class FilterTable extends JPanel {

	private static final long serialVersionUID = 1L;
	private DefaultTableModel model = new DefaultTableModel();
	private JTable table = new JTable();

	public FilterTable() {
		model.addColumn("Filter");
		model.addColumn("Style");

		table.setModel(model);
		this.setLayout(new GridLayout(1, 1));
		JScrollPane scrollPane = new JScrollPane(table);
		add(scrollPane, 0, 0);

		initPopupMenu();

		table.getColumn("Style").setMaxWidth(120);
		table.getColumn("Style").setMinWidth(120);
		table.getColumn("Style").setCellRenderer(new RadioButtonRenderer());
		table.getColumn("Style").setCellEditor(new RadioButtonEditor());
		table.setRowHeight(30);

		model.addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e) {
				// TODO: Vérifier unicité
				// System.err.println("!! Vérifier unicité");
			}
		});
	}

	public void setEnabled(boolean enabled) {
		if (!enabled)
			table.setBackground(Color.LIGHT_GRAY);
		else
			table.setBackground(Color.WHITE);
	}

	public void addRow() {
		model.addRow(new Object[] { "" });
	}

	public void addRow(Pattern pattern) {
		model.addRow(new Object[] { pattern.getPattern() });
	}

	public void removeSelectedRow() {
		int[] selected = getSelectedRows();
		Arrays.sort(selected);
		for (int i = selected.length - 1; i >= 0; i--)
			model.removeRow(selected[i]);
	}

	public void removeAllElements() {
		while (model.getRowCount() > 0)
			model.removeRow(0);
	}

	public int[] getSelectedRows() {
		return table.getSelectedRows();
	}

	public int getRowCount() {
		return model.getRowCount();
	}

	public Pattern getPattern(int row) {
		Pattern p = new Pattern(getPatternTypeAt(row), (String) model.getValueAt(row, 0));
		return p;
	}

	private PatternType getPatternTypeAt(int row) {
		CustomPanel p = (CustomPanel) table.getValueAt(row, 1);
		return p.isFirstSelected() ? PatternType.Java : PatternType.Ant;
	}

	private void initPopupMenu() {
		// Create the popup menu
		final JPopupMenu popup = new JPopupMenu();
		JMenuItem menuItem = new JMenuItem("delete");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeSelectedRow();
			}
		});
		popup.add(menuItem);

		table.addMouseListener(new MouseAdapter() {

			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}

			private void maybeShowPopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
	}
}

class RadioButtonRenderer implements TableCellRenderer {

	public RadioButtonRenderer() {
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value == null) {
			value = new CustomPanel();
			table.setValueAt(value, row, 1);
		}
		return (CustomPanel) value;
	}
}

class RadioButtonEditor extends AbstractCellEditor implements TableCellEditor {

	private static final long serialVersionUID = 1L;
	CustomPanel customPanel = new CustomPanel();

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		if (value == null) {
			value = new CustomPanel();
			table.setValueAt(value, row, 1);
		}
		customPanel = (CustomPanel) value;
		return customPanel;
	}

	public Object getCellEditorValue() {
		return customPanel;
	}
}

@SuppressWarnings("serial")
class CustomPanel extends JPanel {

	JRadioButton radio1 = new JRadioButton("Java", true);
	JRadioButton radio2 = new JRadioButton("Ant", false);

	public CustomPanel() {
		this.add(radio1);
		this.add(radio2);
		ButtonGroup group = new ButtonGroup();
		group.add(radio1);
		group.add(radio2);
	}

	public boolean isFirstSelected() {
		return radio1.isSelected();
	}
}