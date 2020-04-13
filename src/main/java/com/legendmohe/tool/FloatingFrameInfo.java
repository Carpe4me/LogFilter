package com.legendmohe.tool;

import javax.swing.JFrame;

public class FloatingFrameInfo {
    public JFrame frame;
    public boolean isRemoved;

    FloatingFrameInfo(JFrame frame, boolean isRemoved) {
        this.frame = frame;
        this.isRemoved = isRemoved;
    }
}
