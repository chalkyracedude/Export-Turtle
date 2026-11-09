package com.exportturtle.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;

/** Full-width rows for RuneLite's narrow sidebar, without mixed BoxLayout alignment. */
public final class SidebarLayout
{
    public enum StatusTone
    {
        SUCCESS(new Color(159, 255, 56)),
        WAITING(new Color(255, 242, 64)),
        ERROR(new Color(255, 67, 79));

        private final Color color;
        StatusTone(Color color)
        {
            this.color = color;
        }
    }

    public static void updateStatus(JTextArea label, String text, StatusTone tone)
    {
        label.setText(text);
        label.setForeground(tone.color);
    }

    private SidebarLayout()
    {
    }

    public static JPanel column()
    {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        return panel;
    }

    public static void addRow(JPanel panel, Component component, int topGap)
    {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = panel.getComponentCount();
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(topGap, 0, 0, 0);
        panel.add(component, constraints);
    }

    public static JTextArea status(String text)
    {
        JTextArea label = new JTextArea(text, 2, 0);
        label.setFont(UIManager.getFont("Label.font"));
        label.setForeground(StatusTone.WAITING.color);
        label.setOpaque(false);
        label.setEditable(false);
        label.setFocusable(false);
        label.setLineWrap(true);
        label.setWrapStyleWord(true);
        return label;
    }
}
