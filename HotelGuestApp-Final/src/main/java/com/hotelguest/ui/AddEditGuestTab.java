package com.hotelguest.ui;

import com.hotelguest.model.Guest;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;

import static com.hotelguest.ui.MainWindow.store;

public class AddEditGuestTab {

    private final VBox root = new VBox(16);
    private final Guest existing;
    private final Runnable onSaved;

    private final TextField tfName     = new TextField();
    private final TextField tfAddress  = new TextField();
    private final TextField tfCountry  = new TextField();
    private final TextField tfPassport = new TextField();
    private final Spinner<Integer> spGuests = new Spinner<>(1, 50, 1);
    private final DatePicker dpBirth   = new DatePicker();
    private final DatePicker dpIn      = new DatePicker();
    private final DatePicker dpOut     = new DatePicker();
    private final ComboBox<Guest.RoomType>      cbRoom      = new ComboBox<>();
    private final TextField                     tfRoomRate  = new TextField("0.00");
    private final CheckBox                      chkCar      = new CheckBox("Car hired");
    private final ComboBox<Guest.CarType>       cbCar       = new ComboBox<>();
    private final TextField                     tfCarRate   = new TextField("0.00");
    private final CheckBox                      chkInsurance= new CheckBox("Insurance taken");
    private final ComboBox<Guest.CarCondition>  cbCondition = new ComboBox<>();
    private final ComboBox<Guest.BreakfastType> cbBreakfast = new ComboBox<>();
    private final TextField                     tfBreakfast = new TextField("0.00");
    private final TextArea  taComments = new TextArea();
    private final ComboBox<Guest.GuestFlag>     cbFlag      = new ComboBox<>();
    private final Label lblStatus = new Label();
    // Rate labels — stored so refreshCurrency() can update them
    private final Label lblRoomRate  = new Label();
    private final Label lblCarRate   = new Label();
    private final Label lblBfRate    = new Label();

    public AddEditGuestTab(Guest existing, Runnable onSaved) {
        this.existing = existing;
        this.onSaved  = onSaved;
        buildUI();
        if (existing != null) populate(existing);
        else resetDefaults();
    }

    private void buildUI() {
        root.setPadding(new Insets(20)); root.setFillWidth(true);

        cbRoom.getItems().addAll(Guest.RoomType.values());      cbRoom.setValue(Guest.RoomType.DOUBLE);
        cbCar.getItems().addAll(Guest.CarType.values());        cbCar.setValue(Guest.CarType.ECONOMY);
        cbCar.setDisable(true); tfCarRate.setDisable(true); chkInsurance.setDisable(true);
        cbCondition.getItems().addAll(Guest.CarCondition.values()); cbCondition.setValue(Guest.CarCondition.PENDING);
        cbCondition.setDisable(true);
        cbBreakfast.getItems().addAll(Guest.BreakfastType.values()); cbBreakfast.setValue(Guest.BreakfastType.NONE);
        cbFlag.getItems().addAll(Guest.GuestFlag.values());     cbFlag.setValue(Guest.GuestFlag.NONE);
        spGuests.setEditable(true);
        taComments.setPrefRowCount(4); taComments.setWrapText(true);
        taComments.setPromptText("Special requests, preferences, notes…");

        // Car hire toggle
        chkCar.selectedProperty().addListener((o,ov,nv) -> {
            cbCar.setDisable(!nv); tfCarRate.setDisable(!nv);
            chkInsurance.setDisable(!nv); cbCondition.setDisable(!nv);
            if (nv) fillCarRate();
        });
        // Auto-fill rates from settings
        cbRoom.valueProperty().addListener((o,ov,nv) -> fillRoomRate());
        cbCar.valueProperty().addListener((o,ov,nv)  -> { if (chkCar.isSelected()) fillCarRate(); });
        cbBreakfast.valueProperty().addListener((o,ov,nv) -> fillBreakfastPrice());

        dpIn.valueProperty().addListener((o,ov,nv) -> {
            if (dpOut.getValue() != null && !dpOut.getValue().isAfter(nv))
                dpOut.setValue(nv.plusDays(1));
        });

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(10);
        ColumnConstraints c1=new ColumnConstraints(150), c2=new ColumnConstraints(),
                          c3=new ColumnConstraints(150), c4=new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS); c4.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1,c2,c3,c4);

        int r = 0;
        grid.add(sec("Personal Information"), 0, r++, 4, 1);
        grid.addRow(r++, lbl("Full Name *"), tfName, lbl("Passport No. *"), tfPassport);
        grid.addRow(r++, lbl("Address"),    tfAddress, lbl("Country"), tfCountry);
        grid.addRow(r++, lbl("Date of Birth"), dpBirth, lbl("No. of Guests"), spGuests);

        grid.add(sec("Stay Details"), 0, r++, 4, 1);
        grid.addRow(r++, lbl("Check-in Date *"), dpIn, lbl("Check-out Date *"), dpOut);

        grid.add(sec("Accommodation"), 0, r++, 4, 1);
        lblRoomRate.getStyleClass().add("form-label");
        grid.addRow(r++, lbl("Room Type"), cbRoom, lblRoomRate, tfRoomRate);

        grid.add(sec("Breakfast"), 0, r++, 4, 1);
        lblBfRate.getStyleClass().add("form-label");
        grid.addRow(r++, lbl("Breakfast Type"), cbBreakfast, lblBfRate, tfBreakfast);

        grid.add(sec("Car Hire"), 0, r++, 4, 1);
        HBox carChk = new HBox(12, chkCar, cbCar, chkInsurance);
        carChk.setAlignment(Pos.CENTER_LEFT);
        grid.add(carChk, 0, r, 2, 1);
        lblCarRate.getStyleClass().add("form-label");
        grid.addRow(r, new Label(), new Label(), lblCarRate, tfCarRate);
        r++;
        grid.addRow(r++, lbl("Car Condition"), cbCondition, new Label(), new Label());

        grid.add(sec("Guest Status & Comments"), 0, r++, 4, 1);
        grid.addRow(r++, lbl("Status Flag"), cbFlag, new Label(), new Label());
        grid.add(lbl("Comments"), 0, r);
        grid.add(taComments, 1, r, 3, 1);
        r++;

        Button saveBtn = new Button(existing == null ? "➕  Add Guest" : "💾  Save Changes");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> save());

        HBox btnRow = new HBox(12, saveBtn, lblStatus);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        ScrollPane sp = new ScrollPane(grid); sp.setFitToWidth(true);
        VBox.setVgrow(sp, Priority.ALWAYS);
        root.getChildren().addAll(sp, btnRow);
    }

    // ── Rate helpers ──────────────────────────────────────────────────────────

    private double zmwToDisplay(double zmw) {
        var s = store.getSettings();
        if ("ZMW".equals(s.getDisplayCurrency())) return zmw;
        return zmw * s.getExchangeRates().getOrDefault(s.getDisplayCurrency(), 1.0);
    }
    private double displayToZmw(double display) {
        var s = store.getSettings();
        if ("ZMW".equals(s.getDisplayCurrency())) return display;
        double rate = s.getExchangeRates().getOrDefault(s.getDisplayCurrency(), 1.0);
        return rate > 0 ? display / rate : display;
    }
    private String rateLabel(String base) {
        String cur = store.getSettings().getDisplayCurrency();
        return base + " (" + store.getSettings().currencySymbol() + ")"
            + ("ZMW".equals(cur) ? "" : " [enter in "+cur+"]");
    }
    private void fillRoomRate() {
        Guest.RoomType rt = cbRoom.getValue();
        if (rt != null) tfRoomRate.setText(String.format("%.2f",
            zmwToDisplay(store.getSettings().getRoomRates().getOrDefault(rt.getLabel(), 0.0))));
    }
    private void fillCarRate() {
        Guest.CarType ct = cbCar.getValue();
        if (ct != null) tfCarRate.setText(String.format("%.2f",
            zmwToDisplay(store.getSettings().getCarRates().getOrDefault(ct.getLabel(), 0.0))));
    }
    private void fillBreakfastPrice() {
        Guest.BreakfastType bt = cbBreakfast.getValue();
        if (bt != null && bt != Guest.BreakfastType.NONE)
            tfBreakfast.setText(String.format("%.2f",
                zmwToDisplay(store.getSettings().getBreakfastPrices().getOrDefault(bt.getLabel(), 0.0))));
        else tfBreakfast.setText("0.00");
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private void save() {
        String err = validate();
        if (err != null) { lblStatus.setText("⚠ "+err); lblStatus.setStyle("-fx-text-fill:#c0392b;"); return; }
        Guest g = buildGuest();
        if (existing == null) { store.add(g); resetDefaults(); lblStatus.setText("✓ Guest added."); }
        else { store.update(g); lblStatus.setText("✓ Saved."); }
        lblStatus.setStyle("-fx-text-fill:#27ae60;");
        if (onSaved != null) onSaved.run();
    }

    private void resetDefaults() {
        updateRateLabels();
        tfName.clear(); tfAddress.clear(); tfCountry.clear(); tfPassport.clear();
        spGuests.getValueFactory().setValue(1);
        dpBirth.setValue(LocalDate.now().minusYears(30));
        dpIn.setValue(LocalDate.now()); dpOut.setValue(LocalDate.now().plusDays(1));
        cbRoom.setValue(Guest.RoomType.DOUBLE); fillRoomRate();
        chkCar.setSelected(false); cbCar.setValue(Guest.CarType.ECONOMY);
        tfCarRate.setText("0.00"); chkInsurance.setSelected(false);
        cbCondition.setValue(Guest.CarCondition.PENDING);
        cbBreakfast.setValue(Guest.BreakfastType.NONE); tfBreakfast.setText("0.00");
        taComments.clear(); cbFlag.setValue(Guest.GuestFlag.NONE);
    }

    private void populate(Guest g) {
        tfName.setText(g.getName()); tfAddress.setText(g.getAddress());
        tfCountry.setText(g.getCountry() != null ? g.getCountry() : "");
        tfPassport.setText(g.getPassportNumber());
        spGuests.getValueFactory().setValue(g.getNumberOfGuests());
        if (g.getBirthdate()    != null) dpBirth.setValue(g.getBirthdate());
        if (g.getCheckInDate()  != null) dpIn.setValue(g.getCheckInDate());
        if (g.getCheckOutDate() != null) dpOut.setValue(g.getCheckOutDate());
        cbRoom.setValue(g.getRoomType());
        tfRoomRate.setText(String.format("%.2f", zmwToDisplay(g.getRoomRatePerNight())));
        chkCar.setSelected(g.isCarHired());
        cbCar.setDisable(!g.isCarHired()); tfCarRate.setDisable(!g.isCarHired());
        chkInsurance.setDisable(!g.isCarHired()); cbCondition.setDisable(!g.isCarHired());
        if (g.getCarType() != null) cbCar.setValue(g.getCarType());
        tfCarRate.setText(String.format("%.2f", zmwToDisplay(g.getCarRatePerDay())));
        chkInsurance.setSelected(g.isCarInsurance());
        if (g.getCarCondition() != null) cbCondition.setValue(g.getCarCondition());
        cbBreakfast.setValue(g.getBreakfastType() != null ? g.getBreakfastType() : Guest.BreakfastType.NONE);
        tfBreakfast.setText(String.format("%.2f", zmwToDisplay(g.getBreakfastPricePerPersonPerNight())));
        taComments.setText(g.getComments());
        cbFlag.setValue(g.getFlag() != null ? g.getFlag() : Guest.GuestFlag.NONE);
    }

    private Guest buildGuest() {
        Guest g = existing != null ? existing : new Guest();
        g.setName(tfName.getText().trim()); g.setAddress(tfAddress.getText().trim());
        g.setCountry(tfCountry.getText().trim());
        g.setPassportNumber(tfPassport.getText().trim()); g.setNumberOfGuests(spGuests.getValue());
        g.setBirthdate(dpBirth.getValue()); g.setCheckInDate(dpIn.getValue()); g.setCheckOutDate(dpOut.getValue());
        g.setRoomType(cbRoom.getValue());
        g.setRoomRatePerNight(displayToZmw(parseDouble(tfRoomRate.getText())));
        g.setCarHired(chkCar.isSelected());
        g.setCarType(chkCar.isSelected() ? cbCar.getValue() : null);
        g.setCarRatePerDay(chkCar.isSelected() ? displayToZmw(parseDouble(tfCarRate.getText())) : 0);
        g.setCarInsurance(chkCar.isSelected() && chkInsurance.isSelected());
        g.setCarCondition(cbCondition.getValue());
        g.setBreakfastType(cbBreakfast.getValue());
        g.setBreakfastPricePerPersonPerNight(
            cbBreakfast.getValue() == Guest.BreakfastType.NONE ? 0 : displayToZmw(parseDouble(tfBreakfast.getText())));
        g.setComments(taComments.getText()); g.setFlag(cbFlag.getValue());
        // Store booking currency + frozen rate on new records only
        if (existing == null) {
            var s = store.getSettings();
            String cur = s.getDisplayCurrency();
            g.setBookingCurrency(cur);
            g.setBookingRate("ZMW".equals(cur) ? 1.0 : s.getExchangeRates().getOrDefault(cur, 1.0));
        }
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
        try { return Double.parseDouble(s.replace(",",".")); } catch (NumberFormatException e) { return 0; }
    }
    private Label lbl(String t)  { Label l = new Label(t); l.getStyleClass().add("form-label"); return l; }
    private Label sec(String t)  { Label l = new Label(t); l.getStyleClass().add("section-label"); return l; }


    private void updateRateLabels() {
        lblRoomRate.setText(rateLabel("Room Rate/Night"));
        lblCarRate.setText(rateLabel("Car Rate/Day"));
        lblBfRate.setText(rateLabel("Price/Person/Night"));
    }

    /** Call this when the display currency changes so rates re-convert. */
    public void refreshCurrency() {
        if (existing != null) return; // editing keeps original booking currency
        updateRateLabels();
        fillRoomRate();
        if (chkCar.isSelected()) fillCarRate();
        fillBreakfastPrice();
    }

    public Node getRoot() { return root; }
}
