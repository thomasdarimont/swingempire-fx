/*
 * Created on 02.06.2013
 *
 */
package de.swingempire.fx.scene.control.selection;

import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewFocusModel;
import javafx.scene.control.TableView.TableViewSelectionModel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 * @author Jeanette Winzenburg, Berlin
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(Parameterized.class)
public class TableMultipleSelectionIssues extends MultipleSelectionIssues<TableView, TableViewSelectionModel> {

    public TableMultipleSelectionIssues(boolean multiple) {
        super(multiple);
    }

    @Test
    public void testTablePosition() {
        TableView table = getView();
        TablePosition pos = new TablePosition(table, 0, null);
        assertEquals(pos, new TablePosition(table, 0, null));
    }
    
    @Test
    public void testSanity() {
        TableView table = getView();
        assertEquals(1, table.getColumns().size());
        assertEquals(items, table.getItems());
    }

    
    @Override
    protected TableViewSelectionModel getSelectionModel() {
        return getView().getSelectionModel();
    }

    /**
     * 
     */
    @Override
    protected TableView createView(ObservableList items) {
        TableView table = new TableView(items);
        TableColumn column = new TableColumn("numberedItems");
        table.getColumns().add(column);
        TableViewSelectionModel model = table.getSelectionModel();
        assertEquals("sanity: test setup assumes that initial mode is single", 
                SelectionMode.SINGLE, model.getSelectionMode());
        checkMode(model);
        // PENDING JW: this is crude ... think of doing it elsewhere
        // the problem is to keep super blissfully unaware of possible modes
        assertEquals(multipleMode, model.getSelectionMode() == SelectionMode.MULTIPLE);
        return table;
    }

    @Override
    protected TableViewFocusModel getFocusModel() {
        return getView().getFocusModel();
    }

    @Override
    protected int getAnchorIndex() {
        Object anchor = getView().getProperties().get(SelectionIssues.ANCHOR_KEY);
        if (anchor instanceof TablePosition) {
            return ((TablePosition) anchor).getRow();
        }
        return -1;
    }

    
}
