import javax.swing.*;
import java.awt.*;

/** Dropdown editor for the Category column. */
public class CategoryCellEditor extends DefaultCellEditor {
    private static final long serialVersionUID = 1L;
    private final JPanel panel = new JPanel(new BorderLayout());
    private final JComboBox<String> combo;
    private String value;

    @SuppressWarnings("this-escape")
    public CategoryCellEditor(PwdTableModel pwdTableModel) {
        super(new JCheckBox());
        combo = new JComboBox<>(pwdTableModel.getCategoriesModel());
        combo.addActionListener(e -> {
            value = (String) combo.getSelectedItem();
            fireEditingStopped();
        });
        panel.add(combo, BorderLayout.CENTER);
    }

    @Override
    public Component getTableCellEditorComponent(
            JTable table, Object v, boolean isSelected, int row, int column) {
        value = v == null ? "Default Category" : v.toString();
        combo.setSelectedItem(value);
        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        return value;
    }
}
