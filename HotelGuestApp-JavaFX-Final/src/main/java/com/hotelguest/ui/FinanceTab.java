package com.hotelguest.ui;

import com.hotelguest.model.BillItem;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.hotelguest.ui.MainWindow.store;

public class FinanceTab {

    private final ScrollPane root;
    private final VBox content = new VBox(20);

    private final Label lblRevenue = new Label();
    private final Label lblProfit  = new Label();
    private final Label lblOpen    = new Label();
    private final Label lblWeek    = new Label();

    // Profit-per-guest table
    private final TableView<Guest> tblGuests = new TableView<>();

    // Bill items table — one row per unpaid bill item
    // columns: guestId (hidden), billId (hidden), guestName, description, amount, paid
    private final TableView<String[]> tblBills = new TableView<>();

    private final Canvas chartCanvas = new Canvas(600, 130);

    public FinanceTab() {
        content.setPadding(new Insets(20));
        buildSummaryTiles();
        buildGuestProfitTable();
        buildBillItemsTable();
        buildWeeklyChart();
        root = new ScrollPane(content);
        root.setFitToWidth(true);
        refresh();
    }

    // ── Summary tiles ────────────────────────────────────────────────────────

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
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 10; " +
                     "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),6,0,0,2);");
        return box;
    }

    // ── Profit per guest ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void buildGuestProfitTable() {
        tblGuests.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblGuests.setMaxHeight(240);

        TableColumn<Guest,String> cName    = col("Guest",     g -> g.getName());
        TableColumn<Guest,String> cRoom    = col("Room",      g -> g.getRoomTotal()    > 0 ? "$" + fmt(g.getRoomTotal())    : "—");
        TableColumn<Guest,String> cCar     = col("Car",       g -> g.getCarTotal()     > 0 ? "$" + fmt(g.getCarTotal())     : "—");
        TableColumn<Guest,String> cExtra   = col("Extras",    g -> g.getExtraCharges() > 0 ? "$" + fmt(g.getExtraCharges()) : "—");
        TableColumn<Guest,String> cCharged = col("Charged",   g -> "$" + fmt(g.getTotalCharged()));
        TableColumn<Guest,String> cOpen    = col("Open Bill", g -> g.getOpenBill() > 0 ? "$" + fmt(g.getOpenBill()) : "—");
        TableColumn<Guest,String> cProfit  = col("Profit",    g -> "$" + fmt(g.getProfit()));

        cOpen.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty); setText(empty ? null : s);
                setStyle(s != null && s.startsWith("$") ? "-fx-text-fill:#c0392b;-fx-font-weight:bold;" : "");
            }
        });
        cProfit.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty); setText(empty ? null : s);
                setStyle(empty ? "" : "-fx-text-fill:#27ae60;-fx-font-weight:bold;");
            }
        });

        tblGuests.getColumns().addAll(cName, cRoom, cCar, cExtra, cCharged, cOpen, cProfit);
        content.getChildren().addAll(sectionHeader("💰  Profit Per Guest"), tblGuests);
    }

    // ── Outstanding bill items with Pay button ───────────────────────────────

    @SuppressWarnings("unchecked")
    private void buildBillItemsTable() {
        tblBills.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblBills.setMaxHeight(200);

        // row = String[]{ guestId, billId, guestName, description, amount, paid }
        TableColumn<String[],String> cGuest = new TableColumn<>("Guest");
        cGuest.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[2]));

        TableColumn<String[],String> cDesc = new TableColumn<>("Description");
        cDesc.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[3]));

        TableColumn<String[],String> cAmt = new TableColumn<>("Amount");
        cAmt.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[4]));
        cAmt.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<String[],String> cPaid = new TableColumn<>("Paid?");
        cPaid.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[5]));
        cPaid.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty); setText(empty ? null : s);
                setStyle(empty ? "" : ("Yes".equals(s)
                    ? "-fx-text-fill:#27ae60;-fx-font-weight:bold;"
                    : "-fx-text-fill:#c0392b;-fx-font-weight:bold;"));
            }
        });

        tblBills.getColumns().addAll(cGuest, cDesc, cAmt, cPaid);

        // Pay buttons
        Button payOne = new Button("✓  Mark Selected as Paid");
        payOne.getStyleClass().add("btn-primary");
        payOne.setOnAction(e -> paySelected());

        Button payAll = new Button("Pay ALL for Guest…");
        payAll.getStyleClass().add("btn-secondary");
        payAll.setOnAction(e -> payAllForGuest());

        HBox btnRow = new HBox(10, payOne, payAll);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(
            sectionHeader("⚠  Outstanding Bill Items"),
            tblBills,
            btnRow
        );
    }

    private void paySelected() {
        String[] row = tblBills.getSelectionModel().getSelectedItem();
        if (row == null) {
            new Alert(Alert.AlertType.INFORMATION, "Please select a bill item first.", ButtonType.OK).showAndWait();
            return;
        }
        if ("Yes".equals(row[5])) {
            new Alert(Alert.AlertType.INFORMATION, "This item is already paid.", ButtonType.OK).showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Mark as paid?\n\nGuest:  " + row[2] + "\nItem:   " + row[3] + "\nAmount: " + row[4],
            ButtonType.YES, ButtonType.CANCEL);
        confirm.setTitle("Confirm Payment");
        confirm.showAndWait().filter(r -> r == ButtonType.YES).ifPresent(r -> {
            store.markPaid(row[0], row[1]);
            refresh();
        });
    }

    private void payAllForGuest() {
        String[] row = tblBills.getSelectionModel().getSelectedItem();
        if (row == null) {
            new Alert(Alert.AlertType.INFORMATION, "Select any bill item of the guest first.", ButtonType.OK).showAndWait();
            return;
        }
        String guestId   = row[0];
        String guestName = row[2];

        List<String> unpaidIds = new ArrayList<>();
        double total = 0;
        for (String[] r2 : tblBills.getItems()) {
            if (guestId.equals(r2[0]) && "No".equals(r2[5])) {
                unpaidIds.add(r2[1]);
                try { total += Double.parseDouble(r2[4].replace("$","")); } catch (NumberFormatException ignored) {}
            }
        }
        if (unpaidIds.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, guestName + " has no unpaid items.", ButtonType.OK).showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Mark ALL unpaid items as paid for " + guestName + "?\n\n" +
            unpaidIds.size() + " item(s)  ·  Total: $" + String.format("%.2f", total),
            ButtonType.YES, ButtonType.CANCEL);
        confirm.setTitle("Confirm Full Payment");
        confirm.showAndWait().filter(r -> r == ButtonType.YES).ifPresent(r -> {
            for (String bid : unpaidIds) store.markPaid(guestId, bid);
            refresh();
        });
    }

    // ── Weekly chart ─────────────────────────────────────────────────────────

    private void buildWeeklyChart() {
        content.getChildren().addAll(sectionHeader("📊  Weekly Profit (Last 4 Weeks)"), chartCanvas);
    }

    // ── Refresh ──────────────────────────────────────────────────────────────

    public void refresh() {
        lblRevenue.setText("$" + fmt(store.getTotalRevenue()));
        lblProfit .setText("$" + fmt(store.getTotalProfit()));
        lblOpen   .setText("$" + fmt(store.getTotalOpenBills()));
        lblWeek   .setText("$" + fmt(store.getProfitForWeek(LocalDate.now())));

        tblGuests.setItems(FXCollections.observableArrayList(
            store.getGuests().stream()
                .filter(g -> g.getTotalCharged() > 0)
                .sorted((a, b) -> Double.compare(b.getProfit(), a.getProfit()))
                .toList()
        ));

        // Build bill items rows
        List<String[]> rows = new ArrayList<>();
        store.getGuests().stream()
            .filter(g -> g.getOpenBill() > 0)
            .sorted((a, b) -> a.getName().compareTo(b.getName()))
            .forEach(g -> g.getBillItems().stream()
                .filter(b -> !b.isPaid())
                .forEach(b -> rows.add(new String[]{
                    g.getId(), b.getId(), g.getName(),
                    b.getDescription(), "$" + fmt(b.getAmount()), "No"
                }))
            );
        tblBills.setItems(FXCollections.observableArrayList(rows));

        drawWeeklyChart();
    }

    private void drawWeeklyChart() {
        double w = chartCanvas.getWidth(), h = chartCanvas.getHeight();
        GraphicsContext gc = chartCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.web("#f8f9fa")); gc.fillRoundRect(0, 0, w, h, 10, 10);

        double[] profits = new double[4];
        String[]  labels  = new String[4];
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 4; i++) {
            LocalDate d = today.minusWeeks(3 - i);
            profits[i] = store.getProfitForWeek(d);
            labels[i]  = d.with(java.time.DayOfWeek.MONDAY).format(fmt);
        }
        double max = 1;
        for (double p : profits) if (p > max) max = p;

        double barW = 80, gap = (w - 4 * barW) / 5;
        for (int i = 0; i < 4; i++) {
            double barH = Math.max(4, (profits[i] / max) * (h - 40));
            double x = gap + i * (barW + gap);
            double y = h - 30 - barH;
            gc.setFill(Color.web("#2ecc71", 0.85)); gc.fillRoundRect(x, y, barW, barH, 6, 6);
            gc.setFill(Color.web("#27ae60")); gc.setFont(Font.font(11));
            gc.fillText("$" + (int)profits[i], x + barW/2 - 20, Math.max(14, y - 4));
            gc.setFill(Color.web("#555")); gc.fillText(labels[i], x + barW/2 - 22, h - 8);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private TableColumn<Guest, String> col(String title, java.util.function.Function<Guest,String> fn) {
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
