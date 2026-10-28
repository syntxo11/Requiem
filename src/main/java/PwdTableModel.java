import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Table model storing vault entries and categories. */
public class PwdTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    static final String DEFAULT_CATEGORY = "Default Category";
    static final String[] headers = {"Name", "Login", "Password", "Category", "Resource"};

    final ArrayList<String> categories;
    final ArrayList<PwdModel> data;
    private final DefaultComboBoxModel<String> defaultComboBoxModel;
    private boolean dirty;

    public PwdTableModel(ArrayList<PwdModel> data, ArrayList<String> categories) {
        this.data = data == null ? new ArrayList<>() : new ArrayList<>(data);
        this.categories = normalizeCategories(categories);
        this.defaultComboBoxModel = new DefaultComboBoxModel<>();
        rebuildCategoryModel();
        normalizeEntryCategories();
        dirty = false;
    }

    public void clear() {
        if (data.isEmpty()) return;
        int last = data.size() - 1;
        data.clear();
        dirty = true;
        fireTableRowsDeleted(0, last);
    }

    public void add(PwdModel pwdModel) {
        if (pwdModel == null) return;
        if (!categories.contains(pwdModel.getCategory())) {
            pwdModel.setCategory(DEFAULT_CATEGORY);
        }
        int row = data.size();
        data.add(pwdModel);
        dirty = true;
        fireTableRowsInserted(row, row);
    }

    public void remove(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= data.size()) return;
        data.remove(rowIndex);
        dirty = true;
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    public void clearCategories() {
        categories.clear();
        categories.add(DEFAULT_CATEGORY);
        for (PwdModel item : data) {
            item.setCategory(DEFAULT_CATEGORY);
        }
        rebuildCategoryModel();
        dirty = true;
        fireTableDataChanged();
    }

    public boolean addCategory(String category) {
        String normalized = category == null ? "" : category.trim();
        if (normalized.isEmpty() || categories.stream().anyMatch(c -> c.equalsIgnoreCase(normalized))) {
            return false;
        }
        categories.add(normalized);
        defaultComboBoxModel.addElement(normalized);
        dirty = true;
        return true;
    }

    /**
     * Safe default behavior: deleting a category moves its entries to Default Category.
     * The method name is intentionally kept from the original project.
     */
    public void removeCategory(int index) {
        moveCategoryEntriesToDefaultAndRemove(index);
    }

    public int countEntriesInCategory(int index) {
        if (index < 0 || index >= categories.size()) return 0;
        String category = categories.get(index);
        return (int) data.stream().filter(item -> category.equals(item.getCategory())).count();
    }

    public void moveCategoryEntriesToDefaultAndRemove(int index) {
        if (!canRemoveCategory(index)) return;
        String removedCategory = categories.get(index);
        for (PwdModel item : data) {
            if (removedCategory.equals(item.getCategory())) {
                item.setCategory(DEFAULT_CATEGORY);
            }
        }
        categories.remove(index);
        defaultComboBoxModel.removeElementAt(index);
        dirty = true;
        fireTableDataChanged();
    }

    public void deleteCategoryAndEntries(int index) {
        if (!canRemoveCategory(index)) return;
        String removedCategory = categories.get(index);
        data.removeIf(item -> removedCategory.equals(item.getCategory()));
        categories.remove(index);
        defaultComboBoxModel.removeElementAt(index);
        dirty = true;
        fireTableDataChanged();
    }

    public DefaultComboBoxModel<String> getCategoriesModel() {
        return defaultComboBoxModel;
    }

    public List<PwdModel> getData() {
        return List.copyOf(data);
    }

    public List<String> getCategories() {
        return List.copyOf(categories);
    }

    public PwdModel getEntry(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= data.size()) return null;
        return data.get(rowIndex);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        dirty = false;
    }

    public void replaceWith(PwdTableModel other) {
        data.clear();
        data.addAll(other.data);
        categories.clear();
        categories.addAll(other.categories);
        rebuildCategoryModel();
        dirty = false;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return headers.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= data.size()) return null;
        PwdModel item = data.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> item.getName();
            case 1 -> item.getLogin();
            case 2 -> item.getPassword();
            case 3 -> item.getCategory();
            case 4 -> item.getResource();
            default -> null;
        };
    }

    @Override
    public String getColumnName(int column) {
        return headers[column];
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return true;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (value == null || rowIndex < 0 || rowIndex >= data.size()) return;
        PwdModel model = data.get(rowIndex);
        String text = value.toString();
        switch (columnIndex) {
            case 0 -> model.setName(text);
            case 1 -> model.setLogin(text);
            case 2 -> model.setPassword(text);
            case 3 -> model.setCategory(categories.contains(text) ? text : DEFAULT_CATEGORY);
            case 4 -> model.setResource(text);
            default -> {
                return;
            }
        }
        dirty = true;
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    private boolean canRemoveCategory(int index) {
        return index > 0 && index < categories.size() && !DEFAULT_CATEGORY.equals(categories.get(index));
    }

    private static ArrayList<String> normalizeCategories(Collection<String> input) {
        ArrayList<String> result = new ArrayList<>();
        result.add(DEFAULT_CATEGORY);
        if (input != null) {
            for (String value : input) {
                if (value == null) continue;
                String trimmed = value.trim();
                if (trimmed.isEmpty() || DEFAULT_CATEGORY.equalsIgnoreCase(trimmed)) continue;
                if (result.stream().noneMatch(c -> c.equalsIgnoreCase(trimmed))) {
                    result.add(trimmed);
                }
            }
        }
        return result;
    }

    private void normalizeEntryCategories() {
        for (PwdModel item : data) {
            if (!categories.contains(item.getCategory())) {
                item.setCategory(DEFAULT_CATEGORY);
            }
        }
    }

    private void rebuildCategoryModel() {
        defaultComboBoxModel.removeAllElements();
        categories.forEach(defaultComboBoxModel::addElement);
    }
}
