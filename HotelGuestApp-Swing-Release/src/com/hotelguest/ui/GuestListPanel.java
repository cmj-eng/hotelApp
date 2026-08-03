package com.hotelguest.ui;

import com.hotelguest.model.Guest;
import com.hotelguest.model.BillItem;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.hotelguest.Main.store;

public class GuestListPanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final String[] COLS = {
        "Guest Name","Room","Pax","Check-in","Check-out","Nights","Car","Charged","Open Bill","Status","Special Req","Passport"
    };

    private final DefaultTableModel model = new DefaultTableModel(COLS, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField searchField = new JTextField(22);
    private final JComboBox<String> flagFilter = new JComboBox<>(
        new String[]{"All","Very Good","Unreliable","Blacklisted","Special Request"});

    private List<Guest> currentGuests = new ArrayList<>();

    public GuestListPanel() {
        setLayout(new BorderLayout());
        setBackground(UI.BG);
        buildToolbar();
        buildTable();
        refresh();
    }

    private void buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        toolbar.setBackground(UI.WHITE);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UI.BORDER));

        toolbar.add(UI.formLabel("Search:"));
        searchField.setFont(UI.FONT);
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
        });
        toolbar.add(searchField);
        toolbar.add(UI.formLabel("Filter:"));
        flagFilter.setFont(UI.FONT);
        flagFilter.addActionListener(e -> refresh());
        toolbar.add(flagFilter);

        // Spacer
        toolbar.add(Box.createHorizontalStrut(20));

        JButton addCharge = UI.secondaryButton("$ Add Charge");
        addCharge.addActionListener(e -> {
            Guest sel = getSelected();
            if (sel == null) { JOptionPane.showMessageDialog(this, "Select a guest first."); return; }
            addBillItem(sel);
        });
        toolbar.add(addCharge);

        JButton editBtn = UI.secondaryButton("✏ Edit");
        editBtn.addActionListener(e -> {
            Guest sel = getSelected();
            if (sel != null) openEdit(sel);
        });
        toolbar.add(editBtn);

        JButton deleteBtn = UI.dangerButton("🗑 Delete");
        deleteBtn.addActionListener(e -> {
            Guest sel = getSelected();
            if (sel == null) return;
            int r = JOptionPane.showConfirmDialog(this,
                "Delete \"" + sel.getName() + "\"? This cannot be undone.",
                "Delete Guest", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r == JOptionPane.YES_OPTION) { store.delete(sel); refresh(); }
        });
        toolbar.add(deleteBtn);

        JButton addBtn = UI.primaryButton("+ New Guest");
        addBtn.addActionListener(e -> openEdit(null));
        toolbar.add(addBtn);

        add(toolbar, BorderLayout.NORTH);
    }

    private void buildTable() {
        table.setFont(UI.FONT);
        table.setRowHeight(26);
        table.getTableHeader().setFont(UI.FONT_BOLD);
        table.getTableHeader().setBackground(UI.HEADER_BG);
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(235, 235, 235));
        table.setShowGrid(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Column widths
        int[] widths = {160, 90, 40, 100, 100, 55, 80, 80, 80, 90, 80, 110};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Row colouring
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel && row < currentGuests.size()) {
                    Guest g = currentGuests.get(row);
                    if (g.hasSpecialRequest())                         c.setBackground(UI.ROW_SPECIAL);
                    else if (g.getFlag() == Guest.GuestFlag.VERY_GOOD) c.setBackground(UI.ROW_VGOOD);
                    else if (g.getFlag() == Guest.GuestFlag.UNRELIABLE)c.setBackground(UI.ROW_UNREL);
                    else if (g.getFlag() == Guest.GuestFlag.BLACKLISTED)c.setBackground(UI.ROW_BLACK);
                    else                                               c.setBackground(row % 2 == 0 ? UI.WHITE : UI.ROW_ALT);

                    // Red open-bill column
                    if (col == 8 && val != null && val.toString().startsWith("$")) {
                        c.setForeground(UI.RED);
                        ((JLabel)c).setFont(UI.FONT_BOLD);
                    } else {
                        c.setForeground(Color.BLACK);
                        ((JLabel)c).setFont(UI.FONT);
                    }
                }
                ((JLabel)c).setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return c;
            }
        });

        // Double-click to edit
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Guest sel = getSelected();
                    if (sel != null) openEdit(sel);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);
    }

    public void refresh() {
        String search = searchField.getText().toLowerCase();
        String flag = (String) flagFilter.getSelectedItem();

        currentGuests = store.getGuests().stream()
            .filter(g -> search.isEmpty()
                || g.getName().toLowerCase().contains(search)
                || g.getPassportNumber().toLowerCase().contains(search)
                || g.getAddress().toLowerCase().contains(search))
            .filter(g -> {
                if ("All".equals(flag)) return true;
                if ("Very Good".equals(flag))       return g.getFlag() == Guest.GuestFlag.VERY_GOOD;
                if ("Unreliable".equals(flag))      return g.getFlag() == Guest.GuestFlag.UNRELIABLE;
                if ("Blacklisted".equals(flag))     return g.getFlag() == Guest.GuestFlag.BLACKLISTED;
                if ("Special Request".equals(flag)) return g.hasSpecialRequest();
                return true;
            })
            .sorted(Comparator.comparing(Guest::getCheckInDate, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());

        model.setRowCount(0);
        for (Guest g : currentGuests) {
            model.addRow(new Object[]{
                g.getName(),
                g.getRoomType() != null ? g.getRoomType().getLabel() : "",
                g.getNumberOfGuests(),
                g.getCheckInDate() != null  ? g.getCheckInDate().format(FMT)  : "",
                g.getCheckOutDate() != null ? g.getCheckOutDate().format(FMT) : "",
                g.getStayDuration(),
                g.isCarHired() && g.getCarType() != null ? g.getCarType().getLabel() : "—",
                String.format("$%.2f", g.getTotalCharged()),
                g.getOpenBill() > 0 ? String.format("$%.2f", g.getOpenBill()) : "—",
                g.getFlag() != null ? g.getFlag().getLabel() : "",
                g.hasSpecialRequest() ? "⚑ Yes" : "",
                g.getPassportNumber()
            });
        }
    }

    private Guest getSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentGuests.size()) return null;
        return currentGuests.get(row);
    }

    private void openEdit(Guest guest) {
        AddEditGuestDialog dlg = new AddEditGuestDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), guest);
        dlg.setVisible(true);
        Guest result = dlg.getResult();
        if (result != null) {
            if (guest == null) store.add(result);
            else store.update(result);
            refresh();
        }
    }

    private void addBillItem(Guest g) {
        JTextField desc = new JTextField(20);
        JTextField amt  = new JTextField("0.00", 10);
        JCheckBox paid  = new JCheckBox("Already paid");
        JPanel p = new JPanel(new GridLayout(3, 2, 8, 8));
        p.add(new JLabel("Description:")); p.add(desc);
        p.add(new JLabel("Amount ($):"));  p.add(amt);
        p.add(new JLabel());               p.add(paid);
        int r = JOptionPane.showConfirmDialog(this, p, "Add Charge — " + g.getName(),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION || desc.getText().isBlank()) return;
        double amount = 0;
        try { amount = Double.parseDouble(amt.getText().replace(",",".")); } catch (NumberFormatException ignored) {}
        store.addBillItem(g.getId(), new BillItem(desc.getText().trim(), amount, paid.isSelected()));
        refresh();
    }
}
