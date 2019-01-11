package com.legendmohe.tool.view;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;

/**
 * Created by xinyu.he on 2016/2/5.
 */
public class RowsContentDialog extends JDialog {

    public RowsContentDialog(JFrame frame, String title, String content) {
        super(frame, title, true);

        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        c.setPreferredSize(new Dimension(960, 600));

        JTextArea pane = new JTextArea();
        Font font = new Font("monospaced", Font.PLAIN, 14);
        pane.setFont(font);
        pane.setWrapStyleWord(true);
        pane.setLineWrap(true);

        JScrollPane jsp = new JScrollPane(pane);
        c.add(jsp, BorderLayout.CENTER);

        initRowContent(pane, content);

        this.pack();
    }

    private void initRowContent(JTextComponent pane, String content) {
        pane.setText(content);
    }
}