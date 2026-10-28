import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.regex.Pattern;

/** JTable wrapper responsible for filtering and password masking. */
public class PwdTableView {
    private final JTable table;
    private final TableRowSorter<PwdTableModel> sorter;
    private boolean passwordsVisible;

    public PwdTableView(PwdTableModel pwdTableModel) {
        table = new JTable(pwdTableModel);
        sorter = new TableRowSorter<>(pwdTableModel);
        table.setRowSorter(sorter);

        table.getColumn("Category").setCellEditor(new CategoryCellEditor(pwdTableModel));
        table.getColumn("Password").setCellEditor(new GeneratePasswordEditor());
        table.getColumn("Password").setCellRenderer(new PasswordRenderer());

        table.setPreferredScrollableViewportSize(new Dimension(760, 360));
        table.setRowHeight(30);
        table.setAutoCreateRowSorter(false);
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
    }

    public JTable getTable() {
        return table;
    }

    public int getSelectedModelRow() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? -1 : table.convertRowIndexToModel(viewRow);
    }

    public void setFilter(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(normalized), 0, 1, 3, 4));
    }

    public void setPasswordsVisible(boolean visible) {
        passwordsVisible = visible;
        table.repaint();
    }

    public boolean arePasswordsVisible() {
        return passwordsVisible;
    }

    private class PasswordRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        protected void setValue(Object value) {
            if (passwordsVisible) {
                super.setValue(value == null ? "" : value.toString());
            } else {
                String password = value == null ? "" : value.toString();
                super.setValue(password.isEmpty() ? "" : "•".repeat(Math.min(Math.max(password.length(), 8), 16)));
            }
        }
    }
}
