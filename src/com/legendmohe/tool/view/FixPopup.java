package com.legendmohe.tool.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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

    private static final int CONTENT_PANEL_PADDING = 4;

    private static final int TOOLBAR_PANEL_PADDING = 4;

    private static final int BORDER_THICKNESS = 1;

    private Popup mInternalPopup;

    // 鼠标是否悬停在上面
    private boolean mIsMouseEntered;

    private boolean mIsPinned;

    public FixPopup(String message, int maxWidth) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, BORDER_THICKNESS, true));
        setBackground(Color.WHITE);

        JPanel btnPanel = new JPanel(new BorderLayout());
        setupToolBar(btnPanel);
        add(btnPanel);

        JTextArea textArea = createMultiLineLabel(message);
        setupTextArea(textArea, maxWidth);
        add(textArea);

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

    private void setupToolBar(JPanel btnPanel) {
        btnPanel.setBorder(null);
        btnPanel.setBackground(null);
        btnPanel.setOpaque(false);
        btnPanel.setBorder(new EmptyBorder(TOOLBAR_PANEL_PADDING, TOOLBAR_PANEL_PADDING, 0, TOOLBAR_PANEL_PADDING));

        JButton pinBtn = new JButton();
        setupPinButton(pinBtn);
        btnPanel.add(pinBtn, BorderLayout.EAST);
    }

    private void setupPinButton(JButton pinBtn) {
        pinBtn.setBorder(null);
        pinBtn.setBorderPainted(false);
        pinBtn.setContentAreaFilled(false);
        pinBtn.setOpaque(false);
        pinBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mIsPinned = !mIsPinned;
                syncPinBtnState(pinBtn);
            }
        });
        syncPinBtnState(pinBtn);
    }

    private void syncPinBtnState(JButton pinBtn) {
        pinBtn.setText(mIsPinned ? "unpin" : "pin");
        pinBtn.setForeground(mIsPinned ? Color.DARK_GRAY : Color.lightGray);
    }

    private void setupTextArea(JTextArea textArea, int width) {
        FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
        int textWidth = SwingUtilities.computeStringWidth(fm, textArea.getText());
        Dimension d = new Dimension();
        d.setSize(Math.min(width, textWidth) + (CONTENT_PANEL_PADDING + BORDER_THICKNESS) * 2, fm.getHeight() * Math.ceil((double) textWidth / (double) width) + (CONTENT_PANEL_PADDING + BORDER_THICKNESS) * 2);

        textArea.setPreferredSize(d);
        textArea.setBorder(new EmptyBorder(CONTENT_PANEL_PADDING, CONTENT_PANEL_PADDING, CONTENT_PANEL_PADDING, CONTENT_PANEL_PADDING));
    }

    public boolean isMouseEntered() {
        return mIsMouseEntered;
    }

    public boolean isPinned() {
        return mIsPinned;
    }

    public void showPopup(Component owner, int x, int y) {
        hidePopup();
        mInternalPopup = new PopupFactory().getPopup(owner, this, x, y);
        mInternalPopup.show();
    }

    public void hidePopup() {
        if (mInternalPopup != null) {
            mInternalPopup.hide();
            mInternalPopup = null;
        }
    }
}
