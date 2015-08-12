/*
 * Created on 12.08.2015
 *
 */
package de.swingempire.fx.scene.control.cell;

import java.util.logging.Logger;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import de.swingempire.fx.demobean.Person;
import de.swingempire.fx.util.FXUtils;

/**
 * Configure choice items based on row item.
 * http://stackoverflow.com/q/31959021/203657
 * 
 * @author Jeanette Winzenburg, Berlin
 */
public class ChoiceBoxTableCellDynamic extends Application {

    public static interface ChoiceItemProvider<S, T> {
        ObservableList<T> getItems(S source);
    }
    public static class DynamicChoiceBoxTableCell<S, T> extends ChoiceBoxTableCell<S, T> {

        private ChoiceItemProvider<S, T> provider;

        public DynamicChoiceBoxTableCell(ChoiceItemProvider<S, T> provider) {
            super();
            this.provider = provider;
        }
        
        
        /**
         * Not so obvious hook: overridden to update the items of the 
         * choiceBox.
         */
        @Override
        public void startEdit() {
            super.startEdit();
            updateItems();
        }

        /**
         * Obvious hook: override to update the items of the choiceBox.
         * Not fully working - for some reason, the current item isn't
         * selected after starting the edit.
         */
        @Override
        public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            // updateItems();
        }

        /**
         * Dynamically updates the items to current rowItem. 
         */
        @SuppressWarnings("unchecked")
        protected void updateItems() {
            TableRow<S> tableRow = getTableRow();
            S rowItem = tableRow != null ? tableRow.getItem() : null;
            if (provider == null || rowItem == null) return;
            if (provider != null) {
                getItems().setAll(provider.getItems(rowItem));
            } 
        }
    }
    
    private Parent getContent() {
        TableView<Person> table = new TableView<>();
        table.setEditable(true);
        table.setItems(Person.persons());
        TableColumn<Person, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        
        TableColumn<Person, String> email = new TableColumn<>("email");
        email.setCellValueFactory(new PropertyValueFactory<>("email"));
        ChoiceItemProvider<Person, String> provider = rowItem -> {
            String current = rowItem.getEmail();
            String prefix = rowItem.getFirstName().toLowerCase() + "." + rowItem.getLastName().toLowerCase() + "@";
            return FXCollections.observableArrayList(prefix + "example.com", prefix + "alternative.com");
        };
        email.setCellFactory(cb -> new DynamicChoiceBoxTableCell<>(provider));
        table.getColumns().addAll(name, email);
        VBox content = new VBox(10, table);
        return content;
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(getContent()));
        primaryStage.setTitle(FXUtils.version());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
    
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger
            .getLogger(ChoiceBoxTableCellDynamic.class.getName());
}
