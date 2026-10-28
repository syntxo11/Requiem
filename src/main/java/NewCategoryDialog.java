import javax.swing.*;

public class NewCategoryDialog extends AbstractDialog {
    private String category = "";

    public NewCategoryDialog(JFrame mainFrame) {
        super(mainFrame);
    }

    @Override
    public void ShowDialog() {
        dialogResult = false;
        String input = JOptionPane.showInputDialog(mainFrame, "Category name:", "New Category",
                JOptionPane.PLAIN_MESSAGE);
        if (input == null) return;

        category = input.trim();
        if (category.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Category name can't be empty.",
                    "Invalid category", JOptionPane.ERROR_MESSAGE);
            return;
        }
        dialogResult = true;
    }

    public String getCategory() {
        return category;
    }
}
