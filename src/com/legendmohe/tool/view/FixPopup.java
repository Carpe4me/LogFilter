package com.legendmohe.tool.view;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Window;
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
import javax.swing.RootPaneContainer;
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

    private Listener mListener;

    // 鼠标是否悬停在上面
    private boolean mIsMouseEntered;

    private boolean mIsPinned;

    // popup内部的component，用于drag
    private Component mInternalPopupComponent;

    private Object mContext;

    public FixPopup(String message, int maxWidth, Object context) {
        mContext = context;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, BORDER_THICKNESS, true));
        setBackground(new Color(0xDAD8E5));

        JPanel btnPanel = new JPanel(new BorderLayout());
        setupToolBar(btnPanel);
        add(btnPanel);

        JTextArea textArea = createMultiLineLabel(message);
        setupTextArea(textArea, maxWidth);
        add(textArea);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            private int mMouseY;
            private int mMouseX;

            @Override
            public void mouseEntered(MouseEvent e) {
                mIsMouseEntered = true;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mIsMouseEntered = false;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                mMouseX = e.getX();
                mMouseY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (mInternalPopupComponent != null && mIsPinned) {
                    Point point = SwingUtilities.convertPoint(FixPopup.this, e.getX(), e.getY(), mInternalPopupComponent.getParent());
                    mInternalPopupComponent.setLocation(point.x - mMouseX, point.y - mMouseY);
                }
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

        JButton goButton = new JButton();
        setupGoButton(goButton);
        btnPanel.add(goButton, BorderLayout.WEST);
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

    private void setupGoButton(JButton goButton) {
        goButton.setText("<<");
        goButton.setBorder(null);
        goButton.setBorderPainted(false);
        goButton.setContentAreaFilled(false);
        goButton.setOpaque(false);
        goButton.setForeground(Color.GRAY);
        goButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (mListener != null) {
                    mListener.onGoButtonClick(FixPopup.this);
                }
            }
        });
    }

    private void syncPinBtnState(JButton pinBtn) {
        pinBtn.setText(mIsPinned ? "unpin" : "pin");
        pinBtn.setForeground(mIsPinned ? Color.DARK_GRAY : Color.GRAY);
    }

    private void setupTextArea(JTextArea textArea, int width) {
        FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
        int textWidth = SwingUtilities.computeStringWidth(fm, textArea.getText());
        Dimension d = new Dimension();
        d.setSize(Math.min(width, textWidth) + (CONTENT_PANEL_PADDING + BORDER_THICKNESS) * 2, fm.getHeight() * Math.ceil((double) textWidth / (double) width) + (CONTENT_PANEL_PADDING + BORDER_THICKNESS) * 2);

        textArea.setPreferredSize(d);
        textArea.setBorder(new EmptyBorder(CONTENT_PANEL_PADDING, CONTENT_PANEL_PADDING, CONTENT_PANEL_PADDING, CONTENT_PANEL_PADDING));

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
        textArea.addMouseListener(mouseAdapter);
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
        mInternalPopupComponent = getInnerPopupComponent(owner, x, y);
    }

    // refer to javax.swing.PopupFactory.MediumWeightPopup.show
    private Component getInnerPopupComponent(Component owner, int x, int y) {
        Container parent = owner.getParent();
        while (!(parent instanceof Window || parent instanceof Applet) &&
                (parent != null)) {
            parent = parent.getParent();
        }
        if (parent instanceof RootPaneContainer) {
            parent = ((RootPaneContainer) parent).getLayeredPane();
            return parent.getComponentAt(x, y);
        }

        return null;
    }

    public void hidePopup() {
        if (mInternalPopup != null) {
            mInternalPopup.hide();
            mInternalPopup = null;
            mInternalPopupComponent = null;
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public Object getContext() {
        return mContext;
    }

    ///////////////////////////////////listener///////////////////////////////////

    public interface Listener {
        void onGoButtonClick(FixPopup popup);
    }
}
