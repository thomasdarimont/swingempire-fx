/*
 * Created on 24.09.2014
 *
 */
package de.swingempire.fx.scene.control.choiceboxx;

/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */

import java.util.logging.Logger;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.WeakListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import com.sun.javafx.scene.control.skin.ContextMenuContent;

import de.swingempire.fx.property.PathAdapter;


/**
 * C&P'ed core
 * 
 * Experimentation with
 * 
 * - use ListProperty of ChoiceBoxX
 * - cleanup selection code
 * 
 * Changes:
 * 
 * - reflective access to ContextMenuContent.getYMenuOffset
 * - fix https://javafx-jira.kenai.com/browse/RT-38826
 * - extracted method to get displayString and use it whereever the label's text is set
 * - changed hard-coded emptying of label to use converter (allows custom values for null)
 * - changed logic/implementation of selectionChanged to not always clear for 
 *   selectedIndex == -1
 * - changed hard-coded reaction to SELECTION_CHANGED to delegate to selectionChanged method  
 * - Note: should **not** listen to selectedIndex: doesn't get change if uncontained selected
 *   item is changed
 * - changed wiring to listen to selectedItem (or to value)
 * - fixed: converter change didn't update label text
 * - removed indirect listening to selectedItemProperty via registerChangeListener (will break
 *   anyway on change of selectionModel)
 * - replaced manual selection/Model/itemListener by pathAdapter
 * - replaced manual items/content/Listener by listening to itemsListProperty
 * - added support for separatorItem
 * 
 * ChoiceBoxSkin - default implementation
 */
public class ChoiceBoxXSkin<T> extends BehaviorSkinBase<ChoiceBoxX<T>, ChoiceBoxXBehavior<T>> {

    public ChoiceBoxXSkin(ChoiceBoxX<T> control) {
        super(control, new ChoiceBoxXBehavior<T>(control));
        initialize();
        control.requestLayout();
//        registerChangeListener(control.selectionModelProperty(), "SELECTION_MODEL");
        registerChangeListener(control.showingProperty(), "SHOWING");
//        registerChangeListener(control.itemsProperty(), "ITEMS");
//        registerChangeListener(control.getSelectionModel().selectedItemProperty(), "SELECTION_CHANGED");
        registerChangeListener(control.converterProperty(), "CONVERTER");
    }

    private ContextMenu popup;

    // The region that shows the "arrow" box portion
    private StackPane openButton;

    private final ToggleGroup toggleGroup = new ToggleGroup();

    /*
     * Watch for if the user changes the selected index, and if so, we toggle
     * the selection in the toggle group (so the check shows in the right place)
     */
    private SelectionModel<T> selectionModel;

    private Label label;

    private final ListChangeListener<Integer> separatorsListListener = c -> {
        updatePopupItems();
        updateSelection();
        getSkinnable().requestLayout();
    };
    
    private final ListChangeListener<Integer> weakSeparatorsListListener = new WeakListChangeListener<>(separatorsListListener);
    
    private final ListChangeListener<T> choiceBoxItemsListener = new ListChangeListener<T>() {
        @Override public void onChanged(Change<? extends T> c) {
            // brute force fix for RT-38394:
            // update popupItems from scratch on every change
            updatePopupItems();
//            while (c.next()) {
//                if (c.getRemovedSize() > 0 || c.wasPermutated()) {
//                    toggleGroup.getToggles().clear();
//                    popup.getItems().clear();
//                    int i = 0;
//                    for (T obj : c.getList()) {
//                        addPopupItem(obj, i);
//                        i++;
//                    }
//                } else {
//                    for (int i = c.getFrom(); i < c.getTo(); i++) {
//                        final T obj = c.getList().get(i);
//                        addPopupItem(obj, i);
//                    }
//                }
//            }
            // PENDING JW really needed? building up popupItems from scratch should take 
            // care of selection?
            updateSelection();
            getSkinnable().requestLayout(); // RT-18052 resize of choicebox should happen immediately.
        }
    };
    
    private final WeakListChangeListener<T> weakChoiceBoxItemsListener =
            new WeakListChangeListener<T>(choiceBoxItemsListener);

    private PathAdapter<SingleSelectionModel<T>, T> selectedItemPath;

    private void initialize() {
//        updateChoiceBoxItems();
        
        selectedItemPath = new PathAdapter<>(getSkinnable().selectionModelProperty(), p -> p.selectedItemProperty());
        selectedItemPath.addListener((p, old, value) -> {
            updateSelection();
        } );
        
        getSkinnable().itemsListProperty().addListener(weakChoiceBoxItemsListener);
        getSkinnable().separatorsListProperty().addListener(weakSeparatorsListListener);
       
        label = new Label();
        label.setMnemonicParsing(false);  // ChoiceBox doesn't do Mnemonics

        openButton = new StackPane();
        openButton.getStyleClass().setAll("open-button");

        StackPane region = new StackPane();
        region.getStyleClass().setAll("arrow");
        openButton.getChildren().clear();
        openButton.getChildren().addAll(region);

        popup = new ContextMenu();
        // When popup is hidden by autohide - the ChoiceBox Showing property needs
        // to be updated. So we listen to when autohide happens. Calling hide()
        // there after causes Showing to be set to false
        popup.setOnAutoHide(event -> {
            getSkinnable().hide();
        });
        // fix RT-14469 : When tab shifts ChoiceBox focus to another control,
        // its popup should hide.
        getSkinnable().focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                getSkinnable().hide();
            }
        });
        // This is used as a way of accessing the context menu within the ChoiceBox.
        popup.setId("choice-box-popup-menu");
        getChildren().setAll(label, openButton);

        updatePopupItems();

        updateSelectionModel();
        updateSelection();
    }

    /**
     * PENDING JW: 
     * NO-OP - remove
     */
//    private void updateChoiceBoxItems() {
//        if (getChoiceBoxItems() != null) {
//            getChoiceBoxItems().removeListener(weakChoiceBoxItemsListener);
//        }
//        choiceBoxItems = getSkinnable().getItems();
//        if (getChoiceBoxItems() != null) {
//            getChoiceBoxItems().addListener(weakChoiceBoxItemsListener);
//        }
//    }
    
    // Test only purpose    
    String getChoiceBoxSelectedText() {
        return label.getText();
    }

    @SuppressWarnings("rawtypes")
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        if ("ITEMS".equals(p)) {
            throw new IllegalStateException("shouldn't get here");
            // PENDING JW: unused - remove 
//            updateChoiceBoxItems();
//            updatePopupItems();
//            updateSelectionModel();
//            updateSelection();
//            if(selectionModel != null && selectionModel.getSelectedIndex() == -1) {
//                // PENDING JW: uncontained items are explicitly allowed
//                // you **MUST** show them!
//                label.setText(""); // clear label text when selectedIndex is -1
//            }
        } else if (("SELECTION_MODEL").equals(p)) {
            throw new IllegalStateException("shouldn't get here");
//            // PENDING JW: unused - remove
//            updateSelectionModel();
//            // CHANGED JW: RT-38724
//            updateSelection();
        } else if ("SHOWING".equals(p)) {
            if (getSkinnable().isShowing()) {
                MenuItem item = null;

                SelectionModel sm = getSkinnable().getSelectionModel();
                if (sm == null) return;

                long currentSelectedIndex = sm.getSelectedIndex();
                int itemInControlCount = getChoiceBoxItems().size();
                boolean hasSelection = currentSelectedIndex >= 0 && currentSelectedIndex < itemInControlCount;
                if (hasSelection) {
                    // CHANGED JW: use findItem
                    item = getMenuItemFor((int) currentSelectedIndex);
//                    item = popup.getItems().get((int) currentSelectedIndex);
                    if (item != null && item instanceof RadioMenuItem) ((RadioMenuItem)item).setSelected(true);
                } else {
                    if (itemInControlCount > 0) item = popup.getItems().get(0);
                }

                // This is a fix for RT-9071. Ideally this won't be necessary in
                // the long-run, but for now at least this resolves the
                // positioning
                // problem of ChoiceBox inside a Cell.
                getSkinnable().autosize();
                // -- End of RT-9071 fix

                double y = 0;

                if (popup.getSkin() != null) {
                    // PENDING JW: need to adjust for separator!
                    ContextMenuContent cmContent = (ContextMenuContent)popup.getSkin().getNode();
                    if (cmContent != null && currentSelectedIndex != -1) {
                        // PENDING JW: implement the invoke, returns 0 for now
//                        y = -(cmContent.getMenuYOffset((int)currentSelectedIndex));
                        y = - invokeMenuYOffset(cmContent, (int) currentSelectedIndex);
                    }
                }

                popup.show(getSkinnable(), Side.BOTTOM, 2, y);
            } else {
                popup.hide();
            }
        } else if ("CONVERTER".equals(p)) {
            // PENDING JW: no need to update the items ... 
            // if it appears to be needed, somehting is wrong elsewhere
//            updateChoiceBoxItems();
            updatePopupItems();
            // CHANGED JW: need to update label
            updateLabel();
        }
    }

    protected ObservableList<T> getChoiceBoxItems() {
        return getSkinnable().getItems();
    }

    /**
     * PENDING JW: need to reflectively access package private method.
     * 
     * @param content
     * @param index
     * @return
     */
    private double invokeMenuYOffset(ContextMenuContent content, int index) {
        double offset = 0;
        return offset;
    }
    
    /**
     * Changed to enhance separator support.
     * Supports SeparatorMarker (discouraged) and separatorList.
     * 
     * The latter implies that the index-in-items may be != index-in-menuItems.
     * Implemented by:
     * - set a property "data-index" in the menuItem with value = index-in-items
     * - simple add of menuItems
     * - insert separatorItem if necessary
     * 
     * @param o the item in choiceBox' items to map an menuitem to
     * @param i the position of the choiceBox item in its list
     */
    private void addPopupItem(final T o, int i) {
        MenuItem popupItem = null;
        // CHANGED JW: added check for separatorItem
        if (o instanceof Separator || o instanceof SeparatorMarker) {
            // We translate the Separator into a SeparatorMenuItem...
            popupItem = new SeparatorMenuItem();
        } else if (o instanceof SeparatorMenuItem) {
            popupItem = (SeparatorMenuItem) o;
        } else {
            String displayString = getDisplayString(o);
            final RadioMenuItem item = new RadioMenuItem(displayString);
            item.setId("choice-box-menu-item");
            item.getProperties().put("data-index", i);
            item.setToggleGroup(toggleGroup);
            item.setOnAction(e -> {
                if (selectionModel == null) return;
                // blows on duplicates anyway
                int index = getSkinnable().getItems().indexOf(o);
                if (index != (Integer) item.getProperties().get("data-index")) {
                    throw new IllegalStateException("index mismatch: items/popup " 
                            + index + "/" + item.getProperties().get("data-index"));
                }
                selectionModel.select(index);
                item.setSelected(true);
            });
            popupItem = item;
        }
        popupItem.setMnemonicParsing(false);   // ChoiceBox doesn't do Mnemonics
        // CHANGED JW: replaced by simple add
//        popup.getItems().add(i, popupItem);
        popup.getItems().add(popupItem);
        addSeparator(i);
    }
    /**
     * Inserts a separator if the separatorList contains an item with
     * value index
     * @param index
     */
    private void addSeparator(int index) {
        if (!getSkinnable().separatorsListProperty().contains(index)) return;
        popup.getItems().add(new SeparatorMenuItem());
    }

    /**
     * Extracted as fix for RT-38826: label must show uncontained value
     * This method is used whereever we need to display the item, namely
     * when creating the menuItem and updating the label.
     * 
     */
    protected String getDisplayString(final T o) {
        StringConverter<T> c = getSkinnable().getConverter();
        String displayString = (c == null) ? ((o == null) ? "" : o.toString()) :  c.toString(o);
        return displayString;
    }

    private void updatePopupItems() {
        toggleGroup.getToggles().clear();
        popup.getItems().clear();
        toggleGroup.selectToggle(null);

        for (int i = 0; i < getChoiceBoxItems().size(); i++) {
            T o = getChoiceBoxItems().get(i);
            addPopupItem(o, i);
        }
    }

    /**
     * Changed implementation to listen for selectedItemProperty (vs. selectedIndexProperty)
     */
    private void updateSelectionModel() {
//        if (selectionModel != null) {
//            selectionModel.selectedItemProperty().removeListener(selectionChangeListener);
//        }
        this.selectionModel = getSkinnable().getSelectionModel();
//        if (selectionModel != null) {
//            selectionModel.selectedItemProperty().addListener(selectionChangeListener);
//        }
    }

//    private InvalidationListener selectionChangeListener = observable -> {
//        updateSelection();
//    };

    /**
     * PENDING JW: change logic such that we can extract a method that
     * updates the displayValue
     */
    private void updateSelection() {
        // PENDING JW: this is where label was reset to empty always for selectedIndex < 0
        // unspecified behaviour of isEmpty, see https://javafx-jira.kenai.com/browse/RT-38494
        if (selectionModel == null) { // || selectionModel.isEmpty()) {
            toggleGroup.selectToggle(null);
        } else {
            int selectedIndex = selectionModel.getSelectedIndex();
            RadioMenuItem selectedMenuItem = getMenuItemFor(selectedIndex);
            if (selectedMenuItem != null) {
                selectedMenuItem.setSelected(true);
                toggleGroup.selectToggle(null);
            }
//            if (selectedIndex >=0 && selectedIndex < popup.getItems().size()) {
//                MenuItem selectedItem = popup.getItems().get(selectedIndex);
//                if (selectedItem instanceof RadioMenuItem) {
//                    ((RadioMenuItem) selectedItem).setSelected(true);
//                    // PENDING JW: why null the toggleGroup?
//                    toggleGroup.selectToggle(null);
//                }
//            }
        }
        updateLabel();
    }

    /**
     * @param selectedIndex
     * @return
     */
    private RadioMenuItem getMenuItemFor(int dataIndex) {
        if (dataIndex < 0) return null;
        int loopIndex = dataIndex;
        while (loopIndex < popup.getItems().size()) {
            MenuItem item = popup.getItems().get(loopIndex);
            
            ObservableMap<Object, Object> properties = item.getProperties();
            Object object = properties.get("data-index");
            if ((object instanceof Integer) && dataIndex == (Integer) object) {
                return item instanceof RadioMenuItem ? (RadioMenuItem)item : null;
            }
            loopIndex++;
        }
        return null;
    }

    /**
     * Updates Label to show choiceBox' value
     */
    protected void updateLabel() {
        label.setText(getDisplayString(getSkinnable().getValue()));
    }
    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {
        // open button width/height
        double obw = openButton.prefWidth(-1);

        label.resizeRelocate(x, y, w, h);
        openButton.resize(obw, openButton.prefHeight(-1));
        positionInArea(openButton, (x+w) - obw,
                y, obw, h, /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
    }

    @Override protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        final double boxWidth = label.minWidth(-1) + openButton.minWidth(-1);
        final double popupWidth = popup.minWidth(-1);
        return leftInset + Math.max(boxWidth, popupWidth) + rightInset;
    }

    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        final double displayHeight = label.minHeight(-1);
        final double openButtonHeight = openButton.minHeight(-1);
        return topInset + Math.max(displayHeight, openButtonHeight) + bottomInset;
    }

    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        final double boxWidth = label.prefWidth(-1)
                + openButton.prefWidth(-1);
        double popupWidth = popup.prefWidth(-1);
        if (popupWidth <= 0) { // first time: when the popup has not shown yet
            if (popup.getItems().size() > 0){
                // PENDING JW: blows if first is separator
                popupWidth = (new Text(((MenuItem)popup.getItems().get(0)).getText())).prefWidth(-1);
            }
        }
        return (popup.getItems().size() == 0) ? 50 : leftInset + Math.max(boxWidth, popupWidth)
                + rightInset;
    }

    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        final double displayHeight = label.prefHeight(-1);
        final double openButtonHeight = openButton.prefHeight(-1);
        return topInset
                + Math.max(displayHeight, openButtonHeight)
                + bottomInset;
    }
    
    @Override protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return getSkinnable().prefHeight(width);
    }
    
    @Override protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return getSkinnable().prefWidth(height);
    }
    
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(ChoiceBoxXSkin.class
            .getName());
}