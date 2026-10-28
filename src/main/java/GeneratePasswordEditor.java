import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

/** Password-cell editor with configurable SecureRandom password generation. */
public class GeneratePasswordEditor extends AbstractCellEditor implements TableCellEditor {
    private static final long serialVersionUID = 1L;
    private final JPanel panel = new JPanel(new BorderLayout(4, 0));
    private final JButton button = new JButton("Generate");
    private final JTextField textField = new JTextField();
    private String value = "";

    public GeneratePasswordEditor() {
        button.setFocusable(false);
        button.addActionListener(e -> showGeneratorDialog());
        panel.add(textField, BorderLayout.CENTER);
        panel.add(button, BorderLayout.EAST);
    }

    @Override
    public boolean stopCellEditing() {
        value = textField.getText();
        return super.stopCellEditing();
    }

    @Override
    public Component getTableCellEditorComponent(
            JTable table, Object v, boolean isSelected, int row, int column) {
        value = v == null ? "" : v.toString();
        textField.setText(value);
        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        return value;
    }

    private void showGeneratorDialog() {
        JSpinner length = new JSpinner(new SpinnerNumberModel(16, 4, 128, 1));
        JCheckBox uppercase = new JCheckBox("Uppercase (A-Z)", true);
        JCheckBox lowercase = new JCheckBox("Lowercase (a-z)", true);
        JCheckBox digits = new JCheckBox("Numbers (0-9)", true);
        JCheckBox symbols = new JCheckBox("Symbols", true);
        JLabel strength = new JLabel("Strength: Strong");

        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        JPanel lengthRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lengthRow.add(new JLabel("Length:"));
        lengthRow.add(length);
        options.add(lengthRow);
        options.add(uppercase);
        options.add(lowercase);
        options.add(digits);
        options.add(symbols);
        options.add(Box.createVerticalStrut(8));
        options.add(strength);

        int result = JOptionPane.showConfirmDialog(panel, options, "Generate Password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            String generated = PwdGenerator.generatePassword(
                    (Integer) length.getValue(),
                    uppercase.isSelected(),
                    lowercase.isSelected(),
                    digits.isSelected(),
                    symbols.isSelected()
            );
            value = generated;
            textField.setText(generated);
            strength.setText("Strength: " + PwdGenerator.estimateStrength(generated));
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(panel, ex.getMessage(), "Generator Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
