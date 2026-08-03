package com.hotelguest.ui;

import com.hotelguest.model.Guest;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.hotelguest.ui.MainWindow.store;

public class FinanceTab {

    private final ScrollPane root;
    private final VBox content = new VBox(20);

    // Summary labels
    private final Label lblRevenue  = new Label();
    private final Label lblProfit   = new Label();
    private final Label lblOpen     = new Label();
    private final Label lblWeek     = new Label();

    private final TableView<Guest> tblGuests = new TableView<>();
    private final TableView<Guest> tblBills  = new TableView<>();
    private final Canvas chartCanvas = new Canvas(600, 130);

    public FinanceTab() {
        content.setPadding(new Insets(20));
        buildSummaryTiles();
        buildGuestProfitTable();
        buildOpenBillsTable();
        buildWeeklyChart();
        root = new ScrollPane(content);
        root.setFitToWidth(true);
        refresh();
    }

    private void buildSummaryTiles() {
        HBox tiles = new HBox(16,
            tile("Total Revenue",    lblRevenue, "#2980b9"),
            tile("Total Profit",     lblProfit,  "#27ae60"),
            tile("Open Bills",       lblOpen,    "#c0392b"),
            tile("This Week Profit", lblWeek,    "#e67e22")
        );
        tiles.setFillHeight(true);
        for (Node n : tiles.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        content.getChildren().add(tiles);
    }

    private VBox tile(String caption, Label value, String color) {
        value.setFont(Font.font("System", FontWeight.BOLD, 22));
        value.setStyle("-fx-text-fill: " + color + ";");
        Label cap = new Label(caption);
        cap.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        VBox box = new VBox(6, value, cap);
        box.setPadding(new Insets(18));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),6,0,0,2);");
        return box;
    }

    @SuppressWarnings("unchecked")
    private void buildGuestProfitTable() {
        tblGuests.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblGuests.setMaxHeight(240);

        TableColumn<Guest,String> cName    = col("Guest",      g -> g.getName());
        TableColumn<Guest,String> cRoom    = col("Room",       g -> g.getRoomTotal()  > 0 ? "$" + fmt(g.getRoomTotal())  : "—");
        TableColumn<Guest,String> cCar     = col("Car",        g -> g.getCarTotal()   > 0 ? "$" + fmt(g.getCarTotal())   : "—");
        TableColumn<Guest,String> cExtra   = col("Extras",     g -> g.getExtraCharges()>0 ? "$" + fmt(g.getExtraCharges()): "—");
        TableColumn<Guest,String> cCharged = col("Charged",    g -> "$" + fmt(g.getTotalCharged()));
        TableColumn<Guest,String> cOpen    = col("Open Bill",  g -> g.getOpenBill() > 0 ? "$" + fmt(g.getOpenBill()) : "—");
        TableColumn<Guest,String> cProfit  = col("Profit",     g -> "$" + fmt(g.getProfit()));

        cOpen.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty); setText(empty ? null : s);
                setStyle((s != null && s.startsWith("$")) ? "-fx-text-fill:#c0392b;-fx-font-weight:bold;" : "");
            }
        });
        cProfit.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty); setText(empty ? null : s);
                setStyle(empty ? "" : "-fx-text-fill:#27ae60;-fx-font-weight:bold;");
            }
        });

        tblGuests.getColumns().addAll(cName, cRoom, cCar, cExtra, cCharged, cOpen, cProfit);

        Label hdr = sectionHeader("💰  Profit Per Guest");
        content.getChildren().addAll(hdr, tblGuests);
    }

    @SuppressWarnings("unchecked")
    private void buildOpenBillsTable() {
        tblBills.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblBills.setMaxHeight(180);

        TableColumn<Guest,String> cName = col("Guest",    g -> g.getName());
        TableColumn<Guest,String> cRoom = col("Room",     g -> g.getRoomType().getLabel());
        TableColumn<Guest,String> cBill = col("Open Bill",g -> "$" + fmt(g.getOpenBill()));
        cBill.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty); setText(empty ? null : s);
                setStyle(empty ? "" : "-fx-text-fill:#c0392b;-fx-font-weight:bold;");
            }
        });
        tblBills.getColumns().addAll(cName, cRoom, cBill);

        content.getChildren().addAll(sectionHeader("⚠  Outstanding Bills"), tblBills);
    }

    private void buildWeeklyChart() {
        content.getChildren().addAll(sectionHeader("📊  Weekly Profit (Last 4 Weeks)"), chartCanvas);
    }

    public void refresh() {
        lblRevenue.setText("$" + fmt(store.getTotalRevenue()));
        lblProfit.setText ("$" + fmt(store.getTotalProfit()));
        lblOpen.setText   ("$" + fmt(store.getTotalOpenBills()));
        lblWeek.setText   ("$" + fmt(store.getProfitForWeek(LocalDate.now())));

        List<Guest> byProfit = store.getGuests().stream()
            .filter(g -> g.getTotalCharged() > 0)
            .sorted((a, b) -> Double.compare(b.getProfit(), a.getProfit()))
            .toList();
        tblGuests.setItems(FXCollections.observableArrayList(byProfit));

        List<Guest> withBills = store.getGuests().stream()
            .filter(g -> g.getOpenBill() > 0)
            .sorted((a, b) -> Double.compare(b.getOpenBill(), a.getOpenBill()))
            .toList();
        tblBills.setItems(FXCollections.observableArrayList(withBills));

        drawWeeklyChart();
    }

    private void drawWeeklyChart() {
        double w = chartCanvas.getWidth(), h = chartCanvas.getHeight();
        GraphicsContext gc = chartCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.web("#f8f9fa")); gc.fillRoundRect(0, 0, w, h, 10, 10);

        double[] profits = new double[4];
        String[]  labels = new String[4];
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 4; i++) {
            LocalDate date = today.minusWeeks(3 - i);
            profits[i] = store.getProfitForWeek(date);
            LocalDate mon = date.with(java.time.DayOfWeek.MONDAY);
            labels[i] = mon.format(fmt);
        }

        double max = 1;
        for (double p : profits) if (p > max) max = p;

        double barW = 80, gap = (w - 4 * barW) / 5;
        for (int i = 0; i < 4; i++) {
            double barH = (profits[i] / max) * (h - 40);
            double x = gap + i * (barW + gap);
            double y = h - 30 - barH;
            gc.setFill(Color.web("#2ecc71", 0.85)); gc.fillRoundRect(x, y, barW, barH, 6, 6);
            gc.setFill(Color.web("#27ae60")); gc.setFont(Font.font(11));
            gc.fillText("$" + (int)profits[i], x + barW/2 - 20, y - 4);
            gc.setFill(Color.web("#555")); gc.fillText(labels[i], x + barW/2 - 22, h - 8);
        }
    }

    private <T> TableColumn<Guest, String> col(String title, java.util.function.Function<Guest,String> fn) {
        TableColumn<Guest,String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleStringProperty(fn.apply(cd.getValue())));
        return c;
    }

    private String fmt(double v) { return String.format("%.2f", v); }
    private Label sectionHeader(String t) {
        Label l = new Label(t); l.setFont(Font.font("System", FontWeight.BOLD, 14));
        l.setStyle("-fx-text-fill: #333;"); return l;
    }

    public Node getRoot() { return root; }
}
