package com.hotelguest.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class UI {

    // Palette
    public static final Color BG          = new Color(245, 246, 250);
    public static final Color WHITE       = Color.WHITE;
    public static final Color BLUE        = new Color(52, 152, 219);
    public static final Color BLUE_DARK   = new Color(41, 128, 185);
    public static final Color GREEN       = new Color(39, 174, 96);
    public static final Color RED         = new Color(192, 57, 43);
    public static final Color ORANGE      = new Color(230, 126, 34);
    public static final Color PURPLE      = new Color(142, 68, 173);
    public static final Color BORDER      = new Color(220, 220, 220);
    public static final Color ROW_ALT     = new Color(248, 249, 252);
    public static final Color ROW_SPECIAL = new Color(243, 232, 255);
    public static final Color ROW_VGOOD   = new Color(232, 248, 232);
    public static final Color ROW_UNREL   = new Color(255, 246, 232);
    public static final Color ROW_BLACK   = new Color(253, 232, 232);
    public static final Color TEXT_MUTED  = new Color(100, 100, 100);
    public static final Color HEADER_BG   = new Color(240, 242, 245);

    // Fonts
    public static final Font FONT         = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    public static final Font FONT_BOLD    = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    public static final Font FONT_SMALL   = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    public static final Font FONT_SECTION = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    public static final Font FONT_TITLE   = new Font(Font.SANS_SERIF, Font.BOLD, 20);

    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(BLUE);
        b.setForeground(Color.WHITE);
        b.setFont(FONT_BOLD);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        return b;
    }

    public static JButton dangerButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(RED);
        b.setForeground(Color.WHITE);
        b.setFont(FONT_BOLD);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        return b;
    }

    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(236, 240, 241));
        b.setForeground(new Color(44, 62, 80));
        b.setFont(FONT);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        return b;
    }

    public static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_SECTION);
        l.setForeground(new Color(44, 62, 80));
        l.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
        return l;
    }

    public static JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT);
        l.setForeground(TEXT_MUTED);
        return l;
    }

    public static JTextField textField() {
        JTextField tf = new JTextField();
        tf.setFont(FONT);
        return tf;
    }

    public static JTextArea textArea(int rows) {
        JTextArea ta = new JTextArea(rows, 20);
        ta.setFont(FONT);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        return ta;
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)
        );
    }

    public static Border padding(int v, int h) {
        return BorderFactory.createEmptyBorder(v, h, v, h);
    }

    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(WHITE);
        p.setBorder(cardBorder());
        return p;
    }
}
