package com.legendmohe.tool.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Created by hexinyu on 2019/2/2.
 */
public class FixPopup extends JPanel {

    private static final int PANEL_PADDING = 4;

    private static final int BORDER_THICKNESS = 1;

    private Popup mInternalPopup;

    // 鼠标是否悬停在上面
    private boolean mIsMouseEntered;

    public FixPopup(String message, int maxWidth) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, BORDER_THICKNESS, true),
                new EmptyBorder(PANEL_PADDING, PANEL_PADDING, PANEL_PADDING, PANEL_PADDING)
        ));
        setBackground(Color.LIGHT_GRAY);

        JTextArea textArea = createMultiLineLabel(message);
        add(textArea);
        setSize(textArea, maxWidth);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mIsMouseEntered = true;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mIsMouseEntered = false;
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        textArea.addMouseListener(mouseAdapter);
        textArea.addMouseMotionListener(mouseAdapter);
    }

    private JTextArea createMultiLineLabel(String message) {
        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(true);
        textArea.setCursor(null);
        textArea.setOpaque(false);
        textArea.setFocusable(true);
        textArea.setFont(getFont().deriveFont(getFont().getSize()));
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        return textArea;
    }

    private void setSize(JTextArea textArea, int width) {
        FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
        int textWidth = SwingUtilities.computeStringWidth(fm, textArea.getText());

        Dimension d = new Dimension();
        d.setSize(Math.min(width, textWidth) + (PANEL_PADDING + BORDER_THICKNESS) * 2, fm.getHeight() * Math.ceil((double) textWidth / (double) width) + (PANEL_PADDING + BORDER_THICKNESS) * 2);

        setPreferredSize(d);
    }

    public boolean isMouseEntered() {
        return mIsMouseEntered;
    }

    public void showPopup(Component owner, int x, int y) {
        hidePopup();
        mInternalPopup = PopupFactory.getSharedInstance().getPopup(owner, this, x, y);
        mInternalPopup.show();
    }

    public void hidePopup() {
        if (mInternalPopup != null) {
            mInternalPopup.hide();
            mInternalPopup = null;
        }
    }
}
