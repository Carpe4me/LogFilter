package com.legendmohe.tool.view;

import com.legendmohe.tool.util.Utils;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * 进行一些文本转换
 */
public class TextConverterDialog {
    private JList<ResultLine> mListView;
    private JDialog mDialog;
    private ResultLine mLastOutput;
    private List<ResultLine> mResultList = new ArrayList<>();

    public TextConverterDialog() {
        createAndDisplayOptionPane();
    }

    private void createAndDisplayOptionPane() {
        // int 2 long
        JTextField inputTextField = new JTextField();
        JButton convButton = new JButton("int2long");
        convButton.setMargin(new Insets(0, 0, 0, 0));
        convButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String intString = inputTextField.getText();
                if (intString != null && intString.length() > 0) {
                    String longString = convertIntStringToLongString(intString);
                    ResultLine resultLine = new ResultLine("int2long", intString, longString);
                    if (!resultLine.equals(mLastOutput)) {
                        mResultList.add(resultLine);
                        mListView.setListData(mResultList.toArray(new ResultLine[0]));
                        mLastOutput = resultLine;
                    }
                }
            }
        });

        JPanel int2LongPanel = new JPanel(new BorderLayout(5, 5));
        int2LongPanel.add(inputTextField, BorderLayout.CENTER);
        int2LongPanel.add(convButton, BorderLayout.EAST);
        //

        JPanel funcPanel = new JPanel();
        funcPanel.setLayout(new BoxLayout(funcPanel, BoxLayout.Y_AXIS));
        funcPanel.add(int2LongPanel); // 加一个就加一行这个
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(funcPanel, BorderLayout.NORTH);

        mListView = buildListView();
        panel.add(new JScrollPane(mListView), BorderLayout.CENTER);

        JOptionPane optionPane = new JOptionPane(panel);

        JButton clearButton = new JButton("clear");
        clearButton.addActionListener(actionEvent -> {
            mLastOutput = null;
            mResultList.clear();
            mListView.setListData(mResultList.toArray(new ResultLine[0]));
        });
        optionPane.setOptions(new Object[]{
                clearButton
        });
        mDialog = optionPane.createDialog("text converter");
    }

    private JList<ResultLine> buildListView() {
        JList<ResultLine> jList = new JList<>();
        JMenuItem copyRowToClipboard = new JMenuItem(new AbstractAction("copy result to clipboard") {
            @Override
            public void actionPerformed(ActionEvent e) {
                List items = mListView.getSelectedValuesList();
                StringBuilder sb = new StringBuilder();
                for (Object item : items) {
                    sb.append(((ResultLine) item).getPrintableOutput()).append("\n");
                }
                if (sb.length() > 0) {
                    sb.delete(sb.length() - 1, sb.length());
                    Utils.sendContentToClipboard(sb.toString());
                }
            }
        });
        JPopupMenu menuPopup = new JPopupMenu();
        menuPopup.add(copyRowToClipboard);
        jList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                int row = jList.locationToIndex(p);
                if (row < 0 || row > mResultList.size() - 1) {
                    return;
                }
                if (SwingUtilities.isRightMouseButton(e)) {
                    menuPopup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        return jList;
    }

    private String convertIntStringToLongString(String intString) {
        if (intString == null) {
            return null;
        }
        try {
            int intValue = Integer.parseInt(intString);
            return String.valueOf(intValue & 0xffffffffL);
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    public void show() {
        mDialog.setVisible(true);
    }

    private void hide() {
        mDialog.setVisible(false);
    }

    ///////////////////////////////////interface///////////////////////////////////

    private static class ResultLine {
        String title;
        String input;
        String output;

        ResultLine(String title, String input, String output) {
            this.title = title;
            this.input = input;
            this.output = output;
        }

        @Override
        public String toString() {
            return title + ": " + input + "->" + output;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ResultLine)) return false;
            ResultLine that = (ResultLine) o;
            return Objects.equals(title, that.title) &&
                    Objects.equals(input, that.input) &&
                    Objects.equals(output, that.output);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, input, output);
        }

        public String getPrintableOutput() {
            return output;
        }
    }
}
