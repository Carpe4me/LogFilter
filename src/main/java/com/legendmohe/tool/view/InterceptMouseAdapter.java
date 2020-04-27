package com.legendmohe.tool.view;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

// 实现动态拦截parent view对event的处理。如果不想parent处理，就不要调用super
public class InterceptMouseAdapter extends MouseAdapter {

    private JComponent eventHandler;
    private JComponent target;

    public InterceptMouseAdapter(JComponent eventHandler, JComponent target) {
        this.eventHandler = eventHandler;
        this.target = target;
    }

    private MouseEvent convertEvent(MouseEvent e, JComponent target, JComponent eventHandler) {
        return SwingUtilities.convertMouseEvent(target, e, eventHandler);
    }

    public void mouseReleased(MouseEvent e) {
        eventHandler.dispatchEvent(convertEvent(e, target, eventHandler));
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        eventHandler.dispatchEvent(convertEvent(e, target, eventHandler));
    }

    @Override
    public void mousePressed(MouseEvent e) {
        eventHandler.dispatchEvent(convertEvent(e, target, eventHandler));
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        eventHandler.dispatchEvent(convertEvent(e, target, eventHandler));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        eventHandler.dispatchEvent(convertEvent(e, target, eventHandler));
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        eventHandler.dispatchEvent(convertEvent(e, target, eventHandler));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        eventHandler.dispatchEvent(convertEvent(e, target, eventHandler));
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        eventHandler.dispatchEvent(convertEvent(e, target, eventHandler));
    }
}
