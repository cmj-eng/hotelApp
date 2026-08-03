package com.hotelguest.ui;

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
        searchField.setPromptText("Search name, passport, address…");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((o, ov, nv) -> applyFilter());

        flagFilter.getItems().addAll("All", "Very Good", "Unreliable", "Blacklisted", "⚑ Special Request");
        flagFilter.setValue("All");
        flagFilter.setOnAction(e -> applyFilter());

        Button addBtn = new Button("+ New Guest");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> openAddEditDialog(null));

        Button editBtn = new Button("✏ Edit");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> {
            Guest sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) openAddEditDialog(sel);
        });

        Button deleteBtn = new Button("🗑 Delete");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> {
            Guest sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + sel.getName() + "\"? This cannot be undone.", ButtonType.YES, ButtonType.CANCEL);
            conf.setTitle("Delete Guest");
            conf.showAndWait().filter(r -> r == ButtonType.YES).ifPresent(r -> {
                store.delete(sel); refresh();
            });
        });

        Button billBtn = new Button("$ Add Charge");
        billBtn.getStyleClass().add("btn-secondary");
        billBtn.setOnAction(e -> {
            Guest sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) new BillItemDialog(sel).showAndWait().ifPresent(item -> {
                store.addBillItem(sel.getId(), item); refresh();
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(10, searchField, flagFilter, spacer, billBtn, editBtn, deleteBtn, addBtn);
        toolbar.setPadding(new Insets(12, 16, 12, 16));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");

        root.getChildren().add(toolbar);
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Guest g, boolean empty) {
                super.updateItem(g, empty);
                getStyleClass().removeAll("row-special", "row-vgood", "row-unreliable", "row-blacklisted");
                if (g == null || empty) return;
                if (g.hasSpecialRequest())                          getStyleClass().add("row-special");
                if (g.getFlag() == Guest.GuestFlag.VERY_GOOD)      getStyleClass().add("row-vgood");
                if (g.getFlag() == Guest.GuestFlag.UNRELIABLE)     getStyleClass().add("row-unreliable");
                if (g.getFlag() == Guest.GuestFlag.BLACKLISTED)    getStyleClass().add("row-blacklisted");
            }
        });

        // Double-click to edit
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null)
                openAddEditDialog(table.getSelectionModel().getSelectedItem());
        });

        TableColumn<Guest, String> colName    = col("Guest Name", 170, g -> g.getName());
        TableColumn<Guest, String> colRoom    = col("Room",        90, g -> g.getRoomType().getLabel());
        TableColumn<Guest, String> colGuests  = col("Pax",         40, g -> String.valueOf(g.getNumberOfGuests()));
        TableColumn<Guest, String> colIn      = col("Check-in",   100, g -> g.getCheckInDate() != null ? g.getCheckInDate().format(FMT) : "");
        TableColumn<Guest, String> colOut     = col("Check-out",  100, g -> g.getCheckOutDate() != null ? g.getCheckOutDate().format(FMT) : "");
        TableColumn<Guest, String> colNights  = col("Nights",      55, g -> String.valueOf(g.getStayDuration()));
        TableColumn<Guest, String> colCar     = col("Car",         80, g -> g.isCarHired() && g.getCarType() != null ? g.getCarType().getLabel() : "—");
        TableColumn<Guest, String> colCharged = col("Charged",     85, g -> String.format("$%.2f", g.getTotalCharged()));
        TableColumn<Guest, String> colOpen    = col("Open Bill",   85, g -> g.getOpenBill() > 0 ? String.format("$%.2f", g.getOpenBill()) : "—");
        TableColumn<Guest, String> colFlag    = col("Status",      95, g -> g.getFlag().getLabel());
        TableColumn<Guest, String> colReq     = col("Special Req", 85, g -> g.hasSpecialRequest() ? "⚑ Yes" : "");
        TableColumn<Guest, String> colPassport= col("Passport",   110, g -> g.getPassportNumber());

        table.getColumns().addAll(colName, colRoom, colGuests, colIn, colOut, colNights, colCar, colCharged, colOpen, colFlag, colReq, colPassport);

        // Color open-bill cells red
        colOpen.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty ? null : s);
                setStyle((!empty && s != null && s.startsWith("$")) ? "-fx-text-fill: #c0392b; -fx-font-weight: bold;" : "");
            }
        });
    }

    private <T> TableColumn<Guest, String> col(String title, double pref, java.util.function.Function<Guest, String> fn) {
        TableColumn<Guest, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleStringProperty(fn.apply(cd.getValue())));
        c.setPrefWidth(pref);
        return c;
    }

    private void applyFilter() {
        String search = searchField.getText().toLowerCase(Locale.ROOT);
        String flag   = flagFilter.getValue();
        filteredList.setPredicate(g -> {
            boolean matchSearch = search.isEmpty()
                || g.getName().toLowerCase().contains(search)
                || g.getPassportNumber().toLowerCase().contains(search)
                || g.getAddress().toLowerCase().contains(search);
            boolean matchFlag = "All".equals(flag)
                || ("Very Good".equals(flag)     && g.getFlag() == Guest.GuestFlag.VERY_GOOD)
                || ("Unreliable".equals(flag)    && g.getFlag() == Guest.GuestFlag.UNRELIABLE)
                || ("Blacklisted".equals(flag)   && g.getFlag() == Guest.GuestFlag.BLACKLISTED)
                || ("⚑ Special Request".equals(flag) && g.hasSpecialRequest());
            return matchSearch && matchFlag;
        });
    }

    public void refresh() {
        Guest sel = table.getSelectionModel().getSelectedItem();
        masterList.setAll(store.getGuests());
        filteredList = new FilteredList<>(masterList, p -> true);
        table.setItems(filteredList);
        applyFilter();
        if (sel != null) {
            store.getGuests().stream().filter(g -> g.getId().equals(sel.getId()))
                .findFirst().ifPresent(g -> table.getSelectionModel().select(g));
        }
    }

    private void openAddEditDialog(Guest guest) {
        AddEditGuestDialog dlg = new AddEditGuestDialog(guest);
        dlg.showAndWait().ifPresent(g -> {
            if (guest == null) store.add(g); else store.update(g);
            refresh();
        });
    }

    public Node getRoot() { return root; }
}
