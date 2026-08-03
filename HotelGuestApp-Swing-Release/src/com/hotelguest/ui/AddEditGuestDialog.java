package com.hotelguest.ui;

import com.hotelguest.model.Guest;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AddEditGuestDialog extends JDialog {

    private Guest result = null;
    private final Guest source;

    // Fields
    private final JTextField tfName     = UI.textField();
    private final JTextField tfAddress  = UI.textField();
    private final JTextField tfPassport = UI.textField();
    private final JSpinner   spGuests   = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
    private final JTextField tfBirth    = UI.textField();
    private final JTextField tfIn       = UI.textField();
    private final JTextField tfOut      = UI.textField();
    private final JComboBox<Guest.RoomType>  cbRoom  = new JComboBox<>(Guest.RoomType.values());
    private final JTextField tfRoomRate = UI.textField();
    private final JCheckBox  chkCar     = new JCheckBox("Car hired");
    private final JComboBox<Guest.CarType>  cbCar   = new JComboBox<>(Guest.CarType.values());
    private final JTextField tfCarRate  = UI.textField();
    private final JTextArea  taComments = UI.textArea(4);
    private final JComboBox<Guest.GuestFlag> cbFlag = new JComboBox<>(Guest.GuestFlag.values());

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String DATE_HINT = "dd/mm/yyyy";

    public AddEditGuestDialog(Frame owner, Guest existing) {
        super(owner, existing == null ? "New Guest" : "Edit Guest — " + existing.getName(), true);
        this.source = existing;

        setSize(620, 680);
        setResizable(true);
        setLocationRelativeTo(owner);

        cbCar.setEnabled(false);
        tfCarRate.setEnabled(false);
        chkCar.addActionListener(e -> {
            cbCar.setEnabled(chkCar.isSelected());
            tfCarRate.setEnabled(chkCar.isSelected());
        });

        if (existing != null) populate(existing);
        else {
            tfBirth.setText(LocalDate.now().minusYears(30).format(FMT));
            tfIn.setText(LocalDate.now().format(FMT));
            tfOut.setText(LocalDate.now().plusDays(1).format(FMT));
            tfRoomRate.setText("0.00");
            tfCarRate.setText("0.00");
        }

        JScrollPane scroll = new JScrollPane(buildForm());
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JButton saveBtn = UI.primaryButton(existing == null ? "Add Guest" : "Save Changes");
        JButton cancelBtn = UI.secondaryButton("Cancel");
        saveBtn.addActionListener(e -> {
            String err = doValidate();
            if (err != null) { JOptionPane.showMessageDialog(this, err, "Missing info", JOptionPane.WARNING_MESSAGE); return; }
            result = buildGuest();
            dispose();
        });
        cancelBtn.addActionListener(e -> dispose());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        btnRow.setBackground(UI.WHITE);
        btnRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UI.BORDER));
        btnRow.add(cancelBtn); btnRow.add(saveBtn);

        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        add(btnRow, BorderLayout.SOUTH);
    }

    private JPanel buildForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(UI.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        form.add(UI.sectionLabel("Personal Information"));
        form.add(row("Full Name *", tfName));
        form.add(row("Address", tfAddress));
        form.add(row("Passport No. *", tfPassport));
        form.add(row("Date of Birth (dd/mm/yyyy)", tfBirth));
        form.add(row("Number of Guests", spGuests));

        form.add(UI.sectionLabel("Stay Details"));
        form.add(row("Check-in Date (dd/mm/yyyy) *", tfIn));
        form.add(row("Check-out Date (dd/mm/yyyy) *", tfOut));

        form.add(UI.sectionLabel("Accommodation"));
        form.add(row("Room Type", cbRoom));
        form.add(row("Room Rate per Night ($)", tfRoomRate));

        form.add(UI.sectionLabel("Car Hire"));
        JPanel carRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        carRow.setBackground(UI.WHITE);
        carRow.add(chkCar); carRow.add(cbCar);
        form.add(carRow);
        form.add(row("Car Rate per Day ($)", tfCarRate));

        form.add(UI.sectionLabel("Guest Status"));
        form.add(row("Status Flag", cbFlag));

        form.add(UI.sectionLabel("Comments & Remarks"));
        JScrollPane taScroll = new JScrollPane(taComments);
        taScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        taScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        taComments.setFont(UI.FONT);
        taComments.setLineWrap(true);
        taComments.setWrapStyleWord(true);
        form.add(taScroll);

        return form;
    }

    private JPanel row(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(UI.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = UI.formLabel(label);
        l.setPreferredSize(new Dimension(210, 28));
        if (field instanceof JTextField tf) tf.setFont(UI.FONT);
        p.add(l, BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private void populate(Guest g) {
        tfName.setText(g.getName());
        tfAddress.setText(g.getAddress());
        tfPassport.setText(g.getPassportNumber());
        spGuests.setValue(g.getNumberOfGuests());
        if (g.getBirthdate()    != null) tfBirth.setText(g.getBirthdate().format(FMT));
        if (g.getCheckInDate()  != null) tfIn.setText(g.getCheckInDate().format(FMT));
        if (g.getCheckOutDate() != null) tfOut.setText(g.getCheckOutDate().format(FMT));
        if (g.getRoomType()     != null) cbRoom.setSelectedItem(g.getRoomType());
        tfRoomRate.setText(String.format("%.2f", g.getRoomRatePerNight()));
        chkCar.setSelected(g.isCarHired());
        cbCar.setEnabled(g.isCarHired());
        tfCarRate.setEnabled(g.isCarHired());
        if (g.getCarType()  != null) cbCar.setSelectedItem(g.getCarType());
        tfCarRate.setText(String.format("%.2f", g.getCarRatePerDay()));
        taComments.setText(g.getComments());
        if (g.getFlag()     != null) cbFlag.setSelectedItem(g.getFlag());
    }

    private Guest buildGuest() {
        Guest g = source != null ? source : new Guest();
        g.setName(tfName.getText().trim());
        g.setAddress(tfAddress.getText().trim());
        g.setPassportNumber(tfPassport.getText().trim());
        g.setNumberOfGuests((Integer) spGuests.getValue());
        g.setBirthdate(parseDate(tfBirth.getText()));
        g.setCheckInDate(parseDate(tfIn.getText()));
        g.setCheckOutDate(parseDate(tfOut.getText()));
        g.setRoomType((Guest.RoomType) cbRoom.getSelectedItem());
        g.setRoomRatePerNight(parseDouble(tfRoomRate.getText()));
        g.setCarHired(chkCar.isSelected());
        g.setCarType(chkCar.isSelected() ? (Guest.CarType) cbCar.getSelectedItem() : null);
        g.setCarRatePerDay(chkCar.isSelected() ? parseDouble(tfCarRate.getText()) : 0);
        g.setComments(taComments.getText());
        g.setFlag((Guest.GuestFlag) cbFlag.getSelectedItem());
        return g;
    }

    private String doValidate() {
        if (tfName.getText().isBlank()) return "Please enter the guest's full name.";
        if (tfPassport.getText().isBlank()) return "Please enter a passport number.";
        LocalDate in = parseDate(tfIn.getText());
        LocalDate out = parseDate(tfOut.getText());
        if (in == null || out == null) return "Please enter dates in dd/mm/yyyy format.";
        if (!out.isAfter(in)) return "Check-out date must be after check-in date.";
        return null;
    }

    private LocalDate parseDate(String s) {
        try { return LocalDate.parse(s.trim(), FMT); } catch (DateTimeParseException e) { return null; }
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.replace(",",".")); } catch (NumberFormatException e) { return 0; }
    }

    public Guest getResult() { return result; }
}
