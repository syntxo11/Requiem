import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class OpenFileDialog extends AbstractDialog {
    String fileName;

    public OpenFileDialog(JFrame mainFrame) {
        super(mainFrame);
        this.fileName = "";
    }

    @Override
    public void ShowDialog() {
        dialogResult = false;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open encrypted vault");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Encrypted vault (*.vault)", "vault"));

        int selection = fileChooser.showOpenDialog(mainFrame);
        if (selection == JFileChooser.APPROVE_OPTION) {
            fileName = fileChooser.getSelectedFile().getAbsolutePath();
            dialogResult = true;
        }
    }

    public String getFileName() {
        return fileName;
    }
}
