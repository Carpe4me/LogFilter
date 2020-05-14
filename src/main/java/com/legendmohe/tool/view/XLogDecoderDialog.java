package com.legendmohe.tool.view;

import com.legendmohe.tool.util.T;
import com.legendmohe.tool.util.Utils;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * 进行xlog解密
 */
public class XLogDecoderDialog {
    private JDialog mDialog;
    private JLabel dragTargetLabel;
    private Listener mListener;
    private JTextField inputTextField;
    private State mState;
    private Thread decodeThread;

    public XLogDecoderDialog(String decoderPath, Listener listener) {
        mListener = listener;
        createAndDisplayOptionPane(decoderPath);
        setState(State.IDLE);
    }

    private void createAndDisplayOptionPane(String decoderPath) {
        JLabel hintLabel = new JLabel("decoder patch");
        inputTextField = new JTextField(decoderPath);
        JPanel pathInputPanel = new JPanel(new BorderLayout(5, 5));
        pathInputPanel.add(inputTextField, BorderLayout.CENTER);
        pathInputPanel.add(hintLabel, BorderLayout.WEST);

        dragTargetLabel = new JLabel();
        dragTargetLabel.setHorizontalAlignment(JLabel.CENTER);
        dragTargetLabel.setVerticalAlignment(JLabel.CENTER);
        dragTargetLabel.setPreferredSize(new Dimension(500, 300));

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(pathInputPanel, BorderLayout.NORTH);
        panel.add(dragTargetLabel, BorderLayout.CENTER);

        JOptionPane optionPane = new JOptionPane(panel);
        optionPane.setOptions(new Object[0]);
        mDialog = optionPane.createDialog("xlog decoder");
        mDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (decodeThread != null) {
                    decodeThread.interrupt();
                    decodeThread = null;
                }
                if (mListener != null) {
                    mListener.onClose(inputTextField.getText());
                }
            }
        });

        setDnDListener();
    }

    void setDnDListener() {
        new DropTarget(dragTargetLabel, DnDConstants.ACTION_COPY_OR_MOVE,
                new DropTargetAdapter() {

                    public void drop(DropTargetDropEvent event) {
                        // 正在decode时不处理drop
                        if (mState == State.DECODING)
                            return;

                        try {
                            event.acceptDrop(DnDConstants.ACTION_COPY);
                            Transferable t = event.getTransferable();
                            List<?> list = (List<?>) (t
                                    .getTransferData(DataFlavor.javaFileListFlavor));
                            Iterator<?> i = list.iterator();
                            List<File> files = new ArrayList<>();
                            while (i.hasNext()) {
                                File file = (File) i.next();
                                if (file.isFile()) {
                                    files.add(file);
                                }
                            }
                            if (files.size() > 0) {
                                parseXLogFile(files.toArray(new File[0]));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void parseXLogFile(File[] files) {
        if (inputTextField.getText() == null || inputTextField.getText().length() <= 0) {
            return;
        }

        setState(State.DECODING);
        decodeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                T.d("decode start.");
                try {
                    File[] mResultFiles = new File[files.length];
                    for (int i = 0, filesLength = files.length; i < filesLength; i++) {
                        File file = files[i];
                        List<String> result = Utils.processCmd(
                                Utils.getRunnableCmdByPlatform(
                                        inputTextField.getText() + " " + file.getAbsolutePath()
                                )
                        );
                        T.d("decode end:" + file.getAbsolutePath() + "\n result=" + Utils.joinString(result, true));
                        // WARNING ! 这里要跟脚本的输出一致
                        mResultFiles[i] = new File(file.getAbsolutePath() + ".log");
                    }
                    if (mListener != null) {
                        mListener.onLogDecoded(mResultFiles);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    setState(State.ERROR, ex.getMessage());
                }
                if (mState != State.ERROR) {
                    setState(State.IDLE);
                }
            }
        });
        decodeThread.start();
    }

    private void setState(State state) {
        setState(state, null);
    }

    private void setState(State state, Object param) {
        mState = state;
        switch (state) {
            case IDLE:
                dragTargetLabel.setText("drop here to load xlog files");
                break;
            case DECODING:
                dragTargetLabel.setText("decoding...");
                break;
            case ERROR:
                if (param != null) {
                    dragTargetLabel.setText(param.toString());
                }
                break;
        }
    }

    //////////////////////////////////////////////////////////////////////

    public void show() {
        mDialog.setVisible(true);
    }

    private void hide() {
        mDialog.setVisible(false);
    }

    //////////////////////////////////////////////////////////////////////

    public interface Listener {
        void onClose(String decoderPath);
        void onLogDecoded(File[] decodedFiles);
    }

    private enum State {
        IDLE,
        DECODING,
        ERROR,
    }
}
