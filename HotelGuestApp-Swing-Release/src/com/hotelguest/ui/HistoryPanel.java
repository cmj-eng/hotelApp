package com.hotelguest.ui;

import com.hotelguest.model.Guest;
import com.hotelguest.model.HistoryEvent;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.hotelguest.Main.store;

public class HistoryPanel extends JPanel {

    record FlatEvent(String guestName, HistoryEvent event) {}

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final DefaultTableModel model = new DefaultTableModel(
        new String[]{"Date & Time", "Guest", "Type", "Description"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField searchField = new JTextField(28);
    private List<FlatEvent> currentEvents = new ArrayList<>();

    public HistoryPanel() {
        setLayout(new BorderLayout());
        setBackground(UI.BG);

        // Toolbar
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
        add(toolbar, BorderLayout.NORTH);

        // Table
        table.setFont(UI.FONT);
        table.setRowHeight(26);
        table.getTableHeader().setFont(UI.FONT_BOLD);
        table.getTableHeader().setBackground(UI.HEADER_BG);
        table.setGridColor(new Color(235, 235, 235));

        int[] widths = {155, 170, 110, 400};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Row colour by type
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                if (!sel && r < currentEvents.size()) {
                    HistoryEvent.EventType type = currentEvents.get(r).event().getType();
                    if (type != null) switch (type) {
                        case PAYMENT    -> comp.setBackground(new Color(232, 248, 240));
                        case CREATED    -> comp.setBackground(new Color(234, 244, 255));
                        case CHECKED_IN -> comp.setBackground(new Color(240, 255, 232));
                        default         -> comp.setBackground(r % 2 == 0 ? UI.WHITE : UI.ROW_ALT);
                    }
                }
                ((JLabel)comp).setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                comp.setForeground(Color.BLACK);
                return comp;
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    public void refresh() {
        String search = searchField.getText().toLowerCase();
        currentEvents = store.getGuests().stream()
            .flatMap(g -> g.getHistory().stream().map(ev -> new FlatEvent(g.getName(), ev)))
            .filter(fe -> search.isEmpty()
                || fe.guestName().toLowerCase().contains(search)
                || (fe.event().getDescription() != null && fe.event().getDescription().toLowerCase().contains(search)))
            .sorted(Comparator.comparing(
                fe -> fe.event().getDate(),
                Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());

        model.setRowCount(0);
        for (FlatEvent fe : currentEvents) {
            model.addRow(new Object[]{
                fe.event().getDate() != null ? fe.event().getDate().format(FMT) : "",
                fe.guestName(),
                fe.event().getType() != null ? fe.event().getType().name() : "",
                fe.event().getDescription()
            });
        }
    }
}
