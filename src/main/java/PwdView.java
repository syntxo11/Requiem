import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;

/** Main Swing window. Class name kept unchanged from the original university project. */
public class PwdView {
    JFrame mainFrame;

    private final PwdTableModel pwdTableModel;
    private final PwdTableView tableView;
    private final JList<String> categoryList;
    private final JTextField searchField = new JTextField(24);
    private final JLabel statusLabel = new JLabel(" ");
    private String activeFileName = "";

    public PwdView(PwdTableModel pwdTableModel) {
        this.pwdTableModel = pwdTableModel;

        mainFrame = new JFrame("Requiem");
        mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        mainFrame.setMinimumSize(new Dimension(980, 560));
        mainFrame.setLayout(new BorderLayout(10, 10));

        tableView = new PwdTableView(pwdTableModel);
        categoryList = new JList<>(pwdTableModel.getCategoriesModel());
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        mainFrame.setJMenuBar(buildMenuBar());
        mainFrame.add(buildTopPanel(), BorderLayout.NORTH);
        mainFrame.add(buildMainContent(), BorderLayout.CENTER);
        mainFrame.add(buildStatusBar(), BorderLayout.SOUTH);

        pwdTableModel.addTableModelListener(e -> {
            refreshTitle();
            refreshStatus();
        });

        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (confirmUnsavedChanges()) {
                    mainFrame.dispose();
                }
            }
        });

        refreshTitle();
        refreshStatus();
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem newItem = new JMenuItem("New Vault");
        newItem.setIcon(loadIcon("mail_new.png"));
        newItem.addActionListener(e -> createNewVault());

        JMenuItem openItem = new JMenuItem("Open Vault...");
        openItem.setIcon(loadIcon("open_folder_yellow.png"));
        openItem.addActionListener(e -> openVault());

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setIcon(loadIcon("floppy_disk_blue.png"));
        saveItem.addActionListener(e -> saveCurrentVault(false));

        JMenuItem saveAsItem = new JMenuItem("Save As...");
        saveAsItem.setIcon(loadIcon("floppy_disk_blue_1.png"));
        saveAsItem.addActionListener(e -> saveCurrentVault(true));

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            if (confirmUnsavedChanges()) mainFrame.dispose();
        });

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        return menuBar;
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));

        JLabel title = new JLabel("Requiem Vault");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        panel.add(title, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchPanel.add(new JLabel("Search:"));
        searchField.setToolTipText("Search by name, login, category or resource");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { filterChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { filterChanged(); }
        });
        searchPanel.add(searchField);
        panel.add(searchPanel, BorderLayout.EAST);
        return panel;
    }

    private JComponent buildMainContent() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildCategoryPanel(), buildTablePanel());
        splitPane.setResizeWeight(0.18);
        splitPane.setDividerLocation(200);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        return splitPane;
    }

    private JPanel buildCategoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Categories"));
        panel.add(new JScrollPane(categoryList), BorderLayout.CENTER);

        JButton addCategory = new JButton("Add", loadIcon("edit_add.png"));
        addCategory.addActionListener(e -> addCategory());

        JButton deleteCategory = new JButton("Delete", loadIcon("edit_remove.png"));
        deleteCategory.addActionListener(e -> deleteCategory());

        JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
        buttons.add(addCategory);
        buttons.add(deleteCategory);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Entries"));
        panel.add(new JScrollPane(tableView.getTable()), BorderLayout.CENTER);

        JButton addRecord = new JButton("Add Record", loadIcon("edit_add.png"));
        addRecord.addActionListener(e -> addRecord());

        JButton deleteRecord = new JButton("Delete Record", loadIcon("edit_remove.png"));
        deleteRecord.addActionListener(e -> deleteRecord());

        JButton reveal = new JButton("Reveal Passwords");
        reveal.addActionListener(e -> {
            boolean visible = !tableView.arePasswordsVisible();
            tableView.setPasswordsVisible(visible);
            reveal.setText(visible ? "Hide Passwords" : "Reveal Passwords");
        });

        JButton copyLogin = new JButton("Copy Login");
        copyLogin.addActionListener(e -> copySelectedValue(false));

        JButton copyPassword = new JButton("Copy Password");
        copyPassword.addActionListener(e -> copySelectedValue(true));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.add(addRecord);
        actions.add(deleteRecord);
        actions.add(Box.createHorizontalStrut(12));
        actions.add(reveal);
        actions.add(copyLogin);
        actions.add(copyPassword);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));
        panel.add(statusLabel, BorderLayout.WEST);
        return panel;
    }

    private void createNewVault() {
        if (!confirmUnsavedChanges()) return;
        pwdTableModel.clear();
        pwdTableModel.clearCategories();
        pwdTableModel.markClean();
        activeFileName = "";
        searchField.setText("");
        refreshTitle();
        refreshStatus();
    }

    private void openVault() {
        if (!confirmUnsavedChanges()) return;

        KeyInputDialog keyDialog = new KeyInputDialog(mainFrame, false);
        OpenFileDialog openDialog = new OpenFileDialog(mainFrame);
        FileProcessor fileProcessor = new FileProcessor(keyDialog, openDialog);
        PwdTableModel loaded = fileProcessor.getPwdTableModel();

        if (loaded == null) {
            if (!fileProcessor.getLastError().isBlank()) {
                JOptionPane.showMessageDialog(mainFrame, fileProcessor.getLastError(),
                        "Open Vault", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        pwdTableModel.replaceWith(loaded);
        activeFileName = fileProcessor.getActiveFileName();
        searchField.setText("");
        refreshTitle();
        refreshStatus();
    }

    private boolean saveCurrentVault(boolean saveAs) {
        JTable table = tableView.getTable();
        if (table.isEditing() && !table.getCellEditor().stopCellEditing()) return false;

        KeyInputDialog keyDialog = new KeyInputDialog(mainFrame, saveAs || activeFileName.isBlank());
        FileProcessor fileProcessor;
        boolean saved;

        if (saveAs || activeFileName.isBlank()) {
            SaveFileDialog saveDialog = new SaveFileDialog(mainFrame);
            fileProcessor = new FileProcessor(keyDialog, saveDialog);
            saved = fileProcessor.savePwdTableModel(pwdTableModel);
        } else {
            fileProcessor = new FileProcessor(keyDialog);
            saved = fileProcessor.saveToFile(pwdTableModel, activeFileName);
        }

        if (saved) {
            if (!fileProcessor.getActiveFileName().isBlank()) {
                activeFileName = fileProcessor.getActiveFileName();
            }
            refreshTitle();
            refreshStatus();
            return true;
        }

        if (!fileProcessor.getLastError().isBlank()) {
            JOptionPane.showMessageDialog(mainFrame, fileProcessor.getLastError(),
                    "Save Vault", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    private boolean confirmUnsavedChanges() {
        JTable table = tableView.getTable();
        if (table.isEditing() && !table.getCellEditor().stopCellEditing()) return false;
        if (!pwdTableModel.isDirty()) return true;

        Object[] options = {"Save", "Discard", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                mainFrame,
                "You have unsaved changes.",
                "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) return saveCurrentVault(false);
        return choice == 1;
    }

    private void addCategory() {
        NewCategoryDialog dialog = new NewCategoryDialog(mainFrame);
        dialog.ShowDialog();
        if (!dialog.getDialogResult()) return;
        if (!pwdTableModel.addCategory(dialog.getCategory())) {
            JOptionPane.showMessageDialog(mainFrame, "That category already exists.",
                    "Category", JOptionPane.INFORMATION_MESSAGE);
        }
        refreshTitle();
        refreshStatus();
    }

    private void deleteCategory() {
        int index = categoryList.getSelectedIndex();
        if (index < 0) return;
        String category = pwdTableModel.getCategoriesModel().getElementAt(index);
        if (PwdTableModel.DEFAULT_CATEGORY.equals(category)) {
            JOptionPane.showMessageDialog(mainFrame, "Default Category can't be deleted.",
                    "Category", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int affected = pwdTableModel.countEntriesInCategory(index);
        if (affected == 0) {
            int confirm = JOptionPane.showConfirmDialog(mainFrame,
                    "Delete category \"" + category + "\"?",
                    "Delete Category", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) pwdTableModel.removeCategory(index);
            return;
        }

        Object[] options = {
                "Move entries to Default Category",
                "Delete category and " + affected + " entr" + (affected == 1 ? "y" : "ies"),
                "Cancel"
        };
        int choice = JOptionPane.showOptionDialog(
                mainFrame,
                affected + " saved entr" + (affected == 1 ? "y belongs" : "ies belong") +
                        " to \"" + category + "\".",
                "Delete Category",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) pwdTableModel.moveCategoryEntriesToDefaultAndRemove(index);
        if (choice == 1) pwdTableModel.deleteCategoryAndEntries(index);
    }

    private void addRecord() {
        String selectedCategory = categoryList.getSelectedValue();
        if (selectedCategory == null) selectedCategory = PwdTableModel.DEFAULT_CATEGORY;
        pwdTableModel.add(new PwdModel("", "", "", selectedCategory, ""));
        int modelRow = pwdTableModel.getRowCount() - 1;
        int viewRow = tableView.getTable().convertRowIndexToView(modelRow);
        if (viewRow >= 0) {
            tableView.getTable().setRowSelectionInterval(viewRow, viewRow);
            tableView.getTable().scrollRectToVisible(tableView.getTable().getCellRect(viewRow, 0, true));
        }
    }

    private void deleteRecord() {
        int row = tableView.getSelectedModelRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(mainFrame, "Select a record first.",
                    "Delete Record", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(mainFrame,
                "Delete the selected password entry?", "Delete Record", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) pwdTableModel.remove(row);
    }

    private void copySelectedValue(boolean password) {
        int row = tableView.getSelectedModelRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(mainFrame, "Select a record first.",
                    "Clipboard", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        PwdModel item = pwdTableModel.getEntry(row);
        String value = password ? item.getPassword() : item.getLogin();
        if (value.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, password ? "Password is empty." : "Login is empty.",
                    "Clipboard", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
        statusLabel.setText((password ? "Password" : "Login") + " copied. Clipboard will be cleared in 30 seconds.");
        scheduleClipboardClear(value);
    }

    private void scheduleClipboardClear(String copiedValue) {
        Timer timer = new Timer(30_000, e -> {
            try {
                Object current = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                if (copiedValue.equals(current)) {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), null);
                    refreshStatus();
                }
            } catch (Exception ignored) {
                // Clipboard can temporarily be owned by another application; no destructive fallback is needed.
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void filterChanged() {
        tableView.setFilter(searchField.getText());
        refreshStatus();
    }

    private void refreshStatus() {
        int visible = tableView.getTable().getRowCount();
        int total = pwdTableModel.getRowCount();
        String filtered = visible == total ? total + " entries" : visible + " of " + total + " entries";
        statusLabel.setText(filtered + (pwdTableModel.isDirty() ? "  •  Unsaved changes" : "  •  Saved"));
    }

    private void refreshTitle() {
        String file = activeFileName.isBlank() ? "New Vault" : new File(activeFileName).getName();
        mainFrame.setTitle("Requiem - " + file + (pwdTableModel.isDirty() ? " *" : ""));
    }

    private ImageIcon loadIcon(String name) {
        URL resource = PwdView.class.getResource("/icons/" + name);
        return resource == null ? null : new ImageIcon(resource);
    }
}
