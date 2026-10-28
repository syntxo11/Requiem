import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class PwdTableModelTest {
    private PwdTableModel model;

    @BeforeEach
    void setUp() {
        ArrayList<String> categories = new ArrayList<>();
        categories.add("Default Category");
        categories.add("Work");
        model = new PwdTableModel(new ArrayList<>(), categories);
    }

    @Test
    void modelStartsClean() {
        assertFalse(model.isDirty());
    }

    @Test
    void addingEntryMarksModelDirty() {
        model.add(new PwdModel("GitHub", "user", "secret", "Work", "github.com"));
        assertTrue(model.isDirty());
        assertEquals(1, model.getRowCount());
    }

    @Test
    void duplicateCategoryIsRejectedIgnoringCase() {
        assertFalse(model.addCategory("work"));
        assertEquals(2, model.getCategories().size());
    }

    @Test
    void removingCategoryMovesEntriesToDefaultByDefault() {
        model.add(new PwdModel("GitHub", "user", "secret", "Work", "github.com"));
        model.markClean();
        model.removeCategory(1);
        assertEquals("Default Category", model.getEntry(0).getCategory());
        assertEquals(1, model.getCategories().size());
        assertTrue(model.isDirty());
    }

    @Test
    void explicitDeleteCategoryCanDeleteItsEntries() {
        model.add(new PwdModel("GitHub", "user", "secret", "Work", "github.com"));
        model.deleteCategoryAndEntries(1);
        assertEquals(0, model.getRowCount());
    }

    @Test
    void defaultCategoryCannotBeRemoved() {
        model.removeCategory(0);
        assertEquals("Default Category", model.getCategories().get(0));
    }

    @Test
    void editingCellMarksModelDirty() {
        model.add(new PwdModel("GitHub", "user", "secret", "Work", "github.com"));
        model.markClean();
        model.setValueAt("new-user", 0, 1);
        assertEquals("new-user", model.getEntry(0).getLogin());
        assertTrue(model.isDirty());
    }

    @Test
    void unknownEntryCategoryFallsBackToDefault() {
        model.add(new PwdModel("Site", "user", "secret", "Missing", "example.com"));
        assertEquals("Default Category", model.getEntry(0).getCategory());
    }
}
