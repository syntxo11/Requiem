import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class KeyInputDialog extends AbstractDialog {
    private char[] key = new char[0];
    private final boolean confirmPassword;

    public KeyInputDialog(JFrame mainFrame) {
        this(mainFrame, false);
    }

    public KeyInputDialog(JFrame mainFrame, boolean confirmPassword) {
        super(mainFrame);
        this.confirmPassword = confirmPassword;
    }

    @Override
    public void ShowDialog() {
        dialogResult = false;
        JPasswordField pwdField = new JPasswordField(18);
        JPasswordField confirmField = confirmPassword ? new JPasswordField(18) : null;

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel(confirmPassword ? "Master password:" : "Master password:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(pwdField, gbc);

        if (confirmPassword) {
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0;
            form.add(new JLabel("Confirm password:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            form.add(confirmField, gbc);
        }

        String[] options = {"OK", "Cancel"};
        while (true) {
            int result = JOptionPane.showOptionDialog(
                    mainFrame,
                    form,
                    confirmPassword ? "Protect Vault" : "Unlock Vault",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (result != JOptionPane.OK_OPTION) {
                clearKey();
                return;
            }

            char[] entered = pwdField.getPassword();
            if (entered.length == 0) {
                JOptionPane.showMessageDialog(mainFrame, "Master password can't be empty.",
                        "Invalid password", JOptionPane.ERROR_MESSAGE);
                Arrays.fill(entered, '\0');
                continue;
            }

            if (confirmPassword) {
                char[] confirmation = confirmField.getPassword();
                boolean matches = Arrays.equals(entered, confirmation);
                Arrays.fill(confirmation, '\0');
                if (!matches) {
                    JOptionPane.showMessageDialog(mainFrame, "Passwords do not match.",
                            "Invalid password", JOptionPane.ERROR_MESSAGE);
                    Arrays.fill(entered, '\0');
                    pwdField.setText("");
                    confirmField.setText("");
                    continue;
                }
            }

            clearKey();
            key = Arrays.copyOf(entered, entered.length);
            Arrays.fill(entered, '\0');
            dialogResult = true;
            return;
        }
    }

    public char[] getKeyChars() {
        return Arrays.copyOf(key, key.length);
    }

    public void clearKey() {
        Arrays.fill(key, '\0');
        key = new char[0];
    }
}
