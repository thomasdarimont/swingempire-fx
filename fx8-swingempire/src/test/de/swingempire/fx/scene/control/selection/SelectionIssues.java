/*
 * Created on 02.06.2013
 *
 */
package de.swingempire.fx.scene.control.selection;

import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Control;
import javafx.scene.control.FocusModel;
import javafx.scene.control.SelectionModel;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.swingempire.fx.junit.JavaFXThreadingRule;
import fx.util.StageLoader;

import static org.junit.Assert.*;

/**
 * Testing SelectionModel api.
 * 
 * Reported: https://javafx-jira.kenai.com/browse/RT-38494
 * mismatch between spec and implementation
 * 
 * 
 * 
 * @author Jeanette Winzenburg, Berlin
 */
@RunWith(JUnit4.class)
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class SelectionIssues<V extends Control, T extends SelectionModel> {
    
    public final static String ANCHOR_KEY = "anchor";
  
    @ClassRule
    public static TestRule classRule = new JavaFXThreadingRule();


    /**
     * The model set to the views. It contains 5 string items, originally
     * in descending order. Invoking sort will revert the order.
     */
    protected ObservableList items;
    protected V view;

//------------  test interplay of selection/focus/anchor 
//------------ focus    
    
    /**
     * https://javafx-jira.kenai.com/browse/RT-30931
     * What's happening if the item at the focused/selected index is 
     * removed?
     * 
     * The issue mentions 3 options
     * 1. "shift down both", that is the next item is focused and selected
     * 2. "shift down focus, unselect", that is selected item is cleared, focus one down
     * 3. "clear all", that is selected and focus cleared
     * 
     * UX preferred 1, here we seem to have a shift up?
     * The issue is still open.
     */
    @Test
    public void testFocusOnRemoveItemAtSelectedFocused() {
        int index = 2;
        getSelectionModel().select(index);
        items.remove(index);
        assertEquals("open 30931 - focus after remove focused", index, getFocusIndex(index));
    }
    @Test
    public void testSelectedOnRemoveItemAtSelectedFocused() {
        int index = 2;
        getSelectionModel().select(index);
        items.remove(index);
        assertEquals("open 30931 - selection after remove focused", index, getSelectionModel().getSelectedIndex());
    }
    
    @Test
    public void testAnchorOnRemoveItemAtSelectedFocused() {
        StageLoader loader = new StageLoader(getView());
        int index = 2;
        getSelectionModel().select(index);
        items.remove(index);
        assertEquals("open 30931 - anchor after remove focused", 
                index, getAnchorIndex(index));
    }
    
    @Test
    public void testFocusOnRemoveItemAbove() {
        int index = 2;
        getSelectionModel().select(index);
        items.remove(1);
        assertEquals("open 30931 - focus after remove above focused", index -1, getFocusIndex(index));
    }
    
    @Test
    public void testSelectedOnRemoveItemAbove() {
        int index = 2;
        getSelectionModel().select(index);
        items.remove(1);
        assertEquals("open 30931 - selected after remove above focused", index-1, getSelectionModel().getSelectedIndex());
    }
    
    
    @Test
    public void testFocusOnClearSelection() {
        int index = 2;
        getSelectionModel().select(index);
        getSelectionModel().clearSelection();
        assertEquals("focus must be cleared clearSelection", -1, getFocusIndex(index));
        
    }
    @Test
    public void testFocusOnClearAndSelect() {
        int index = 2;
        getSelectionModel().select(index);
        getSelectionModel().clearAndSelect(index + 1);
        assertEquals("focus must same as selected", index + 1, getFocusIndex(index + 1));
        
    }
    @Test
    public void testFocus() {
        int index = 2;
        getSelectionModel().select(index);
        assertEquals("focus must be same as selected index", index, getFocusIndex(index));
    }
    
    @Test
    public void testFocusOnClearSelectionAt() {
        int index = 2;
        getSelectionModel().select(index);
        getSelectionModel().clearSelection(index);
        assertEquals("focus must be cleared", -1, getFocusIndex(-1));
    }
    @Test
    public void testFocusOnSelectNext() {
        int index = 2;
        getSelectionModel().select(index);
        getSelectionModel().selectNext();
        int next = index + 1;
        assertEquals("focus must be same as next index", next, 
                getFocusIndex(next));
    }

    @Test
    public void testFocusOnSelectPrevious() {
        int index = 2;
        getSelectionModel().select(index);
        getSelectionModel().selectPrevious();
        int previous = index - 1;
        assertEquals("focus must be same as previous index", previous, 
                getFocusIndex(previous));
    }

    @Test
    public void testFocusOnSelectFirst() {
        int index = 2;
        getSelectionModel().select(index);
        getSelectionModel().selectFirst();
        int first = 0;
        assertEquals("focus must be same as first index", first, 
                getFocusIndex(first));
    }

    @Test
    public void testFocusOnSelectLast() {
        int index = 2;
        getSelectionModel().select(index);
        getSelectionModel().selectLast();
        int last = items.size() - 1;
        assertEquals("focus must be same as first index", last, 
                getFocusIndex(last));
    }
    
    @Test
    public void testFocusWithoutSelection() {
        if (getFocusModel() == null) return;
        int index = 2;
        getSelectionModel().select(index);
        getFocusModel().focusNext();
        int next = getFocusModel().getFocusedIndex();
        assertEquals(index +1, next);
        assertFalse(getSelectionModel().isSelected(next));
    }
    
    @Test
    public void testAnchorOnFocusNextWithoutSelection() {
        if (getFocusModel() == null) return;
        StageLoader loader = new StageLoader(getView());
        int index = 2;
        getSelectionModel().select(index);
        getFocusModel().focusNext();
        int next = getFocusModel().getFocusedIndex();
        assertEquals("anchor must be unchanged when moving focus", 
                index, getAnchorIndex(index));
    }
    /**
     * Anchor must be set on select in empty selection.
     * 
     * Note: plain anchor testing doesn't make sense here - it's controlled by behaviour which is 
     * part of skin which is not yet installed after instantiation ... so 
     * need to force the creation by adding it to a life stage.
     */
    @Test
    public void testAnchor() {
        StageLoader loader = new StageLoader(getView());
        int index = 2;
        getSelectionModel().select(index);
        assertEquals("anchor must be same as selected index", index, 
                getAnchorIndex(index));
    }
    
    /**
     * Anchor testing: must be reset on clearAndSelect.
     */
    @Test
    public void testAnchorClearAndSelect() {
        StageLoader loader = new StageLoader(getView());
        int index = 2;
        getSelectionModel().select(index);
        getSelectionModel().clearAndSelect(index + 1);
        int selected = getSelectionModel().getSelectedIndex();
        assertEquals("anchor must be same as selected index", 
                selected, getAnchorIndex(selected));
    }
    
    @Test
    public void testAnchorClearSelectionAt() {
        StageLoader loader = new StageLoader(getView());
        int index = 2;
        getSelectionModel().select(index);
        getSelectionModel().clearSelection(index);
        assertEquals("anchor must be cleared", 
                -1, getAnchorIndex(-1));
    }
    
    @Test
    public void testAnchorClearSelection() {
        StageLoader loader = new StageLoader(getView());
        int index = 2;
        getSelectionModel().select(index);
        getSelectionModel().clearSelection();
        assertEquals("anchor must be cleared", 
                -1, getAnchorIndex(-1));
    }
    /**
     * Anchor must not be moved after adding/removing items below.
     */
    @Test
    public void testAnchorOnRemoveItemBelow() {
        StageLoader loader = new StageLoader(getView());
        int index = 2;
        getSelectionModel().select(index);
        items.remove(index + 1);
        int selected = getSelectionModel().getSelectedIndex();
        assertEquals("anchor must be same as selected index", 
                selected, getAnchorIndex(selected));
    }
    
    /**
     * Anchor must not be moved after adding/removing items below.
     */
    @Test
    public void testAnchorOnInsertItemBelow() {
        StageLoader loader = new StageLoader(getView());
        int index = 2;
        getSelectionModel().select(index);
        items.add(index + 1, "6-item");
        int selected = getSelectionModel().getSelectedIndex();
        assertEquals("anchor must be same as selected index", 
                selected, getAnchorIndex(selected));
    }
    
    /**
     * Anchor must be move after adding/removing items above.
     */
    @Test
    public void testAnchorOnInsertItemAbove() {
        StageLoader loader = new StageLoader(getView());
        int index = 2;
        getSelectionModel().select(index);
        items.add(0, "6-item");
        int selected = getSelectionModel().getSelectedIndex();
        assertEquals("anchor must be same as selected index", 
                selected, getAnchorIndex(selected));
    }
    
    /**
     * Anchor must be move after adding/removing items above.
     */
    @Test
    public void testAnchorOnRemoveItemAbove() {
        StageLoader loader = new StageLoader(getView());
        int index = 2;
        getSelectionModel().select(index);
        items.remove(0);
        int selected = getSelectionModel().getSelectedIndex();
        assertEquals("anchor must be same as selected index", 
                selected, getAnchorIndex(selected));
    }
    
//    * Reported: https://javafx-jira.kenai.com/browse/RT-38494
//    * mismatch between spec and implementation
    /**
     * Bug or feature? Can select the item in empty selection
     * which then still reports to be empty.
     * 
     * Violates doc of isEmpty:
     * "test whether there are any selected indices/items.
     * It will return true if there are no selected items."
     * 
     * Reported: https://javafx-jira.kenai.com/browse/RT-38494
     * mismatch between spec and implementation
     * 
     */
    @Test
    public void testSelectUncontainedIfEmptySelection() {
        Object item = "uncontained";
        getSelectionModel().select(item);
        assertEquals("sanity: the item is selected", item, getSelectionModel().getSelectedItem());
        assertFalse("selection must not be empty", getSelectionModel().isEmpty());
    }
    
    /**
     * Issue: broken invariant: if selectedIndex >= 0 --> selectItem = get(selectedIndex)
     * 
     * Bug or feature? select(T) explicitly states that it _attempts_ to select the item
     * (and stops at the first find), but doesn't specify what happens if T isn't found.
     * 
     * This is done in getSelectedItem it specifies the item being the one
     *  "which resides in the selectedIndex position". Also in selectedItemProperty: 
     * "The selected item is either null, 
     * to represent that there is no selection, or an Object that is retrieved 
     * from the underlying data model"
     *  
     * ... so looks like a bug to me.
     *  
     * Actually, doesn't make sense to allow the index/item correlation being unsynched in the 
     * _general_ case - it's a special need if we have selections that are not necessarily 
     * backed in the model (like in swing comboboxModel)  
     * 
     * Reported: https://javafx-jira.kenai.com/browse/RT-38494
     * mismatch between spec and implementation
     * 
     */
    @Test
    public void testSelectUncontainedIfNotEmptySelection() {
        int index = 2;
        getSelectionModel().select(index);
        Object item = "uncontained";
        getSelectionModel().select(item);
        assertEquals(index, getSelectionModel().getSelectedIndex());
        assertEquals("selectedItem must be model item at selectedIndex", 
                items.get(index), getSelectionModel().getSelectedItem());
        // this is what passes, but is inconsistent with the doc of getSelectedItem
        assertEquals("uncontained item must be selected item", item, getSelectionModel().getSelectedItem());
    }
    
    /**
     * Issue: selectedIndex gets out off synch on select null item.
     * Strictly speaking, there are two issues: 
     * a) the doc doesn't specify of what happens if the item is null (it does of index being
     *   off range)
     * b) whatever happens, it must keep the invariant selectedItem == get(selectedIndex)
     * 
     * The invariant isn't explicitly documented, just ... 
     * Note: in 8u113 the behaviour is changed - selecting null clears the selection
     * in SingleSelectionModel and TableViewSelectionModel in singleSelection 
     * 
     * Reported: https://javafx-jira.kenai.com/browse/RT-38494
     * mismatch between spec and implementation
     */
    @Test
    public void testSelectNullItem() {
        int index = 2;
        getSelectionModel().select(index);
        Object item = getSelectionModel().getSelectedItem();
        getSelectionModel().select(null);
        assertEquals("unspecified behaviour on select(null): what to expect?", item, getSelectionModel().getSelectedItem());
        fail("unspecified behaviour for selecting null item");
    }
    
    /**
     * Reported: https://javafx-jira.kenai.com/browse/RT-38494
     * mismatch between spec and implementation
     */
    @Test
    public void testSelectNullIndex() {
        int index = 2;
        getSelectionModel().select(index);
        Object item = getSelectionModel().getSelectedItem();
        getSelectionModel().select(null);
        assertEquals("unspecified behaviour on select(null) ", index, getSelectionModel().getSelectedIndex());
        fail("unspecified behaviour for selecting null item");
    }

    /**
     * SelectionModel is documented to do nothing if the index is out of range.
     * Test that selectedItem is unchanged.
     * 
     * Reported: https://javafx-jira.kenai.com/browse/RT-38494
     * mismatch between spec and implementation
     */
    @Test
    public void testSelectMinusOneItem() {
        int index = 2;
        getSelectionModel().select(index);
        getSelectionModel().select(-1);
        assertEquals("selecting off-range (here: - 1) must have no effect on selectedItem", items.get(index), getSelectionModel().getSelectedItem());
    }

    /**
     * SelectionModel is documented to do nothing if the index is out of range.
     * Test that the selectedIndex is unchanged.
     * 
     * Reported: https://javafx-jira.kenai.com/browse/RT-38494
     * mismatch between spec and implementation
     */
    @Test
    public void testSelectMinusOneIndex() {
        int index = 2;
        getSelectionModel().select(index);
        getSelectionModel().select(-1);
        assertEquals("selecting off-range (here: -1) must have no effect on selectedIndex", index, getSelectionModel().getSelectedIndex());
     }
    /**
     * Test clearSelection(index).
     * 
     * Mostly passes, fails for TableViewSelectionModel.
     */
    @Test
    public void testClearSelectionAtIndex() {
        int index = 2;
        getSelectionModel().select(index);
        getSelectionModel().clearSelection(index);
        assertTrue("selection must be empty after unselect the selected", 
                getSelectionModel().isEmpty());
        assertFalse("index must be unselected", getSelectionModel().isSelected(index));
        assertEquals("index must be cleared", 
                -1, getSelectionModel().getSelectedIndex());
    }

    /**
     * Strictly speaking the api doc of select(index) is incorrect: "will not 
     * clear the selection of other indices" - should specify that it certainly
     * will if selection constraints apply (like in singleSelection mode/implementations)
     */
    @Test
    public void testSelectOther() {
        int index = 2;
        getSelectionModel().select(index);
        getSelectionModel().select(index + 1);
        assertEquals(index +1, getSelectionModel().getSelectedIndex());
    }
    
// ---------------- sugar methods on empty selection
    
    @Test
    public void testSelectNextEmpty() {
        getSelectionModel().selectNext();
        assertEquals("frist index must be selected", 0, getSelectionModel().getSelectedIndex());
        assertEquals("first index must be focused", 0, getFocusIndex(0));
    }
//---------------------------- passing tests    
    
    @Test
    public void testSelectionAfterInsertAbove() {
        int index = 2;
        getSelectionModel().select(index);
        items.add(0, "6-item");
        assertEquals("selection moved by one after inserting item", 
                index +1, getSelectionModel().getSelectedIndex());
    }
    
    @Test
    public void testSelectionAfterRemoveAbove() {
        int index = 2;
        getSelectionModel().select(index);
        items.remove(0);
        assertEquals("selection moved by one after inserting item", 
                index -1, getSelectionModel().getSelectedIndex());
        
    }
    /**
     * Incorrect selection behaviour after sorting the model
     * 
     * a) selectedIndex must be updated to new position (instead of being cleared)
     * b) in the unusual case that the model decides to clear the selected index, 
     *    it  must report having done so in isEmpty
     * 
     * Passes in fx8(u113)
     */
    @Test
    public void testSelectedIndexAfterSort() {
        int first = 0;
        int last = items.size() -1;
        getSelectionModel().select(first);
        FXCollections.sort(items);
        assertFalse("selection must not be empty after sorting", getSelectionModel().isEmpty());
        assertTrue("last index must be selected", getSelectionModel().isSelected(last));
        assertEquals(last, getSelectionModel().getSelectedIndex());        
    }
    /**
     * SelectionModel is documented to do nothing if the index is out of range.
     */
    @Test
    public void testSelectOffRange() {
        int index = 2;
        getSelectionModel().select(index);
        getSelectionModel().select(items.size());
        assertEquals("selecting off-range (here: size) must have no effect on selectedIndex", index, getSelectionModel().getSelectedIndex());
        assertEquals("selecting off-range (here: size) must have no effect on selectedItem", items.get(index), getSelectionModel().getSelectedItem());
    }

    @Test
    public void testSelectItemSyncIndex() {
        int index = 2;
        Object item = items.get(index);
        getSelectionModel().select(item);
        assertEquals(index, getSelectionModel().getSelectedIndex());
    }
    
    /**
     * Natural (though not explicitly specified) synch of selectedIndex and 
     * selectedItem.
     */
    @Test
    public void testSelectIndexSyncItem() {
        int index = 2;
        getSelectionModel().select(index);
        assertEquals(items.get(index), getSelectionModel().getSelectedItem());
    }

//-------------- initial state
    
    // as of 8u20, the ListView, TableView have the first row selected
    // done in a focusListener as fix for rt-25679
    // further changes expected in 8u40 - looks like moved to the model somehow
    // recent changeset has all tests calling clearSelection
    // http://hg.openjdk.java.net/openjfx/8u-dev/rt/rev/258d08a27dc0
    // == fix for F2 having no effect
    @Test
    public void testInitialSelection() {
        assertEquals(-1, getSelectionModel().getSelectedIndex());
        assertEquals(null, getSelectionModel().getSelectedItem());
        assertTrue(getSelectionModel().isEmpty());
    }

    @Test
    public void testInitialFocus() {
        assertEquals(-1, getFocusIndex(-1));
    }

    @Test
    public void testInitialAnchor() {
        assertEquals(-1, getAnchorIndex(-1));
    }
    
    @Before
    public void setUp() throws Exception {
        // JW: need more items for multipleSelection
        items = FXCollections.observableArrayList(
                "9-item", "8-item", "7-item", "6-item", 
                "5-item", "4-item", "3-item", "2-item", "1-item");
        view = createView(items);
    }
    
    protected abstract V createView(ObservableList items);
    
    protected abstract T getSelectionModel();

    protected V getView() {
        return view;
    }
    
    /**
     * Returns the index of the anchor value. Note that subclasses which store a
     * compound value need to override and extract the index.
     * 
     * Same trick as with focusIndex: 
     * Subclasses that don't have the notion of anchor shcould override to
     * return the input index.
     * 
     * @return
     */
    protected int getAnchorIndex(int index) {
        Object anchor = getView().getProperties().get(ANCHOR_KEY);
        return anchor != null ? (int) anchor : -1;
    }


    /**
     * The signature is a bit of a hack: simple singleSelectionModels don't have the
     * notion of focus/anchor. The related tests don't make much sense then. Without 
     * having (or me not knowing them) parameterized ignors, the tests would fail. Too
     * lazy to override/ignore in each subclass, so this method will simply
     * return  the input index if focusModel == null.
     *  
     * @param index the default value for views that don't have a focusModel
     * @return 
     */
    protected int getFocusIndex(int index) {
        return getFocusModel() != null? getFocusModel().getFocusedIndex() : index;
    }
    
    protected abstract FocusModel getFocusModel();
    
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(SelectionIssues.class
            .getName());

}