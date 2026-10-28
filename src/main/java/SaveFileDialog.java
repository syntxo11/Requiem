import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class SaveFileDialog extends AbstractDialog {
    String fileName;

    public SaveFileDialog(JFrame mainFrame) {
        super(mainFrame);
        this.fileName = "";
    }

    @Override
    public void ShowDialog() {
        dialogResult = false;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save encrypted vault");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Encrypted vault (*.vault)", "vault"));

        int selection = fileChooser.showSaveDialog(mainFrame);
        if (selection == JFileChooser.APPROVE_OPTION) {
            File selected = fileChooser.getSelectedFile();
            String path = selected.getAbsolutePath();
            if (!path.toLowerCase().endsWith(".vault")) {
                path += ".vault";
            }
            if (new File(path).exists()) {
                int confirm = JOptionPane.showConfirmDialog(mainFrame,
                        "Overwrite existing vault?", "Save Vault", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;
            }
            fileName = path;
            dialogResult = true;
        }
    }

    public String getFileName() {
        return fileName;
    }
}
