package com.hotelguest.ui;

import com.hotelguest.model.Guest;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;

import static com.hotelguest.ui.MainWindow.store;

public class AddEditGuestDialog extends Dialog<Guest> {

    private final Guest source;
    private final TextField tfName     = new TextField();
    private final TextField tfAddress  = new TextField();
    private final TextField tfCountry  = new TextField();
    private final TextField tfPassport = new TextField();
    private final Spinner<Integer> spGuests = new Spinner<>(1,50,1);
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

    public AddEditGuestDialog(Guest existing) {
        this.source = existing;
        setTitle(existing == null ? "New Guest" : "Edit Guest — " + existing.getName());
        setResizable(true);

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText(existing == null ? "Add Guest" : "Save Changes");

        cbRoom.getItems().addAll(Guest.RoomType.values());      cbRoom.setValue(Guest.RoomType.DOUBLE);
        cbCar.getItems().addAll(Guest.CarType.values());        cbCar.setValue(Guest.CarType.ECONOMY);
        cbCar.setDisable(true); tfCarRate.setDisable(true); chkInsurance.setDisable(true);
        cbCondition.getItems().addAll(Guest.CarCondition.values()); cbCondition.setValue(Guest.CarCondition.PENDING);
        cbCondition.setDisable(true);
        cbBreakfast.getItems().addAll(Guest.BreakfastType.values()); cbBreakfast.setValue(Guest.BreakfastType.NONE);
        cbFlag.getItems().addAll(Guest.GuestFlag.values());     cbFlag.setValue(Guest.GuestFlag.NONE);
        spGuests.setEditable(true);
        taComments.setWrapText(true); taComments.setPrefRowCount(4);

        chkCar.selectedProperty().addListener((o,ov,nv) -> {
            cbCar.setDisable(!nv); tfCarRate.setDisable(!nv);
            chkInsurance.setDisable(!nv); cbCondition.setDisable(!nv);
            if (nv) fillCarRate();
        });
        cbRoom.valueProperty().addListener((o,ov,nv)      -> fillRoomRate());
        cbCar.valueProperty().addListener((o,ov,nv)       -> { if (chkCar.isSelected()) fillCarRate(); });
        cbBreakfast.valueProperty().addListener((o,ov,nv) -> fillBreakfastPrice());
        dpIn.valueProperty().addListener((o,ov,nv) -> {
            if (dpOut.getValue() != null && !dpOut.getValue().isAfter(nv))
                dpOut.setValue(nv.plusDays(1));
        });

        if (existing != null) populate(existing);
        else {
            dpBirth.setValue(LocalDate.now().minusYears(30));
            dpIn.setValue(LocalDate.now()); dpOut.setValue(LocalDate.now().plusDays(1));
            fillRoomRate();
        }

        ScrollPane sp = buildScrollPane();
        sp.setPrefHeight(550);
        sp.setMaxHeight(Double.MAX_VALUE);
        getDialogPane().setContent(sp);
        getDialogPane().setPrefWidth(580);
        getDialogPane().setPrefHeight(680);
        getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        setResultConverter(btn -> btn != ButtonType.OK ? null : buildGuest());
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String err = validate();
            if (err != null) { new Alert(Alert.AlertType.WARNING, err, ButtonType.OK).showAndWait(); e.consume(); }
        });
    }

    private ScrollPane buildScrollPane() {
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(16));
        ColumnConstraints c1 = new ColumnConstraints(160);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        int r = 0;
        grid.add(sec("Personal Information"),0,r++,2,1);
        grid.addRow(r++, lbl("Full Name *"),        tfName);
        grid.addRow(r++, lbl("Address"),             tfAddress);
        grid.addRow(r++, lbl("Country"),             tfCountry);
        grid.addRow(r++, lbl("Passport No. *"),      tfPassport);
        grid.addRow(r++, lbl("Date of Birth"),       dpBirth);
        grid.addRow(r++, lbl("No. of Guests"),       spGuests);
        grid.add(sec("Stay Details"),0,r++,2,1);
        grid.addRow(r++, lbl("Check-in Date *"),     dpIn);
        grid.addRow(r++, lbl("Check-out Date *"),    dpOut);
        grid.add(sec("Accommodation"),0,r++,2,1);
        grid.addRow(r++, lbl("Room Type"),           cbRoom);
        grid.addRow(r++, lbl(rateLabel("Room Rate/Night")), tfRoomRate);
        grid.add(sec("Breakfast"),0,r++,2,1);
        grid.addRow(r++, lbl("Breakfast Type"),      cbBreakfast);
        grid.addRow(r++, lbl(rateLabel("Price/Person/Night")), tfBreakfast);
        grid.add(sec("Car Hire"),0,r++,2,1);
        HBox carRow = new HBox(10, chkCar, cbCar, chkInsurance);
        grid.add(carRow, 0, r++, 2, 1);
        grid.addRow(r++, lbl(rateLabel("Car Rate/Day")), tfCarRate);
        grid.addRow(r++, lbl("Car Condition"),       cbCondition);
        grid.add(sec("Guest Status"),0,r++,2,1);
        grid.addRow(r++, lbl("Status Flag"),         cbFlag);
        grid.add(sec("Comments & Remarks"),0,r++,2,1);
        grid.add(taComments, 0, r, 2, 1);

        ScrollPane sp = new ScrollPane(grid); sp.setFitToWidth(true); return sp;
    }

    // ── Currency helpers ──────────────────────────────────────────────────────
    private double zmwToDisplay(double zmw) {
        var s = store.getSettings();
        if ("ZMW".equals(s.getDisplayCurrency())) return zmw;
        return zmw * s.getExchangeRates().getOrDefault(s.getDisplayCurrency(), 1.0);
    }
    private double displayToZmw(double d) {
        var s = store.getSettings();
        if ("ZMW".equals(s.getDisplayCurrency())) return d;
        double rate = s.getExchangeRates().getOrDefault(s.getDisplayCurrency(), 1.0);
        return rate > 0 ? d / rate : d;
    }
    private String rateLabel(String base) {
        String cur = store.getSettings().getDisplayCurrency();
        return base + " (" + store.getSettings().currencySymbol() + ")"
            + ("ZMW".equals(cur) ? "" : " ["+cur+"]");
    }
    private void fillRoomRate() {
        Guest.RoomType rt = cbRoom.getValue();
        if (rt!=null) tfRoomRate.setText(String.format("%.2f",
            zmwToDisplay(store.getSettings().getRoomRates().getOrDefault(rt.getLabel(),0.0))));
    }
    private void fillCarRate() {
        Guest.CarType ct = cbCar.getValue();
        if (ct!=null) tfCarRate.setText(String.format("%.2f",
            zmwToDisplay(store.getSettings().getCarRates().getOrDefault(ct.getLabel(),0.0))));
    }
    private void fillBreakfastPrice() {
        Guest.BreakfastType bt = cbBreakfast.getValue();
        if (bt!=null && bt!=Guest.BreakfastType.NONE)
            tfBreakfast.setText(String.format("%.2f",
                zmwToDisplay(store.getSettings().getBreakfastPrices().getOrDefault(bt.getLabel(),0.0))));
        else tfBreakfast.setText("0.00");
    }

    private void populate(Guest g) {
        tfName.setText(g.getName()); tfAddress.setText(g.getAddress());
        tfCountry.setText(g.getCountry()!=null?g.getCountry():"");
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
        if (g.getCarType()!=null) cbCar.setValue(g.getCarType());
        tfCarRate.setText(String.format("%.2f", zmwToDisplay(g.getCarRatePerDay())));
        chkInsurance.setSelected(g.isCarInsurance());
        if (g.getCarCondition()!=null) cbCondition.setValue(g.getCarCondition());
        cbBreakfast.setValue(g.getBreakfastType()!=null?g.getBreakfastType():Guest.BreakfastType.NONE);
        tfBreakfast.setText(String.format("%.2f", zmwToDisplay(g.getBreakfastPricePerPersonPerNight())));
        taComments.setText(g.getComments());
        cbFlag.setValue(g.getFlag()!=null?g.getFlag():Guest.GuestFlag.NONE);
    }

    private Guest buildGuest() {
        Guest g = source!=null?source:new Guest();
        g.setName(tfName.getText().trim()); g.setAddress(tfAddress.getText().trim());
        g.setCountry(tfCountry.getText().trim());
        g.setPassportNumber(tfPassport.getText().trim()); g.setNumberOfGuests(spGuests.getValue());
        g.setBirthdate(dpBirth.getValue()); g.setCheckInDate(dpIn.getValue()); g.setCheckOutDate(dpOut.getValue());
        g.setRoomType(cbRoom.getValue());
        g.setRoomRatePerNight(displayToZmw(parseDouble(tfRoomRate.getText())));
        g.setCarHired(chkCar.isSelected());
        g.setCarType(chkCar.isSelected()?cbCar.getValue():null);
        g.setCarRatePerDay(chkCar.isSelected()?displayToZmw(parseDouble(tfCarRate.getText())):0);
        g.setCarInsurance(chkCar.isSelected()&&chkInsurance.isSelected());
        g.setCarCondition(cbCondition.getValue());
        g.setBreakfastType(cbBreakfast.getValue());
        g.setBreakfastPricePerPersonPerNight(
            cbBreakfast.getValue()==Guest.BreakfastType.NONE?0:displayToZmw(parseDouble(tfBreakfast.getText())));
        g.setComments(taComments.getText()); g.setFlag(cbFlag.getValue());
        if (source == null) {
            var s = store.getSettings();
            String cur = s.getDisplayCurrency();
            g.setBookingCurrency(cur);
            g.setBookingRate("ZMW".equals(cur)?1.0:s.getExchangeRates().getOrDefault(cur,1.0));
        }
        return g;
    }

    private String validate() {
        if (tfName.getText().isBlank()) return "Please enter the guest's full name.";
        if (tfPassport.getText().isBlank()) return "Please enter a passport number.";
        if (dpIn.getValue()==null||dpOut.getValue()==null) return "Please set check-in and check-out dates.";
        if (!dpOut.getValue().isAfter(dpIn.getValue())) return "Check-out date must be after check-in date.";
        return null;
    }
    private double parseDouble(String s) {
        try { return Double.parseDouble(s.replace(",",".")); } catch (NumberFormatException e) { return 0; }
    }
    private Label lbl(String t) { Label l=new Label(t); l.getStyleClass().add("form-label"); return l; }
    private Label sec(String t) { Label l=new Label(t); l.getStyleClass().add("section-label"); return l; }
}
