package com.exportturtle.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicCheckBoxUI;
import javax.swing.plaf.basic.BasicComboBoxUI;

/** Scoped Swing styling: never changes RuneLite's global look and feel. */
public final class ExportTheme
{
    public static final Color BACKGROUND = new Color(11, 27, 18);
    public static final Color GOLD = new Color(216, 188, 120);
    public static final Color PANEL = new Color(15, 34, 27);
    public static final Color PREVIEW_BORDER = new Color(108, 132, 110);
    public static final Color INPUT = new Color(9, 23, 18);
    private static final Color CONTROL = new Color(48, 102, 66);
    private static final Color HOVER = new Color(61, 125, 81);
    public static final Color BORDER = new Color(76, 122, 87);
    private static final Color TEXT = new Color(239, 230, 198);
    private static final Color SELECTED = new Color(122, 210, 49);

    private ExportTheme()
    {
    }

    public static void apply(Component component)
    {
        if (component instanceof JCheckBox)
        {
            JCheckBox check = (JCheckBox)component;
            check.setUI(new BasicCheckBoxUI());
            check.setForeground(TEXT);
            Icon icon = new CheckIcon();
            check.setIcon(icon);
            check.setSelectedIcon(icon);
            check.setDisabledIcon(icon);
            check.setDisabledSelectedIcon(icon);
            check.setOpaque(false);
        }
        else if (component instanceof JButton)
        {
            JButton button = (JButton)component;
            button.setUI(new BasicButtonUI());
            button.setOpaque(true);
            button.setForeground(TEXT);
            button.setBackground(CONTROL);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(4, 2, 4, 2)));
            button.getModel().addChangeListener(event
                -> button.setBackground(
                    button.getModel().isRollover() || button.getModel().isPressed() ? HOVER : CONTROL));
            button.setRolloverEnabled(true);
        }
        else if (component instanceof JComboBox)
        {
            JComboBox<?> combo = (JComboBox<?>)component;
            combo.setUI(new BasicComboBoxUI() {
                @Override protected JButton createArrowButton()
                {
                    return new BasicArrowButton(BasicArrowButton.SOUTH, CONTROL, BORDER, TEXT, HOVER);
                }

                @Override
                public void paintCurrentValueBackground(
                    Graphics graphics, java.awt.Rectangle bounds, boolean focused)
                {
                    graphics.setColor(INPUT);
                    graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                }

                @Override
                public void paintCurrentValue(Graphics graphics, java.awt.Rectangle bounds, boolean focused)
                {
                    // Keep keyboard focus behavior without the look-and-feel's gray selection fill.
                    super.paintCurrentValue(graphics, bounds, false);
                }
            });
            combo.setBackground(INPUT);
            combo.setForeground(TEXT);
            combo.setBorder(BorderFactory.createLineBorder(BORDER));
            combo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean selected, boolean focus)
                {
                    Component label = super.getListCellRendererComponent(list, value, index, selected, focus);
                    label.setBackground(selected ? CONTROL : INPUT);
                    label.setForeground(TEXT);
                    Object details = combo.getClientProperty("exportturtle.savedPoses");
                    if (label instanceof JLabel && details instanceof Map)
                    {
                        Object saved = ((Map<?, ?>) details).get(value);
                        ((JLabel)label).setText(String.valueOf(value) + (saved == null ? "" : " •"));
                        ((JLabel)label).setToolTipText(saved == null ? "Empty slot" : saved.toString());
                    }
                    return label;
                }
            });
        }
        else if (component instanceof JLabel)
            component.setForeground(TEXT);
        if (component instanceof Container)
            for (Component child : ((Container)component).getComponents())
                apply(child);
    }

    private static final class CheckIcon implements Icon
    {
        @Override public int getIconWidth()
        {
            return 16;
        }
        @Override public int getIconHeight()
        {
            return 16;
        }
        @Override public void paintIcon(Component component, Graphics graphics, int x, int y)
        {
            boolean checked = ((AbstractButton)component).isSelected();
            graphics.setColor(checked ? SELECTED : INPUT);
            graphics.fillRoundRect(x, y, 15, 15, 4, 4);
            graphics.setColor(BORDER);
            graphics.drawRoundRect(x, y, 15, 15, 4, 4);
            if (checked)
            {
                graphics.setColor(BACKGROUND);
                graphics.drawLine(x + 3, y + 8, x + 6, y + 11);
                graphics.drawLine(x + 6, y + 11, x + 12, y + 4);
            }
        }
    }
}
