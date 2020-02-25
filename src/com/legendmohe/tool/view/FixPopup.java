package com.legendmohe.tool.view;

import com.legendmohe.tool.Utils;
import com.legendmohe.tool.config.Constant;

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
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
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
    private final JPanel mBottomPanel;

    public FixPopup(String message, int maxWidth, int minWidth, Object context) {
        mContext = context;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, BORDER_THICKNESS, true));
        setBackground(new Color(Constant.COLOR_LOG_TABLE_POPUP_BACKGROUND));

        JPanel btnPanel = new JPanel(new BorderLayout());
        setupToolBar(btnPanel);
        add(btnPanel);

        JTextArea textArea = createMultiLineLabel(message);
        setupTextArea(textArea, maxWidth, minWidth);
        add(textArea);

        mBottomPanel = createBottomPanel();
        add(mBottomPanel);

        MouseAdapter draggableMouseAdapter = new MouseAdapter() {
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
                if (mInternalPopupComponent != null) {
                    Point point = SwingUtilities.convertPoint(FixPopup.this, e.getX(), e.getY(), mInternalPopupComponent.getParent());
                    mInternalPopupComponent.setLocation(point.x - mMouseX, point.y - mMouseY);
                }
            }
        };
        MouseAdapter normalMouseAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mIsMouseEntered = true;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mIsMouseEntered = false;
            }
        };

        // 设置所有component
        addMouseListener(normalMouseAdapter);
        addMouseMotionListener(normalMouseAdapter);
        List<Component> allComponents = Utils.getAllComponents(this);
        for (Component component : allComponents) {
            if (component == btnPanel) {
                component.addMouseListener(draggableMouseAdapter);
                component.addMouseMotionListener(draggableMouseAdapter);
            } else {
                component.addMouseListener(normalMouseAdapter);
            }
        }
    }

    private JPanel createBottomPanel() {
        JPanel resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.X_AXIS));
        resultPanel.setVisible(false);
        resultPanel.setBorder(null);
        resultPanel.setBackground(null);
        resultPanel.setOpaque(false);
        resultPanel.setBorder(new EmptyBorder(0, TOOLBAR_PANEL_PADDING, TOOLBAR_PANEL_PADDING, TOOLBAR_PANEL_PADDING));
        return resultPanel;
    }

    private JTextArea createMultiLineLabel(String message) {
        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setCursor(null);
        textArea.setOpaque(false);
        textArea.setFocusable(true);
        textArea.setFont(getFont().deriveFont(getFont().getSize()));
//        textArea.setWrapStyleWord(true); // 高度会计算错误
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

    private void setupTextArea(JTextArea textArea, int width, int minWidth) {
        FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
        String content = textArea.getText();
        int targetWidth = 0;
        double targetHeight = 0;

        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            int textWidth = line.length() == 0 ? 1 : SwingUtilities.computeStringWidth(fm, line);
            int textHeight = fm.getHeight();
            // 选取最大的width
            targetWidth = Math.max(targetWidth, Math.max(minWidth, Math.min(width, textWidth)));
            // 累加height
            targetHeight += textHeight * Math.ceil((double) textWidth / (double) targetWidth);
        }

        Dimension d = new Dimension();
        d.setSize(targetWidth + (CONTENT_PANEL_PADDING) * 2, targetHeight + (CONTENT_PANEL_PADDING) * 2);

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

            Point location = new Point(x, y);
            SwingUtilities.convertPointFromScreen(location, parent);
            Component componentAt = parent.getComponentAt(location.x, location.y);
            // patch
            return componentAt.getName().equalsIgnoreCase("panel0") ? componentAt : null;
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

    public void addBottomComponent(JComponent component) {
        if (!mBottomPanel.isVisible()) {
            mBottomPanel.setVisible(true);
        }
        mBottomPanel.add(component);
        mBottomPanel.add(Box.createHorizontalGlue());
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
