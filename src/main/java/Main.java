import javax.swing.*;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            configureLookAndFeel();
            ArrayList<String> pwdCategories = new ArrayList<>();
            pwdCategories.add(PwdTableModel.DEFAULT_CATEGORY);

            ArrayList<PwdModel> pwdModels = new ArrayList<>();
            PwdTableModel pwdTableModel = new PwdTableModel(pwdModels, pwdCategories);
            new PwdView(pwdTableModel);
        });
    }

    private static void configureLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Swing will use the default cross-platform look and feel.
        }
    }
}
