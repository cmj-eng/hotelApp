package com.hotelguest.ui;

import com.hotelguest.model.Guest;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AddEditGuestDialog extends Dialog<Guest> {

    private final Guest source;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Fields
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

    public AddEditGuestDialog(Guest existing) {
        this.source = existing;
        setTitle(existing == null ? "New Guest" : "Edit Guest — " + existing.getName());
        setResizable(true);

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText(existing == null ? "Add Guest" : "Save Changes");

        buildForm();
        if (existing != null) populate(existing);

        getDialogPane().setContent(buildScrollPane());
        getDialogPane().setPrefWidth(560);
        getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            return buildGuest();
        });

        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String err = validate();
            if (err != null) {
                new Alert(Alert.AlertType.WARNING, err, ButtonType.OK).showAndWait();
                e.consume();
            }
        });
    }

    private void buildForm() {
        // Combos
        cbRoom.getItems().addAll(Guest.RoomType.values());
        cbRoom.setValue(Guest.RoomType.DOUBLE);
        cbCar.getItems().addAll(Guest.CarType.values());
        cbCar.setDisable(true);
        cbFlag.getItems().addAll(Guest.GuestFlag.values());
        cbFlag.setValue(Guest.GuestFlag.NONE);

        // Dates
        dpBirth.setValue(LocalDate.now().minusYears(30));
        dpIn.setValue(LocalDate.now());
        dpOut.setValue(LocalDate.now().plusDays(1));

        dpIn.valueProperty().addListener((o, ov, nv) -> {
            if (dpOut.getValue() != null && !dpOut.getValue().isAfter(nv))
                dpOut.setValue(nv.plusDays(1));
        });

        // Car toggle
        chkCar.selectedProperty().addListener((o, ov, nv) -> cbCar.setDisable(!nv));
        cbCar.setValue(Guest.CarType.ECONOMY);

        // Spinner
        spGuests.setEditable(true);

        // Comments
        taComments.setWrapText(true);
        taComments.setPrefRowCount(4);
        taComments.setPromptText("Special requests, preferences, notes…");
    }

    private ScrollPane buildScrollPane() {
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(16));

        int r = 0;
        grid.add(sectionLabel("Personal Information"), 0, r++, 2, 1);
        grid.addRow(r++, label("Full Name *"),    tfName);
        grid.addRow(r++, label("Address"),        tfAddress);
        grid.addRow(r++, label("Passport No. *"), tfPassport);
        grid.addRow(r++, label("Date of Birth"),  dpBirth);
        grid.addRow(r++, label("No. of Guests"),  spGuests);

        grid.add(sectionLabel("Stay Details"), 0, r++, 2, 1);
        grid.addRow(r++, label("Check-in Date *"),  dpIn);
        grid.addRow(r++, label("Check-out Date *"), dpOut);

        grid.add(sectionLabel("Accommodation"), 0, r++, 2, 1);
        grid.addRow(r++, label("Room Type"),       cbRoom);
        grid.addRow(r++, label("Room Rate/Night"), tfRoomRate);

        grid.add(sectionLabel("Car Hire"), 0, r++, 2, 1);
        grid.addRow(r++, chkCar, cbCar);
        grid.addRow(r++, label("Car Rate/Day"), tfCarRate);

        grid.add(sectionLabel("Guest Status"), 0, r++, 2, 1);
        grid.addRow(r++, label("Flag"), cbFlag);

        grid.add(sectionLabel("Comments & Remarks"), 0, r++, 2, 1);
        grid.add(taComments, 0, r, 2, 1);

        ColumnConstraints c1 = new ColumnConstraints(130);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        ScrollPane sp = new ScrollPane(grid);
        sp.setFitToWidth(true);
        return sp;
    }

    private void populate(Guest g) {
        tfName.setText(g.getName());
        tfAddress.setText(g.getAddress());
        tfPassport.setText(g.getPassportNumber());
        spGuests.getValueFactory().setValue(g.getNumberOfGuests());
        if (g.getBirthdate() != null) dpBirth.setValue(g.getBirthdate());
        if (g.getCheckInDate() != null) dpIn.setValue(g.getCheckInDate());
        if (g.getCheckOutDate() != null) dpOut.setValue(g.getCheckOutDate());
        cbRoom.setValue(g.getRoomType());
        tfRoomRate.setText(String.format("%.2f", g.getRoomRatePerNight()));
        chkCar.setSelected(g.isCarHired());
        if (g.getCarType() != null) cbCar.setValue(g.getCarType());
        tfCarRate.setText(String.format("%.2f", g.getCarRatePerDay()));
        taComments.setText(g.getComments());
        cbFlag.setValue(g.getFlag());
    }

    private Guest buildGuest() {
        Guest g = source != null ? source : new Guest();
        g.setName(tfName.getText().trim());
        g.setAddress(tfAddress.getText().trim());
        g.setPassportNumber(tfPassport.getText().trim());
        g.setNumberOfGuests(spGuests.getValue());
        g.setBirthdate(dpBirth.getValue());
        g.setCheckInDate(dpIn.getValue());
        g.setCheckOutDate(dpOut.getValue());
        g.setRoomType(cbRoom.getValue());
        g.setRoomRatePerNight(parseDouble(tfRoomRate.getText()));
        g.setCarHired(chkCar.isSelected());
        g.setCarType(chkCar.isSelected() ? cbCar.getValue() : null);
        g.setCarRatePerDay(chkCar.isSelected() ? parseDouble(tfCarRate.getText()) : 0);
        g.setComments(taComments.getText());
        g.setFlag(cbFlag.getValue());
        return g;
    }

    private String validate() {
        if (tfName.getText().isBlank()) return "Please enter the guest's full name.";
        if (tfPassport.getText().isBlank()) return "Please enter a passport number.";
        if (dpIn.getValue() == null || dpOut.getValue() == null) return "Please set check-in and check-out dates.";
        if (!dpOut.getValue().isAfter(dpIn.getValue())) return "Check-out date must be after check-in date.";
        return null;
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.replace(",", ".")); } catch (NumberFormatException e) { return 0; }
    }

    private Label label(String text)  { Label l = new Label(text); l.getStyleClass().add("form-label"); return l; }
    private Label sectionLabel(String text) {
        Label l = new Label(text); l.getStyleClass().add("section-label"); return l;
    }
}
