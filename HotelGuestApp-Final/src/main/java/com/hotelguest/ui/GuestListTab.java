package com.hotelguest.ui;

import com.hotelguest.model.BillItem;
import com.hotelguest.model.Guest;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static com.hotelguest.ui.MainWindow.store;

public class GuestListTab {

    private final VBox root = new VBox(0);
    private final TableView<Guest> table = new TableView<>();
    private final ObservableList<Guest> masterList = FXCollections.observableArrayList();
    private FilteredList<Guest> filteredList;
    private final TextField searchField = new TextField();
    private final ComboBox<String> flagFilter = new ComboBox<>();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public GuestListTab() {
        buildToolbar();
        buildTable();
        refresh();
        root.getChildren().add(table);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private void buildToolbar() {
        searchField.setPromptText("Search name, passport, country…");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((o,ov,nv) -> applyFilter());

        flagFilter.getItems().addAll("All","Very Good","Unreliable","Blacklisted","⚑ Special Request");
        flagFilter.setValue("All");
        flagFilter.setOnAction(e -> applyFilter());

        Button addBtn = new Button("+ New Guest");    addBtn.getStyleClass().add("btn-primary");
        Button editBtn = new Button("✏ Edit");        editBtn.getStyleClass().add("btn-secondary");
        Button deleteBtn = new Button("🗑 Delete");   deleteBtn.getStyleClass().add("btn-danger");
        Button chargeBtn = new Button("$ Add Charge");chargeBtn.getStyleClass().add("btn-secondary");

        addBtn.setOnAction(e -> openAddEdit(null));
        editBtn.setOnAction(e -> { Guest g = table.getSelectionModel().getSelectedItem(); if (g!=null) openAddEdit(g); });
        deleteBtn.setOnAction(e -> {
            Guest g = table.getSelectionModel().getSelectedItem(); if (g==null) return;
            Alert a = new Alert(Alert.AlertType.CONFIRMATION,"Delete \""+g.getName()+"\"?",ButtonType.YES,ButtonType.CANCEL);
            a.showAndWait().filter(r->r==ButtonType.YES).ifPresent(r->{ store.delete(g); refresh(); });
        });
        chargeBtn.setOnAction(e -> {
            Guest g = table.getSelectionModel().getSelectedItem();
            if (g==null) { new Alert(Alert.AlertType.INFORMATION,"Select a guest first.",ButtonType.OK).showAndWait(); return; }
            new BillItemDialog(g).showAndWait().ifPresent(item -> { store.addBillItem(g.getId(),item); refresh(); });
        });

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(10, searchField, flagFilter, spacer, chargeBtn, editBtn, deleteBtn, addBtn);
        toolbar.setPadding(new Insets(12,16,12,16));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");
        root.getChildren().add(toolbar);
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Row colouring
        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Guest g, boolean empty) {
                super.updateItem(g, empty);
                getStyleClass().removeAll("row-special","row-vgood","row-unreliable","row-blacklisted");
                if (g==null||empty) return;
                if (g.hasSpecialRequest())                              getStyleClass().add("row-special");
                if (g.getFlag()==Guest.GuestFlag.VERY_GOOD)            getStyleClass().add("row-vgood");
                if (g.getFlag()==Guest.GuestFlag.UNRELIABLE)           getStyleClass().add("row-unreliable");
                if (g.getFlag()==Guest.GuestFlag.BLACKLISTED)          getStyleClass().add("row-blacklisted");
            }
        });

        // Double-click → edit
        table.setOnMouseClicked(e -> {
            if (e.getClickCount()==2 && table.getSelectionModel().getSelectedItem()!=null)
                openAddEdit(table.getSelectionModel().getSelectedItem());
        });

        TableColumn<Guest,String> cName    = col("Guest Name", 160, g -> g.getName());
        TableColumn<Guest,String> cCountry = col("Country",     90, g -> g.getCountry()!=null?g.getCountry():"");
        TableColumn<Guest,String> cRoom    = col("Room",        85, g -> g.getRoomType()!=null?g.getRoomType().getLabel():"");
        TableColumn<Guest,String> cPax     = col("Pax",         35, g -> String.valueOf(g.getNumberOfGuests()));
        TableColumn<Guest,String> cIn      = col("Check-in",    95, g -> g.getCheckInDate()!=null?g.getCheckInDate().format(FMT):"");
        TableColumn<Guest,String> cOut     = col("Check-out",   95, g -> g.getCheckOutDate()!=null?g.getCheckOutDate().format(FMT):"");
        TableColumn<Guest,String> cNights  = col("Nights",      50, g -> String.valueOf(g.getStayDuration()));
        TableColumn<Guest,String> cBf      = col("Breakfast",   80, g -> g.getBreakfastType()!=null&&g.getBreakfastType()!=Guest.BreakfastType.NONE?g.getBreakfastType().getLabel():"—");
        TableColumn<Guest,String> cCar     = col("Car",         75, g -> g.isCarHired()&&g.getCarType()!=null?g.getCarType().getLabel():"—");
        TableColumn<Guest,String> cCond    = col("Condition",   75, g -> g.isCarHired()&&g.getCarCondition()!=null?g.getCarCondition().getLabel():"—");
        // Charged in booking currency (frozen rate)
        TableColumn<Guest,String> cCharged = col("Charged",     85, g -> g.fmtBooked(g.getTotalCharged()));
        // Open bill ALWAYS in ZMW — that's what guest pays
        TableColumn<Guest,String> cOpen    = col("Open Bill(K)",85, g -> g.getOpenBill()>0?String.format("K%.2f",g.getOpenBill()):"—");
        TableColumn<Guest,String> cFlag    = col("Status",      90, g -> g.getFlag()!=null?g.getFlag().getLabel():"");

        // Red open-bill cells
        cOpen.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s,boolean empty) {
                super.updateItem(s,empty); setText(empty?null:s);
                setStyle(s!=null&&s.startsWith("K")?"-fx-text-fill:#c0392b;-fx-font-weight:bold;":"");
            }
        });
        // Car condition colouring
        cCond.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s,boolean empty) {
                super.updateItem(s,empty); setText(empty?null:s);
                if (!empty&&s!=null) setStyle(
                    s.equals("Damaged")   ? "-fx-text-fill:#c0392b;-fx-font-weight:bold;" :
                    s.equals("Undamaged") ? "-fx-text-fill:#27ae60;-fx-font-weight:bold;" : "");
            }
        });

        table.getColumns().addAll(cName,cCountry,cRoom,cPax,cIn,cOut,cNights,cBf,cCar,cCond,cCharged,cOpen,cFlag);
    }

    private <T> TableColumn<Guest,String> col(String title, double pref,
            java.util.function.Function<Guest,String> fn) {
        TableColumn<Guest,String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleStringProperty(fn.apply(cd.getValue())));
        c.setPrefWidth(pref);
        return c;
    }

    private void applyFilter() {
        String search = searchField.getText().toLowerCase(Locale.ROOT);
        String flag   = flagFilter.getValue();
        filteredList.setPredicate(g -> {
            boolean ms = search.isEmpty()
                || g.getName().toLowerCase().contains(search)
                || g.getPassportNumber().toLowerCase().contains(search)
                || g.getAddress().toLowerCase().contains(search)
                || (g.getCountry()!=null&&g.getCountry().toLowerCase().contains(search));
            boolean mf = "All".equals(flag)
                || ("Very Good".equals(flag)         && g.getFlag()==Guest.GuestFlag.VERY_GOOD)
                || ("Unreliable".equals(flag)        && g.getFlag()==Guest.GuestFlag.UNRELIABLE)
                || ("Blacklisted".equals(flag)       && g.getFlag()==Guest.GuestFlag.BLACKLISTED)
                || ("⚑ Special Request".equals(flag) && g.hasSpecialRequest());
            return ms && mf;
        });
    }

    public void refresh() {
        Guest sel = table.getSelectionModel().getSelectedItem();
        masterList.setAll(store.getGuests());
        filteredList = new FilteredList<>(masterList, p -> true);
        table.setItems(filteredList);
        applyFilter();
        if (sel!=null) store.getGuests().stream().filter(g->g.getId().equals(sel.getId()))
            .findFirst().ifPresent(g->table.getSelectionModel().select(g));
    }

    private void openAddEdit(Guest guest) {
        AddEditGuestDialog dlg = new AddEditGuestDialog(guest);
        dlg.showAndWait().ifPresent(g -> { if (guest==null) store.add(g); else store.update(g); refresh(); });
    }

    public Node getRoot() { return root; }
}
