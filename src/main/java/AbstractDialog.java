import javax.swing.*;

/** Base class for modal dialogs used by the application. */
public abstract class AbstractDialog {
    protected boolean dialogResult;
    protected final JFrame mainFrame;

    public AbstractDialog(JFrame mainFrame) {
        this.dialogResult = false;
        this.mainFrame = mainFrame;
    }

    public abstract void ShowDialog();

    public boolean getDialogResult() {
        return dialogResult;
    }
}
