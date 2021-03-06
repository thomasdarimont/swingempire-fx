/*
 * Created on 14.10.2014
 *
 */
package de.swingempire.fx.chart;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collector;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Correlate colors between different charts
 * http://stackoverflow.com/q/28217355/203657
 * 
 * Accepted answer: have a map of which data is at which location (sequence)
 * in the first chart, in the second (or other dependents) update the default
 * colors by setting style classes to the default-something-xx from the first.
 * Has to be done after styling! didn't check for side-effects.
 * 
 * @author Jeanette Winzenburg, Berlin
 */
public class CorrelatedChartColors extends Application {

    @Override
    public void start(Stage primaryStage) {
        PieChart pieChart = new PieChart();
        pieChart.setData(getPieData());
        
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        final StackedBarChart<String, Number> sbc =
                new StackedBarChart<>(xAxis, yAxis);
        ObservableList<Series<String, Number>> barData = createBarData(getPieData());
        // simulate client code that re-orders/filters the data
        FXCollections.shuffle(barData);
        sbc.setData(barData);

        primaryStage.setTitle("Correlated Charts");
        Scene scene = new Scene(new HBox(pieChart, sbc));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Creates and returns data for StackedBarChart from the given pieData.
     */
    @SuppressWarnings("unchecked")
    private ObservableList<Series<String, Number>> createBarData(
            ObservableList<Data> pieChartData) {
        ObservableList<Series<String, Number>> data = pieChartData.stream()
            .map(p -> new XYChart.Data<>("none", (Number) p.getPieValue(), p.getName())) 
            .map(xy -> new Series<>((String)xy.getExtraValue(), 
                        FXCollections.observableArrayList(xy)))
            .collect(toObservableList())
//            .collect(Collectors.collectingAndThen(Collectors.toList(), FXCollections::observableArrayList));
        ;
        return data;
    }

    /**
     * Creates and returns data for PieChart.
     */
    private ObservableList<Data> getPieData() {
        ObservableList<Data> pieData = FXCollections.observableArrayList();
        pieData.addAll(new PieChart.Data("java", 17.56), 
                new PieChart.Data("C", 17.06), 
                new PieChart.Data("PHP", 6.0),
                new PieChart.Data("(Visual)Basic", 4.76),
                new PieChart.Data("Other", 31.37));
        return pieData;
    }
    
    public static <T>  Collector<T, ?, ObservableList<T>> toObservableList() {
        return Collector.of((Supplier<ObservableList<T>>) FXCollections::observableArrayList,
                List::add,
                (left, right) -> {
                    left.addAll(right);
                    return left;
                });
    }

    public static void main(String[] args) {
        launch(args);
    }

}
