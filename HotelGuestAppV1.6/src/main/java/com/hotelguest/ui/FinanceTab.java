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
import java.util.ArrayList;
import java.util.List;

import static com.hotelguest.ui.MainWindow.store;

public class FinanceTab {

    private final ScrollPane root;
    private final VBox content = new VBox(20);
    private final Label lblRevenue = new Label();
    private final Label lblProfit  = new Label();
    private final Label lblOpen    = new Label();
    private final Label lblWeek    = new Label();
    private final TableView<Guest>    tblGuests = new TableView<>();
    private final TableView<String[]> tblBills  = new TableView<>();
    private final Canvas chartCanvas = new Canvas(600, 130);

    // Finance always in ZMW
    private String fmt(double v) { return String.format("K%.2f", v); }

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

    private void buildSummaryTiles() {
        HBox tiles = new HBox(16,
            tile("Total Revenue (ZMW)",    lblRevenue, "#2980b9"),
            tile("Total Profit (ZMW)",     lblProfit,  "#27ae60"),
            tile("Open Bills (ZMW)",       lblOpen,    "#c0392b"),
            tile("This Week (ZMW)",        lblWeek,    "#e67e22")
        );
        tiles.setFillHeight(true);
        for (Node n : tiles.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        content.getChildren().add(tiles);
    }

    private VBox tile(String caption, Label value, String color) {
        value.setFont(Font.font("System", FontWeight.BOLD, 22));
        value.setStyle("-fx-text-fill:"+color+";");
        Label cap = new Label(caption);
        cap.setStyle("-fx-text-fill:#666;-fx-font-size:12px;");
        VBox box = new VBox(6, value, cap);
        box.setPadding(new Insets(18));
        box.setStyle("-fx-background-color:white;-fx-background-radius:10;" +
                     "-fx-border-color:#e0e0e0;-fx-border-radius:10;" +
                     "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),6,0,0,2);");
        return box;
    }

    @SuppressWarnings("unchecked")
    private void buildGuestProfitTable() {
        tblGuests.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblGuests.setMaxHeight(240);

        // Charged in booking currency; everything else ZMW
        TableColumn<Guest,String> cName    = gcol("Guest",    g -> g.getName());
        TableColumn<Guest,String> cRoom    = gcol("Room(K)",  g -> fmt(g.getRoomTotal()));
        TableColumn<Guest,String> cCar     = gcol("Car(K)",   g -> fmt(g.getCarTotal()));
        TableColumn<Guest,String> cBf      = gcol("Bfast(K)", g -> fmt(g.getBreakfastTotal()));
        TableColumn<Guest,String> cExtra   = gcol("Extra(K)", g -> fmt(g.getExtraCharges()));
        TableColumn<Guest,String> cCharged = gcol("Charged",  g -> g.fmtBooked(g.getTotalCharged()));
        TableColumn<Guest,String> cOpen    = gcol("Open(K)",  g -> g.getOpenBill()>0?fmt(g.getOpenBill()):"—");
        TableColumn<Guest,String> cProfit  = gcol("Profit(K)",g -> fmt(g.getProfit()));

        cOpen.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s,boolean empty) {
                super.updateItem(s,empty); setText(empty?null:s);
                setStyle(s!=null&&!s.equals("—")?"-fx-text-fill:#c0392b;-fx-font-weight:bold;":"");
            }
        });
        cProfit.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s,boolean empty) {
                super.updateItem(s,empty); setText(empty?null:s);
                setStyle(empty?"":"-fx-text-fill:#27ae60;-fx-font-weight:bold;");
            }
        });

        tblGuests.getColumns().addAll(cName,cRoom,cCar,cBf,cExtra,cCharged,cOpen,cProfit);
        content.getChildren().addAll(sectionHeader("💰  Profit Per Guest (ZMW unless marked)"),tblGuests);
    }

    @SuppressWarnings("unchecked")
    private void buildBillItemsTable() {
        tblBills.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblBills.setMaxHeight(200);

        // row = String[]{ guestId, billId, guestName, description, amountZMW, paid }
        TableColumn<String[],String> cGuest = new TableColumn<>("Guest");
        cGuest.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[2]));
        TableColumn<String[],String> cDesc  = new TableColumn<>("Description");
        cDesc.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[3]));
        TableColumn<String[],String> cAmt   = new TableColumn<>("Amount (ZMW)");
        cAmt.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[4]));
        TableColumn<String[],String> cPaid  = new TableColumn<>("Paid?");
        cPaid.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[5]));
        cPaid.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s,boolean empty) {
                super.updateItem(s,empty); setText(empty?null:s);
                setStyle(empty?"":(s.equals("Yes")?"-fx-text-fill:#27ae60;-fx-font-weight:bold;"
                    :"-fx-text-fill:#c0392b;-fx-font-weight:bold;"));
            }
        });
        tblBills.getColumns().addAll(cGuest,cDesc,cAmt,cPaid);

        Button payOne = new Button("✓  Mark Selected as Paid");
        payOne.getStyleClass().add("btn-primary");
        payOne.setOnAction(e -> paySelected());
        Button payAll = new Button("Pay ALL for Guest…");
        payAll.getStyleClass().add("btn-secondary");
        payAll.setOnAction(e -> payAllForGuest());

        HBox btnRow = new HBox(10, payOne, payAll);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().addAll(sectionHeader("⚠  Outstanding Bill Items (ZMW)"),tblBills,btnRow);
    }

    private void paySelected() {
        String[] row = tblBills.getSelectionModel().getSelectedItem();
        if (row==null) { new Alert(Alert.AlertType.INFORMATION,"Select a bill item first.",ButtonType.OK).showAndWait(); return; }
        if ("Yes".equals(row[5])) { new Alert(Alert.AlertType.INFORMATION,"Already paid.",ButtonType.OK).showAndWait(); return; }
        new Alert(Alert.AlertType.CONFIRMATION,
            "Mark as paid?\n\nGuest: "+row[2]+"\nItem:  "+row[3]+"\nAmount: "+row[4],
            ButtonType.YES,ButtonType.CANCEL).showAndWait()
            .filter(r->r==ButtonType.YES).ifPresent(r->{ store.markPaid(row[0],row[1]); refresh(); });
    }

    private void payAllForGuest() {
        String[] row = tblBills.getSelectionModel().getSelectedItem();
        if (row==null) { new Alert(Alert.AlertType.INFORMATION,"Select any item of the guest first.",ButtonType.OK).showAndWait(); return; }
        String guestId=row[0], guestName=row[2];
        List<String> ids = new ArrayList<>(); double total=0;
        for (String[] r2 : tblBills.getItems()) {
            if (guestId.equals(r2[0])&&"No".equals(r2[5])) {
                ids.add(r2[1]);
                try { total+=Double.parseDouble(r2[4].replace("K","")); } catch(Exception ignored){}
            }
        }
        if (ids.isEmpty()) { new Alert(Alert.AlertType.INFORMATION,guestName+" has no unpaid items.",ButtonType.OK).showAndWait(); return; }
        new Alert(Alert.AlertType.CONFIRMATION,
            "Pay ALL for "+guestName+"?\n"+ids.size()+" item(s) · Total: "+String.format("K%.2f",total),
            ButtonType.YES,ButtonType.CANCEL).showAndWait()
            .filter(r->r==ButtonType.YES).ifPresent(r->{ ids.forEach(id->store.markPaid(guestId,id)); refresh(); });
    }

    private void buildWeeklyChart() {
        content.getChildren().addAll(sectionHeader("📊  Weekly Profit — Last 4 Weeks (ZMW)"),chartCanvas);
    }

    public void refresh() {
        lblRevenue.setText(fmt(store.getTotalRevenue()));
        lblProfit .setText(fmt(store.getTotalProfit()));
        lblOpen   .setText(fmt(store.getTotalOpenBills()));
        lblWeek   .setText(fmt(store.getProfitForWeek(LocalDate.now())));

        tblGuests.setItems(FXCollections.observableArrayList(
            store.getGuests().stream().filter(g->g.getTotalCharged()>0)
                .sorted((a,b)->Double.compare(b.getProfit(),a.getProfit())).toList()));

        List<String[]> rows = new ArrayList<>();
        store.getGuests().stream().filter(g->g.getOpenBill()>0)
            .sorted((a,b)->a.getName().compareTo(b.getName()))
            .forEach(g->g.getBillItems().stream().filter(b->!b.isPaid())
                .forEach(b->rows.add(new String[]{
                    g.getId(), b.getId(), g.getName(),
                    b.getDescription(), fmt(b.getAmount()), "No"
                })));
        tblBills.setItems(FXCollections.observableArrayList(rows));
        drawWeeklyChart();
    }

    private void drawWeeklyChart() {
        double w=chartCanvas.getWidth(), h=chartCanvas.getHeight();
        GraphicsContext gc=chartCanvas.getGraphicsContext2D();
        gc.clearRect(0,0,w,h);
        gc.setFill(Color.web("#f8f9fa")); gc.fillRoundRect(0,0,w,h,10,10);
        double[] profits=new double[4]; String[] labels=new String[4];
        DateTimeFormatter fmt=DateTimeFormatter.ofPattern("MMM d");
        LocalDate today=LocalDate.now();
        for (int i=0;i<4;i++) {
            LocalDate d=today.minusWeeks(3-i);
            profits[i]=store.getProfitForWeek(d);
            labels[i]=d.with(java.time.DayOfWeek.MONDAY).format(fmt);
        }
        double max=1; for (double p:profits) if (p>max) max=p;
        double barW=80, gap=(w-4*barW)/5;
        for (int i=0;i<4;i++) {
            double barH=Math.max(4,(profits[i]/max)*(h-40));
            double x=gap+i*(barW+gap), y=h-30-barH;
            gc.setFill(Color.web("#2ecc71",0.85)); gc.fillRoundRect(x,y,barW,barH,6,6);
            gc.setFill(Color.web("#27ae60")); gc.setFont(Font.font(11));
            gc.fillText("K"+(int)profits[i], x+barW/2-20, Math.max(14,y-4));
            gc.setFill(Color.web("#555")); gc.fillText(labels[i], x+barW/2-22, h-8);
        }
    }

    private TableColumn<Guest,String> gcol(String title, java.util.function.Function<Guest,String> fn) {
        TableColumn<Guest,String> c=new TableColumn<>(title);
        c.setCellValueFactory(cd->new SimpleStringProperty(fn.apply(cd.getValue())));
        return c;
    }
    private Label sectionHeader(String t) {
        Label l=new Label(t); l.setFont(Font.font("System",FontWeight.BOLD,14));
        l.setStyle("-fx-text-fill:#333;"); return l;
    }

    public Node getRoot() { return root; }
}
