package com.hotelguest.ui;

import com.hotelguest.model.BillItem;
import com.hotelguest.model.Guest;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class BillItemDialog extends Dialog<BillItem> {

    public BillItemDialog(Guest guest) {
        setTitle("Add Charge — " + guest.getName());
        setHeaderText("Enter bill item details");

        TextField tfDesc = new TextField();
        tfDesc.setPromptText("e.g. Minibar, Spa, Room service");
        TextField tfAmt = new TextField("0.00");
        CheckBox chkPaid = new CheckBox("Already paid");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Description:"), tfDesc);
        grid.addRow(1, new Label("Amount ($):"),  tfAmt);
        grid.addRow(2, new Label(),               chkPaid);
        getDialogPane().setContent(grid);

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button ok = (Button) getDialogPane().lookupButton(ButtonType.OK);
        ok.setText("Add Item");
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (tfDesc.getText().isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Please enter a description.", ButtonType.OK).showAndWait();
                e.consume();
            }
        });
        getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            double amt = 0;
            try { amt = Double.parseDouble(tfAmt.getText().replace(",", ".")); } catch (NumberFormatException ignored) {}
            return new BillItem(tfDesc.getText().trim(), amt, chkPaid.isSelected());
        });
    }
}
