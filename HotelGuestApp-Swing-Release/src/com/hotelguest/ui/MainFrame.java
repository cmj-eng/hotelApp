package com.hotelguest.ui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("Hotel Guest Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 740);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        GuestListPanel guestList = new GuestListPanel();
        AddEditGuestPanel addGuest = new AddEditGuestPanel(null, guestList::refresh);
        FinancePanel finance = new FinancePanel();
        HistoryPanel history = new HistoryPanel();
        DataPortPanel dataPort = new DataPortPanel(() -> {
            guestList.refresh();
            finance.refresh();
            history.refresh();
        });

        tabs.addTab("  Guests  ",   guestList);
        tabs.addTab("  New Guest  ", addGuest);
        tabs.addTab("  Finance  ",   finance);
        tabs.addTab("  History  ",   history);
        tabs.addTab("  Data  ",      dataPort);

        tabs.addChangeListener(e -> {
            int i = tabs.getSelectedIndex();
            if (i == 2) finance.refresh();
            if (i == 3) history.refresh();
        });

        add(tabs);
    }
}
