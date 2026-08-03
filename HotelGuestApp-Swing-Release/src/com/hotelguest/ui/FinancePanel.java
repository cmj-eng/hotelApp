package com.hotelguest.ui;

import com.hotelguest.model.Guest;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.hotelguest.Main.store;

public class FinancePanel extends JPanel {

    private final JLabel lblRevenue = tile("$0.00");
    private final JLabel lblProfit  = tile("$0.00");
    private final JLabel lblOpen    = tile("$0.00");
    private final JLabel lblWeek    = tile("$0.00");

    private final DefaultTableModel profitModel  = new DefaultTableModel(
        new String[]{"Guest","Room","Car","Extras","Charged","Open Bill","Profit"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel billModel = new DefaultTableModel(
        new String[]{"Guest","Room Type","Open Bill"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable profitTable = styledTable(profitModel);
    private final JTable billTable   = styledTable(billModel);

    private final WeeklyChartPanel chart = new WeeklyChartPanel();

    public FinancePanel() {
        setLayout(new BorderLayout());
        setBackground(UI.BG);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UI.BG);
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Tiles
        JPanel tileRow = new JPanel(new GridLayout(1, 4, 14, 0));
        tileRow.setBackground(UI.BG);
        tileRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        tileRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        tileRow.add(tileCard("Total Revenue", lblRevenue, UI.BLUE));
        tileRow.add(tileCard("Total Profit",  lblProfit,  UI.GREEN));
        tileRow.add(tileCard("Open Bills",    lblOpen,    UI.RED));
        tileRow.add(tileCard("This Week",     lblWeek,    UI.ORANGE));
        content.add(tileRow);
        content.add(Box.createVerticalStrut(20));

        // Profit table
        content.add(sectionLabel("Profit Per Guest"));
        content.add(Box.createVerticalStrut(6));
        JScrollPane ps = new JScrollPane(profitTable); ps.setAlignmentX(Component.LEFT_ALIGNMENT);
        ps.setPreferredSize(new Dimension(0, 200)); ps.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        content.add(ps);
        content.add(Box.createVerticalStrut(16));

        // Open bills table
        content.add(sectionLabel("Outstanding Bills"));
        content.add(Box.createVerticalStrut(6));
        JScrollPane bs = new JScrollPane(billTable); bs.setAlignmentX(Component.LEFT_ALIGNMENT);
        bs.setPreferredSize(new Dimension(0, 140)); bs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        content.add(bs);
        content.add(Box.createVerticalStrut(16));

        // Chart
        content.add(sectionLabel("Weekly Profit (Last 4 Weeks)"));
        content.add(Box.createVerticalStrut(6));
        chart.setAlignmentX(Component.LEFT_ALIGNMENT);
        chart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        content.add(chart);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll);

        refresh();
    }

    public void refresh() {
        lblRevenue.setText(fmt(store.getTotalRevenue()));
        lblProfit .setText(fmt(store.getTotalProfit()));
        lblOpen   .setText(fmt(store.getTotalOpenBills()));
        lblWeek   .setText(fmt(store.getProfitForWeek(LocalDate.now())));

        profitModel.setRowCount(0);
        store.getGuests().stream()
            .filter(g -> g.getTotalCharged() > 0)
            .sorted(Comparator.comparingDouble(Guest::getProfit).reversed())
            .forEach(g -> profitModel.addRow(new Object[]{
                g.getName(), fmt(g.getRoomTotal()), fmt(g.getCarTotal()),
                fmt(g.getExtraCharges()), fmt(g.getTotalCharged()),
                g.getOpenBill() > 0 ? fmt(g.getOpenBill()) : "—", fmt(g.getProfit())
            }));

        billModel.setRowCount(0);
        store.getGuests().stream()
            .filter(g -> g.getOpenBill() > 0)
            .sorted(Comparator.comparingDouble(Guest::getOpenBill).reversed())
            .forEach(g -> billModel.addRow(new Object[]{
                g.getName(), g.getRoomType().getLabel(), fmt(g.getOpenBill())
            }));

        // Colour open bill column red in profit table (col 5) and bill table (col 2)
        colorColumn(profitTable, 5, UI.RED);
        colorColumn(profitTable, 6, UI.GREEN);
        colorColumn(billTable,   2, UI.RED);

        chart.repaint();
    }

    private void colorColumn(JTable t, int col, Color color) {
        t.getColumnModel().getColumn(col).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(tbl, val, sel, foc, r, c);
                if (!sel) { comp.setForeground("—".equals(val) ? Color.GRAY : color); }
                ((JLabel)comp).setFont(UI.FONT_BOLD);
                ((JLabel)comp).setBorder(BorderFactory.createEmptyBorder(0,6,0,6));
                return comp;
            }
        });
    }

    private JLabel tile(String text) {
        JLabel l = new JLabel(text, SwingConstants.LEFT);
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        return l;
    }

    private JPanel tileCard(String caption, JLabel value, Color color) {
        value.setForeground(color);
        JLabel cap = new JLabel(caption);
        cap.setFont(UI.FONT_SMALL); cap.setForeground(UI.TEXT_MUTED);
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 4));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UI.BORDER, 1, true),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        p.setBackground(UI.WHITE);
        p.add(value); p.add(cap);
        return p;
    }

    private JTable styledTable(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setFont(UI.FONT); t.setRowHeight(24);
        t.getTableHeader().setFont(UI.FONT_BOLD);
        t.getTableHeader().setBackground(UI.HEADER_BG);
        t.setGridColor(new Color(235,235,235));
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(tbl, val, sel, foc, r, c);
                if (!sel) comp.setBackground(r % 2 == 0 ? UI.WHITE : UI.ROW_ALT);
                ((JLabel)comp).setBorder(BorderFactory.createEmptyBorder(0,6,0,6));
                return comp;
            }
        });
        return t;
    }

    private JLabel sectionLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        l.setForeground(new Color(44,62,80));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private String fmt(double v) { return String.format("$%.2f", v); }

    // ── Inner chart panel ────────────────────────────────────────────────────
    class WeeklyChartPanel extends JPanel {
        WeeklyChartPanel() {
            setBackground(UI.WHITE);
            setPreferredSize(new Dimension(0, 155));
            setBorder(BorderFactory.createLineBorder(UI.BORDER, 1, true));
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            double[] profits = new double[4];
            String[]  labels = new String[4];
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 4; i++) {
                LocalDate d = today.minusWeeks(3 - i);
                profits[i] = store.getProfitForWeek(d);
                labels[i]  = d.with(DayOfWeek.MONDAY).format(fmt);
            }
            double max = 1;
            for (double p : profits) if (p > max) max = p;

            int barW = 80, totalBars = 4;
            int totalBarSpace = barW * totalBars;
            int gap = (w - totalBarSpace) / (totalBars + 1);
            int chartH = h - 40;

            for (int i = 0; i < 4; i++) {
                int barH = (int) (profits[i] / max * chartH);
                int x = gap + i * (barW + gap);
                int y = chartH - barH;
                g.setColor(new Color(46, 204, 113, 200));
                g.fillRoundRect(x, y, barW, barH, 8, 8);
                g.setColor(new Color(39, 174, 96));
                g.setFont(UI.FONT_SMALL);
                g.drawString("$" + (int)profits[i], x + barW/2 - 18, y - 4);
                g.setColor(UI.TEXT_MUTED);
                g.drawString(labels[i], x + barW/2 - 22, h - 8);
            }
        }
    }
}
