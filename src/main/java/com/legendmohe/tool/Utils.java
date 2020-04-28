package com.legendmohe.tool;

import com.legendmohe.tool.thirdparty.util.OsCheck;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

/**
 * Created by xinyu.he on 2016/1/7.
 */
public class Utils {
    public static void openWebPage(String urlString) {
        try {
            openWebPage(new URL(urlString).toURI());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void openWebPage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void openWebPage(URL url) {
        try {
            openWebPage(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void openInExplorer(File file) {
        try {
            if (!file.isDirectory()) {
                file = file.getParentFile();
            }
            Desktop.getDesktop().open(file);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void runCmd(String[] cmd) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(true);
        builder.start();
    }

    public static void sendContentToClipboard(String content) {
        StringSelection stsel = new StringSelection(content);
        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        systemClipboard.setContents(stsel, stsel);
    }

    public static int sift4(CharSequence s1, CharSequence s2, int maxOffset) {
        final int l1 = s1.length(), l2 = s2.length();
        if (s1 == null || l1 == 0)
            return ((s2 == null || l2 == 0) ? 0 : l2);
        if (s2 == null || l2 == 0)
            return (l1);

        int c1 = 0;
        int c2 = 0;
        int lcss = 0;
        int local_cs = 0;
        while ((c1 < l1) && (c2 < l2)) {
            if (s1.charAt(c1) == s2.charAt(c2))
                local_cs++;
            else {
                lcss += local_cs;
                local_cs = 0;
                if (c1 != c2) {
                    c1 = c2 = Math.max(c1, c2);
                }
                for (int i = 1; i < maxOffset && (c1 + i < l1 || c2 + i < l2); i++) {
                    if ((c1 + i < l1) && c2 < l2 && (s1.charAt(c1 + i) == s2.charAt(c2))) {
                        c1 += i;
                        local_cs++;
                        break;
                    }
                    if ((c2 + i < l2) && c1 < l1 && (s1.charAt(c1) == s2.charAt(c2 + i))) {
                        c2 += i;
                        local_cs++;
                        break;
                    }
                }
            }
            c1++;
            c2++;
        }
        lcss += local_cs;
        return Math.max(l1, l2) - lcss;
    }

    ///////////////////////////////////file///////////////////////////////////

    public static List<File> listFiles(File folder) {
        List<File> files = new ArrayList<>();
        if (folder != null && folder.exists() && folder.isDirectory()) {
            File[] listFiles = folder.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isFile();
                }
            });
            if (listFiles != null) {
                files = Arrays.asList(listFiles);
            }
        }
        return files;
    }

    public static String fileContent2String(File src) throws IOException {
        if (src == null || !src.exists() || !src.isFile()) {
            return "";
        }
        StringBuffer fileData = new StringBuffer();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(src), StandardCharsets.UTF_8));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }

    public static String escapeHTML(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    ///////////////////////////////////string///////////////////////////////////

    public static boolean isEmpty(String src) {
        return src == null || src.length() <= 0;
    }

    ///////////////////////////////////system///////////////////////////////////

    private static String OS;
    private static String JAVA_VERSION;

    static {
        OS = System.getProperty("os.name");
        JAVA_VERSION = System.getProperty("java.specification.version");
    }

    public static String getOsName() {
        return OS;
    }

    public static String getJavaVersion() {
        return JAVA_VERSION;
    }

    public static boolean isWindows() {
        return getOsName().startsWith("Windows");
    }

    ///////////////////////////////////undo///////////////////////////////////

    public final static String UNDO_ACTION = "Undo";

    public final static String REDO_ACTION = "Redo";

    public static void makeUndoable(JTextComponent pTextComponent) {
        final UndoManager undoMgr = new UndoManager();

        // Add listener for undoable events
        pTextComponent.getDocument().addUndoableEditListener(new UndoableEditListener() {
            public void undoableEditHappened(UndoableEditEvent evt) {
                undoMgr.addEdit(evt.getEdit());
            }
        });

        // Add undo/redo actions
        pTextComponent.getActionMap().put(UNDO_ACTION, new AbstractAction(UNDO_ACTION) {
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undoMgr.canUndo()) {
                        undoMgr.undo();
                    }
                } catch (CannotUndoException e) {
                    e.printStackTrace();
                }
            }
        });
        pTextComponent.getActionMap().put(REDO_ACTION, new AbstractAction(REDO_ACTION) {
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undoMgr.canRedo()) {
                        undoMgr.redo();
                    }
                } catch (CannotRedoException e) {
                    e.printStackTrace();
                }
            }
        });

        if (OsCheck.getOperatingSystemType() == OsCheck.OSType.MacOS) {
            // Create keyboard accelerators for undo/redo actions (Ctrl+Z/Ctrl+Y)
            pTextComponent.getInputMap().put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.META_DOWN_MASK), UNDO_ACTION);
            pTextComponent.getInputMap().put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), REDO_ACTION);
        } else {
            // Create keyboard accelerators for undo/redo actions (Ctrl+Z/Ctrl+Y)
            pTextComponent.getInputMap().put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), UNDO_ACTION);
            pTextComponent.getInputMap().put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), REDO_ACTION);
        }
    }

    public static void showMsgDialog(Component component, String s) {
        JOptionPane.showMessageDialog(component, s);
    }

    public static List<Component> getAllComponents(final Container c) {
        Component[] comps = c.getComponents();
        List<Component> compList = new ArrayList<Component>();
        for (Component comp : comps) {
            compList.add(comp);
            if (comp instanceof Container)
                compList.addAll(getAllComponents((Container) comp));
        }
        return compList;
    }

    ///////////////////////////////////base///////////////////////////////////

    public static List<Integer> toIntegerList(int[] src) {
        List<Integer> intList = new ArrayList<>();
        if (src != null) {
            for (int i : src) {
                intList.add(i);
            }
        }
        return intList;
    }

    ////////////////////////////////icon//////////////////////////////////////

    public static ImageIcon createImageIcon(Color color, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(color);
        graphics.fillRect(0, 0, width, height);
        return new ImageIcon(image);
    }

    ////////////////////////////////key//////////////////////////////////////

    public static int getControlKeyMask() {
        if (OsCheck.getOperatingSystemType() == OsCheck.OSType.MacOS) {
            return InputEvent.META_MASK;
        } else {
            return InputEvent.CTRL_MASK;
        }
    }

    public static boolean isControlKeyPressed(KeyEvent e) {
        int keyMask = getControlKeyMask();
        return (e.getModifiers() & keyMask) == keyMask;
    }

    public static boolean isAltKeyPressed(KeyEvent e) {
        int keyMask = InputEvent.ALT_MASK;
        return (e.getModifiers() & keyMask) == keyMask;
    }

    public static List<String> processCmd(String[] cmd) throws IOException {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true);
            Process oProcess = processBuilder.start();

            BufferedReader stdOut = new BufferedReader(new InputStreamReader(
                    oProcess.getInputStream()));

            String s;
            ArrayList<String> sb = new ArrayList<>();
            while ((s = stdOut.readLine()) != null) {
                if (s.trim().length() != 0)
                    sb.add(s);
            }
            return sb;
        } catch (IOException e) {
            T.e("e = " + e);
            throw e;
        }
    }

    public static String joinString(List<String> result, boolean appendNewLine) {
        if (result == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (String s : result) {
            sb.append(s);
            if (appendNewLine)
                sb.append("\n");
        }
        return sb.toString();
    }

    public static String[] getRunnableCmdByPlatform(String customCmd) {
        String[] cmd;
        if (isWindows()) {
            cmd = new String[]{"cmd.exe", "/C", customCmd};
        } else {
            cmd = new String[]{"/bin/bash", "-l", "-c", customCmd};
        }
        return cmd;
    }

    ///////////////////////////////////regex///////////////////////////////////

    private static Map<String, Pattern> sPatternCache = new HashMap<>();

    public static Map<String, Pattern> getsPatternCache() {
        return sPatternCache;
    }

    public static Pattern findPatternOrCreate(String strFind) {
        return sPatternCache.computeIfAbsent(strFind, s -> Pattern.compile("(" + s + ")", Pattern.CASE_INSENSITIVE));
    }
}
