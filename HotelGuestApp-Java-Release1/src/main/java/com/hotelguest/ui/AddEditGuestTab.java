package com.hotelguest.ui;

import com.hotelguest.model.Guest;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.time.LocalDate;

import static com.hotelguest.ui.MainWindow.store;

public class AddEditGuestTab {

    private final VBox root = new VBox(16);
    private final Guest existing;
    private final Runnable onSaved;

    private final TextField tfName     = new TextField();
    private final TextField tfAddress  = new TextField();
    private final TextField tfPassport = new TextField();
    private final Spinner<Integer> spGuests = new Spinner<>(1, 50, 1);
    private final DatePicker dpBirth   = new DatePicker();
    private final DatePicker dpIn      = new DatePicker();
    private final DatePicker dpOut     = new DatePicker();
    private final ComboBox<Guest.RoomType>  cbRoom  = new ComboBox<>();
    private final TextField tfRoomRate = new TextField("0.00");
    private final CheckBox  chkCar     = new CheckBox("Car hired");
    private final ComboBox<Guest.CarType>  cbCar   = new ComboBox<>();
    private final TextField tfCarRate  = new TextField("0.00");
    private final TextArea  taComments = new TextArea();
    private final ComboBox<Guest.GuestFlag> cbFlag = new ComboBox<>();
    private final Label lblStatus = new Label();

    public AddEditGuestTab(Guest existing, Runnable onSaved) {
        this.existing = existing;
        this.onSaved  = onSaved;
        buildUI();
        if (existing != null) populate(existing);
    }

    private void buildUI() {
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        cbRoom.getItems().addAll(Guest.RoomType.values()); cbRoom.setValue(Guest.RoomType.DOUBLE);
        cbCar.getItems().addAll(Guest.CarType.values());   cbCar.setValue(Guest.CarType.ECONOMY);
        cbCar.setDisable(true);
        cbFlag.getItems().addAll(Guest.GuestFlag.values()); cbFlag.setValue(Guest.GuestFlag.NONE);
        dpBirth.setValue(LocalDate.now().minusYears(30));
        dpIn.setValue(LocalDate.now());
        dpOut.setValue(LocalDate.now().plusDays(1));
        spGuests.setEditable(true);
        taComments.setPrefRowCount(4); taComments.setWrapText(true);
        taComments.setPromptText("Special requests, preferences, notes…");
        chkCar.selectedProperty().addListener((o, ov, nv) -> cbCar.setDisable(!nv));
        dpIn.valueProperty().addListener((o, ov, nv) -> {
            if (dpOut.getValue() != null && !dpOut.getValue().isAfter(nv))
                dpOut.setValue(nv.plusDays(1));
        });

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints(150);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setHgrow(Priority.ALWAYS);
        ColumnConstraints c3 = new ColumnConstraints(150);
        ColumnConstraints c4 = new ColumnConstraints(); c4.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2, c3, c4);

        int r = 0;
        grid.add(sectionLabel("Personal Information"), 0, r++, 4, 1);
        grid.addRow(r++, label("Full Name *"), tfName, label("Passport No. *"), tfPassport);
        grid.addRow(r++, label("Address"), tfAddress, label("Date of Birth"), dpBirth);
        grid.addRow(r++, label("No. of Guests"), spGuests, new Label(), new Label());

        grid.add(sectionLabel("Stay Details"), 0, r++, 4, 1);
        grid.addRow(r++, label("Check-in Date *"), dpIn, label("Check-out Date *"), dpOut);

        grid.add(sectionLabel("Accommodation"), 0, r++, 4, 1);
        grid.addRow(r++, label("Room Type"), cbRoom, label("Room Rate / Night ($)"), tfRoomRate);

        grid.add(sectionLabel("Car Hire"), 0, r++, 4, 1);
        grid.addRow(r++, chkCar, cbCar, label("Car Rate / Day ($)"), tfCarRate);

        grid.add(sectionLabel("Guest Status & Comments"), 0, r++, 4, 1);
        grid.addRow(r++, label("Status Flag"), cbFlag, new Label(), new Label());
        grid.add(label("Comments"), 0, r);
        grid.add(taComments, 1, r, 3, 1);
        r++;

        Button saveBtn = new Button(existing == null ? "➕  Add Guest" : "💾  Save Changes");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            String err = validate();
            if (err != null) { lblStatus.setText("⚠ " + err); lblStatus.setStyle("-fx-text-fill: #c0392b;"); return; }
            Guest g = buildGuest();
            if (existing == null) { store.add(g); reset(); lblStatus.setText("✓ Guest added."); }
            else { store.update(g); lblStatus.setText("✓ Saved."); }
            lblStatus.setStyle("-fx-text-fill: #27ae60;");
            if (onSaved != null) onSaved.run();
        });

        HBox btnRow = new HBox(12, saveBtn, lblStatus);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        ScrollPane sp = new ScrollPane(grid);
        sp.setFitToWidth(true);
        VBox.setVgrow(sp, Priority.ALWAYS);

        root.getChildren().addAll(sp, btnRow);
    }

    private void reset() {
        tfName.clear(); tfAddress.clear(); tfPassport.clear();
        spGuests.getValueFactory().setValue(1);
        dpBirth.setValue(LocalDate.now().minusYears(30));
        dpIn.setValue(LocalDate.now()); dpOut.setValue(LocalDate.now().plusDays(1));
        cbRoom.setValue(Guest.RoomType.DOUBLE); tfRoomRate.setText("0.00");
        chkCar.setSelected(false); cbCar.setValue(Guest.CarType.ECONOMY); tfCarRate.setText("0.00");
        taComments.clear(); cbFlag.setValue(Guest.GuestFlag.NONE);
    }

    private void populate(Guest g) {
        tfName.setText(g.getName()); tfAddress.setText(g.getAddress()); tfPassport.setText(g.getPassportNumber());
        spGuests.getValueFactory().setValue(g.getNumberOfGuests());
        if (g.getBirthdate()    != null) dpBirth.setValue(g.getBirthdate());
        if (g.getCheckInDate()  != null) dpIn.setValue(g.getCheckInDate());
        if (g.getCheckOutDate() != null) dpOut.setValue(g.getCheckOutDate());
        cbRoom.setValue(g.getRoomType()); tfRoomRate.setText(String.format("%.2f", g.getRoomRatePerNight()));
        chkCar.setSelected(g.isCarHired());
        if (g.getCarType() != null) cbCar.setValue(g.getCarType());
        tfCarRate.setText(String.format("%.2f", g.getCarRatePerDay()));
        taComments.setText(g.getComments()); cbFlag.setValue(g.getFlag());
    }

    private Guest buildGuest() {
        Guest g = existing != null ? existing : new Guest();
        g.setName(tfName.getText().trim()); g.setAddress(tfAddress.getText().trim());
        g.setPassportNumber(tfPassport.getText().trim());
        g.setNumberOfGuests(spGuests.getValue());
        g.setBirthdate(dpBirth.getValue()); g.setCheckInDate(dpIn.getValue()); g.setCheckOutDate(dpOut.getValue());
        g.setRoomType(cbRoom.getValue()); g.setRoomRatePerNight(parseDouble(tfRoomRate.getText()));
        g.setCarHired(chkCar.isSelected()); g.setCarType(chkCar.isSelected() ? cbCar.getValue() : null);
        g.setCarRatePerDay(chkCar.isSelected() ? parseDouble(tfCarRate.getText()) : 0);
        g.setComments(taComments.getText()); g.setFlag(cbFlag.getValue());
        return g;
    }

    private String validate() {
        if (tfName.getText().isBlank()) return "Please enter the guest's full name.";
        if (tfPassport.getText().isBlank()) return "Please enter a passport number.";
        if (dpIn.getValue() == null || dpOut.getValue() == null) return "Please set both dates.";
        if (!dpOut.getValue().isAfter(dpIn.getValue())) return "Check-out must be after check-in.";
        return null;
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.replace(",", ".")); } catch (NumberFormatException e) { return 0; }
    }

    private Label label(String t) { Label l = new Label(t); l.getStyleClass().add("form-label"); return l; }
    private Label sectionLabel(String t) { Label l = new Label(t); l.getStyleClass().add("section-label"); return l; }

    public Node getRoot() { return root; }
}
