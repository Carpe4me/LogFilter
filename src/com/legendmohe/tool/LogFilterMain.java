package com.legendmohe.tool;

import com.legendmohe.tool.annotation.CheckBoxSaveState;
import com.legendmohe.tool.annotation.FieldSaveState;
import com.legendmohe.tool.annotation.StateSaver;
import com.legendmohe.tool.annotation.TextFieldSaveState;
import com.legendmohe.tool.diff.DiffService;
import com.legendmohe.tool.view.DumpsysViewDialog;
import com.legendmohe.tool.view.PackageViewDialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

public class LogFilterMain extends JFrame implements INotiEvent, BaseLogTable.BaseLogTableListener {
    private static final long serialVersionUID = 1L;

    static final String LOGFILTER = "LogFilter";
    static final String VERSION = "Version 1.8.1";
    static final String OUTPUT_LOG_DIR = "log";
    static final String CONFIG_BASE_DIR = "conf";
    final String COMBO_ANDROID = "Android          ";
    final String COMBO_IOS = "ios";
    final String COMBO_CUSTOM_COMMAND = "custom command";
    final String IOS_DEFAULT_CMD = "adb logcat -v time ";
    final String IOS_SELECTED_CMD_FIRST = "adb -s ";
    final String IOS_SELECTED_CMD_LAST = " logcat -v time ";
    final String ANDROID_DEFAULT_CMD = "logcat -v time";
    final String ANDROID_THREAD_CMD = "logcat -v threadtime";
    final String ANDROID_EVENT_CMD = "logcat -b events -v time";
    final String ANDROID_RADIO_CMD = "logcat -b radio -v time";
    final String ANDROID_CUSTOM_CMD = "shell cat /proc/kmsg";
    //    final String ANDROID_CUSTOM_CMD = "logcat -v time -f /dev/kmsg | cat /proc/kmsg";
    final String ANDROID_DEFAULT_CMD_FIRST = "adb ";
    final String ANDROID_SELECTED_CMD_FIRST = "adb -s ";
    // final String ANDROID_SELECTED_CMD_LAST = " logcat -v time ";
    final String[] DEVICES_CMD = {"adb devices -l", "", ""};

    static final int DEFAULT_WIDTH = 1200;
    static final int DEFAULT_HEIGHT = 720;
    static final int MIN_WIDTH = 1100;
    static final int MIN_HEIGHT = 500;

    static final int DEVICES_ANDROID = 0;
    static final int DEVICES_IOS = 1;
    static final int DEVICES_CUSTOM = 2;

    static final int STATUS_CHANGE = 1;
    static final int STATUS_PARSING = 2;
    static final int STATUS_READY = 4;

    private static final String DIFF_PROGRAM_PATH = "C:\\Program Files\\KDiff3\\kdiff3.exe";
    private static final String CALC_PROGRAM_PATH = "calc";

    // 对应menu的index，必须从0开始
    public static final int PARSER_TYPE_LOGCAT = 0;
    public static final int PARSER_TYPE_BIGO_DEV_LOG = 1;
    public static final int PARSER_TYPE_BIGO_XLOG = 2;

    JLabel m_tfStatus;
    IndicatorPanel m_ipIndicator;
    ArrayList<LogInfo> m_arLogInfoAll;
    ArrayList<LogInfo> m_arLogInfoFiltered;
    HashMap<Integer, Integer> m_hmMarkedInfoAll;
    HashMap<Integer, Integer> m_hmMarkedInfoFiltered;
    ConcurrentHashMap<Integer, Integer> m_hmErrorAll;
    ConcurrentHashMap<Integer, Integer> m_hmErrorFiltered;
    ILogParser m_iLogParser;
    LogTable m_tbLogTable;
    JScrollPane m_logScrollVPane;
    LogFilterTableModel m_tmLogTableModel;
    boolean m_bUserFilter;

    // Word Filter, tag filter
    JTextField m_tfSearch;
    @TextFieldSaveState
    JTextField m_tfHighlight;

    @TextFieldSaveState
    JTextField m_tfFindWord;
    @TextFieldSaveState
    JTextField m_tfRemoveWord;
    @TextFieldSaveState
    JTextField m_tfShowTag;
    @TextFieldSaveState
    JTextField m_tfRemoveTag;
    @TextFieldSaveState
    JTextField m_tfShowPid;
    @TextFieldSaveState
    JTextField m_tfShowTid;
    @TextFieldSaveState
    JTextField m_tfBookmarkTag;

    // Device
    JButton m_btnDevice;
    JList<TargetDevice> m_lDeviceList;
    JComboBox<String> m_comboDeviceCmd;
    JComboBox<String> m_comboCmd;
    JButton m_btnSetFont;

    // Log filter enable/disable
    @CheckBoxSaveState
    JCheckBox m_chkEnableFind;
    @CheckBoxSaveState
    JCheckBox m_chkEnableRemove;
    @CheckBoxSaveState
    JCheckBox m_chkEnableShowTag;
    @CheckBoxSaveState
    JCheckBox m_chkEnableRemoveTag;
    @CheckBoxSaveState
    JCheckBox m_chkEnableShowPid;
    @CheckBoxSaveState
    JCheckBox m_chkEnableShowTid;
    @CheckBoxSaveState
    JCheckBox m_chkEnableHighlight;
    @CheckBoxSaveState
    JCheckBox m_chkEnableBookmarkTag;

    private JCheckBox m_chkEnableTimeTag;

    // Log filter
    @CheckBoxSaveState
    JCheckBox m_chkVerbose;
    @CheckBoxSaveState
    JCheckBox m_chkDebug;
    @CheckBoxSaveState
    JCheckBox m_chkInfo;
    @CheckBoxSaveState
    JCheckBox m_chkWarn;
    @CheckBoxSaveState
    JCheckBox m_chkError;
    @CheckBoxSaveState
    JCheckBox m_chkFatal;

    // Show column
    @CheckBoxSaveState
    JCheckBox m_chkClmBookmark;
    @CheckBoxSaveState
    JCheckBox m_chkClmLine;
    @CheckBoxSaveState
    JCheckBox m_chkClmDate;
    @CheckBoxSaveState
    JCheckBox m_chkClmTime;
    @CheckBoxSaveState
    JCheckBox m_chkClmLogLV;
    @CheckBoxSaveState
    JCheckBox m_chkClmPid;
    @CheckBoxSaveState
    JCheckBox m_chkClmThread;
    @CheckBoxSaveState
    JCheckBox m_chkClmTag;
    @CheckBoxSaveState
    JCheckBox m_chkClmMessage;

    @TextFieldSaveState
    JTextField m_tfFontSize;
    // JTextField m_tfProcessCmd;

    JComboBox<String> m_comboEncode;
    //    JComboBox m_jcFontType;
    JButton m_btnRun;
    JButton m_btnClear;
    JToggleButton m_tbtnPause;
    JButton m_btnStop;

    String m_strLogFileName;
    TargetDevice m_selectedDevice;
    // String m_strProcessCmd;
    Process m_Process;
    Thread m_thProcess;
    Thread m_thWatchFile;
    Thread m_thFilterParse;
    boolean m_bPauseADB;

    Object FILE_LOCK;
    Object FILTER_LOCK;
    volatile int m_nChangedFilter;
    int m_nFilterLogLV;
    @FieldSaveState
    int m_nWinWidth = DEFAULT_WIDTH;
    @FieldSaveState
    int m_nWinHeight = DEFAULT_HEIGHT;
    int m_nLastWidth;
    int m_nLastHeight;
    @FieldSaveState
    int m_nWindState;
    static RecentFileMenu m_recentMenu;

    @FieldSaveState
    int m_parserType = PARSER_TYPE_LOGCAT;

    @FieldSaveState
    int[] m_colWidths = LogFilterTableModel.DEFULT_WIDTH;

    @FieldSaveState
    private String m_strLastDir;

    private static JMenuItem sDisconnectDiffMenuItem;
    private static JMenuItem sConnectDiffMenuItem;

    public DiffService mDiffService;
    private JLabel m_tfDiffPort;
    private JLabel m_tfDiffState;
    private JCheckBox mSyncScrollCheckBox;
    private JCheckBox mSyncSelectedCheckBox;
    private boolean mSyncScrollEnable;
    private boolean mSyncScrollSelected;

    private final StateSaver mStateSaver;
    private JTextField m_tfFromTimeTag;
    private JTextField m_tfToTimeTag;
    private JSplitPane mSplitPane;
    @FieldSaveState
    private int mSplitPaneDividerLocation = -1;

    private SubLogTable m_tSublogTable;
    private LogFilterTableModel m_tSubLogTableModel;
    private JScrollPane m_subLogScrollVPane;
    ArrayList<LogInfo> m_arSubLogInfoAll;

    public static void main(final String args[]) {
        final LogFilterMain mainFrame = new LogFilterMain();
        mainFrame.setTitle(LOGFILTER + " " + VERSION);
        mainFrame.addWindowStateListener(new WindowStateListener() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                mainFrame.m_nWindState = e.getNewState();
            }
        });

        JMenuBar menubar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem fileOpen = new JMenuItem("Open");
        fileOpen.setMnemonic(KeyEvent.VK_O);
        fileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                ActionEvent.ALT_MASK));
        fileOpen.setToolTipText("Open log file");
        fileOpen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainFrame.openFileBrowserToLoad(FileType.LOGFILE);
            }
        });

        JMenu modeMenu = new JMenu("Mode");

        JMenuItem modeOpen = new JMenuItem("Open Mode File");
        modeOpen.setToolTipText("Open .mode file");
        modeOpen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainFrame.openFileBrowserToLoad(FileType.MODEFILE);
            }
        });
        JMenuItem modeSave = new JMenuItem("Save Mode File");
        modeSave.setToolTipText("Save .mode file");
        modeSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainFrame.openFileBrowserToSave(FileType.MODEFILE);
            }
        });

        modeMenu.add(modeOpen);
        modeMenu.add(modeSave);

        m_recentMenu = new RecentFileMenu("RecentFile", 10) {
            public void onSelectFile(String filePath) {
                File recentFile = new File(filePath);
                mainFrame.parseFile(new File(filePath));
                m_recentMenu.addEntry(recentFile.getAbsolutePath());
            }
        };

        fileMenu.add(fileOpen);
        fileMenu.add(modeMenu);
        fileMenu.add(m_recentMenu);

        JMenu netMenu = new JMenu("Net");
        JMenu diffMenu = new JMenu("Diff Service");

        sDisconnectDiffMenuItem = new JMenuItem("disconnect");
        sDisconnectDiffMenuItem.setEnabled(false);
        sConnectDiffMenuItem = new JMenuItem("connect to diff server");

        sDisconnectDiffMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainFrame.mDiffService.disconnectDiffClient();
                sConnectDiffMenuItem.setEnabled(true);
                sDisconnectDiffMenuItem.setEnabled(false);
            }
        });
        sConnectDiffMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String serverPort = JOptionPane.showInputDialog(
                        mainFrame,
                        "Enter Server Port",
                        "",
                        JOptionPane.QUESTION_MESSAGE
                );
                if (serverPort != null && serverPort.length() != 0) {
                    if (mainFrame.mDiffService.setupDiffClient(serverPort)) {
                        sConnectDiffMenuItem.setEnabled(false);
                        sDisconnectDiffMenuItem.setEnabled(true);
                    }
                }
            }
        });

        diffMenu.add(sConnectDiffMenuItem);
        diffMenu.add(sDisconnectDiffMenuItem);
        netMenu.add(diffMenu);

        JMenu parserMenu = new JMenu("Parser");
        parserMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                JMenuItem menuItem = parserMenu.getItem(mainFrame.m_parserType);
                menuItem.setSelected(true);
            }

            @Override
            public void menuDeselected(MenuEvent e) {

            }

            @Override
            public void menuCanceled(MenuEvent e) {

            }
        });

        JRadioButtonMenuItem logcatParserMenu = new JRadioButtonMenuItem("Logcat Parser", mainFrame.m_parserType == PARSER_TYPE_LOGCAT);
        logcatParserMenu.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    mainFrame.m_iLogParser = new LogCatParser();
                    mainFrame.m_parserType = PARSER_TYPE_LOGCAT;
                }
            }
        });
        parserMenu.add(logcatParserMenu);

        JRadioButtonMenuItem bigoParserMenu = new JRadioButtonMenuItem("BigoDevLog Parser", mainFrame.m_parserType == PARSER_TYPE_BIGO_DEV_LOG);
        bigoParserMenu.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    mainFrame.m_iLogParser = new BigoDevLogParser();
                    mainFrame.m_parserType = PARSER_TYPE_BIGO_DEV_LOG;
                }
            }
        });
        parserMenu.add(bigoParserMenu);

        JRadioButtonMenuItem bigoXLogParserMenu = new JRadioButtonMenuItem("BigoXLog Parser", mainFrame.m_parserType == PARSER_TYPE_BIGO_XLOG);
        bigoXLogParserMenu.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    mainFrame.m_iLogParser = new BigoDevLogParser();
                    mainFrame.m_parserType = PARSER_TYPE_BIGO_XLOG;
                }
            }
        });
        parserMenu.add(bigoXLogParserMenu);

        // 就这样放进去就可以了。。。
        ButtonGroup parserBG = new ButtonGroup();
        parserBG.add(logcatParserMenu);
        parserBG.add(bigoParserMenu);
        parserBG.add(bigoXLogParserMenu);

        JMenu viewMenu = initAndGetViewMenu(mainFrame);

        menubar.add(fileMenu);
        menubar.add(netMenu);
        menubar.add(viewMenu);
        menubar.add(parserMenu);
        mainFrame.setJMenuBar(menubar);

        mainFrame.pack();
        mainFrame.restoreSplitPane();

        if (args != null && args.length > 0) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    File argFile = new File(args[0]);
                    mainFrame.parseFile(argFile.getAbsoluteFile());
                    m_recentMenu.addEntry(argFile.getAbsolutePath());
                }
            });
        }
    }

    private static JMenu initAndGetViewMenu(final LogFilterMain mainFrame) {
        JMenu viewMenu = new JMenu("View");
        JMenuItem packagesMenuItem = new JMenuItem("Show Running Packages");
        packagesMenuItem.setToolTipText("show running packages on current android device");
        packagesMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainFrame.openPackagesView();
            }
        });
        viewMenu.add(packagesMenuItem);

        JMenuItem ActivitiesMenuItem = new JMenuItem("Show Running Activities");
        ActivitiesMenuItem.setToolTipText("show running activities on current android device");
        ActivitiesMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainFrame.openDumpsysView("dumpsys activity activities");
            }
        });
        viewMenu.add(ActivitiesMenuItem);

        JMenuItem pendingAlarmMenuItem = new JMenuItem("Show Pending Alarms");
        pendingAlarmMenuItem.setToolTipText("show pending alarms on current android device");
        pendingAlarmMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainFrame.openDumpsysView("dumpsys alarm");
            }
        });
        viewMenu.add(pendingAlarmMenuItem);

        JMenuItem pendingIntentMenuItem = new JMenuItem("Show Pending Intents");
        pendingIntentMenuItem.setToolTipText("show pending intents on current android device");
        pendingIntentMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainFrame.openDumpsysView("dumpsys activity intents");
            }
        });
        viewMenu.add(pendingIntentMenuItem);

        JMenuItem meminfoMenuItem = new JMenuItem("Show Memory Info");
        meminfoMenuItem.setToolTipText("show memory info on current android device");
        meminfoMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainFrame.openDumpsysView("dumpsys meminfo");
            }
        });
        viewMenu.add(meminfoMenuItem);

        JMenuItem cpuinfoMenuItem = new JMenuItem("Show CPU Info");
        cpuinfoMenuItem.setToolTipText("show CPU info on current android device");
        cpuinfoMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainFrame.openDumpsysView("dumpsys cpuinfo");
            }
        });
        viewMenu.add(cpuinfoMenuItem);

        JMenu customMenu = new JMenu("Custom Info");
        loadCustomMenu(mainFrame, customMenu);
        viewMenu.add(customMenu);
        return viewMenu;
    }

    private static void loadCustomMenu(final LogFilterMain mainFrame, JMenu customMenu) {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(INI_FILE_DUMPSYS));
        } catch (FileNotFoundException e) {
            T.d(INI_FILE_DUMPSYS + " not exist!");
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Enumeration names = p.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            final String cmd = p.getProperty(name);
            if (cmd != null) {
                JMenuItem newMenuItem = new JMenuItem(name);
                newMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        mainFrame.openDumpsysView(cmd);
                    }
                });
                customMenu.add(newMenuItem);
            }
        }
    }

    private void restoreSplitPane() {
        mSplitPane.setResizeWeight(1.0);
        mSplitPane.setOneTouchExpandable(true);
        mSplitPane.setDividerLocation(mSplitPaneDividerLocation);
    }

    String makeFilename() {
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return OUTPUT_LOG_DIR + File.separator + "LogFilter_" + format.format(now) + ".txt";
    }

    void exit() {
        if (m_Process != null)
            m_Process.destroy();
        if (m_thProcess != null)
            m_thProcess.interrupt();
        if (m_thWatchFile != null)
            m_thWatchFile.interrupt();
        if (m_thFilterParse != null)
            m_thFilterParse.interrupt();

        saveColor();
        m_nWinWidth = getSize().width;
        m_nWinHeight = getSize().height;
        mSplitPaneDividerLocation = mSplitPane.getDividerLocation();
        mStateSaver.save();
        System.exit(0);
    }

    /**
     * @throws HeadlessException
     */
    public LogFilterMain() {
        super();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
        initValue();

        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());

        pane.add(getOptionPanel(), BorderLayout.NORTH);
        pane.add(getStatusPanel(), BorderLayout.SOUTH);
        pane.add(getLogPanel(), BorderLayout.CENTER);

        setDnDListener();
        addChangeListener();
        startFilterParse();

        setVisible(true);
        addDesc();

        // register state saver
        mStateSaver = new StateSaver(this, INI_FILE_STATE);
        mStateSaver.load();

        loadUI();
        loadColor();
        loadCmd();
        loadParser();
        initDiffService();

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(mKeyEventDispatcher);
    }

    private void loadUI() {

        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setExtendedState(m_nWindState);
        setPreferredSize(new Dimension(m_nWinWidth, m_nWinHeight));

        loadTableColumnState();

        getLogTable().setFontSize(Integer.parseInt(m_tfFontSize
                .getText()));
        getSubTable().setFontSize(Integer.parseInt(m_tfFontSize
                .getText()));

        updateTable(-1, false);

    }

    final static String INI_FILE_CMD = CONFIG_BASE_DIR + File.separator + "LogFilterCmd.ini";
    final static String INI_FILE_COLOR = CONFIG_BASE_DIR + File.separator + "LogFilterColor.ini";
    final static String INI_FILE_STATE = CONFIG_BASE_DIR + File.separator + "LogFilterState.ser";
    final static String INI_FILE_DUMPSYS = CONFIG_BASE_DIR + File.separator + "LogFilterDumpsysCmd.ini";
    final String INI_CMD_COUNT = "CMD_COUNT";
    final String INI_CMD = "CMD_";
    final String INI_COLOR_0 = "INI_COLOR_0";
    final String INI_COLOR_1 = "INI_COLOR_1";
    final String INI_COLOR_2 = "INI_COLOR_2";
    final String INI_COLOR_3 = "INI_COLOR_3(E)";
    final String INI_COLOR_4 = "INI_COLOR_4(W)";
    final String INI_COLOR_5 = "INI_COLOR_5";
    final String INI_COLOR_6 = "INI_COLOR_6(I)";
    final String INI_COLOR_7 = "INI_COLOR_7(D)";
    final String INI_COLOR_8 = "INI_COLOR_8(F)";
    final String INI_HIGILIGHT_COUNT = "INI_HIGILIGHT_COUNT";
    final String INI_HIGILIGHT_ = "INI_HIGILIGHT_";

    void loadCmd() {
        try {
            Properties p = new Properties();

            try {
                p.load(new FileInputStream(INI_FILE_CMD));
            } catch (FileNotFoundException e) {
                T.d(INI_FILE_CMD + " not exist!");
            }

            if (p.getProperty(INI_CMD_COUNT) == null) {
                p.setProperty(INI_CMD + "0", ANDROID_THREAD_CMD);
                p.setProperty(INI_CMD + "1", ANDROID_DEFAULT_CMD);
                p.setProperty(INI_CMD + "2", ANDROID_RADIO_CMD);
                p.setProperty(INI_CMD + "3", ANDROID_EVENT_CMD);
                p.setProperty(INI_CMD + "4", ANDROID_CUSTOM_CMD);
                p.setProperty(INI_CMD_COUNT, "5");
                p.store(new FileOutputStream(INI_FILE_CMD), null);
            }

            T.d("p.getProperty(INI_CMD_COUNT) = "
                    + p.getProperty(INI_CMD_COUNT));
            int nCount = Integer.parseInt(p.getProperty(INI_CMD_COUNT));
            T.d("nCount = " + nCount);
            for (int nIndex = 0; nIndex < nCount; nIndex++) {
                T.d("CMD = " + INI_CMD + nIndex);
                m_comboCmd.addItem(p.getProperty(INI_CMD + nIndex));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void loadParser() {
        switch (m_parserType) {
            case PARSER_TYPE_BIGO_DEV_LOG:
                m_iLogParser = new BigoDevLogParser();
                break;
            case PARSER_TYPE_BIGO_XLOG:
                m_iLogParser = new BigoXLogParser();
                break;
            case PARSER_TYPE_LOGCAT:
            default:
                m_iLogParser = new LogCatParser();
                break;
        }
    }

    void loadColor() {
        try {
            Properties p = new Properties();

            p.load(new FileInputStream(INI_FILE_COLOR));

            LogColor.COLOR_0 = Integer.parseInt(p.getProperty(INI_COLOR_0)
                    .replace("0x", ""), 16);
            LogColor.COLOR_1 = Integer.parseInt(p.getProperty(INI_COLOR_1)
                    .replace("0x", ""), 16);
            LogColor.COLOR_2 = Integer.parseInt(p.getProperty(INI_COLOR_2)
                    .replace("0x", ""), 16);
            LogColor.COLOR_ERROR = LogColor.COLOR_3 = Integer.parseInt(p
                    .getProperty(INI_COLOR_3).replace("0x", ""), 16);
            LogColor.COLOR_WARN = LogColor.COLOR_4 = Integer.parseInt(p
                    .getProperty(INI_COLOR_4).replace("0x", ""), 16);
            LogColor.COLOR_5 = Integer.parseInt(p.getProperty(INI_COLOR_5)
                    .replace("0x", ""), 16);
            LogColor.COLOR_INFO = LogColor.COLOR_6 = Integer.parseInt(p
                    .getProperty(INI_COLOR_6).replace("0x", ""), 16);
            LogColor.COLOR_DEBUG = LogColor.COLOR_7 = Integer.parseInt(p
                    .getProperty(INI_COLOR_7).replace("0x", ""), 16);
            LogColor.COLOR_FATAL = LogColor.COLOR_8 = Integer.parseInt(p
                    .getProperty(INI_COLOR_8).replace("0x", ""), 16);

            int nCount = Integer.parseInt(p.getProperty(INI_HIGILIGHT_COUNT,
                    "0"));
            if (nCount > 0) {
                LogColor.COLOR_HIGHLIGHT = new String[nCount];
                for (int nIndex = 0; nIndex < nCount; nIndex++)
                    LogColor.COLOR_HIGHLIGHT[nIndex] = p.getProperty(
                            INI_HIGILIGHT_ + nIndex).replace("0x", "");
            } else {
                LogColor.COLOR_HIGHLIGHT = new String[1];
                LogColor.COLOR_HIGHLIGHT[0] = "ffff";
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    void saveColor() {
        try {
            Properties p = new Properties();

            p.setProperty(INI_COLOR_0,
                    "0x" + Integer.toHexString(LogColor.COLOR_0).toUpperCase());
            p.setProperty(INI_COLOR_1,
                    "0x" + Integer.toHexString(LogColor.COLOR_1).toUpperCase());
            p.setProperty(INI_COLOR_2,
                    "0x" + Integer.toHexString(LogColor.COLOR_2).toUpperCase());
            p.setProperty(INI_COLOR_3,
                    "0x" + Integer.toHexString(LogColor.COLOR_3).toUpperCase());
            p.setProperty(INI_COLOR_4,
                    "0x" + Integer.toHexString(LogColor.COLOR_4).toUpperCase());
            p.setProperty(INI_COLOR_5,
                    "0x" + Integer.toHexString(LogColor.COLOR_5).toUpperCase());
            p.setProperty(INI_COLOR_6,
                    "0x" + Integer.toHexString(LogColor.COLOR_6).toUpperCase());
            p.setProperty(INI_COLOR_7,
                    "0x" + Integer.toHexString(LogColor.COLOR_7).toUpperCase());
            p.setProperty(INI_COLOR_8,
                    "0x" + Integer.toHexString(LogColor.COLOR_8).toUpperCase());

            if (LogColor.COLOR_HIGHLIGHT != null) {
                p.setProperty(INI_HIGILIGHT_COUNT, ""
                        + LogColor.COLOR_HIGHLIGHT.length);
                for (int nIndex = 0; nIndex < LogColor.COLOR_HIGHLIGHT.length; nIndex++)
                    p.setProperty(INI_HIGILIGHT_ + nIndex, "0x"
                            + LogColor.COLOR_HIGHLIGHT[nIndex].toUpperCase());
            }

            p.store(new FileOutputStream(INI_FILE_COLOR), "done.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void addDesc(String strMessage) {
        LogInfo logInfo = new LogInfo();
        logInfo.setLine(m_arLogInfoAll.size() + 1);
        logInfo.setMessage(strMessage);
        m_arLogInfoAll.add(logInfo);
    }

    void addDesc() {
        addDesc(VERSION);
        addDesc("");
        addDesc("Xinyu.he fork from https://github.com/iookill/LogFilter");
    }

    public void markLogInfo(int nIndex, int nLine, boolean bBookmark) {
        synchronized (FILTER_LOCK) {
            LogInfo logInfo = m_arLogInfoAll.get(nLine);
            logInfo.setMarked(bBookmark);
            m_arLogInfoAll.set(nLine, logInfo);

            if (logInfo.isMarked()) {
                m_arSubLogInfoAll.add(logInfo);
                m_hmMarkedInfoAll.put(nLine, nLine);
                if (m_bUserFilter)
                    m_hmMarkedInfoFiltered.put(nLine, nIndex);
            } else {
                m_arSubLogInfoAll.remove(logInfo);
                m_hmMarkedInfoAll.remove(nLine);
                if (m_bUserFilter)
                    m_hmMarkedInfoFiltered.remove(nLine);
            }
        }
        m_ipIndicator.repaint();

        m_arSubLogInfoAll.sort(new Comparator<LogInfo>() {
            @Override
            public int compare(LogInfo o1, LogInfo o2) {
                return o1.getLine() - o2.getLine();
            }
        });
        updateSubTable(-1);
    }

    void clearData() {
        m_arSubLogInfoAll.clear();
        m_arLogInfoAll.clear();
        m_arLogInfoFiltered.clear();
        m_hmMarkedInfoAll.clear();
        m_hmMarkedInfoFiltered.clear();
        m_hmErrorAll.clear();
        m_hmErrorFiltered.clear();
    }

    Component getBookmarkPanel() {
        JPanel jp = new JPanel();
        jp.setLayout(new BorderLayout());

        m_ipIndicator = new IndicatorPanel(this);
        m_ipIndicator.setData(m_arLogInfoAll, m_hmMarkedInfoAll, m_hmErrorAll);
        jp.add(m_ipIndicator, BorderLayout.CENTER);
        return jp;
    }

    Component getDevicePanel() {
        JPanel jpOptionDevice = new JPanel();
        jpOptionDevice.setBorder(BorderFactory
                .createTitledBorder("Device select"));
        jpOptionDevice.setLayout(new BorderLayout());
//         jpOptionDevice.setPreferredSize(new Dimension(200, 100));

        JPanel jpCmd = new JPanel();
        m_comboDeviceCmd = new JComboBox<String>();
        m_comboDeviceCmd.addItem(COMBO_ANDROID);
        // m_comboDeviceCmd.addItem(COMBO_IOS);
        // m_comboDeviceCmd.addItem(CUSTOM_COMMAND);
        m_comboDeviceCmd.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED)
                    return;

                DefaultListModel listModel = (DefaultListModel) m_lDeviceList
                        .getModel();
                listModel.clear();
                if (e.getItem().equals(COMBO_CUSTOM_COMMAND)) {
                    m_comboDeviceCmd.setEditable(true);
                } else {
                    m_comboDeviceCmd.setEditable(false);
                }
            }
        });

        m_btnDevice = new JButton("OK");
        m_btnDevice.setMargin(new Insets(0, 0, 0, 0));
        m_btnDevice.addActionListener(m_alButtonListener);

        jpCmd.add(m_comboDeviceCmd);
        jpCmd.add(m_btnDevice);

        jpOptionDevice.add(jpCmd, BorderLayout.NORTH);

        final DefaultListModel<TargetDevice> listModel = new DefaultListModel<>();
        m_lDeviceList = new JList<>(listModel);
        JScrollPane vbar = new JScrollPane(m_lDeviceList);
        vbar.setPreferredSize(new Dimension(100, 50));
        m_lDeviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_lDeviceList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                JList deviceList = (JList) e.getSource();
                m_selectedDevice = (TargetDevice) deviceList.getSelectedValue();
            }
        });
        jpOptionDevice.add(vbar, BorderLayout.CENTER);


        JPanel cmdPanel = new JPanel(new BorderLayout());
        JPanel funcPanel = new JPanel(new GridLayout(1, 4));
        funcPanel.setPreferredSize(new Dimension(100, 20));
        m_btnClear = new JButton("Clear");
        m_btnClear.setMargin(new Insets(0, 0, 0, 0));
        m_btnClear.setEnabled(false);
        m_btnRun = new JButton("Run");
        m_btnRun.setMargin(new Insets(0, 0, 0, 0));

        m_tbtnPause = new JToggleButton("Pause");
        m_tbtnPause.setMargin(new Insets(0, 0, 0, 0));
        m_tbtnPause.setEnabled(false);
        m_btnStop = new JButton("Stop");
        m_btnStop.setMargin(new Insets(0, 0, 0, 0));
        m_btnStop.setEnabled(false);
        m_btnRun.addActionListener(m_alButtonListener);
        m_btnStop.addActionListener(m_alButtonListener);
        m_btnClear.addActionListener(m_alButtonListener);
        m_tbtnPause.addActionListener(m_alButtonListener);

        funcPanel.add(m_btnClear);
        funcPanel.add(m_btnRun);
        funcPanel.add(m_btnStop);
        funcPanel.add(m_tbtnPause);
        cmdPanel.add(funcPanel, BorderLayout.SOUTH);

        JPanel adbPanel = new JPanel();
        JLabel jlProcessCmd = new JLabel("Cmd : ");
        m_comboCmd = new JComboBox<>();
        m_comboCmd.setPreferredSize(new Dimension(180, 25));
        adbPanel.add(jlProcessCmd);
        adbPanel.add(m_comboCmd);
        cmdPanel.add(adbPanel);

        jpOptionDevice.add(cmdPanel, BorderLayout.SOUTH);
        return jpOptionDevice;
    }

    void addLogInfo(LogInfo logInfo) {
        synchronized (FILTER_LOCK) {
            m_arLogInfoAll.add(logInfo);
            // addTagList(logInfo.m_strTag);
            if (logInfo.getLogLV().equals("E")
                    || logInfo.getLogLV().equals("ERROR"))
                m_hmErrorAll.put(logInfo.getLine() - 1,
                        logInfo.getLine() - 1);

            if (m_bUserFilter) {
                if (m_ipIndicator.m_chBookmark.isSelected()
                        || m_ipIndicator.m_chError.isSelected()) {
                    boolean bAddFilteredArray = false;
                    if (logInfo.isMarked()
                            && m_ipIndicator.m_chBookmark.isSelected()) {
                        bAddFilteredArray = true;
                        m_hmMarkedInfoFiltered.put(logInfo.getLine() - 1,
                                m_arLogInfoFiltered.size());
                        if (logInfo.getLogLV().equals("E")
                                || logInfo.getLogLV().equals("ERROR"))
                            m_hmErrorFiltered.put(logInfo.getLine() - 1,
                                    m_arLogInfoFiltered.size());
                    }
                    if ((logInfo.getLogLV().equals("E") || logInfo.getLogLV()
                            .equals("ERROR"))
                            && m_ipIndicator.m_chError.isSelected()) {
                        bAddFilteredArray = true;
                        m_hmErrorFiltered.put(logInfo.getLine() - 1,
                                m_arLogInfoFiltered.size());
                        if (logInfo.isMarked())
                            m_hmMarkedInfoFiltered.put(logInfo.getLine() - 1,
                                    m_arLogInfoFiltered.size());
                    }

                    if (bAddFilteredArray)
                        m_arLogInfoFiltered.add(logInfo);
                } else if (checkLogLVFilter(logInfo) && checkPidFilter(logInfo)
                        && checkTidFilter(logInfo)
                        && checkShowTagFilter(logInfo)
                        && checkRemoveTagFilter(logInfo)
                        && checkFindFilter(logInfo)
                        && checkRemoveFilter(logInfo)
                        && checkFromTimeFilter(logInfo)
                        && checkToTimeFilter(logInfo)
                        && checkBookmarkFilter(logInfo)) {
                    m_arLogInfoFiltered.add(logInfo);
                    if (logInfo.isMarked())
                        m_hmMarkedInfoFiltered.put(logInfo.getLine() - 1,
                                m_arLogInfoFiltered.size());
                    if (logInfo.getLogLV() == "E"
                            || logInfo.getLogLV() == "ERROR")
                        if (logInfo.getLogLV().equals("E")
                                || logInfo.getLogLV().equals("ERROR"))
                            m_hmErrorFiltered.put(logInfo.getLine() - 1,
                                    m_arLogInfoFiltered.size());
                }
            }
        }
    }

    void addChangeListener() {
        m_tfSearch.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfHighlight.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfFindWord.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfRemoveWord.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfShowTag.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfRemoveTag.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfBookmarkTag.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfShowPid.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfShowTid.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfFromTimeTag.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfToTimeTag.getDocument().addDocumentListener(m_dlFilterListener);

        m_chkEnableFind.addItemListener(m_itemListener);
        m_chkEnableRemove.addItemListener(m_itemListener);
        m_chkEnableShowPid.addItemListener(m_itemListener);
        m_chkEnableShowTid.addItemListener(m_itemListener);
        m_chkEnableShowTag.addItemListener(m_itemListener);
        m_chkEnableRemoveTag.addItemListener(m_itemListener);
        m_chkEnableBookmarkTag.addItemListener(m_itemListener);
        m_chkEnableHighlight.addItemListener(m_itemListener);
        m_chkEnableTimeTag.addItemListener(m_itemListener);

        m_chkVerbose.addItemListener(m_itemListener);
        m_chkDebug.addItemListener(m_itemListener);
        m_chkInfo.addItemListener(m_itemListener);
        m_chkWarn.addItemListener(m_itemListener);
        m_chkError.addItemListener(m_itemListener);
        m_chkFatal.addItemListener(m_itemListener);
        m_chkClmBookmark.addItemListener(m_itemListener);
        m_chkClmLine.addItemListener(m_itemListener);
        m_chkClmDate.addItemListener(m_itemListener);
        m_chkClmTime.addItemListener(m_itemListener);
        m_chkClmLogLV.addItemListener(m_itemListener);
        m_chkClmPid.addItemListener(m_itemListener);
        m_chkClmThread.addItemListener(m_itemListener);
        m_chkClmTag.addItemListener(m_itemListener);
        m_chkClmMessage.addItemListener(m_itemListener);

        m_logScrollVPane.getViewport().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                // m_ipIndicator.m_bDrawFull = false;
                if (getExtendedState() != JFrame.MAXIMIZED_BOTH) {
                    m_nLastWidth = getWidth();
                    m_nLastHeight = getHeight();
                }
                m_ipIndicator.repaint();
            }
        });
    }

    Component getFilterPanel() {
        m_chkEnableFind = new JCheckBox();
        m_chkEnableRemove = new JCheckBox();
        m_chkEnableShowTag = new JCheckBox();
        m_chkEnableRemoveTag = new JCheckBox();
        m_chkEnableShowPid = new JCheckBox();
        m_chkEnableShowTid = new JCheckBox();
        m_chkEnableBookmarkTag = new JCheckBox();
        m_chkEnableTimeTag = new JCheckBox();
        m_chkEnableFind.setSelected(true);
        m_chkEnableRemove.setSelected(true);
        m_chkEnableShowTag.setSelected(true);
        m_chkEnableRemoveTag.setSelected(true);
        m_chkEnableShowPid.setSelected(true);
        m_chkEnableShowTid.setSelected(true);
        m_chkEnableBookmarkTag.setSelected(false);
        m_chkEnableTimeTag.setSelected(true);

        m_tfFindWord = new JTextField();
        m_tfRemoveWord = new JTextField();
        m_tfShowTag = new JTextField();
        m_tfRemoveTag = new JTextField();
        m_tfShowPid = new JTextField();
        m_tfShowTid = new JTextField();
        m_tfBookmarkTag = new JTextField();
        m_tfFromTimeTag = new JTextField();
        m_tfToTimeTag = new JTextField();

        JPanel jpMain = new JPanel(new BorderLayout());

        JPanel jpWordFilter = new JPanel(new BorderLayout());
        jpWordFilter.setBorder(BorderFactory.createTitledBorder("Word filter"));

        JPanel jpFind = new JPanel(new BorderLayout());
        JLabel find = new JLabel();
        find.setText("include:");
        jpFind.add(find, BorderLayout.WEST);
        jpFind.add(m_tfFindWord, BorderLayout.CENTER);
        jpFind.add(m_chkEnableFind, BorderLayout.EAST);

        JPanel jpRemove = new JPanel(new BorderLayout());
        JLabel remove = new JLabel();
        remove.setText("exclude:");
        jpRemove.add(remove, BorderLayout.WEST);
        jpRemove.add(m_tfRemoveWord, BorderLayout.CENTER);
        jpRemove.add(m_chkEnableRemove, BorderLayout.EAST);

        jpWordFilter.add(jpFind, BorderLayout.NORTH);
        jpWordFilter.add(jpRemove);

        jpMain.add(jpWordFilter, BorderLayout.NORTH);

        JPanel jpTagFilter = new JPanel(new GridLayout(5, 1));
        jpTagFilter.setBorder(BorderFactory.createTitledBorder("Tag filter"));

        JPanel jpPidTid = new JPanel(new GridLayout(1, 2));

        JPanel jpPid = new JPanel(new BorderLayout());
        JLabel pid = new JLabel();
        pid.setText("Pid : ");
        jpPid.add(pid, BorderLayout.WEST);
        jpPid.add(m_tfShowPid, BorderLayout.CENTER);
        jpPid.add(m_chkEnableShowPid, BorderLayout.EAST);

        JPanel jpTid = new JPanel(new BorderLayout());
        JLabel tid = new JLabel();
        tid.setText("Tid : ");
        jpTid.add(tid, BorderLayout.WEST);
        jpTid.add(m_tfShowTid, BorderLayout.CENTER);
        jpTid.add(m_chkEnableShowTid, BorderLayout.EAST);

        jpPidTid.add(jpPid);
        jpPidTid.add(jpTid);

        JPanel jpShow = new JPanel(new BorderLayout());
        JLabel show = new JLabel();
        show.setText("Tag Include: ");
        jpShow.add(show, BorderLayout.WEST);
        jpShow.add(m_tfShowTag, BorderLayout.CENTER);
        jpShow.add(m_chkEnableShowTag, BorderLayout.EAST);

        JPanel jpRemoveTag = new JPanel(new BorderLayout());
        JLabel removeTag = new JLabel();
        removeTag.setText("Tag Exclude: ");
        jpRemoveTag.add(removeTag, BorderLayout.WEST);
        jpRemoveTag.add(m_tfRemoveTag, BorderLayout.CENTER);
        jpRemoveTag.add(m_chkEnableRemoveTag, BorderLayout.EAST);

        JPanel jpBmTag = new JPanel(new BorderLayout());
        JLabel bkTag = new JLabel();
        bkTag.setText("Bookmark: ");
        jpBmTag.add(bkTag, BorderLayout.WEST);
        jpBmTag.add(m_tfBookmarkTag, BorderLayout.CENTER);
        jpBmTag.add(m_chkEnableBookmarkTag, BorderLayout.EAST);

        JPanel jpFromTimeTag = new JPanel(new BorderLayout());
        JLabel jlFromTimeTag = new JLabel();
        jlFromTimeTag.setText("from time");
        jpFromTimeTag.add(jlFromTimeTag, BorderLayout.WEST);
        jpFromTimeTag.add(m_tfFromTimeTag, BorderLayout.CENTER);

        JPanel jpToTimeTag = new JPanel(new BorderLayout());
        JLabel jlToTimeTag = new JLabel();
        jlToTimeTag.setText("to");
        jpToTimeTag.add(jlToTimeTag, BorderLayout.WEST);
        jpToTimeTag.add(m_tfToTimeTag, BorderLayout.CENTER);

        JPanel jpTimeTag = new JPanel(new GridLayout(1, 2));
        jpTimeTag.add(jpFromTimeTag);
        jpTimeTag.add(jpToTimeTag);
        JPanel jpTimeMainTag = new JPanel(new BorderLayout());
        jpTimeMainTag.add(jpTimeTag, BorderLayout.CENTER);
        jpTimeMainTag.add(m_chkEnableTimeTag, BorderLayout.EAST);

        jpTagFilter.add(jpPidTid);
        jpTagFilter.add(jpShow);
        jpTagFilter.add(jpRemoveTag);
        jpTagFilter.add(jpBmTag);
        jpTagFilter.add(jpTimeMainTag);

        jpMain.add(jpTagFilter, BorderLayout.CENTER);

        JPanel jpActionPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        JButton clearFieldBtn = new JButton("Clean Filter");
        clearFieldBtn.setMargin(new Insets(0, 0, 0, 0));
        clearFieldBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m_tfFindWord.setText("");
                m_tfRemoveWord.setText("");
                m_tfShowTag.setText("");
                m_tfRemoveTag.setText("");
                m_tfShowPid.setText("");
                m_tfShowTid.setText("");
                m_tfBookmarkTag.setText("");
            }
        });
        jpActionPanel.add(clearFieldBtn);

        JButton followBtn = new JButton("Follow");
        followBtn.setMargin(new Insets(0, 0, 0, 0));
        followBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int endLine = m_tmLogTableModel.getRowCount();
                updateTable(endLine - 1, true);
            }
        });
        jpActionPanel.add(followBtn);

        jpMain.add(jpActionPanel, BorderLayout.SOUTH);

        return jpMain;
    }

    Component getHighlightPanel() {
        m_chkEnableHighlight = new JCheckBox();
        m_chkEnableHighlight.setSelected(true);

        m_tfHighlight = new JTextField();

        JPanel jpMain = new JPanel(new BorderLayout());
        jpMain.setBorder(BorderFactory.createTitledBorder("Highlight"));
        jpMain.add(m_tfHighlight);
        jpMain.add(m_chkEnableHighlight, BorderLayout.EAST);

        return jpMain;
    }

    Component getSearchPanel() {
        m_tfSearch = new JTextField();
        m_tfSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getLogTable().gotoNextSearchResult();
            }
        });

        JPanel jpMain = new JPanel(new BorderLayout());
        jpMain.setBorder(BorderFactory.createTitledBorder("Search"));
        jpMain.add(m_tfSearch, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        JButton preButton = new JButton();
        preButton.setMargin(new Insets(0, 0, 0, 0));
        preButton.setText("<=");
        preButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getLogTable().gotoPreSearchResult();
            }
        });
        buttonPanel.add(preButton);

        JButton nextButton = new JButton();
        nextButton.setMargin(new Insets(0, 0, 0, 0));
        nextButton.setText("=>");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getLogTable().gotoNextSearchResult();
            }
        });
        buttonPanel.add(nextButton);

        jpMain.add(buttonPanel, BorderLayout.EAST);

        return jpMain;
    }

    Component getCheckPanel() {
        m_chkVerbose = new JCheckBox();
        m_chkDebug = new JCheckBox();
        m_chkInfo = new JCheckBox();
        m_chkWarn = new JCheckBox();
        m_chkError = new JCheckBox();
        m_chkFatal = new JCheckBox();

        m_chkClmBookmark = new JCheckBox();
        m_chkClmLine = new JCheckBox();
        m_chkClmDate = new JCheckBox();
        m_chkClmTime = new JCheckBox();
        m_chkClmLogLV = new JCheckBox();
        m_chkClmPid = new JCheckBox();
        m_chkClmThread = new JCheckBox();
        m_chkClmTag = new JCheckBox();
        m_chkClmMessage = new JCheckBox();

        JPanel jpMain = new JPanel();
        jpMain.setLayout(new BoxLayout(jpMain, BoxLayout.PAGE_AXIS));

        JPanel jpLogFilter = new JPanel();
        jpLogFilter.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        jpLogFilter.setBorder(BorderFactory.createTitledBorder("Log filter"));
        m_chkVerbose.setText("Verbose");
        m_chkVerbose.setSelected(true);
        m_chkDebug.setText("Debug");
        m_chkDebug.setSelected(true);
        m_chkInfo.setText("Info");
        m_chkInfo.setSelected(true);
        m_chkWarn.setText("Warn");
        m_chkWarn.setSelected(true);
        m_chkError.setText("Error");
        m_chkError.setSelected(true);
        m_chkFatal.setText("Fatal");
        m_chkFatal.setSelected(true);
        jpLogFilter.add(m_chkVerbose);
        jpLogFilter.add(m_chkDebug);
        jpLogFilter.add(m_chkInfo);
        jpLogFilter.add(m_chkWarn);
        jpLogFilter.add(m_chkError);
        jpLogFilter.add(m_chkFatal);

        JPanel jpShowColumn = new JPanel();
        jpShowColumn.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        jpShowColumn.setBorder(BorderFactory.createTitledBorder("Show column"));
        m_chkClmBookmark.setText("Mark");
        m_chkClmBookmark.setToolTipText("Bookmark");
        m_chkClmLine.setText("Line");
        m_chkClmLine.setSelected(true);
        m_chkClmDate.setText("Date");
        m_chkClmDate.setSelected(true);
        m_chkClmTime.setText("Time");
        m_chkClmTime.setSelected(true);
        m_chkClmLogLV.setText("LogLV");
        m_chkClmLogLV.setSelected(true);
        m_chkClmPid.setText("Pid");
        m_chkClmPid.setSelected(true);
        m_chkClmThread.setText("Thread");
        m_chkClmThread.setSelected(true);
        m_chkClmTag.setText("Tag");
        m_chkClmTag.setSelected(true);
        m_chkClmMessage.setText("Msg");
        m_chkClmMessage.setSelected(true);
        jpShowColumn.add(m_chkClmLine);
        jpShowColumn.add(m_chkClmDate);
        jpShowColumn.add(m_chkClmTime);
        jpShowColumn.add(m_chkClmLogLV);
        jpShowColumn.add(m_chkClmPid);
        jpShowColumn.add(m_chkClmThread);
        jpShowColumn.add(m_chkClmTag);
        jpShowColumn.add(m_chkClmBookmark);
        jpShowColumn.add(m_chkClmMessage);

        jpMain.add(jpLogFilter);
        jpMain.add(jpShowColumn);
        jpMain.add(getHighlightPanel());
        jpMain.add(getSearchPanel());
        return jpMain;
    }

    JPanel getToolPanel() {
        JPanel jpTool = new JPanel();
        jpTool.setLayout(new GridLayout(5, 1, 2, 2));
        jpTool.setBorder(BorderFactory.createTitledBorder("Tools"));
        jpTool.setMinimumSize(new Dimension(80, 0));
        jpTool.setPreferredSize(new Dimension(80, 0));

        JButton calcButton = new JButton("Calc");
        calcButton.setMargin(new Insets(0, 0, 0, 0));
        calcButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Utils.runCmd(new String[]{CALC_PROGRAM_PATH});
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        jpTool.add(calcButton);
        return jpTool;
    }

    Component getOptionFilter() {
        JPanel optionFilter = new JPanel(new BorderLayout());

        optionFilter.add(getDevicePanel(), BorderLayout.WEST);
        optionFilter.add(getFilterPanel(), BorderLayout.CENTER);

        JPanel aPanel = new JPanel(new BorderLayout());
        aPanel.add(getCheckPanel(), BorderLayout.WEST);
        aPanel.add(getToolPanel(), BorderLayout.EAST);
        optionFilter.add(aPanel, BorderLayout.EAST);

        return optionFilter;
    }

    Component getOptionMenu() {
        JPanel optionMenu = new JPanel(new BorderLayout());
        JPanel optionWest = new JPanel(new FlowLayout(FlowLayout.LEADING));

        JLabel jlFont = new JLabel("Font Size : ");
        m_tfFontSize = new JTextField(2);
        m_tfFontSize.setHorizontalAlignment(SwingConstants.RIGHT);
        m_tfFontSize.setText("12");

        m_btnSetFont = new JButton("OK");
        m_btnSetFont.setMargin(new Insets(0, 0, 0, 0));
        m_btnSetFont.addActionListener(m_alButtonListener);

        JLabel jlEncode = new JLabel("Text Encode : ");
        m_comboEncode = new JComboBox<>();
        m_comboEncode.addItem("UTF-8");
        m_comboEncode.addItem("Local");

        JLabel jlGoto = new JLabel("Goto : ");
        final JTextField tfGoto = new JTextField(6);
        tfGoto.setHorizontalAlignment(SwingConstants.RIGHT);
        tfGoto.addCaretListener(new CaretListener() {
            public void caretUpdate(CaretEvent e) {
                try {
                    int nIndex = Integer.parseInt(tfGoto.getText()) - 1;
                    getLogTable().showRowCenterIfNotInRect(nIndex, true);
                } catch (Exception err) {
                }
            }
        });

        mSyncScrollCheckBox = new JCheckBox("sync scroll");
        mSyncScrollCheckBox.setEnabled(false);
        mSyncScrollCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                JCheckBox check = (JCheckBox) e.getSource();
                enableSyncScroll(check.isSelected());
            }
        });

        mSyncSelectedCheckBox = new JCheckBox("sync selected");
        mSyncSelectedCheckBox.setEnabled(false);
        mSyncSelectedCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                JCheckBox check = (JCheckBox) e.getSource();
                enableSyncSelected(check.isSelected());
            }
        });

        JButton preHistoryButton = new JButton("<");
        preHistoryButton.setMargin(new Insets(0, 5, 0, 5));
        preHistoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m_tbLogTable.historyBack();
            }
        });

        JButton nextHistoryButton = new JButton(">");
        nextHistoryButton.setMargin(new Insets(0, 5, 0, 5));
        nextHistoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m_tbLogTable.historyForward();
            }
        });

        optionWest.add(mSyncScrollCheckBox);
        optionWest.add(mSyncSelectedCheckBox);
        optionWest.add(jlFont);
        optionWest.add(m_tfFontSize);
        optionWest.add(m_btnSetFont);
        optionWest.add(jlEncode);
        optionWest.add(m_comboEncode);
        optionWest.add(jlGoto);
        optionWest.add(tfGoto);

        optionWest.add(preHistoryButton);
        optionWest.add(nextHistoryButton);

        optionMenu.add(optionWest, BorderLayout.CENTER);
        return optionMenu;
    }

    Component getOptionPanel() {
        JPanel optionMain = new JPanel(new BorderLayout());

        optionMain.add(getOptionFilter(), BorderLayout.CENTER);
        optionMain.add(getOptionMenu(), BorderLayout.SOUTH);

        return optionMain;
    }

    Component getStatusPanel() {
        JPanel mainP = new JPanel(new BorderLayout());

        JPanel tfPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        Border border = BorderFactory.createCompoundBorder(new EmptyBorder(0, 4, 0, 0), new EtchedBorder());

        m_tfStatus = new JLabel("ready");
        m_tfStatus.setBorder(border);
        m_tfDiffPort = new JLabel("not bind");
        m_tfDiffPort.setBorder(border);
        m_tfDiffState = new JLabel("disconnected");
        m_tfDiffState.setBorder(border);


        tfPanel.add(m_tfDiffState, constraints);
        tfPanel.add(m_tfDiffPort, constraints);
        tfPanel.add(m_tfStatus, constraints);

        mainP.add(tfPanel, BorderLayout.EAST);

        return mainP;
    }

    Component getLogPanel() {
        JPanel mainLogPanel = new JPanel(new BorderLayout());
        mainLogPanel.add(getBookmarkPanel(), BorderLayout.WEST);

        m_tmLogTableModel = new LogFilterTableModel();
        m_tmLogTableModel.setData(m_arLogInfoAll);
        m_tbLogTable = new LogTable(m_tmLogTableModel, this);
        m_logScrollVPane = new JScrollPane(m_tbLogTable);
        mainLogPanel.add(m_logScrollVPane, BorderLayout.CENTER);

        m_tSubLogTableModel = new LogFilterTableModel();
        m_tSubLogTableModel.setData(m_arSubLogInfoAll);
        m_tSublogTable = new SubLogTable(m_tSubLogTableModel, this);
        m_subLogScrollVPane = new JScrollPane(m_tSublogTable);

        mSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainLogPanel, m_subLogScrollVPane);
        return mSplitPane;
    }

    void initValue() {
        m_bPauseADB = false;
        FILE_LOCK = new Object();
        FILTER_LOCK = new Object();
        m_nChangedFilter = STATUS_READY;
        m_nFilterLogLV = LogInfo.LOG_LV_ALL;

        m_arLogInfoAll = new ArrayList<LogInfo>();
        m_arLogInfoFiltered = new ArrayList<LogInfo>();
        m_hmMarkedInfoAll = new HashMap<Integer, Integer>();
        m_hmMarkedInfoFiltered = new HashMap<Integer, Integer>();
        m_hmErrorAll = new ConcurrentHashMap<Integer, Integer>();
        m_hmErrorFiltered = new ConcurrentHashMap<Integer, Integer>();
        m_arSubLogInfoAll = new ArrayList<>();

        File confDir = new File(CONFIG_BASE_DIR);
        if (!confDir.exists()) {
            confDir.mkdirs();
            T.d("create conf directory: " + confDir.getAbsolutePath());
        }
        File outputDir = new File(OUTPUT_LOG_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
            T.d("create log directory: " + outputDir.getAbsolutePath());
        }
        m_strLogFileName = makeFilename();
        // m_strProcessCmd = ANDROID_DEFAULT_CMD + m_strLogFileName;
    }

    void parseFile(final File file) {
        if (file == null) {
            T.e("file == null");
            return;
        }

        setTitle(file.getPath());
        new Thread(new Runnable() {
            public void run() {
                FileInputStream fstream = null;
                DataInputStream in = null;
                BufferedReader br = null;
                int nIndex = 1;

                try {
                    fstream = new FileInputStream(file);
                    in = new DataInputStream(fstream);
                    if (m_comboEncode.getSelectedItem().equals("UTF-8"))
                        br = new BufferedReader(new InputStreamReader(in,
                                "UTF-8"));
                    else
                        br = new BufferedReader(new InputStreamReader(in));

                    String strLine;

                    setStatus("Parsing");
                    clearData();
                    getLogTable().clearSelection();
                    getSubTable().clearSelection();

                    while ((strLine = br.readLine()) != null) {
                        if (!"".equals(strLine.trim())) {
                            LogInfo logInfo = m_iLogParser.parseLog(strLine);
                            logInfo.setType(LogInfo.TYPE.SYSTEM);
                            logInfo.setLine(nIndex++);
                            addLogInfo(logInfo);
                        }
                    }
                    runFilter();
                    setStatus("Parse complete");
                } catch (Exception ioe) {
                    T.e(ioe);
                }
                try {
                    if (br != null)
                        br.close();
                    if (in != null)
                        in.close();
                    if (fstream != null)
                        fstream.close();
                } catch (Exception e) {
                    T.e(e);
                }
            }
        }).start();
    }

    void pauseProcess() {
        if (m_tbtnPause.isSelected()) {
            m_bPauseADB = true;
            m_tbtnPause.setText("Resume");
        } else {
            m_bPauseADB = false;
            m_tbtnPause.setText("Pause");
        }
    }

    @Override
    public void onSetBookmark(int nLine, String strBookmark) {
        LogInfo logInfo = m_arLogInfoAll.get(nLine);
        logInfo.setBookmark(strBookmark);
        m_arLogInfoAll.set(nLine, logInfo);
    }

    private String[] getADBValidCmd() {
        String strCommand = DEVICES_CMD[m_comboDeviceCmd.getSelectedIndex()];
        String[] cmd;
        if (OSUtil.isWindows()) {
            cmd = new String[]{"cmd.exe", "/C", strCommand};
        } else {
            cmd = new String[]{"/bin/bash", "-l", "-c", strCommand};
        }
        return cmd;
    }

    private void addDevicesToListModelFromCmd(String[] cmd, DefaultListModel<TargetDevice> listModel) {
        try {
            listModel.clear();
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true);
            Process oProcess = processBuilder.start();

            BufferedReader stdOut = new BufferedReader(new InputStreamReader(
                    oProcess.getInputStream()));

            String s;
            while ((s = stdOut.readLine()) != null) {
                if (!s.startsWith("List of devices attached") && s.length() != 0) {
                    listModel.addElement(new TargetDevice(s));
                }
            }
        } catch (Exception e) {
            T.e("e = " + e);
            listModel.addElement(new TargetDevice(e.getMessage()));
        }
    }

    public void setSearchFocus() {
//        m_tfFindWord.requestFocus();
        m_tfSearch.requestFocus();
    }

    private void setHighLightFocus() {
        m_tfHighlight.requestFocus();
    }

    private void setWordIncludeFocus() {
        m_tfFindWord.requestFocus();
    }

    private void setTagIncludeFocus() {
        m_tfShowTag.requestFocus();
    }

    private void setMainTableFocus() {
        m_tbLogTable.requestFocus();
    }

    public void searchKeyword(String keyword) {
        m_tfSearch.setText(keyword);
    }

    void setDnDListener() {

        new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE,
                new DropTargetListener() {
                    public void dropActionChanged(DropTargetDragEvent dtde) {
                    }

                    public void dragOver(DropTargetDragEvent dtde) {
                    }

                    public void dragExit(DropTargetEvent dte) {
                    }

                    public void dragEnter(DropTargetDragEvent event) {
                    }

                    public void drop(DropTargetDropEvent event) {
                        try {
                            event.acceptDrop(DnDConstants.ACTION_COPY);
                            Transferable t = event.getTransferable();
                            List<?> list = (List<?>) (t
                                    .getTransferData(DataFlavor.javaFileListFlavor));
                            Iterator<?> i = list.iterator();
                            if (i.hasNext()) {
                                File file = (File) i.next();
                                setTitle(file.getPath());

                                stopProcess();
                                parseFile(file);
                                m_recentMenu.addEntry(file.getAbsolutePath());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    void setLogLV(int nLogLV, boolean bChecked) {
        if (bChecked)
            m_nFilterLogLV |= nLogLV;
        else
            m_nFilterLogLV &= ~nLogLV;
        m_nChangedFilter = STATUS_CHANGE;
        runFilter();
    }

    void useFilter(JCheckBox checkBox) {
        if (checkBox.equals(m_chkEnableFind)) {
            getLogTable().setFilterFind(checkBox.isSelected() ? m_tfFindWord
                    .getText() : "");
            getSubTable().SetHighlight(checkBox.isSelected() ? m_tfFindWord
                    .getText() : "");
        }
        if (checkBox.equals(m_chkEnableRemove)) {
            getLogTable().SetFilterRemove(checkBox.isSelected() ? m_tfRemoveWord
                    .getText() : "");
            getSubTable().SetHighlight(checkBox.isSelected() ? m_tfRemoveWord
                    .getText() : "");
        }
        if (checkBox.equals(m_chkEnableShowPid)) {
            getLogTable().SetFilterShowPid(checkBox.isSelected() ? m_tfShowPid
                    .getText() : "");
            getSubTable().SetHighlight(checkBox.isSelected() ? m_tfShowPid
                    .getText() : "");
        }
        if (checkBox.equals(m_chkEnableShowTid)) {
            getLogTable().SetFilterShowTid(checkBox.isSelected() ? m_tfShowTid
                    .getText() : "");
            getSubTable().SetHighlight(checkBox.isSelected() ? m_tfShowTid
                    .getText() : "");
        }
        if (checkBox.equals(m_chkEnableShowTag)) {
            getLogTable().SetFilterShowTag(checkBox.isSelected() ? m_tfShowTag
                    .getText() : "");
            getSubTable().SetHighlight(checkBox.isSelected() ? m_tfShowTag
                    .getText() : "");
        }
        if (checkBox.equals(m_chkEnableRemoveTag)) {
            getLogTable().SetFilterRemoveTag(checkBox.isSelected() ? m_tfRemoveTag
                    .getText() : "");
            getSubTable().SetHighlight(checkBox.isSelected() ? m_tfRemoveTag
                    .getText() : "");
        }
        if (checkBox.equals(m_chkEnableBookmarkTag)) {
            getLogTable().SetFilterBookmarkTag(checkBox.isSelected() ? m_tfBookmarkTag
                    .getText() : "");
            getSubTable().SetHighlight(checkBox.isSelected() ? m_tfBookmarkTag
                    .getText() : "");
        }
        if (checkBox.equals(m_chkEnableHighlight)) {
            getLogTable().SetHighlight(checkBox.isSelected() ? m_tfHighlight
                    .getText() : "");
            getSubTable().SetHighlight(checkBox.isSelected() ? m_tfHighlight
                    .getText() : "");
        }
        if (checkBox.equals(m_chkEnableTimeTag)) {
            if (checkBox.isSelected()) {
                getLogTable().SetFilterFromTime(m_tfFromTimeTag.getText());
                getLogTable().SetFilterToTime(m_tfToTimeTag.getText());
            } else {
                getLogTable().SetFilterFromTime("");
                getLogTable().SetFilterToTime("");
            }
        }
        m_nChangedFilter = STATUS_CHANGE;
        runFilter();
    }

    void setProcessBtn(boolean bStart) {
        if (bStart) {
            m_btnRun.setEnabled(false);
            m_btnStop.setEnabled(true);
            m_btnClear.setEnabled(true);
            m_tbtnPause.setEnabled(true);
        } else {
            m_btnRun.setEnabled(true);
            m_btnStop.setEnabled(false);
            m_btnClear.setEnabled(false);
            m_tbtnPause.setEnabled(false);
            m_tbtnPause.setSelected(false);
            m_tbtnPause.setText("Pause");
        }
    }

    String getProcessCmd() {
        if (m_lDeviceList.getSelectedIndex() < 0
                || m_selectedDevice == null
                || m_selectedDevice.code.length() == 0)
            return ANDROID_DEFAULT_CMD_FIRST + m_comboCmd.getSelectedItem();
        else
            return ANDROID_SELECTED_CMD_FIRST
                    + m_selectedDevice.code
                    + " "
                    + m_comboCmd.getSelectedItem();
    }

    void setStatus(String strText) {
        m_tfStatus.setText(strText);
    }

    public void setTitle(String strTitle) {
        super.setTitle(strTitle);
    }

    void stopProcess() {
        setProcessBtn(false);
        if (m_Process != null)
            m_Process.destroy();
        if (m_thProcess != null)
            m_thProcess.interrupt();
        if (m_thWatchFile != null)
            m_thWatchFile.interrupt();
        m_Process = null;
        m_thProcess = null;
        m_thWatchFile = null;
        m_bPauseADB = false;
    }

    void startFileParse() {
        m_thWatchFile = new Thread(new Runnable() {
            public void run() {
                FileInputStream fstream = null;
                DataInputStream in = null;
                BufferedReader br = null;

                try {
                    fstream = new FileInputStream(m_strLogFileName);
                    in = new DataInputStream(fstream);
                    if (m_comboEncode.getSelectedItem().equals("UTF-8"))
                        br = new BufferedReader(new InputStreamReader(in,
                                "UTF-8"));
                    else
                        br = new BufferedReader(new InputStreamReader(in));

                    String strLine;

                    setTitle(m_strLogFileName);

                    m_arLogInfoAll.clear();

                    boolean bEndLine;
                    int nSelectedIndex;
                    int nAddCount;
                    int nPreRowCount = 0;
                    int nEndLine;

                    while (true) {
                        Thread.sleep(50);

                        if (m_nChangedFilter == STATUS_CHANGE
                                || m_nChangedFilter == STATUS_PARSING)
                            continue;
                        if (m_bPauseADB)
                            continue;

                        bEndLine = false;
                        nSelectedIndex = getLogTable().getSelectedRow();
                        nPreRowCount = getLogTable().getRowCount();
                        nAddCount = 0;

                        if (nSelectedIndex == -1
                                || nSelectedIndex == getLogTable().getRowCount() - 1)
                            bEndLine = true;

                        synchronized (FILE_LOCK) {
                            int nLine = m_arLogInfoAll.size() + 1;
                            while (!m_bPauseADB
                                    && (strLine = br.readLine()) != null) {
                                if (strLine != null
                                        && !"".equals(strLine.trim())) {
                                    LogInfo logInfo = m_iLogParser
                                            .parseLog(strLine);
                                    logInfo.setLine(nLine++);
                                    addLogInfo(logInfo);
                                    nAddCount++;
                                }
                            }
                        }
                        if (nAddCount == 0)
                            continue;

                        synchronized (FILTER_LOCK) {
                            if (m_bUserFilter == false) {
                                m_tmLogTableModel.setData(m_arLogInfoAll);
                                m_ipIndicator.setData(m_arLogInfoAll,
                                        m_hmMarkedInfoAll, m_hmErrorAll);
                            } else {
                                m_tmLogTableModel.setData(m_arLogInfoFiltered);
                                m_ipIndicator
                                        .setData(m_arLogInfoFiltered,
                                                m_hmMarkedInfoFiltered,
                                                m_hmErrorFiltered);
                            }

                            nEndLine = m_tmLogTableModel.getRowCount();
                            if (nPreRowCount != nEndLine) {
                                if (bEndLine)
                                    updateTable(nEndLine - 1, true);
                                else
                                    updateTable(nSelectedIndex, false);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    T.e(e);
                } catch (Exception e) {
                    e.printStackTrace();
                    T.e(e);
                }
                try {
                    if (br != null)
                        br.close();
                    if (in != null)
                        in.close();
                    if (fstream != null)
                        fstream.close();
                } catch (Exception e) {
                    T.e(e);
                }
                System.out.println("End m_thWatchFile thread");
            }
        });
        m_thWatchFile.start();
    }

    void runFilter() {
        checkUseFilter();
        while (m_nChangedFilter == STATUS_PARSING)
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        synchronized (FILTER_LOCK) {
            FILTER_LOCK.notify();
        }
    }

    void startFilterParse() {
        m_thFilterParse = new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        synchronized (FILTER_LOCK) {
                            m_nChangedFilter = STATUS_READY;
                            FILTER_LOCK.wait();

                            m_nChangedFilter = STATUS_PARSING;

                            m_arLogInfoFiltered.clear();
                            m_hmMarkedInfoFiltered.clear();
                            m_hmErrorFiltered.clear();
                            getLogTable().clearSelection();
                            getSubTable().clearSelection();

                            if (m_bUserFilter == false) {
                                m_tmLogTableModel.setData(m_arLogInfoAll);
                                m_ipIndicator.setData(m_arLogInfoAll,
                                        m_hmMarkedInfoAll, m_hmErrorAll);
                                LogInfo latestInfo = getLogTable().getLatestSelectedLogInfo();
                                if (latestInfo != null) {
                                    int i = 0;
                                    for (LogInfo info : m_arLogInfoAll) {
                                        i++;
                                        if (info.getLine() >= latestInfo.getLine()) {
                                            break;
                                        }
                                    }
                                    updateTable(i - 1, true);
                                } else {
                                    updateTable(m_arLogInfoFiltered.size() - 1, true);
                                }
                                m_nChangedFilter = STATUS_READY;
                                continue;
                            }
                            m_tmLogTableModel.setData(m_arLogInfoFiltered);
                            m_ipIndicator.setData(m_arLogInfoFiltered,
                                    m_hmMarkedInfoFiltered, m_hmErrorFiltered);
                            // updateTable(-1);
                            setStatus("Parsing");

                            int nRowCount = m_arLogInfoAll.size();
                            LogInfo logInfo;
                            boolean bAddFilteredArray;

                            for (int nIndex = 0; nIndex < nRowCount; nIndex++) {
                                if (nIndex % 10000 == 0)
                                    Thread.sleep(1);
                                if (m_nChangedFilter == STATUS_CHANGE) {
                                    break;
                                }
                                logInfo = m_arLogInfoAll.get(nIndex);

                                if (checkLogLVFilter(logInfo)
                                        && checkPidFilter(logInfo)
                                        && checkTidFilter(logInfo)
                                        && checkShowTagFilter(logInfo)
                                        && checkRemoveTagFilter(logInfo)
                                        && checkFindFilter(logInfo)
                                        && checkRemoveFilter(logInfo)
                                        && checkFromTimeFilter(logInfo)
                                        && checkToTimeFilter(logInfo)
                                        && checkBookmarkFilter(logInfo)) {
                                    if (m_ipIndicator.m_chBookmark.isSelected()
                                            || m_ipIndicator.m_chError.isSelected()) {
                                        bAddFilteredArray = false;
                                        if (logInfo.isMarked()
                                                && m_ipIndicator.m_chBookmark
                                                .isSelected()) {
                                            bAddFilteredArray = true;
                                            m_hmMarkedInfoFiltered.put(logInfo.getLine() - 1,
                                                    m_arLogInfoFiltered.size());
                                            if (logInfo.getLogLV().equals("E")
                                                    || logInfo.getLogLV()
                                                    .equals("ERROR"))
                                                m_hmErrorFiltered.put(logInfo.getLine() - 1,
                                                        m_arLogInfoFiltered.size());
                                        }
                                        if ((logInfo.getLogLV().equals("E") || logInfo.getLogLV().equals("ERROR"))
                                                && m_ipIndicator.m_chError
                                                .isSelected()) {
                                            bAddFilteredArray = true;
                                            m_hmErrorFiltered.put(logInfo.getLine() - 1,
                                                    m_arLogInfoFiltered.size());
                                            if (logInfo.isMarked())
                                                m_hmMarkedInfoFiltered.put(logInfo.getLine() - 1,
                                                        m_arLogInfoFiltered.size());
                                        }

                                        if (bAddFilteredArray)
                                            m_arLogInfoFiltered.add(logInfo);
                                    } else {
                                        m_arLogInfoFiltered.add(logInfo);
                                        if (logInfo.isMarked())
                                            m_hmMarkedInfoFiltered.put(logInfo.getLine() - 1,
                                                    m_arLogInfoFiltered.size());
                                        if (logInfo.getLogLV().equals("E")
                                                || logInfo.getLogLV()
                                                .equals("ERROR"))
                                            m_hmErrorFiltered.put(logInfo.getLine() - 1,
                                                    m_arLogInfoFiltered.size());
                                    }
                                }
                            }
                            if (m_nChangedFilter == STATUS_PARSING) {
                                m_nChangedFilter = STATUS_READY;
                                m_tmLogTableModel.setData(m_arLogInfoFiltered);
                                m_ipIndicator.setData(m_arLogInfoFiltered,
                                        m_hmMarkedInfoFiltered,
                                        m_hmErrorFiltered);
                                LogInfo latestInfo = getLogTable().getLatestSelectedLogInfo();
                                if (latestInfo != null) {
                                    int i = 0;
                                    for (LogInfo info : m_arLogInfoFiltered) {
                                        i++;
                                        if (info.getLine() >= latestInfo.getLine()) {
                                            break;
                                        }
                                    }
                                    updateTable(i - 1, true);
                                } else {
                                    updateTable(m_arLogInfoFiltered.size() - 1, true);
                                }
                                setStatus("Complete");
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("m_thFilterParse exit normal");
                } catch (Exception e) {
                    e.printStackTrace();
                    T.e(e);
                }
                System.out.println("End m_thFilterParse thread");
            }
        });
        m_thFilterParse.start();
    }

    void startProcess() {
        clearData();
        getLogTable().clearSelection();
        getSubTable().clearSelection();
        // 自动切换到logcatparser
        m_parserType = PARSER_TYPE_LOGCAT;
        m_iLogParser = new LogCatParser();

        m_thProcess = new Thread(new Runnable() {
            public void run() {
                try {
                    String s;
                    m_Process = null;

                    T.d("getProcessCmd() = " + getProcessCmd());
                    m_Process = Runtime.getRuntime().exec(getProcessCmd());
                    BufferedReader stdOut = new BufferedReader(
                            new InputStreamReader(m_Process.getInputStream(),
                                    "UTF-8"));
                    Writer fileOut = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(m_strLogFileName), "UTF-8"));

                    startFileParse();

                    while ((s = stdOut.readLine()) != null) {
                        if (s != null && !"".equals(s.trim())) {
                            synchronized (FILE_LOCK) {
                                fileOut.write(s);
                                fileOut.write("\r\n");
                                // fileOut.newLine();
                                fileOut.flush();
                            }
                        }
                    }
                    fileOut.close();
                    // T.d("Exit Code: " + m_Process.exitValue());
                } catch (Exception e) {
                    T.e("e = " + e);
                }
                stopProcess();
            }
        });
        m_thProcess.start();
        setProcessBtn(true);
    }

    boolean checkLogLVFilter(LogInfo logInfo) {
        if (m_nFilterLogLV == LogInfo.LOG_LV_ALL)
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_VERBOSE) != 0
                && (logInfo.getLogLV().equals("V") || logInfo.getLogLV()
                .equals("VERBOSE")))
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_DEBUG) != 0
                && (logInfo.getLogLV().equals("D") || logInfo.getLogLV()
                .equals("DEBUG")))
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_INFO) != 0
                && (logInfo.getLogLV().equals("I") || logInfo.getLogLV()
                .equals("INFO")))
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_WARN) != 0
                && (logInfo.getLogLV().equals("W") || logInfo.getLogLV()
                .equals("WARN")))
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_ERROR) != 0
                && (logInfo.getLogLV().equals("E") || logInfo.getLogLV()
                .equals("ERROR")))
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_FATAL) != 0
                && (logInfo.getLogLV().equals("F") || logInfo.getLogLV()
                .equals("FATAL")))
            return true;

        return false;
    }

    boolean checkPidFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterShowPid().length() <= 0)
            return true;

        StringTokenizer stk = new StringTokenizer(
                getLogTable().GetFilterShowPid(), "|", false);

        while (stk.hasMoreElements()) {
            if (logInfo.getPid().toLowerCase().contains(
                    stk.nextToken().toLowerCase()))
                return true;
        }

        return false;
    }

    boolean checkTidFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterShowTid().length() <= 0)
            return true;

        StringTokenizer stk = new StringTokenizer(
                getLogTable().GetFilterShowTid(), "|", false);

        while (stk.hasMoreElements()) {
            if (logInfo.getThread().toLowerCase().contains(
                    stk.nextToken().toLowerCase()))
                return true;
        }

        return false;
    }

    boolean checkFindFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterFind().length() <= 0)
            return true;

        StringTokenizer stk = new StringTokenizer(getLogTable().GetFilterFind(),
                "|", false);

        while (stk.hasMoreElements()) {
            if (logInfo.getMessage().toLowerCase().contains(
                    stk.nextToken().toLowerCase()))
                return true;
        }

        return false;
    }

    boolean checkRemoveFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterRemove().length() <= 0)
            return true;

        StringTokenizer stk = new StringTokenizer(
                getLogTable().GetFilterRemove(), "|", false);

        while (stk.hasMoreElements()) {
            if (logInfo.getMessage().toLowerCase().contains(
                    stk.nextToken().toLowerCase()))
                return false;
        }

        return true;
    }

    boolean checkShowTagFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterShowTag().length() <= 0)
            return true;

        StringTokenizer stk = new StringTokenizer(
                getLogTable().GetFilterShowTag(), "|", false);

        while (stk.hasMoreElements()) {
            if (logInfo.getTag().toLowerCase().contains(
                    stk.nextToken().toLowerCase()))
                return true;
        }

        return false;
    }

    boolean checkRemoveTagFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterRemoveTag().length() <= 0)
            return true;

        StringTokenizer stk = new StringTokenizer(
                getLogTable().GetFilterRemoveTag(), "|", false);

        while (stk.hasMoreElements()) {
            if (logInfo.getTag().toLowerCase().contains(
                    stk.nextToken().toLowerCase()))
                return false;
        }

        return true;
    }

    boolean checkBookmarkFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterBookmarkTag().length() <= 0 && logInfo.getBookmark().length() <= 0)
            return true;

        StringTokenizer stk = new StringTokenizer(
                getLogTable().GetFilterBookmarkTag(), "|", false);

        while (stk.hasMoreElements()) {
            if (logInfo.getBookmark().toLowerCase().contains(
                    stk.nextToken().toLowerCase()))
                return true;
        }

        return false;
    }

    boolean checkToTimeFilter(LogInfo logInfo) {
        if (logInfo.getTimestamp() == -1)
            return true;
        if (getLogTable().GetFilterToTime() == -1) {
            return true;
        }
//        T.d("checkToTimeFilter:" + logInfo.getTime() + " | " + logInfo.getTimestamp() + " | " + getLogTable().GetFilterToTime());
        return logInfo.getTimestamp() <= getLogTable().GetFilterToTime();
    }

    boolean checkFromTimeFilter(LogInfo logInfo) {
        if (logInfo.getTimestamp() == -1)
            return true;
        if (getLogTable().GetFilterFromTime() == -1) {
            return true;
        }

//        T.d("checkFromTimeFilter:" + logInfo.getTime() + " | " + logInfo.getTimestamp() + " | " + getLogTable().GetFilterFromTime());
        return logInfo.getTimestamp() >= getLogTable().GetFilterFromTime();
    }

    boolean checkUseFilter() {
        if (!m_ipIndicator.m_chBookmark.isSelected()
                && !m_ipIndicator.m_chError.isSelected()
                && checkLogLVFilter(new LogInfo())
                && (getLogTable().GetFilterShowPid().length() == 0 || !m_chkEnableShowPid.isSelected())
                && (getLogTable().GetFilterShowTid().length() == 0 || !m_chkEnableShowTid.isSelected())
                && (getLogTable().GetFilterShowTag().length() == 0 || !m_chkEnableShowTag.isSelected())
                && (getLogTable().GetFilterRemoveTag().length() == 0 || !m_chkEnableRemoveTag.isSelected())
                && (getLogTable().GetFilterBookmarkTag().length() == 0 || !m_chkEnableBookmarkTag.isSelected())
                && ((getLogTable().GetFilterFromTime() == -1l && getLogTable().GetFilterToTime() == -1l) || !m_chkEnableTimeTag.isSelected())
                && (getLogTable().GetFilterFind().length() == 0 || !m_chkEnableFind.isSelected())
                && (getLogTable().GetFilterRemove().length() == 0 || !m_chkEnableRemove.isSelected())
                ) {
            m_bUserFilter = false;
        } else
            m_bUserFilter = true;
        return m_bUserFilter;
    }

    ActionListener m_alButtonListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(m_btnDevice)) {
                m_selectedDevice = null;
                String[] cmd = getADBValidCmd();
                addDevicesToListModelFromCmd(cmd, (DefaultListModel<TargetDevice>) m_lDeviceList.getModel());
            } else if (e.getSource().equals(m_btnSetFont)) {
                getLogTable().setFontSize(Integer.parseInt(m_tfFontSize
                        .getText()));
                getSubTable().setFontSize(Integer.parseInt(m_tfFontSize
                        .getText()));
                updateTable(-1, false);
            } else if (e.getSource().equals(m_btnRun)) {
                startProcess();
            } else if (e.getSource().equals(m_btnStop)) {
                stopProcess();
            } else if (e.getSource().equals(m_btnClear)) {
                boolean bBackup = m_bPauseADB;
                m_bPauseADB = true;
                clearData();
                updateTable(-1, false);
                m_bPauseADB = bBackup;
            } else if (e.getSource().equals(m_tbtnPause)) {
                pauseProcess();
            }
        }
    };

    public void notiEvent(EventParam param) {
        switch (param.type) {
            case EVENT_CLICK_BOOKMARK:
            case EVENT_CLICK_ERROR:
                m_nChangedFilter = STATUS_CHANGE;
                runFilter();
                break;
            case EVENT_CHANGE_FILTER_SHOW_PID:
                m_tfShowPid.setText(getLogTable().GetFilterShowPid());
                break;
            case EVENT_CHANGE_FILTER_SHOW_TAG:
                m_tfShowTag.setText(getLogTable().GetFilterShowTag());
                break;
            case EVENT_CHANGE_FILTER_REMOVE_TAG:
                m_tfRemoveTag.setText(getLogTable().GetFilterRemoveTag());
                break;
            case EVENT_CHANGE_FILTER_FROM_TIME: {
                String fromTimeStr = (String) param.param1;
                m_tfFromTimeTag.setText(fromTimeStr);
            }
            break;
            case EVENT_CHANGE_FILTER_TO_TIME: {
                String toTimeStr = (String) param.param1;
                m_tfToTimeTag.setText(toTimeStr);
            }
            break;
            case EVENT_CHANGE_SELECTION: {
                LogInfo target = (LogInfo) param.param1;
                m_tbLogTable.changeSelection(target, false, false);
            }
            break;
        }
    }

    void updateTable(int nRow, boolean bMove) {
        // System.out.println("updateTable nRow:" + nRow + " | " + bMove);
        m_tmLogTableModel.fireTableDataChanged();
        m_logScrollVPane.validate();
        // if(nRow >= 0)
        // m_tbLogTable.changeSelection(nRow, 0, false, false);
        getLogTable().invalidate();
        getLogTable().repaint();
        if (nRow >= 0)
            getLogTable().changeSelection(nRow, 0, false, false, bMove);

        updateSubTable(-1);
    }

    void updateSubTable(int nRow) {
//        System.out.println("updateSubTable nRow:" + nRow + " | " + bMove);
        m_tSubLogTableModel.fireTableDataChanged();
        m_subLogScrollVPane.validate();
        getSubTable().invalidate();
        getSubTable().repaint();
        if (nRow >= 0)
            getSubTable().showRowCenterIfNotInRect(nRow, true);
    }

    DocumentListener m_dlFilterListener = new DocumentListener() {
        public void changedUpdate(DocumentEvent arg0) {
            try {
                if (arg0.getDocument().equals(m_tfFindWord.getDocument())
                        && m_chkEnableFind.isSelected()) {
                    getLogTable().setFilterFind(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                } else if (arg0.getDocument()
                        .equals(m_tfRemoveWord.getDocument())
                        && m_chkEnableRemove.isSelected()) {
                    getLogTable().SetFilterRemove(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                } else if (arg0.getDocument().equals(m_tfShowPid.getDocument())
                        && m_chkEnableShowPid.isSelected()) {
                    getLogTable().SetFilterShowPid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                } else if (arg0.getDocument().equals(m_tfShowTid.getDocument())
                        && m_chkEnableShowTid.isSelected()) {
                    getLogTable().SetFilterShowTid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                } else if (arg0.getDocument().equals(m_tfShowTag.getDocument())
                        && m_chkEnableShowTag.isSelected()) {
                    getLogTable().SetFilterShowTag(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                } else if (arg0.getDocument().equals(m_tfRemoveTag.getDocument())
                        && m_chkEnableRemoveTag.isSelected()) {
                    getLogTable().SetFilterRemoveTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                } else if (arg0.getDocument().equals(m_tfBookmarkTag.getDocument())
                        && m_chkEnableBookmarkTag.isSelected()) {
                    getLogTable().SetFilterBookmarkTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                } else if (arg0.getDocument().equals(m_tfHighlight.getDocument())
                        && m_chkEnableHighlight.isSelected()) {
                    getLogTable().SetHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getSubTable().SetHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                } else if (arg0.getDocument().equals(m_tfSearch.getDocument())) {
                    getLogTable().SetSearchHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getSubTable().SetSearchHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getLogTable().gotoNextSearchResult();
                } else if (arg0.getDocument().equals(m_tfFromTimeTag.getDocument())) {
                    getLogTable().SetFilterFromTime(m_tfFromTimeTag.getText());
                } else if (arg0.getDocument().equals(m_tfToTimeTag.getDocument())) {
                    getLogTable().SetFilterToTime(m_tfToTimeTag.getText());
                }
                m_nChangedFilter = STATUS_CHANGE;
                runFilter();
            } catch (Exception e) {
                T.e(e);
            }
        }

        public void insertUpdate(DocumentEvent arg0) {
            try {
                if (arg0.getDocument().equals(m_tfFindWord.getDocument())
                        && m_chkEnableFind.isSelected())
                    getLogTable().setFilterFind(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument()
                        .equals(m_tfRemoveWord.getDocument())
                        && m_chkEnableRemove.isSelected())
                    getLogTable().SetFilterRemove(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfShowPid.getDocument())
                        && m_chkEnableShowPid.isSelected())
                    getLogTable().SetFilterShowPid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfShowTid.getDocument())
                        && m_chkEnableShowTid.isSelected())
                    getLogTable().SetFilterShowTid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfShowTag.getDocument())
                        && m_chkEnableShowTag.isSelected())
                    getLogTable().SetFilterShowTag(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfRemoveTag.getDocument())
                        && m_chkEnableRemoveTag.isSelected())
                    getLogTable().SetFilterRemoveTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfBookmarkTag.getDocument())
                        && m_chkEnableBookmarkTag.isSelected())
                    getLogTable().SetFilterBookmarkTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfHighlight.getDocument())
                        && m_chkEnableHighlight.isSelected()) {
                    getLogTable().SetHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getSubTable().SetHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                } else if (arg0.getDocument().equals(m_tfSearch.getDocument())) {
                    getLogTable().SetSearchHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getSubTable().SetSearchHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getLogTable().gotoNextSearchResult();
                } else if (arg0.getDocument().equals(m_tfFromTimeTag.getDocument())) {
                    getLogTable().SetFilterFromTime(m_tfFromTimeTag.getText());
                } else if (arg0.getDocument().equals(m_tfToTimeTag.getDocument())) {
                    getLogTable().SetFilterToTime(m_tfToTimeTag.getText());
                }
                m_nChangedFilter = STATUS_CHANGE;
                runFilter();
            } catch (Exception e) {
                T.e(e);
            }
        }

        public void removeUpdate(DocumentEvent arg0) {
            try {
                if (arg0.getDocument().equals(m_tfFindWord.getDocument())
                        && m_chkEnableFind.isSelected())
                    getLogTable().setFilterFind(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument()
                        .equals(m_tfRemoveWord.getDocument())
                        && m_chkEnableRemove.isSelected())
                    getLogTable().SetFilterRemove(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfShowPid.getDocument())
                        && m_chkEnableShowPid.isSelected())
                    getLogTable().SetFilterShowPid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfShowTid.getDocument())
                        && m_chkEnableShowTid.isSelected())
                    getLogTable().SetFilterShowTid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfShowTag.getDocument())
                        && m_chkEnableShowTag.isSelected())
                    getLogTable().SetFilterShowTag(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfRemoveTag.getDocument())
                        && m_chkEnableRemoveTag.isSelected())
                    getLogTable().SetFilterRemoveTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfBookmarkTag.getDocument())
                        && m_chkEnableBookmarkTag.isSelected())
                    getLogTable().SetFilterBookmarkTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfHighlight.getDocument())
                        && m_chkEnableHighlight.isSelected()) {
                    getLogTable().SetHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getSubTable().SetHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                } else if (arg0.getDocument().equals(m_tfSearch.getDocument())) {
                    getLogTable().SetSearchHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getSubTable().SetSearchHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getLogTable().gotoNextSearchResult();
                } else if (arg0.getDocument().equals(m_tfFromTimeTag.getDocument())) {
                    getLogTable().SetFilterFromTime(m_tfFromTimeTag.getText());
                } else if (arg0.getDocument().equals(m_tfToTimeTag.getDocument())) {
                    getLogTable().SetFilterToTime(m_tfToTimeTag.getText());
                }
                m_nChangedFilter = STATUS_CHANGE;
                runFilter();
            } catch (Exception e) {
                T.e(e);
            }
        }
    };


    private void loadTableColumnState() {
        for (int nIndex = 0; nIndex < LogFilterTableModel.COMUMN_MAX; nIndex++) {
            LogFilterTableModel.setColumnWidth(nIndex, m_colWidths[nIndex]);
        }
        m_colWidths = LogFilterTableModel.ColWidth;

        getLogTable().showColumn(LogFilterTableModel.COMUMN_BOOKMARK,
                m_chkClmBookmark.isSelected());
        getLogTable().showColumn(LogFilterTableModel.COMUMN_LINE,
                m_chkClmLine.isSelected());
        getLogTable().showColumn(LogFilterTableModel.COMUMN_DATE,
                m_chkClmDate.isSelected());
        getLogTable().showColumn(LogFilterTableModel.COMUMN_TIME,
                m_chkClmTime.isSelected());
        getLogTable().showColumn(LogFilterTableModel.COMUMN_LOGLV,
                m_chkClmLogLV.isSelected());
        getLogTable().showColumn(LogFilterTableModel.COMUMN_PID,
                m_chkClmPid.isSelected());
        getLogTable().showColumn(LogFilterTableModel.COMUMN_THREAD,
                m_chkClmThread.isSelected());
        getLogTable().showColumn(LogFilterTableModel.COMUMN_TAG,
                m_chkClmTag.isSelected());
        getLogTable().showColumn(LogFilterTableModel.COMUMN_MESSAGE,
                m_chkClmMessage.isSelected());

        getSubTable().showColumn(LogFilterTableModel.COMUMN_BOOKMARK,
                m_chkClmBookmark.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COMUMN_LINE,
                m_chkClmLine.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COMUMN_DATE,
                m_chkClmDate.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COMUMN_TIME,
                m_chkClmTime.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COMUMN_LOGLV,
                m_chkClmLogLV.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COMUMN_PID,
                m_chkClmPid.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COMUMN_THREAD,
                m_chkClmThread.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COMUMN_TAG,
                m_chkClmTag.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COMUMN_MESSAGE,
                m_chkClmMessage.isSelected());
    }

    ItemListener m_itemListener = new ItemListener() {
        public void itemStateChanged(ItemEvent itemEvent) {
            JCheckBox check = (JCheckBox) itemEvent.getSource();

            if (check.equals(m_chkVerbose)) {
                setLogLV(LogInfo.LOG_LV_VERBOSE, check.isSelected());
            } else if (check.equals(m_chkDebug)) {
                setLogLV(LogInfo.LOG_LV_DEBUG, check.isSelected());
            } else if (check.equals(m_chkInfo)) {
                setLogLV(LogInfo.LOG_LV_INFO, check.isSelected());
            } else if (check.equals(m_chkWarn)) {
                setLogLV(LogInfo.LOG_LV_WARN, check.isSelected());
            } else if (check.equals(m_chkError)) {
                setLogLV(LogInfo.LOG_LV_ERROR, check.isSelected());
            } else if (check.equals(m_chkFatal)) {
                setLogLV(LogInfo.LOG_LV_FATAL, check.isSelected());
            } else if (check.equals(m_chkClmBookmark)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_BOOKMARK,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COMUMN_BOOKMARK,
                        check.isSelected());
            } else if (check.equals(m_chkClmLine)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_LINE,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COMUMN_LINE,
                        check.isSelected());
            } else if (check.equals(m_chkClmDate)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_DATE,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COMUMN_DATE,
                        check.isSelected());
            } else if (check.equals(m_chkClmTime)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_TIME,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COMUMN_TIME,
                        check.isSelected());
            } else if (check.equals(m_chkClmLogLV)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_LOGLV,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COMUMN_LOGLV,
                        check.isSelected());
            } else if (check.equals(m_chkClmPid)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_PID,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COMUMN_PID,
                        check.isSelected());
            } else if (check.equals(m_chkClmThread)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_THREAD,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COMUMN_THREAD,
                        check.isSelected());
            } else if (check.equals(m_chkClmTag)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_TAG,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COMUMN_TAG,
                        check.isSelected());
            } else if (check.equals(m_chkClmMessage)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_MESSAGE,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COMUMN_MESSAGE,
                        check.isSelected());
            } else if (check.equals(m_chkEnableFind)
                    || check.equals(m_chkEnableRemove)
                    || check.equals(m_chkEnableShowPid)
                    || check.equals(m_chkEnableShowTid)
                    || check.equals(m_chkEnableShowTag)
                    || check.equals(m_chkEnableRemoveTag)
                    || check.equals(m_chkEnableBookmarkTag)
                    || check.equals(m_chkEnableTimeTag)
                    || check.equals(m_chkEnableHighlight)) {
                useFilter(check);
            }
        }
    };

    private KeyEventDispatcher mKeyEventDispatcher = new KeyEventDispatcher() {
        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            if (!LogFilterMain.this.isFocused()) {
                return false;
            }
            boolean altPressed = ((e.getModifiers() & InputEvent.ALT_MASK) == InputEvent.ALT_MASK);
            boolean ctrlPressed = ((e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK);
            switch (e.getKeyCode()) {
                case KeyEvent.VK_F2:
                    if (e.isControlDown() && e.getID() == KeyEvent.KEY_PRESSED) {
                        int[] arSelectedRow = getLogTable().getSelectedRows();
                        for (int nIndex : arSelectedRow) {
                            LogInfo logInfo = m_tmLogTableModel.getRow(nIndex);
                            logInfo.setMarked(!logInfo.isMarked());
                            markLogInfo(nIndex, logInfo.getLine() - 1, logInfo.isMarked());
                        }
                        getLogTable().repaint();
                    } else if (!e.isControlDown() && e.getID() == KeyEvent.KEY_PRESSED)
                        getLogTable().gotoPreBookmark();
                    break;
                case KeyEvent.VK_F3:
                    if (e.getID() == KeyEvent.KEY_PRESSED)
                        getLogTable().gotoNextBookmark();
                    return false;
                case KeyEvent.VK_F:
                    if (e.getID() == KeyEvent.KEY_PRESSED && ctrlPressed) {
                        setSearchFocus();
                    }
                    break;
                case KeyEvent.VK_H:
                    if (e.getID() == KeyEvent.KEY_PRESSED && ctrlPressed) {
                        setHighLightFocus();
                    }
                    break;
                case KeyEvent.VK_W:
                    if (e.getID() == KeyEvent.KEY_PRESSED && ctrlPressed) {
                        setWordIncludeFocus();
                    }
                    break;
                case KeyEvent.VK_T:
                    if (e.getID() == KeyEvent.KEY_PRESSED && ctrlPressed) {
                        setTagIncludeFocus();
                    }
                    break;
                case KeyEvent.VK_B:
                    if (e.getID() == KeyEvent.KEY_PRESSED && ctrlPressed) {
                        setMainTableFocus();
                    }
                    break;
                case KeyEvent.VK_F5:
                    if (e.getID() == KeyEvent.KEY_PRESSED)
                        getLogTable().gotoNextSearchResult();
                    break;
                case KeyEvent.VK_F4:
                    if (e.getID() == KeyEvent.KEY_PRESSED)
                        getLogTable().gotoPreSearchResult();
                    break;
                case KeyEvent.VK_LEFT:
                    if (e.getID() == KeyEvent.KEY_PRESSED && altPressed)
                        getLogTable().historyBack();
                    break;
                case KeyEvent.VK_RIGHT:
                    if (e.getID() == KeyEvent.KEY_PRESSED && altPressed)
                        getLogTable().historyForward();
                    break;
            }
            return false;
        }
    };

    public void openFileBrowserToLoad(FileType type) {
        FileDialog fd = new FileDialog(this, "File open", FileDialog.LOAD);
        if (type == FileType.LOGFILE) {
            fd.setDirectory(m_strLastDir);
        }
        fd.setVisible(true);
        if (fd.getFile() != null) {
            switch (type) {
                case LOGFILE:
                    parseFile(new File(fd.getDirectory() + fd.getFile()));
                    m_recentMenu.addEntry(fd.getDirectory() + fd.getFile());
                    m_strLastDir = fd.getDirectory();
                    break;
                case MODEFILE:
                    loadModeFile(new File(fd.getDirectory() + fd.getFile()));
                    break;
            }
        }
    }

    private void openPackagesView() {
        String title = "packages";
        String deviceID = null;
        if (m_selectedDevice != null) {
            title = m_selectedDevice.toString();
            deviceID = m_selectedDevice.code;
        }
        PackageViewDialog packageViewDialog = new PackageViewDialog(this, title, deviceID, new PackageViewDialog.PackageViewDialogListener() {

            @Override
            public void onFliterPidSelected(String value) {
                String pidShow = m_tbLogTable.m_strPidShow;
                if (pidShow.contains("|" + value)) {
                    pidShow = pidShow.replace("|" + value, "");
                } else if (pidShow.contains(value)) {
                    pidShow = pidShow.replace(value, "");
                } else {
                    pidShow += "|" + value;
                }
                m_tbLogTable.m_strPidShow = pidShow;
                LogFilterMain.this.notiEvent(new INotiEvent.EventParam(INotiEvent.TYPE.EVENT_CHANGE_FILTER_SHOW_PID));
            }
        });
        packageViewDialog.setModal(false);
        packageViewDialog.setVisible(true);
    }

    private void openDumpsysView(String cmd) {
        String title = "Running Tasks";
        String deviceID = null;
        if (m_selectedDevice != null) {
            title = m_selectedDevice.toString();
            deviceID = m_selectedDevice.code;
        }
        DumpsysViewDialog dumpsysViewDialog = new DumpsysViewDialog(this, title, deviceID, cmd, new DumpsysViewDialog.DumpsysViewDialogListener() {


            @Override
            public void onRowSingleClick(String value) {

            }

            @Override
            public void onRowDoubleClick(String value) {

            }
        });
        dumpsysViewDialog.setModal(false);
        dumpsysViewDialog.setVisible(true);
    }

    private void openFileBrowserToSave(FileType type) {
        FileDialog fd = new FileDialog(this, "File save", FileDialog.SAVE);
        if (type != FileType.MODEFILE) {
            return;
        }
        fd.setVisible(true);
        if (fd.getFile() != null) {
            switch (type) {
                case MODEFILE:
                    saveModeFile(new File(fd.getDirectory() + fd.getFile()));
                    break;
            }
        }
    }

    private void saveModeFile(File file) {
        if (file == null) {
            T.e("mode file == null");
            return;
        }
        mStateSaver.save(file.getAbsolutePath());
    }

    private void loadModeFile(File file) {
        if (file == null) {
            T.e("mode file == null");
            return;
        }
        mStateSaver.load(file.getAbsolutePath());
    }

    public void refreshDiffMenuBar() {
        if (mDiffService.getDiffServiceType() == DiffService.DiffServiceType.AS_SERVER) {
            if (mDiffService.isDiffConnected()) {
                sConnectDiffMenuItem.setEnabled(false);
                sDisconnectDiffMenuItem.setEnabled(true);
            } else {
                sConnectDiffMenuItem.setEnabled(true);
                sDisconnectDiffMenuItem.setEnabled(false);
            }
        }
    }

    public void refreshUIWithDiffState() {
        if (!mDiffService.isDiffConnected()) {
            mSyncScrollCheckBox.setEnabled(false);
            mSyncSelectedCheckBox.setEnabled(false);
            m_tfDiffState.setBackground(null);
            m_tfDiffState.setText("disconnected");
        } else {
            mSyncScrollCheckBox.setEnabled(true);
            mSyncSelectedCheckBox.setEnabled(true);
            m_tfDiffState.setBackground(Color.GREEN);
            switch (mDiffService.getDiffServiceType()) {
                case AS_CLIENT:
                    m_tfDiffState.setText("as client");
                    break;
                case AS_SERVER:
                    m_tfDiffState.setText("as server");
                    break;
            }
        }
    }

    private void initDiffService() {
        int port = 20000 + new Random().nextInt(10000);
        m_tfDiffPort.setText("port: " + port);
        mDiffService = new DiffService(this, port);
        m_tbLogTable.setDiffService(mDiffService);
    }

    public LogTable getLogTable() {
        return m_tbLogTable;
    }

    public SubLogTable getSubTable() {
        return m_tSublogTable;
    }

    public void searchSimilar(String cmd) {
        m_tbLogTable.searchSimilarForward(cmd);
    }

    public void compareWithSelectedRows(String targetRows) {
        String fmtRows = m_tbLogTable.getFormatSelectedRows(
                new int[]{LogFilterTableModel.COMUMN_LINE, LogFilterTableModel.COMUMN_TIME}
        );
        if (fmtRows == null || fmtRows.length() == 0) {
            return;
        }

        try {
            File tempFile1 = File.createTempFile("target", ".txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile1));
            bw.write(targetRows);
            bw.close();

            File tempFile2 = File.createTempFile("src", ".txt");
            bw = new BufferedWriter(new FileWriter(tempFile2));
            bw.write(fmtRows);
            bw.close();

            Utils.runCmd(new String[]{DIFF_PROGRAM_PATH, tempFile1.getAbsolutePath(), tempFile2.getAbsolutePath()});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int mLastVBarValue = 0;
    private AdjustmentListener mScrollListener = new AdjustmentListener() {
        @Override
        public void adjustmentValueChanged(AdjustmentEvent e) {
            JScrollBar scrollBar = (JScrollBar) e.getSource();
            if (scrollBar == m_logScrollVPane.getHorizontalScrollBar()) {
                T.d("HorizontalScrollBar: " + scrollBar.getValue());
            } else if (scrollBar == m_logScrollVPane.getVerticalScrollBar()) {
                mDiffService.writeDiffCommand(
                        DiffService.DiffServiceCmdType.SYNC_SCROLL_V,
                        String.valueOf(scrollBar.getValue() - mLastVBarValue)
                );
                mLastVBarValue = scrollBar.getValue();
            }
        }
    };

    public void enableSyncScroll(boolean enable) {
        mSyncScrollEnable = enable;
        if (mSyncScrollEnable) {
//            m_logScrollVPane.getHorizontalScrollBar().addAdjustmentListener(mScrollListener);
            m_logScrollVPane.getVerticalScrollBar().addAdjustmentListener(mScrollListener);
        } else {
//            m_logScrollVPane.getHorizontalScrollBar().removeAdjustmentListener(mScrollListener);
            m_logScrollVPane.getVerticalScrollBar().removeAdjustmentListener(mScrollListener);
        }
    }


    private void enableSyncSelected(boolean enable) {
        mSyncScrollSelected = enable;
    }

    public void handleScrollVSyncEvent(String cmd) {
        JScrollBar scrollBar = m_logScrollVPane.getVerticalScrollBar();
        int scrollChanged = Integer.valueOf(cmd);
        int newValue = scrollBar.getValue() + scrollChanged;

        if (newValue >= scrollBar.getMinimum() && newValue <= scrollBar.getMaximum())
            scrollBar.setValue(newValue);
    }

    public void onSelectedRowChanged(int lastRowIndex, int rowIndex, LogInfo logInfo) {
        if (mSyncScrollSelected) {
            if (lastRowIndex > rowIndex) {
                mDiffService.writeDiffCommand(
                        DiffService.DiffServiceCmdType.SYNC_SELECTED_BACKWARD,
                        logInfo.getMessage()
                );
            } else {
                mDiffService.writeDiffCommand(
                        DiffService.DiffServiceCmdType.SYNC_SELECTED_FORWARD,
                        logInfo.getMessage()
                );
            }
        }
    }

    public void handleSelectedForwardSyncEvent(String cmd) {
        m_tbLogTable.searchSimilarForward(cmd);
    }

    public void handleSelectedBackwardSyncEvent(String cmd) {
        m_tbLogTable.searchSimilarBackward(cmd);
    }

    private enum FileType {
        LOGFILE, MODEFILE
    }

    public class TargetDevice {
        String code;
        String product;
        String model;
        String device;

        public TargetDevice(String src) {
            src = src.replace("\t", " ");
            int codeIdx = src.indexOf(' ');
            if (codeIdx == -1) {
                this.code = src;
                return;
            }

            this.code = src.substring(0, codeIdx);
            int infoIdx = src.indexOf("product:");
            if (infoIdx != -1) {
                String infoStr = src.substring(infoIdx);
                String[] infos = infoStr.split("\\s+");
                this.product = infos[0].substring("product:".length());
                this.model = infos[1].substring("model:".length());
                this.device = infos[2].substring("device:".length());
            }
        }

        @Override
        public String toString() {
            return "[" + this.model + "]" + this.code;
        }
    }
}


