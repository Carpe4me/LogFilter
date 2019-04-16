package com.legendmohe.tool.view;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;

import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * Created by legendmohe on 2019/4/16.
 */
public class ExpandableSplitPane extends JSplitPane {

    private HiddenListener mHiddenListener;

    private JButton mLeftButton;

    private JButton mRightButton;

    //////////////////////////////////////////////////////////////////////

    public ExpandableSplitPane() {
    }

    public ExpandableSplitPane(int newOrientation) {
        super(newOrientation);
    }

    public ExpandableSplitPane(int newOrientation, boolean newContinuousLayout) {
        super(newOrientation, newContinuousLayout);
    }

    public ExpandableSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent) {
        super(newOrientation, newLeftComponent, newRightComponent);
    }

    public ExpandableSplitPane(int newOrientation, boolean newContinuousLayout, Component newLeftComponent, Component newRightComponent) {
        super(newOrientation, newContinuousLayout, newLeftComponent, newRightComponent);
    }

    //////////////////////////////////////////////////////////////////////

    public void setOneSideHidden(Component whichSide, boolean isHidden) {
        if (whichSide == getLeftComponent()) {
            // if right commponent hidden
            if (isRightComponentHidden()) {
                // show right and hide left
                if (isHidden) {
                    clickDividerButton(mLeftButton);
                    clickDividerButton(mLeftButton);
                }
            } else if (isLeftComponentHidden()) {
                // show left
                if (!isHidden) {
                    clickDividerButton(mRightButton);
                }
            } else {
                if (isHidden) {
                    clickDividerButton(mLeftButton);
                }
            }
        } else if (whichSide == getRightComponent()) {
            // if left commponent hidden
            if (isLeftComponentHidden()) {
                // show right and hide left
                if (isHidden) {
                    clickDividerButton(mRightButton);
                    clickDividerButton(mRightButton);
                }
            } else if (isRightComponentHidden()) {
                // show left
                if (!isHidden) {
                    clickDividerButton(mRightButton);
                }
            } else {
                if (isHidden) {
                    clickDividerButton(mRightButton);
                }
            }
        }
    }

    public boolean isSideHidden(Component whichSide) {
        if (whichSide == getLeftComponent()) {
            return isLeftComponentHidden();
        } else if (whichSide == getRightComponent()) {
            return isRightComponentHidden();
        }
        return false;
    }

    @Override
    public void setOneTouchExpandable(boolean expandable) {
        super.setOneTouchExpandable(expandable);
        if (expandable) {
            final BasicSplitPaneUI ui = ((BasicSplitPaneUI) getUI());

            Field keepHidden = null;
            try {
                keepHidden = BasicSplitPaneUI.class.getDeclaredField("keepHidden");
                keepHidden.setAccessible(true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            final Field finalKeepHidden = keepHidden;

            BasicSplitPaneDivider divider = ui.getDivider();
            try {
                Field leftButton = BasicSplitPaneDivider.class.getDeclaredField("leftButton");
                leftButton.setAccessible(true);
                Field rightButton = BasicSplitPaneDivider.class.getDeclaredField("rightButton");
                rightButton.setAccessible(true);

                mLeftButton = (JButton) leftButton.get(divider);
                mRightButton = (JButton) rightButton.get(divider);

                mLeftButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            boolean keepHidden = (boolean) finalKeepHidden.get(ui);
                            handleActionPerformed(mLeftButton, keepHidden);
                        } catch (IllegalAccessException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                mRightButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            boolean keepHidden = (boolean) finalKeepHidden.get(ui);
                            handleActionPerformed(mRightButton, keepHidden);
                        } catch (IllegalAccessException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            mRightButton = mLeftButton = null;
        }
    }

    ///////////////////////////////////private///////////////////////////////////

    private void handleActionPerformed(JButton whichButton, boolean keepHidden) {
        if (mHiddenListener != null) {
            if (whichButton == mLeftButton) {
                if (isNoSideHidden() && !keepHidden) {
                    mHiddenListener.onStateChanged(this, getLeftComponent(), true);
                } else if (isRightComponentHidden() && keepHidden) {
                    mHiddenListener.onStateChanged(this, getRightComponent(), false);
                }
            } else if (whichButton == mRightButton) {
                if (isNoSideHidden() && !keepHidden) {
                    mHiddenListener.onStateChanged(this, getRightComponent(), true);
                } else if (isLeftComponentHidden() && keepHidden) {
                    mHiddenListener.onStateChanged(this, getLeftComponent(), false);
                }
            }
        }
    }

    private void clickDividerButton(JButton leftButton) {
        leftButton.doClick();
    }

    private boolean isNoSideHidden() {
        return (getDividerLocation() >= getMinimumDividerLocation()) && (getDividerLocation() <= getMaximumDividerLocation());
    }

    private boolean isLeftComponentHidden() {
        return getDividerLocation() < getMinimumDividerLocation();
    }

    private boolean isRightComponentHidden() {
        return getDividerLocation() > getMaximumDividerLocation();
    }

    ///////////////////////////////////listener///////////////////////////////////

    public void setHiddenListener(HiddenListener hiddenListener) {
        mHiddenListener = hiddenListener;
    }

    public interface HiddenListener {
        void onStateChanged(ExpandableSplitPane pane, Component whichSide, boolean hidden);
    }
}
