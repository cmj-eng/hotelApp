package com.hotelguest.ui;

import com.hotelguest.model.BillItem;
import com.hotelguest.model.Guest;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class BillItemDialog extends Dialog<BillItem> {

    public BillItemDialog(Guest guest) {
        setTitle("Add Charge — " + guest.getName());

        // Use the guest's booking currency — that's what staff entered rates in
        // and what the guest expects to see on their bill.
        String cur = guest.getBookingCurrency() != null ? guest.getBookingCurrency() : "ZMW";
        double rate = guest.getBookingRate() > 0 ? guest.getBookingRate() : 1.0;
        String sym = switch (cur) {
            case "EUR" -> "€"; case "USD" -> "$"; case "GBP" -> "£";
            case "ZMW" -> "K"; default -> cur + " ";
        };

        // Show hint if not ZMW so staff know the conversion
        String hint = cur.equals("ZMW")
            ? "Amounts stored in ZMW."
            : "Enter amount in " + cur + " — will be stored in ZMW automatically.";
        setHeaderText(hint);

        TextField tfDesc = new TextField();
        tfDesc.setPromptText("e.g. Minibar, Spa, Room service");
        TextField tfAmt = new TextField("0.00");
        CheckBox chkPaid = new CheckBox("Already paid");

        // Live ZMW preview when not booking in ZMW
        Label lblPreview = new Label();
        lblPreview.setStyle("-fx-text-fill:#888;-fx-font-size:11px;");
        if (!cur.equals("ZMW")) {
            tfAmt.textProperty().addListener((obs, ov, nv) -> {
                try {
                    double display = Double.parseDouble(nv.replace(",", "."));
                    double zmw = rate > 0 ? display / rate : display;
                    lblPreview.setText(String.format("= K%.2f ZMW", zmw));
                } catch (NumberFormatException e) {
                    lblPreview.setText("");
                }
            });
        }

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Description:"),          tfDesc);
        grid.addRow(1, new Label("Amount (" + sym + "):"), tfAmt);
        if (!cur.equals("ZMW")) grid.addRow(2, new Label(), lblPreview);
        grid.addRow(cur.equals("ZMW") ? 2 : 3, new Label(), chkPaid);
        getDialogPane().setContent(grid);

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button ok = (Button) getDialogPane().lookupButton(ButtonType.OK);
        ok.setText("Add Charge");
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (tfDesc.getText().isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Please enter a description.", ButtonType.OK).showAndWait();
                e.consume();
            }
        });
        getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            double display = 0;
            try { display = Double.parseDouble(tfAmt.getText().replace(",", ".")); }
            catch (NumberFormatException ignored) {}
            // Convert from booking currency back to ZMW for storage
            double zmw = cur.equals("ZMW") ? display : (rate > 0 ? display / rate : display);
            return new BillItem(tfDesc.getText().trim(), zmw, chkPaid.isSelected());
        });
    }
}
