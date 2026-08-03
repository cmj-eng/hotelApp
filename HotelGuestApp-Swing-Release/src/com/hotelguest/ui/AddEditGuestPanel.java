package com.hotelguest.ui;

import com.hotelguest.model.Guest;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static com.hotelguest.Main.store;

public class AddEditGuestPanel extends JPanel {

    private final Guest existing;
    private final Runnable onSaved;

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
    private final JLabel lblStatus = new JLabel(" ");

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public AddEditGuestPanel(Guest existing, Runnable onSaved) {
        this.existing = existing;
        this.onSaved  = onSaved;
        setLayout(new BorderLayout());
        setBackground(UI.BG);

        cbCar.setEnabled(false); tfCarRate.setEnabled(false);
        chkCar.addActionListener(e -> { cbCar.setEnabled(chkCar.isSelected()); tfCarRate.setEnabled(chkCar.isSelected()); });

        if (existing != null) populate(existing);
        else resetDefaults();

        JScrollPane scroll = new JScrollPane(buildForm());
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        JButton saveBtn = UI.primaryButton(existing == null ? "➕  Add Guest" : "💾  Save Changes");
        saveBtn.addActionListener(e -> save());
        lblStatus.setFont(UI.FONT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        btnRow.setBackground(UI.WHITE);
        btnRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UI.BORDER));
        btnRow.add(saveBtn); btnRow.add(lblStatus);
        add(btnRow, BorderLayout.SOUTH);
    }

    private JPanel buildForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(UI.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        // Two-column layout using GridBagLayout
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(UI.WHITE);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        addSection(form, "Personal Information");
        addGridRow(grid, 0, "Full Name *", tfName, "Passport No. *", tfPassport);
        addGridRow(grid, 1, "Address", tfAddress, "Date of Birth (dd/mm/yyyy)", tfBirth);
        addGridRow(grid, 2, "Number of Guests", spGuests, "", new JLabel());
        form.add(grid);

        JPanel grid2 = new JPanel(new GridBagLayout()); grid2.setBackground(UI.WHITE); grid2.setAlignmentX(Component.LEFT_ALIGNMENT);
        addSection(form, "Stay & Accommodation");
        addGridRow(grid2, 0, "Check-in (dd/mm/yyyy) *", tfIn, "Check-out (dd/mm/yyyy) *", tfOut);
        addGridRow(grid2, 1, "Room Type", cbRoom, "Room Rate / Night ($)", tfRoomRate);
        form.add(grid2);

        JPanel carPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        carPanel.setBackground(UI.WHITE); carPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        addSection(form, "Car Hire");
        carPanel.add(chkCar); carPanel.add(cbCar);
        carPanel.add(new JLabel("  Rate / Day ($):")); carPanel.add(tfCarRate);
        tfCarRate.setPreferredSize(new Dimension(80, 28));
        form.add(carPanel);

        JPanel grid3 = new JPanel(new GridBagLayout()); grid3.setBackground(UI.WHITE); grid3.setAlignmentX(Component.LEFT_ALIGNMENT);
        addSection(form, "Guest Status & Comments");
        addGridRow(grid3, 0, "Status Flag", cbFlag, "", new JLabel());
        form.add(grid3);

        form.add(UI.formLabel("Comments & Remarks:"));
        taComments.setAlignmentX(Component.LEFT_ALIGNMENT);
        JScrollPane taScroll = new JScrollPane(taComments);
        taScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        taScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        form.add(Box.createVerticalStrut(4));
        form.add(taScroll);

        return form;
    }

    private void addSection(JPanel form, String title) {
        form.add(Box.createVerticalStrut(6));
        form.add(UI.sectionLabel(title));
    }

    private void addGridRow(JPanel grid, int row, String l1, JComponent f1, String l2, JComponent f2) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridy = row;
        gbc.gridx = 0; gbc.weightx = 0; grid.add(UI.formLabel(l1), gbc);
        gbc.gridx = 1; gbc.weightx = 1; grid.add(f1, gbc);
        gbc.gridx = 2; gbc.weightx = 0; grid.add(UI.formLabel(l2), gbc);
        gbc.gridx = 3; gbc.weightx = 1; grid.add(f2, gbc);
    }

    private void save() {
        String err = doValidate();
        if (err != null) { lblStatus.setForeground(UI.RED); lblStatus.setText("⚠ " + err); return; }
        Guest g = buildGuest();
        if (existing == null) { store.add(g); resetDefaults(); lblStatus.setForeground(UI.GREEN); lblStatus.setText("✓ Guest added successfully."); }
        else { store.update(g); lblStatus.setForeground(UI.GREEN); lblStatus.setText("✓ Saved."); }
        if (onSaved != null) onSaved.run();
    }

    private void resetDefaults() {
        tfName.setText(""); tfAddress.setText(""); tfPassport.setText("");
        spGuests.setValue(1);
        tfBirth.setText(LocalDate.now().minusYears(30).format(FMT));
        tfIn.setText(LocalDate.now().format(FMT));
        tfOut.setText(LocalDate.now().plusDays(1).format(FMT));
        cbRoom.setSelectedIndex(0); tfRoomRate.setText("0.00");
        chkCar.setSelected(false); cbCar.setSelectedIndex(0); tfCarRate.setText("0.00");
        taComments.setText(""); cbFlag.setSelectedIndex(0);
    }

    private void populate(Guest g) {
        tfName.setText(g.getName()); tfAddress.setText(g.getAddress()); tfPassport.setText(g.getPassportNumber());
        spGuests.setValue(g.getNumberOfGuests());
        if (g.getBirthdate()    != null) tfBirth.setText(g.getBirthdate().format(FMT));
        if (g.getCheckInDate()  != null) tfIn.setText(g.getCheckInDate().format(FMT));
        if (g.getCheckOutDate() != null) tfOut.setText(g.getCheckOutDate().format(FMT));
        if (g.getRoomType()     != null) cbRoom.setSelectedItem(g.getRoomType());
        tfRoomRate.setText(String.format("%.2f", g.getRoomRatePerNight()));
        chkCar.setSelected(g.isCarHired()); cbCar.setEnabled(g.isCarHired()); tfCarRate.setEnabled(g.isCarHired());
        if (g.getCarType()  != null) cbCar.setSelectedItem(g.getCarType());
        tfCarRate.setText(String.format("%.2f", g.getCarRatePerDay()));
        taComments.setText(g.getComments());
        if (g.getFlag()     != null) cbFlag.setSelectedItem(g.getFlag());
    }

    private Guest buildGuest() {
        Guest g = existing != null ? existing : new Guest();
        g.setName(tfName.getText().trim()); g.setAddress(tfAddress.getText().trim());
        g.setPassportNumber(tfPassport.getText().trim()); g.setNumberOfGuests((Integer) spGuests.getValue());
        g.setBirthdate(parseDate(tfBirth.getText())); g.setCheckInDate(parseDate(tfIn.getText())); g.setCheckOutDate(parseDate(tfOut.getText()));
        g.setRoomType((Guest.RoomType) cbRoom.getSelectedItem()); g.setRoomRatePerNight(parseDouble(tfRoomRate.getText()));
        g.setCarHired(chkCar.isSelected()); g.setCarType(chkCar.isSelected() ? (Guest.CarType) cbCar.getSelectedItem() : null);
        g.setCarRatePerDay(chkCar.isSelected() ? parseDouble(tfCarRate.getText()) : 0);
        g.setComments(taComments.getText()); g.setFlag((Guest.GuestFlag) cbFlag.getSelectedItem());
        return g;
    }

    private String doValidate() {
        if (tfName.getText().isBlank()) return "Please enter the guest's full name.";
        if (tfPassport.getText().isBlank()) return "Please enter a passport number.";
        LocalDate in = parseDate(tfIn.getText()), out = parseDate(tfOut.getText());
        if (in == null || out == null) return "Please enter dates in dd/mm/yyyy format.";
        if (!out.isAfter(in)) return "Check-out must be after check-in.";
        return null;
    }

    private LocalDate parseDate(String s) {
        try { return LocalDate.parse(s.trim(), FMT); } catch (DateTimeParseException e) { return null; }
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.replace(",",".")); } catch (NumberFormatException e) { return 0; }
    }
}
