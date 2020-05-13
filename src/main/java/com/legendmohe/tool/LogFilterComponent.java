package com.legendmohe.tool;

import com.legendmohe.tool.annotation.CheckBoxSaveState;
import com.legendmohe.tool.annotation.FieldSaveState;
import com.legendmohe.tool.annotation.TextFieldSaveState;
import com.legendmohe.tool.annotation.UIStateSaver;
import com.legendmohe.tool.config.Constant;
import com.legendmohe.tool.config.ThemeConstant;
import com.legendmohe.tool.diff.DiffService;
import com.legendmohe.tool.logflow.LogFlowManager;
import com.legendmohe.tool.logtable.BaseLogTable;
import com.legendmohe.tool.logtable.LogTable;
import com.legendmohe.tool.logtable.SubLogTable;
import com.legendmohe.tool.logtable.model.LogFilterTableModel;
import com.legendmohe.tool.parser.AbstractLogParser;
import com.legendmohe.tool.parser.BigoDevLogParser;
import com.legendmohe.tool.parser.BigoIOSDevLogParser;
import com.legendmohe.tool.parser.BigoXLogParser;
import com.legendmohe.tool.parser.DefaultLogParser;
import com.legendmohe.tool.parser.ILogParser;
import com.legendmohe.tool.parser.IMODevLogParser;
import com.legendmohe.tool.parser.LogCatParser;
import com.legendmohe.tool.thirdparty.Debouncer;
import com.legendmohe.tool.view.DumpsysViewDialog;
import com.legendmohe.tool.view.ListDialog;
import com.legendmohe.tool.view.LogFlowDialog;
import com.legendmohe.tool.view.PackageViewDialog;
import com.legendmohe.tool.view.TextContentDialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
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
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.OverlayLayout;
import javax.swing.ScrollPaneConstants;
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
import javax.swing.table.AbstractTableModel;

public class LogFilterComponent extends JComponent implements EventBus, BaseLogTable.BaseLogTableListener, IDiffCmdHandler, LogFilterFrame.CloseableTab {
    private static final long serialVersionUID = 1L;

    private static final Map<Integer, ILogParser> sTypeToParserMap = new HashMap<>();
    private static final Map<Integer, String> sTypeToParserNameMap = new HashMap<>();
    private List<FloatingFrameInfo> mFloatingFrameInfos = new ArrayList<>();

    {
        sTypeToParserMap.put(Constant.PARSER_TYPE_LOGCAT, new LogCatParser());
        sTypeToParserMap.put(Constant.PARSER_TYPE_BIGO_DEV_LOG, new BigoDevLogParser());
        sTypeToParserMap.put(Constant.PARSER_TYPE_BIGO_XLOG, new BigoXLogParser());
        sTypeToParserMap.put(Constant.PARSER_TYPE_IMO_DEV_LOG, new IMODevLogParser());
        sTypeToParserMap.put(Constant.PARSER_TYPE_IOS_DEV_LOG, new BigoIOSDevLogParser());
        sTypeToParserMap.put(Constant.PARSER_TYPE_DEFAULT_LOG, new DefaultLogParser());

        sTypeToParserNameMap.put(Constant.PARSER_TYPE_LOGCAT, "logcat");
        sTypeToParserNameMap.put(Constant.PARSER_TYPE_BIGO_DEV_LOG, "bigo dev log");
        sTypeToParserNameMap.put(Constant.PARSER_TYPE_BIGO_XLOG, "bigo xlog");
        sTypeToParserNameMap.put(Constant.PARSER_TYPE_IMO_DEV_LOG, "imo dev log");
        sTypeToParserNameMap.put(Constant.PARSER_TYPE_IOS_DEV_LOG, "ios dev log");
        sTypeToParserNameMap.put(Constant.PARSER_TYPE_DEFAULT_LOG, "default");
    }

    LogFilterFrame.FrameInfoProvider frameInfoProvider;
    LogFlowManager logFlowManager = new LogFlowManager();
    JProgressBar m_progressLoading;
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
    boolean mFilterEnabled;

    // Word Filter, tag filter
    JTextField m_tfSearch;
    JTextField m_tfHighlight;
    @TextFieldSaveState
    JTextField m_tfIncludeWord;
    @TextFieldSaveState
    JTextField m_tfExcludeWord;
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
    @TextFieldSaveState
    JTextField m_tfFontSize;
    @TextFieldSaveState
    JTextField m_tfShowFileName;

    private JTextField m_tfFromTimeTag;
    private JTextField m_tfToTimeTag;
    private JTextField m_tfGoto;

    // Device
    JButton m_btnDevice;
    JList<TargetDevice> m_lDeviceList;
    JComboBox<String> m_comboDeviceCmd;
    JComboBox<String> m_comboCmd;
    JButton m_btnSetFont;

    // Log filter enable/disable
    @CheckBoxSaveState
    JCheckBox m_chkEnableIncludeWord;
    @CheckBoxSaveState
    JCheckBox m_chkEnableExcludeWord;
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
    @CheckBoxSaveState
    JCheckBox m_chkEnableLogFlowTag;
    @CheckBoxSaveState
    JCheckBox m_chkEnableFileNameFilter;

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
    @CheckBoxSaveState
    JCheckBox m_chkClmFile;

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
    volatile int mLogParsingState;
    int m_nFilterLogLV;

    @FieldSaveState
    int m_parserType = Constant.PARSER_TYPE_DEFAULT_LOG;

    @FieldSaveState
    int[] m_colWidths = LogFilterTableModel.DEFAULT_WIDTH;

    @FieldSaveState
    private String m_strLastDir;

    private JMenuItem mDisconnectDiffMenuItem;
    private JMenuItem mConnectDiffMenuItem;

    public DiffService mDiffService;
    private JLabel m_tfDiffPort;
    private JLabel m_tfDiffState;
    private JLabel m_tfParserType;
    private JCheckBox mSyncScrollCheckBox;
    private JCheckBox mSyncSelectedCheckBox;
    private boolean mSyncScrollEnable;
    private boolean mSyncScrollSelected;

    private final UIStateSaver mUIStateSaver;
    private JSplitPane mLogSplitPane;
    @FieldSaveState
    private int mLogSplitPaneDividerLocation = -1;
    private JSplitPane mMainSplitPane;
    @FieldSaveState
    private int mMainSplitPaneDividerLocation = -1;

    private SubLogTable m_tSublogTable;
    private LogFilterTableModel m_tSubLogTableModel;
    private JScrollPane m_subLogScrollVPane;
    ArrayList<LogInfo> m_arSubLogInfoAll;
    private JPanel mSearchPanel;

    @FieldSaveState
    private final Set<String> mFilterTagHistory = new HashSet<>();

    // 当前处理的文件集合
    private File[] mLastParseredFiles;
    private String mCurTitle = "";

    private final Map<Component, List<String>> mRecentlyInputHistory = new HashMap<>();


    ///////////////////////////////////constructor///////////////////////////////////

    public LogFilterComponent(LogFilterFrame.FrameInfoProvider frameInfoProvider) {
        super();
        this.frameInfoProvider = frameInfoProvider;
        initValue();

        JMenuBar jMenuBar = setupMenuBar();

        setLayout(new BorderLayout());
        add(createMainSplitPane(), BorderLayout.CENTER);
        add(createStatusPanel(), BorderLayout.SOUTH);
        add(jMenuBar, BorderLayout.NORTH);

        setDnDListener();
        addChangeListener();
        addUndoListener();
        bindRecentlyPopup();
        startFilterParse();

        // register state saver
        mUIStateSaver = new UIStateSaver(this, Constant.INI_FILE_STATE);
        mUIStateSaver.load();

        loadUI();
        loadCmd();
        initLogFlow();

        loadParser();
        addDesc();

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(mKeyEventDispatcher);

    }

    ///////////////////////////////////setup process///////////////////////////////////

    private JMenuBar setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Conf");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenu modeMenu = new JMenu("Mode");

        JMenuItem modeOpen = new JMenuItem("Open Mode File");
        modeOpen.setToolTipText("Open .mode file");
        modeOpen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                LogFilterComponent.this.openFileBrowserToLoad(FileType.MODE);
            }
        });
        JMenuItem modeSave = new JMenuItem("Save Mode File");
        modeSave.setToolTipText("Save .mode file");
        modeSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                LogFilterComponent.this.openFileBrowserToSave(FileType.MODE);
            }
        });

        modeMenu.add(modeOpen);
        modeMenu.add(modeSave);

        fileMenu.add(modeMenu);

        JMenu toolsMenu = new JMenu("Tools");

        JMenu viewMenu = initAndGetViewMenu(this);
        toolsMenu.add(viewMenu);

        JMenu diffMenu = new JMenu("Diff Service");

        JCheckBoxMenuItem enableDiffServer = new JCheckBoxMenuItem("enable diff server");
        enableDiffServer.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (enableDiffServer.getState()) {
                    m_tfDiffPort.setVisible(true);
                    mConnectDiffMenuItem.setEnabled(true);
                    if (mDiffService == null) {
                        initDiffService();
                    }
                } else {
                    LogFilterComponent.this.mDiffService.disconnectDiffClient();
                    m_tfDiffPort.setVisible(false);
                    mConnectDiffMenuItem.setEnabled(false);
                    mDisconnectDiffMenuItem.setEnabled(false);
                }
            }
        });
        enableDiffServer.setState(false);

        mDisconnectDiffMenuItem = new JMenuItem("disconnect");
        mDisconnectDiffMenuItem.setEnabled(false);
        mConnectDiffMenuItem = new JMenuItem("connect to diff server");
        mConnectDiffMenuItem.setEnabled(false);

        mDisconnectDiffMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                LogFilterComponent.this.mDiffService.disconnectDiffClient();
                mConnectDiffMenuItem.setEnabled(true);
                mDisconnectDiffMenuItem.setEnabled(false);
            }
        });
        mConnectDiffMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (frameInfoProvider.getContainerFrame() != null) {
                    String serverPort = JOptionPane.showInputDialog(
                            frameInfoProvider.getContainerFrame(),
                            "Enter Server Port",
                            "",
                            JOptionPane.QUESTION_MESSAGE
                    );
                    if (serverPort != null && serverPort.length() != 0) {
                        if (LogFilterComponent.this.mDiffService.setupDiffClient(serverPort)) {
                            mConnectDiffMenuItem.setEnabled(false);
                            mDisconnectDiffMenuItem.setEnabled(true);
                        }
                    }
                }
            }
        });

        diffMenu.add(enableDiffServer);
        diffMenu.add(mConnectDiffMenuItem);
        diffMenu.add(mDisconnectDiffMenuItem);

        toolsMenu.add(diffMenu);

        JMenu parserMenu = new JMenu("Parser");
        parserMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                JMenuItem menuItem = parserMenu.getItem(LogFilterComponent.this.m_parserType);
                menuItem.setSelected(true);
            }

            @Override
            public void menuDeselected(MenuEvent e) {

            }

            @Override
            public void menuCanceled(MenuEvent e) {

            }
        });

        JRadioButtonMenuItem defaultLogParserMenu = new JRadioButtonMenuItem("DefaultLog Parser", LogFilterComponent.this.m_parserType == Constant.PARSER_TYPE_DEFAULT_LOG);
        defaultLogParserMenu.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    LogFilterComponent.this.switchToLogParser(Constant.PARSER_TYPE_DEFAULT_LOG);
                }
            }
        });
        parserMenu.add(defaultLogParserMenu);

        JRadioButtonMenuItem logcatParserMenu = new JRadioButtonMenuItem("Logcat Parser", LogFilterComponent.this.m_parserType == Constant.PARSER_TYPE_LOGCAT);
        logcatParserMenu.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    LogFilterComponent.this.switchToLogParser(Constant.PARSER_TYPE_LOGCAT);
                }
            }
        });
        parserMenu.add(logcatParserMenu);

        JRadioButtonMenuItem bigoParserMenu = new JRadioButtonMenuItem("BigoDevLog Parser", LogFilterComponent.this.m_parserType == Constant.PARSER_TYPE_BIGO_DEV_LOG);
        bigoParserMenu.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    LogFilterComponent.this.switchToLogParser(Constant.PARSER_TYPE_BIGO_DEV_LOG);
                }
            }
        });
        parserMenu.add(bigoParserMenu);

        JRadioButtonMenuItem bigoXLogParserMenu = new JRadioButtonMenuItem("BigoXLog Parser", LogFilterComponent.this.m_parserType == Constant.PARSER_TYPE_BIGO_XLOG);
        bigoXLogParserMenu.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    LogFilterComponent.this.switchToLogParser(Constant.PARSER_TYPE_BIGO_XLOG);
                }
            }
        });
        parserMenu.add(bigoXLogParserMenu);

        JRadioButtonMenuItem imoDevLogParserMenu = new JRadioButtonMenuItem("IMODevLog Parser", LogFilterComponent.this.m_parserType == Constant.PARSER_TYPE_IMO_DEV_LOG);
        imoDevLogParserMenu.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    LogFilterComponent.this.switchToLogParser(Constant.PARSER_TYPE_IMO_DEV_LOG);
                }
            }
        });
        parserMenu.add(imoDevLogParserMenu);

        JRadioButtonMenuItem iosDevLogParserMenu = new JRadioButtonMenuItem("IOSDevLog Parser", LogFilterComponent.this.m_parserType == Constant.PARSER_TYPE_IOS_DEV_LOG);
        iosDevLogParserMenu.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    LogFilterComponent.this.switchToLogParser(Constant.PARSER_TYPE_IOS_DEV_LOG);
                }
            }
        });
        parserMenu.add(iosDevLogParserMenu);

        // 就这样放进去就可以了。。。
        ButtonGroup parserBG = new ButtonGroup();
        parserBG.add(defaultLogParserMenu);
        parserBG.add(logcatParserMenu);
        parserBG.add(bigoParserMenu);
        parserBG.add(bigoXLogParserMenu);
        parserBG.add(imoDevLogParserMenu);
        parserBG.add(iosDevLogParserMenu);

        JMenu flowMenu = new JMenu("Flow");
        JMenuItem showAllFlow = new JMenuItem("show all log flow");
        showAllFlow.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                LogFilterComponent.this.showAllFlow();
            }
        });

        JCheckBoxMenuItem showFlowInLogTable = new JCheckBoxMenuItem("show log flow in line");
        showFlowInLogTable.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                handleShowFlowInLogTableStateChanged(showFlowInLogTable.getState());
            }
        });
        showFlowInLogTable.setState(mShowFlowInLogTable);

        flowMenu.add(showFlowInLogTable);
        flowMenu.add(showAllFlow);

        if (frameInfoProvider.getContainerFrame() != null) {
            menuBar.add(fileMenu);
            menuBar.add(toolsMenu);
            menuBar.add(flowMenu);
        }
        menuBar.add(parserMenu);
        return menuBar;
    }

    private JMenu initAndGetViewMenu(final LogFilterComponent mainFrame) {
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

    private void loadCustomMenu(final LogFilterComponent mainFrame, JMenu customMenu) {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(Constant.INI_FILE_DUMP_SYS));
        } catch (FileNotFoundException e) {
            T.d(Constant.INI_FILE_DUMP_SYS + " not exist!");
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

    public void restoreSplitPane() {
        mLogSplitPane.setResizeWeight(1.0);
        mLogSplitPane.setOneTouchExpandable(true);
        if (mLogSplitPaneDividerLocation > 0) {
            mLogSplitPane.setDividerLocation(mLogSplitPaneDividerLocation);
        }

        mMainSplitPane.setResizeWeight(1.0);
        mMainSplitPane.setOneTouchExpandable(true);
        if (mMainSplitPaneDividerLocation > 0) {
            mMainSplitPane.setDividerLocation(mMainSplitPaneDividerLocation);
        }
    }

    private String makeFilename() {
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return Constant.OUTPUT_LOG_DIR + File.separator + "LogFilter_" + format.format(now) + ".txt";
    }

    public void exit() {
        if (m_Process != null)
            m_Process.destroy();
        if (m_thProcess != null)
            m_thProcess.interrupt();
        if (m_thWatchFile != null)
            m_thWatchFile.interrupt();
        if (m_thFilterParse != null)
            m_thFilterParse.interrupt();

        if (mLogSplitPane.getDividerLocation() > 1) {
            mLogSplitPaneDividerLocation = mLogSplitPane.getDividerLocation();
        }
        if (mMainSplitPane.getDividerLocation() > 1) {
            mMainSplitPaneDividerLocation = mMainSplitPane.getDividerLocation();
        }
        mUIStateSaver.save();
    }

    private void loadUI() {

        loadTableColumnState();

        if (m_tfFontSize.getText().length() > 0) {
            getLogTable().setFontSize(Integer.parseInt(m_tfFontSize
                    .getText()));
            getSubTable().setFontSize(Integer.parseInt(m_tfFontSize
                    .getText()));
        }

        updateLogTable(-1, false);
    }

    void loadCmd() {
        try {
            Properties p = new Properties();

            try {
                p.load(new FileInputStream(Constant.INI_FILE_CMD));
            } catch (FileNotFoundException e) {
                T.d(Constant.INI_FILE_CMD + " not exist!");
            }

            if (p.getProperty(Constant.INI_CMD_COUNT) == null) {
                p.setProperty(Constant.INI_CMD + "0", Constant.ANDROID_THREAD_CMD);
                p.setProperty(Constant.INI_CMD + "1", Constant.ANDROID_DEFAULT_CMD);
                p.setProperty(Constant.INI_CMD + "2", Constant.ANDROID_RADIO_CMD);
                p.setProperty(Constant.INI_CMD + "3", Constant.ANDROID_EVENT_CMD);
                p.setProperty(Constant.INI_CMD + "4", Constant.ANDROID_CUSTOM_CMD);
                p.setProperty(Constant.INI_CMD_COUNT, "5");
                p.store(new FileOutputStream(Constant.INI_FILE_CMD), null);
            }

            T.d("p.getProperty(INI_CMD_COUNT) = "
                    + p.getProperty(Constant.INI_CMD_COUNT));
            int nCount = Integer.parseInt(p.getProperty(Constant.INI_CMD_COUNT));
            T.d("nCount = " + nCount);
            for (int nIndex = 0; nIndex < nCount; nIndex++) {
                T.d("CMD = " + Constant.INI_CMD + nIndex);
                m_comboCmd.addItem(p.getProperty(Constant.INI_CMD + nIndex));
            }
        } catch (Exception e) {
            T.e(e.getMessage());
        }
    }

    private void loadParser() {
        switchToLogParser(m_parserType);
    }


    /*
            // F2 上一个标签 F3 下一个标签
            // ctrl + F2 标记行（可多选）
            // ctrl + F 搜索关键词
            // F4 上一个搜索结果 F5 下一个搜索结果
            // ctrl + H 高亮关键词
            // ctrl + W 过滤msg关键词
            // ctrl + T 过滤tag关键词
            // ctrl + B 聚焦到log table
            // alt + 左箭头 上一个历史行 alt + 右箭头 下一个历史行
     */
    void addDesc() {
        appendDescToTable(Constant.VERSION);
        appendDescToTable("");
        appendDescToTable("Xinyu.he fork from https://github.com/iookill/LogFilter");
        appendDescToTable("");
        appendDescToTable("<Hot key>");
        appendDescToTable("F2 上一个标签 F3 下一个标签");
        appendDescToTable("ctrl + F2 标记行（可多选）");
        appendDescToTable("ctrl + F 搜索关键词");
        appendDescToTable("F4 上一个搜索结果 F5 下一个搜索结果");
        appendDescToTable("trl + H 高亮关键词");
        appendDescToTable("ctrl + W 过滤msg关键词");
        appendDescToTable("ctrl + T 过滤tag关键词");
        appendDescToTable("ctrl + B 聚焦到log table");
        appendDescToTable("alt + 左箭头 上一个历史行 alt + 右箭头 下一个历史行");
    }

    private void appendDescToTable(String strMessage) {
        LogInfo logInfo = new LogInfo();
        logInfo.setLine(m_arLogInfoAll.size() + 1);
        logInfo.setMessage(strMessage);
        m_arLogInfoAll.add(logInfo);
    }

    public void markLogInfo(int nIndex, int line, boolean isMark) {
        synchronized (FILTER_LOCK) {
            LogInfo logInfo = m_arLogInfoAll.get(line);
            logInfo.setMarked(isMark);
            m_arLogInfoAll.set(line, logInfo);

            if (logInfo.isMarked()) {
                m_arSubLogInfoAll.add(logInfo);
                m_hmMarkedInfoAll.put(line, line);
                if (mFilterEnabled)
                    m_hmMarkedInfoFiltered.put(line, m_arLogInfoFiltered.size());
            } else {
                m_arSubLogInfoAll.remove(logInfo);
                m_hmMarkedInfoAll.remove(line);
                if (mFilterEnabled)
                    m_hmMarkedInfoFiltered.remove(line);
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

    @Override
    public void showInMarkTable(int selectedRow, int line) {
        synchronized (FILTER_LOCK) {
            LogInfo target = m_arLogInfoAll.get(line);
            getSubTable().changeSelection(target);
        }
    }

    @Override
    public void showRowsContent(String content) {
        if (content != null && content.length() > 0) {
            openShowRowContentDialog(content);
        }
    }

    @Override
    public int[] getSupportedColumns() {
        if (m_iLogParser == null) {
            return AbstractLogParser.gDefColumns;
        }
        return m_iLogParser.getSupportedColumns();
    }

    @Override
    public LogFlowManager getLogFlowManager() {
        return logFlowManager;
    }

    void clearData() {
        m_arSubLogInfoAll.clear();
        m_arLogInfoAll.clear();
        m_arLogInfoFiltered.clear();
        m_hmMarkedInfoAll.clear();
        m_hmMarkedInfoFiltered.clear();
        m_hmErrorAll.clear();
        m_hmErrorFiltered.clear();
        mLastProcessFlowLine = -1;
        logFlowManager.reset();
    }

    Component createIndicatorPanel() {
        JPanel jp = new JPanel();
        jp.setLayout(new BorderLayout());

        m_ipIndicator = new IndicatorPanel(this);
        m_ipIndicator.setData(m_arLogInfoAll, m_hmMarkedInfoAll, m_hmErrorAll);
        jp.add(m_ipIndicator, BorderLayout.CENTER);
        return jp;
    }

    Component createDevicePanel() {
        JPanel jpOptionDevice = new JPanel();
        jpOptionDevice.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        jpOptionDevice.setLayout(new BorderLayout());

        JPanel jpCmd = new JPanel();
        m_comboDeviceCmd = new JComboBox<String>();
        m_comboDeviceCmd.addItem(Constant.COMBO_ANDROID);
        m_comboDeviceCmd.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED)
                    return;

                DefaultListModel listModel = (DefaultListModel) m_lDeviceList
                        .getModel();
                listModel.clear();
                m_comboDeviceCmd.setEditable(e.getItem().equals(Constant.COMBO_CUSTOM_COMMAND));
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
        vbar.setBorder(BorderFactory.createEmptyBorder());
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
            // 实时显示log flow
            if (mShowFlowInLogTable) {
                appendFlowLogAndSetLogState(logInfo);
            }

            if (logInfo.getLogLV().equals("E")
                    || logInfo.getLogLV().equals("ERROR"))
                m_hmErrorAll.put(logInfo.getLine() - 1,
                        logInfo.getLine() - 1);

            if (mFilterEnabled) {
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
                        && checkBookmarkFilter(logInfo)
                        && checkLogFlowFilter(logInfo)
                        && checkFileNameFilter(logInfo)
                ) {
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
        m_tfSearch.getDocument().addDocumentListener(mFilterListener);
        m_tfHighlight.getDocument().addDocumentListener(mFilterListener);
        m_tfIncludeWord.getDocument().addDocumentListener(mFilterListener);
        m_tfExcludeWord.getDocument().addDocumentListener(mFilterListener);
        m_tfShowTag.getDocument().addDocumentListener(mFilterListener);
        m_tfRemoveTag.getDocument().addDocumentListener(mFilterListener);
        m_tfBookmarkTag.getDocument().addDocumentListener(mFilterListener);
        m_tfShowPid.getDocument().addDocumentListener(mFilterListener);
        m_tfShowTid.getDocument().addDocumentListener(mFilterListener);
        m_tfShowFileName.getDocument().addDocumentListener(mFilterListener);
        m_tfFromTimeTag.getDocument().addDocumentListener(mFilterListener);
        m_tfToTimeTag.getDocument().addDocumentListener(mFilterListener);

        m_chkEnableIncludeWord.addItemListener(m_itemListener);
        m_chkEnableExcludeWord.addItemListener(m_itemListener);
        m_chkEnableShowPid.addItemListener(m_itemListener);
        m_chkEnableShowTid.addItemListener(m_itemListener);
        m_chkEnableShowTag.addItemListener(m_itemListener);
        m_chkEnableRemoveTag.addItemListener(m_itemListener);
        m_chkEnableBookmarkTag.addItemListener(m_itemListener);
        m_chkEnableLogFlowTag.addItemListener(m_itemListener);
        m_chkEnableFileNameFilter.addItemListener(m_itemListener);
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
        m_chkClmFile.addItemListener(m_itemListener);

        m_logScrollVPane.getViewport().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                // m_ipIndicator.m_bDrawFull = false;
                frameInfoProvider.onViewPortChanged(LogFilterComponent.this, e);
                m_ipIndicator.repaint();
            }
        });
    }

    private void addUndoListener() {
        Utils.makeUndoable(m_tfSearch);
        Utils.makeUndoable(m_tfHighlight);
        Utils.makeUndoable(m_tfIncludeWord);
        Utils.makeUndoable(m_tfExcludeWord);
        Utils.makeUndoable(m_tfShowTag);
        Utils.makeUndoable(m_tfRemoveTag);
        Utils.makeUndoable(m_tfShowPid);
        Utils.makeUndoable(m_tfShowTid);
        Utils.makeUndoable(m_tfBookmarkTag);
        Utils.makeUndoable(m_tfFontSize);
        Utils.makeUndoable(m_tfShowFileName);
        Utils.makeUndoable(m_tfGoto);
        Utils.makeUndoable(m_tfToTimeTag);
        Utils.makeUndoable(m_tfFromTimeTag);
    }

    private void bindRecentlyPopup() {
        bindHistoryInput(m_tfSearch);
        bindHistoryInput(m_tfHighlight);
        bindHistoryInput(m_tfIncludeWord);
        bindHistoryInput(m_tfExcludeWord);
        bindHistoryInput(m_tfShowTag);
        bindHistoryInput(m_tfRemoveTag);
        bindHistoryInput(m_tfShowPid);
        bindHistoryInput(m_tfShowTid);
        bindHistoryInput(m_tfBookmarkTag);
        bindHistoryInput(m_tfFontSize);
        bindHistoryInput(m_tfShowFileName);
        bindHistoryInput(m_tfGoto);
        bindHistoryInput(m_tfToTimeTag);
        bindHistoryInput(m_tfFromTimeTag);
    }

    /*
    添加输入历史功能
     */
    private void bindHistoryInput(JTextField textField) {
        if (!mRecentlyInputHistory.containsKey(textField)) {
            mRecentlyInputHistory.put(textField, new ArrayList<>());
        }
        // build poup menu
        final JPopupMenu popup = new JPopupMenu();
        textField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!popup.isVisible()) {
                    List<String> historyInputList = mRecentlyInputHistory.get(textField);
                    if (historyInputList == null || historyInputList.size() <= 0)
                        return;
                    popup.removeAll();
                    for (String his : historyInputList) {
                        // New project menu item
                        JMenuItem menuItem = new JMenuItem(his);
                        menuItem.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent e) {
                                historyInputList.remove(his);
                                historyInputList.add(0, his);
                                textField.setText(his);
                                popup.setVisible(false);
                            }
                        });
                        popup.add(menuItem);
                    }
                    popup.show(textField,
                            0, textField.getHeight());
                    textField.requestFocus();
                }
            }
        });

        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                List<String> historyInputList = mRecentlyInputHistory.get(textField);
                if (historyInputList == null)
                    return;
                if (historyInputList.size() > 0 && !popup.isVisible()) {
                    return;
                }
                String newContent = textField.getText();
                if (newContent != null && newContent.length() > 0 && !historyInputList.contains(newContent)) {
                    historyInputList.remove(newContent);
                    historyInputList.add(0, newContent);
                }
            }
        });
    }

    Component createFilterPanel() {
        m_chkEnableIncludeWord = new JCheckBox();
        m_chkEnableExcludeWord = new JCheckBox();
        m_chkEnableShowTag = new JCheckBox();
        m_chkEnableRemoveTag = new JCheckBox();
        m_chkEnableShowPid = new JCheckBox();
        m_chkEnableShowTid = new JCheckBox();
        m_chkEnableBookmarkTag = new JCheckBox();
        m_chkEnableLogFlowTag = new JCheckBox();
        m_chkEnableFileNameFilter = new JCheckBox();
        m_chkEnableTimeTag = new JCheckBox();
        m_chkEnableIncludeWord.setSelected(true);
        m_chkEnableExcludeWord.setSelected(true);
        m_chkEnableShowTag.setSelected(true);
        m_chkEnableRemoveTag.setSelected(true);
        m_chkEnableShowPid.setSelected(true);
        m_chkEnableShowTid.setSelected(true);
        m_chkEnableBookmarkTag.setSelected(false);
        m_chkEnableLogFlowTag.setSelected(false);
        m_chkEnableFileNameFilter.setSelected(false);
        m_chkEnableTimeTag.setSelected(true);

        m_tfIncludeWord = new JTextField();
        m_tfExcludeWord = new JTextField();
        m_tfShowTag = new JTextField();
        m_tfRemoveTag = new JTextField();
        m_tfShowPid = new JTextField();
        m_tfShowTid = new JTextField();
        m_tfShowFileName = new JTextField();
        m_tfBookmarkTag = new JTextField();
        m_tfFromTimeTag = new JTextField();
        m_tfToTimeTag = new JTextField();

        JPanel jpMain = new JPanel(new BorderLayout());

        JPanel jpWordFilter = new JPanel();
        jpWordFilter.setBorder(BorderFactory.createTitledBorder("Word filter"));
        jpWordFilter.setLayout(new BoxLayout(jpWordFilter, BoxLayout.Y_AXIS));

        JPanel jpInclide = new JPanel(new BorderLayout());
        JLabel find = new JLabel();
        find.setText("include:");
        jpInclide.add(find, BorderLayout.WEST);
        jpInclide.add(m_tfIncludeWord, BorderLayout.CENTER);
        jpInclide.add(m_chkEnableIncludeWord, BorderLayout.EAST);

        JPanel jpExclude = new JPanel(new BorderLayout());
        JLabel remove = new JLabel();
        remove.setText("exclude:");
        jpExclude.add(remove, BorderLayout.WEST);
        jpExclude.add(m_tfExcludeWord, BorderLayout.CENTER);
        jpExclude.add(m_chkEnableExcludeWord, BorderLayout.EAST);

        JPanel jpLFTag = new JPanel(new FlowLayout());
        JLabel lfTag = new JLabel();
        lfTag.setText("Filter LogFlow: ");
        jpLFTag.add(lfTag);
        jpLFTag.add(m_chkEnableLogFlowTag);
        m_chkEnableLogFlowTag.setSelected(mShowFlowInLogTable);

        jpWordFilter.add(jpInclide);
        jpWordFilter.add(jpExclude);
        jpWordFilter.add(jpLFTag);

        jpMain.add(jpWordFilter, BorderLayout.NORTH);

        JPanel jpTagFilter = new JPanel();
        jpTagFilter.setLayout(new BoxLayout(jpTagFilter, BoxLayout.Y_AXIS));
        jpTagFilter.setBorder(BorderFactory.createTitledBorder("Tag filter"));

        JPanel jpPidTid = new JPanel(new GridLayout(1, 3));

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

        JPanel tagIncludeExtPanel = new JPanel(new BorderLayout());
        JButton tagIncludeExtBtn = createExtDialogButton(Constant.EXT_DIALOG_TYPE_INCLUDE_TAG);
        tagIncludeExtPanel.add(tagIncludeExtBtn, BorderLayout.WEST);
        tagIncludeExtPanel.add(m_chkEnableShowTag, BorderLayout.EAST);
        jpShow.add(tagIncludeExtPanel, BorderLayout.EAST);

        JPanel jpRemoveTag = new JPanel(new BorderLayout());
        JLabel removeTag = new JLabel();
        removeTag.setText("Tag Exclude: ");
        jpRemoveTag.add(removeTag, BorderLayout.WEST);
        jpRemoveTag.add(m_tfRemoveTag, BorderLayout.CENTER);

        JPanel tagExcludeExtPanel = new JPanel(new BorderLayout());
        JButton tagExcludeExtBtn = createExtDialogButton(Constant.EXT_DIALOG_TYPE_EXCLUDE_TAG);
        tagExcludeExtPanel.add(tagExcludeExtBtn, BorderLayout.WEST);
        tagExcludeExtPanel.add(m_chkEnableRemoveTag, BorderLayout.EAST);
        jpRemoveTag.add(tagExcludeExtPanel, BorderLayout.EAST);

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
        jlToTimeTag.setText("to time");
        jpToTimeTag.add(jlToTimeTag, BorderLayout.WEST);
        jpToTimeTag.add(m_tfToTimeTag, BorderLayout.CENTER);

        JPanel jpTimeTag = new JPanel(new GridLayout(2, 1));
        jpTimeTag.add(jpFromTimeTag);
        jpTimeTag.add(jpToTimeTag);
        JPanel jpTimeMainTag = new JPanel(new BorderLayout());
        jpTimeMainTag.add(jpTimeTag, BorderLayout.CENTER);
        jpTimeMainTag.add(m_chkEnableTimeTag, BorderLayout.EAST);

        JPanel jpFile = new JPanel(new BorderLayout());
        JLabel jLFile = new JLabel();
        jLFile.setText("File : ");
        jpFile.add(jLFile, BorderLayout.WEST);
        jpFile.add(m_tfShowFileName, BorderLayout.CENTER);
        jpFile.add(m_chkEnableFileNameFilter, BorderLayout.EAST);

        jpTagFilter.add(jpPidTid);
        jpTagFilter.add(jpShow);
        jpTagFilter.add(jpRemoveTag);
        jpTagFilter.add(jpBmTag);
        jpTagFilter.add(jpTimeMainTag);
        jpTagFilter.add(jpFile);

        jpMain.add(jpTagFilter, BorderLayout.CENTER);

        return jpMain;
    }

    private JButton createExtDialogButton(int type) {
        JButton tagIncludeExtBtn = new JButton("...");
        tagIncludeExtBtn.setBorder(new EmptyBorder(3, 3, 3, 3));
        tagIncludeExtBtn.setBorderPainted(false);
        tagIncludeExtBtn.setContentAreaFilled(false);
        tagIncludeExtBtn.setOpaque(false);
        tagIncludeExtBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Set<String> tagSet = getAllInfoContent(LogFilterTableModel.COLUMN_TAG);
                if (tagSet.size() <= 0) {
                    return;
                }
                // 字典序
                List<String> resultList = new ArrayList<>(tagSet);
                Collections.sort(resultList, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        boolean containsO1 = mFilterTagHistory.contains(o1);
                        boolean containsO2 = mFilterTagHistory.contains(o2);
                        if (containsO1 && containsO2) {
                            return o1.compareTo(o2);
                        }
                        if (containsO1) {
                            return -1;
                        }
                        if (containsO2) {
                            return 1;
                        }
                        return o1.compareTo(o2);
                    }
                });

                JList list = new JList(resultList.toArray(new String[resultList.size()]));
                ListDialog dialog = new ListDialog("Please select an item in the list: ", list);
                dialog.setOnOk(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        List selectedItems = dialog.getSelectedItems();
                        for (Object selectedItem : selectedItems) {
                            String tag = (String) selectedItem;
                            switch (type) {
                                case Constant.EXT_DIALOG_TYPE_INCLUDE_TAG: {
                                    getLogTable().appendFilterShowTag(tag);
                                }
                                break;
                                case Constant.EXT_DIALOG_TYPE_EXCLUDE_TAG: {
                                    getLogTable().appendFilterRemoveTag(tag);
                                }
                                break;
                            }
                            mFilterTagHistory.add(tag);
                        }
                        switch (type) {
                            case Constant.EXT_DIALOG_TYPE_INCLUDE_TAG: {
                                postEvent(new EventBus.Event(TYPE.EVENT_CHANGE_FILTER_SHOW_TAG));
                            }
                            break;
                            case Constant.EXT_DIALOG_TYPE_EXCLUDE_TAG: {
                                postEvent(new EventBus.Event(TYPE.EVENT_CHANGE_FILTER_REMOVE_TAG));
                            }
                            break;
                        }
                    }
                });
                dialog.show();
            }

            private Set<String> getAllInfoContent(int column) {
                Set<String> tagSet = new HashSet<>();
                for (LogInfo logInfo : m_arLogInfoAll) {
                    if (logInfo != null && logInfo.getTag() != null && logInfo.getTag().length() > 0) {
                        tagSet.add(logInfo.getContentByColumn(column).toString());
                    }
                }
                return tagSet;
            }
        });
        return tagIncludeExtBtn;
    }

    Component createHighlightPanel() {
        m_chkEnableHighlight = new JCheckBox();
        m_chkEnableHighlight.setSelected(true);

        m_tfHighlight = new JTextField();

        JPanel jpMain = new JPanel(new BorderLayout());
        jpMain.setBorder(BorderFactory.createTitledBorder("Highlight"));
        jpMain.add(m_tfHighlight);
        jpMain.add(m_chkEnableHighlight, BorderLayout.EAST);

        return jpMain;
    }

    Component createSearchPanel() {
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

    Component createCheckPanel() {
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
        m_chkClmFile = new JCheckBox();

        JPanel jpMain = new JPanel();
        jpMain.setLayout(new BoxLayout(jpMain, BoxLayout.Y_AXIS));

        JPanel jpLogFilter = new JPanel();
        jpLogFilter.setLayout(new GridLayout(3, 2));
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
        jpShowColumn.setLayout(new GridLayout(5, 2));
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
        m_chkClmFile.setText("File");
        m_chkClmFile.setSelected(true);
        jpShowColumn.add(m_chkClmLine);
        jpShowColumn.add(m_chkClmDate);
        jpShowColumn.add(m_chkClmTime);
        jpShowColumn.add(m_chkClmLogLV);
        jpShowColumn.add(m_chkClmPid);
        jpShowColumn.add(m_chkClmThread);
        jpShowColumn.add(m_chkClmTag);
        jpShowColumn.add(m_chkClmBookmark);
        jpShowColumn.add(m_chkClmMessage);
        jpShowColumn.add(m_chkClmFile);

        jpMain.add(jpLogFilter);
        jpMain.add(jpShowColumn);
        return jpMain;
    }

    Component createOptionFilter() {
        JPanel optionFilter = new JPanel();
        optionFilter.setLayout(new BoxLayout(optionFilter, BoxLayout.Y_AXIS));
        optionFilter.add(createDevicePanel());
        optionFilter.add(createFilterPanel());
        optionFilter.add(createCheckPanel());
        return wrapWithFloatingPanel(optionFilter);
    }

    // 提供floating 功能
    private JComponent wrapWithFloatingPanel(JComponent target) {
        if (frameInfoProvider.getContainerFrame() == null || !frameInfoProvider.enableFloatingWindow()) {
            return target;
        }
        JPanel floatingPanel = new JPanel(new BorderLayout());
        floatingPanel.add(target, BorderLayout.CENTER);
        floatingPanel.add(createFloatingBtnHeader(() -> floatingPanel), BorderLayout.NORTH);
        return floatingPanel;
    }

    private JPanel createFloatingBtnHeader(FloatingTagetProvider target) {
        // floating window btn
        JPanel btnPanel = new JPanel(new BorderLayout());
        JButton button = new JButton("floating");
        button.setFont(new Font(button.getFont().getName(), button.getFont().getStyle(), 10));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusable(false);
        button.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        button.addActionListener(e -> {
            FloatingFrameInfo frameInfo = frameInfoProvider.onFilterFloating(LogFilterComponent.this, target.getTarget(), mCurTitle);
            if (!frameInfo.isRemoved) {
                mFloatingFrameInfos.add(frameInfo);
            } else {
                mFloatingFrameInfos.remove(frameInfo);
            }
        });
        btnPanel.add(button, BorderLayout.EAST);
        return btnPanel;
    }

    Component createOptionMenu() {
        JPanel optionMenu = new JPanel(new BorderLayout());
        optionMenu.setBorder(BorderFactory.createLineBorder(ThemeConstant.getColorLogTableCellBorder(), 1));
        JPanel optionWest = new JPanel(new FlowLayout(FlowLayout.LEADING));

        JLabel jlFont = new JLabel("Font Size : ");
        m_tfFontSize = new JTextField(4);
        m_tfFontSize.setHorizontalAlignment(SwingConstants.RIGHT);
        m_tfFontSize.setText("12");

        m_btnSetFont = new JButton("OK");
        m_btnSetFont.setMargin(new Insets(0, 0, 0, 0));
        m_btnSetFont.addActionListener(m_alButtonListener);

        JLabel jlGoto = new JLabel("Goto : ");
        m_tfGoto = new JTextField(6);
        m_tfGoto.setHorizontalAlignment(SwingConstants.RIGHT);
        m_tfGoto.addCaretListener(new CaretListener() {
            public void caretUpdate(CaretEvent e) {
                try {
                    int nIndex = Integer.parseInt(m_tfGoto.getText()) - 1;
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
        mSyncScrollCheckBox.setVisible(false);

        mSyncSelectedCheckBox = new JCheckBox("sync selected");
        mSyncSelectedCheckBox.setEnabled(false);
        mSyncSelectedCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                JCheckBox check = (JCheckBox) e.getSource();
                enableSyncSelected(check.isSelected());
            }
        });
        mSyncSelectedCheckBox.setVisible(false);

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

        JPanel jpActionPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        JButton clearFieldBtn = new JButton("Clean Filter");
        clearFieldBtn.setMargin(new Insets(0, 0, 0, 0));
        clearFieldBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m_tfIncludeWord.setText("");
                m_tfExcludeWord.setText("");
                m_tfShowTag.setText("");
                m_tfRemoveTag.setText("");
                m_tfShowPid.setText("");
                m_tfShowFileName.setText("");
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
                updateLogTable(endLine - 1, true);
            }
        });
        jpActionPanel.add(followBtn);

        optionWest.add(mSyncScrollCheckBox);
        optionWest.add(mSyncSelectedCheckBox);
        optionWest.add(jlFont);
        optionWest.add(m_tfFontSize);
        optionWest.add(m_btnSetFont);
        optionWest.add(jlGoto);
        optionWest.add(m_tfGoto);
        optionWest.add(preHistoryButton);
        optionWest.add(nextHistoryButton);
        optionWest.add(jpActionPanel);

        optionMenu.add(optionWest, BorderLayout.CENTER);

        mSearchPanel = new JPanel(new GridLayout(1, 2));
        mSearchPanel.add(createHighlightPanel());
        mSearchPanel.add(createSearchPanel());
        mSearchPanel.setVisible(false);
        optionMenu.add(mSearchPanel, BorderLayout.SOUTH);

        JScrollPane scrollPane = new JScrollPane(optionMenu);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    Component createMainSplitPane() {
        mMainSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                createOptionPanel(),
                createLogPanel()
        );
        mMainSplitPane.setContinuousLayout(true);
        mMainSplitPane.addContainerListener(new ContainerAdapter() {
            @Override
            public void componentAdded(ContainerEvent e) {
                restoreSplitPane();
            }
        });
        return mMainSplitPane;
    }

    Component createOptionPanel() {
        if (!frameInfoProvider.enableFloatingWindow()) {
            JScrollPane scrollPane = new JScrollPane(createOptionFilter());
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            return scrollPane;
        }
        return createOptionFilter();
    }

    Component createStatusPanel() {
        JPanel mainP = new JPanel(new BorderLayout());

        JPanel tfPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(4, 4, 4, 4);

        Border border = BorderFactory.createCompoundBorder(new EtchedBorder(), new EmptyBorder(0, 4, 0, 4));

        m_tfDiffPort = new JLabel("not bind");
        m_tfDiffPort.setBorder(border);
        m_tfDiffPort.setVisible(false);
        m_tfDiffState = new JLabel("disconnected");
        m_tfDiffState.setBorder(border);
        m_tfDiffState.setVisible(false);
        m_tfParserType = new JLabel("");
        m_tfParserType.setBorder(border);

        m_progressLoading = new JProgressBar(0, 100);
        m_progressLoading.setVisible(false);
        m_progressLoading.setIndeterminate(false);
        m_progressLoading.setStringPainted(true);
        m_progressLoading.setString("");
        m_progressLoading.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        tfPanel.add(m_progressLoading, constraints);
        tfPanel.add(m_tfDiffState, constraints);
        tfPanel.add(m_tfDiffPort, constraints);
        tfPanel.add(m_tfParserType, constraints);

        mainP.add(tfPanel, BorderLayout.EAST);
        return mainP;
    }

    Component createLogPanel() {
        JPanel mainLogPanel = new JPanel(new BorderLayout());

        mainLogPanel.add(createOptionMenu(), BorderLayout.NORTH);

        m_tmLogTableModel = new LogFilterTableModel();
        m_tmLogTableModel.setData(m_arLogInfoAll);
        m_tbLogTable = new LogTable(m_tmLogTableModel, this);
        m_logScrollVPane = new JScrollPane(m_tbLogTable);
        m_logScrollVPane.setBorder(BorderFactory.createEmptyBorder());
        m_logScrollVPane.getVerticalScrollBar().addAdjustmentListener(m_tbLogTable);

        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new OverlayLayout(tablePanel));
        tablePanel.add(m_logScrollVPane);
        mainLogPanel.add(tablePanel, BorderLayout.CENTER);

        mainLogPanel.add(createIndicatorPanel(), BorderLayout.WEST);

        m_tSubLogTableModel = new LogFilterTableModel();
        m_tSubLogTableModel.setData(m_arSubLogInfoAll);
        m_tSublogTable = new SubLogTable(m_tSubLogTableModel, this);
        m_subLogScrollVPane = new JScrollPane(m_tSublogTable);
        m_subLogScrollVPane.setBorder(BorderFactory.createEmptyBorder());

        mLogSplitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                mainLogPanel,
                wrapWithFloatingPanel(m_subLogScrollVPane)
        );
        return mLogSplitPane;
    }

    void initValue() {
        m_bPauseADB = false;
        FILE_LOCK = new Object();
        FILTER_LOCK = new Object();
        mLogParsingState = Constant.PARSING_STATUS_READY;
        m_nFilterLogLV = LogInfo.LOG_LV_ALL;

        m_arLogInfoAll = new ArrayList<LogInfo>();
        m_arLogInfoFiltered = new ArrayList<LogInfo>();
        m_hmMarkedInfoAll = new HashMap<Integer, Integer>();
        m_hmMarkedInfoFiltered = new HashMap<Integer, Integer>();
        m_hmErrorAll = new ConcurrentHashMap<Integer, Integer>();
        m_hmErrorFiltered = new ConcurrentHashMap<Integer, Integer>();
        m_arSubLogInfoAll = new ArrayList<>();

        File confDir = new File(Constant.CONFIG_BASE_DIR);
        if (!confDir.exists()) {
            confDir.mkdirs();
            T.d("create conf directory: " + confDir.getAbsolutePath());
        }
        File outputDir = new File(Constant.OUTPUT_LOG_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
            T.d("create log directory: " + outputDir.getAbsolutePath());
        }
        m_strLogFileName = makeFilename();
        // m_strProcessCmd = ANDROID_DEFAULT_CMD + m_strLogFileName;
    }

    void parseLogFile(final File[] files) {
        if (files == null) {
            T.e("files == null");
            return;
        }
        if (files.length <= 0) {
            T.e("files size <= 0");
            return;
        }

        StringBuilder title = new StringBuilder();
        StringBuilder filePathBuilder = new StringBuilder();
        for (File file : files) {
            if (file == null) {
                T.e("parse null file");
                return;
            }
            filePathBuilder.append(file.getAbsolutePath()).append("|");
            title.append(file.getName()).append(" | ");
        }
        frameInfoProvider.beforeLogFileParse(filePathBuilder.deleteCharAt(filePathBuilder.length() - 1).toString(), this);
        setTitleAndTips(title.substring(0, title.length() - 3), filePathBuilder.toString());
        // parsing
        new Thread(new Runnable() {
            public void run() {
                setLoadingState(LoadingState.LOADING, "parsing");
                clearData();
                getLogTable().clearSelection();
                getSubTable().clearSelection();

                List<LogInfo> newLogInfos = new ArrayList<>();
                int fileIdx = 0;
                for (File file : files) {
                    FileInputStream fstream = null;
                    DataInputStream in = null;
                    BufferedReader br = null;
                    fileIdx++;
                    try {
                        fstream = new FileInputStream(file);
                        in = new DataInputStream(fstream);
                        br = new BufferedReader(new InputStreamReader(in,
                                StandardCharsets.UTF_8));

                        String strLine;
                        while ((strLine = br.readLine()) != null) {
                            if (!"".equals(strLine.trim())) {
                                LogInfo logInfo = m_iLogParser.parseLog(strLine);
                                logInfo.setType(LogInfo.TYPE.SYSTEM);
                                // 处理空白行
                                if (logInfo.getTag() == null || logInfo.getTag().length() <= 0) {
                                    if (newLogInfos.size() > 1) {
                                        LogInfo oldInfo = newLogInfos.get(newLogInfos.size() - 1);
                                        logInfo.setTimestamp(oldInfo.getTimestamp());
                                        // add last tag for tag group
                                        logInfo.setTag(oldInfo.getTag());
                                        logInfo.setThread(oldInfo.getThread());
                                        logInfo.setPid(oldInfo.getPid());
                                        logInfo.setTime(oldInfo.getTime());
                                        logInfo.setLogLV(oldInfo.getLogLV());
                                        logInfo.setDate(oldInfo.getDate());
                                        logInfo.setTextColor(oldInfo.getTextColor());
                                    } else {
                                        logInfo.setTimestamp(0);
                                    }
                                    logInfo.setSingleMsgLine(true);
                                } else {
                                    logInfo.setSingleMsgLine(false);
                                }
                                logInfo.setFileName(String.valueOf(fileIdx));
                                newLogInfos.add(logInfo);
                            }
                        }
                    } catch (Exception ioe) {
                        ioe.printStackTrace();
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
                // merge and sort
                if (files.length > 1) {
                    newLogInfos.sort(new Comparator<LogInfo>() {
                        @Override
                        public int compare(LogInfo o1, LogInfo o2) {
                            return (int) (o1.getTimestamp() - o2.getTimestamp());
                        }
                    });
                }
                int lineIdx = 1;
                for (LogInfo info : newLogInfos) {
                    info.setLine(lineIdx++);
                    addLogInfo(info);
                }
                runFilter();

                mLastParseredFiles = files;
                setLoadingState(LoadingState.IDLE, "");
            }
        }).start();
    }

    void pauseLogcatParserProcess() {
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
        String strCommand = Constant.DEVICES_CMD[m_comboDeviceCmd.getSelectedIndex()];
        String[] cmd;
        if (Utils.isWindows()) {
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

    public void showPanelAndSetSearchFocus() {
        if (!mSearchPanel.isVisible() || m_tfHighlight.hasFocus()) {
            mSearchPanel.setVisible(true);
            m_tfSearch.requestFocus();
        } else {
            mSearchPanel.setVisible(false);
        }
    }

    private void showPanelAndSetHighLightFocus() {
        if (!mSearchPanel.isVisible() || m_tfSearch.hasFocus()) {
            mSearchPanel.setVisible(true);
            m_tfHighlight.requestFocus();
        } else {
            mSearchPanel.setVisible(false);
        }
    }

    private void setWordIncludeFocus() {
        m_tfIncludeWord.requestFocus();
    }

    private void setTagIncludeFocus() {
        m_tfShowTag.requestFocus();
    }

    private void setMainTableFocus() {
        m_tbLogTable.requestFocus();
    }

    @Override
    public void searchKeyword(String keyword) {
        m_tfSearch.setText(keyword);
    }

    void setDnDListener() {
        new DropTarget(mMainSplitPane, DnDConstants.ACTION_COPY_OR_MOVE,
                new DropTargetAdapter() {
                    public void drop(DropTargetDropEvent event) {
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
                                stopLogcatParserProcess();
                                parseLogFile(files.toArray(new File[0]));
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
        mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
        runFilter();
    }

    void useFilter(JCheckBox checkBox) {
        if (checkBox.equals(m_chkEnableIncludeWord)) {
            getLogTable().setFilterFind(checkBox.isSelected() ? m_tfIncludeWord
                    .getText() : "");
        }
        if (checkBox.equals(m_chkEnableExcludeWord)) {
            getLogTable().SetFilterRemove(checkBox.isSelected() ? m_tfExcludeWord
                    .getText() : "");
        }
        if (checkBox.equals(m_chkEnableShowPid)) {
            getLogTable().SetFilterShowPid(checkBox.isSelected() ? m_tfShowPid
                    .getText() : "");
        }
        if (checkBox.equals(m_chkEnableShowTid)) {
            getLogTable().SetFilterShowTid(checkBox.isSelected() ? m_tfShowTid
                    .getText() : "");
        }
        if (checkBox.equals(m_chkEnableShowTag)) {
            getLogTable().SetFilterShowTag(checkBox.isSelected() ? m_tfShowTag
                    .getText() : "");
        }
        if (checkBox.equals(m_chkEnableRemoveTag)) {
            getLogTable().SetFilterRemoveTag(checkBox.isSelected() ? m_tfRemoveTag
                    .getText() : "");
        }
        if (checkBox.equals(m_chkEnableBookmarkTag)) {
            getLogTable().SetFilterBookmarkTag(checkBox.isSelected() ? m_tfBookmarkTag
                    .getText() : "");
        }
        if (checkBox.equals(m_chkEnableLogFlowTag)) {
            getLogTable().SetFilterLogFlow(checkBox.isSelected());
        }
        if (checkBox.equals(m_chkEnableFileNameFilter)) {
            getLogTable().SetFilterFileName(checkBox.isSelected() ? m_tfShowFileName
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
        mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
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
            return Constant.ANDROID_DEFAULT_CMD_FIRST + m_comboCmd.getSelectedItem();
        else
            return Constant.ANDROID_SELECTED_CMD_FIRST
                    + m_selectedDevice.code
                    + " "
                    + m_comboCmd.getSelectedItem();
    }

    void setLoadingState(LoadingState status, String text) {
        switch (status) {
            case IDLE:
                m_progressLoading.setString("");
                m_progressLoading.setVisible(false);
                m_progressLoading.setIndeterminate(false);
                break;
            case LOADING:
                m_progressLoading.setString(text);
                m_progressLoading.setVisible(true);
                m_progressLoading.setIndeterminate(true);
                break;
        }
    }

    public void setTitleAndTips(String strTitle, String tips) {
        mCurTitle = strTitle;
        frameInfoProvider.setTabTitle(LogFilterComponent.this, strTitle, tips);
    }

    // 是否已经加载过log
    public boolean hasLoadLogFileOrRunLogcat() {
        return !mCurTitle.isEmpty();
    }

    void stopLogcatParserProcess() {
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

    ///////////////////////////////////parser///////////////////////////////////

    void startLogcatParse() {
        m_thWatchFile = new Thread(new Runnable() {
            public void run() {
                FileInputStream fstream = null;
                DataInputStream in = null;
                BufferedReader br = null;

                try {
                    setLoadingState(LoadingState.LOADING, "dumping");
                    fstream = new FileInputStream(m_strLogFileName);
                    in = new DataInputStream(fstream);
                    br = new BufferedReader(new InputStreamReader(in,
                            StandardCharsets.UTF_8));

                    String strLine;

                    setTitleAndTips(m_strLogFileName, m_strLogFileName);

                    m_arLogInfoAll.clear();
                    mLastParseredFiles = null;

                    boolean bEndLine;
                    int nSelectedIndex;
                    int nAddCount;
                    int nPreRowCount = 0;
                    int nEndLine;

                    while (true) {
                        Thread.sleep(50);

                        if (mLogParsingState == Constant.PARSING_STATUS_CHANGE_PENDING
                                || mLogParsingState == Constant.PARSING_STATUS_PARSING)
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
                                    logInfo.setFileName("logcat");
                                    addLogInfo(logInfo);
                                    nAddCount++;
                                }
                            }
                        }
                        if (nAddCount == 0)
                            continue;

                        synchronized (FILTER_LOCK) {
                            if (mFilterEnabled == false) {
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
                                    updateLogTable(nEndLine - 1, true);
                                else
                                    updateLogTable(nSelectedIndex, false);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    T.e(e);
                } catch (Exception e) {
                    e.printStackTrace();
                    T.e(e);
                } finally {
                    setLoadingState(LoadingState.IDLE, "");
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
                T.w("End m_thWatchFile thread");
            }
        });
        m_thWatchFile.start();
    }

    private Debouncer debouncer = new Debouncer();

    void runFilter() {
        debouncer.debounce(mCurTitle != null ? mCurTitle : LogFilterComponent.class,
                () -> {
                    checkUseFilter();
                    while (mLogParsingState == Constant.PARSING_STATUS_PARSING)
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    synchronized (FILTER_LOCK) {
                        FILTER_LOCK.notify();
                    }
                },
                300, TimeUnit.MILLISECONDS
        );
    }

    void startFilterParse() {
        m_thFilterParse = new Thread(() -> {
            try {
                while (true) {
                    synchronized (FILTER_LOCK) {
                        mLogParsingState = Constant.PARSING_STATUS_READY;
                        FILTER_LOCK.wait();

                        mLogParsingState = Constant.PARSING_STATUS_PARSING;

                        m_arLogInfoFiltered.clear();
                        m_hmMarkedInfoFiltered.clear();
                        m_hmErrorFiltered.clear();
                        getLogTable().clearSelection();
                        getSubTable().clearSelection();

                        try {
                            if (mFilterEnabled == false) {
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
                                    updateLogTable(i - 1, true);
                                } else {
                                    updateLogTable(m_arLogInfoFiltered.size() - 1, true);
                                }
                                mLogParsingState = Constant.PARSING_STATUS_READY;
                                continue;
                            }
                            m_tmLogTableModel.setData(m_arLogInfoFiltered);
                            m_ipIndicator.setData(m_arLogInfoFiltered,
                                    m_hmMarkedInfoFiltered, m_hmErrorFiltered);
                            // updateTable(-1);
                            setLoadingState(LoadingState.LOADING, "filtering");

                            int nRowCount = m_arLogInfoAll.size();
                            LogInfo logInfo;
                            boolean bAddFilteredArray;

                            for (int nIndex = 0; nIndex < nRowCount; nIndex++) {
                                if (nIndex % 10000 == 0)
                                    Thread.sleep(1);
                                if (mLogParsingState == Constant.PARSING_STATUS_CHANGE_PENDING) {
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
                                        && checkBookmarkFilter(logInfo)
                                        && checkLogFlowFilter(logInfo)
                                        && checkFileNameFilter(logInfo)
                                ) {
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
                            if (mLogParsingState == Constant.PARSING_STATUS_PARSING) {
                                mLogParsingState = Constant.PARSING_STATUS_READY;
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
                                    updateLogTable(i - 1, true);
                                } else {
                                    updateLogTable(m_arLogInfoFiltered.size() - 1, true);
                                }
                                setLoadingState(LoadingState.IDLE, "");
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            setLoadingState(LoadingState.IDLE, "");
                            T.e("m_thFilterParse current filter loop error ex=" + ex);
                        }
                    }
                }
            } catch (InterruptedException e) {
                T.e("m_thFilterParse exit normal");
            } catch (Exception e) {
                e.printStackTrace();
                T.e(e);
            }
            setLoadingState(LoadingState.IDLE, "");
            T.w("End m_thFilterParse thread");
        });
        m_thFilterParse.start();
    }

    void startLogcatParserProcess() {
        clearData();
        getLogTable().clearSelection();
        getSubTable().clearSelection();
        // 自动切换到logcatparser
        switchToLogParser(Constant.PARSER_TYPE_LOGCAT);

        m_thProcess = new Thread(() -> {
            try {
                String s;
                m_Process = null;

                T.d("getProcessCmd() = " + getProcessCmd());
                m_Process = Runtime.getRuntime().exec(getProcessCmd());
                BufferedReader stdOut = new BufferedReader(
                        new InputStreamReader(m_Process.getInputStream(),
                                StandardCharsets.UTF_8));
                Writer fileOut = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(m_strLogFileName), StandardCharsets.UTF_8));

                startLogcatParse();

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
            stopLogcatParserProcess();
        });
        m_thProcess.start();
        setProcessBtn(true);
    }

    ///////////////////////////////////filter///////////////////////////////////

    boolean checkLogLVFilter(LogInfo logInfo) {
        if (m_nFilterLogLV == LogInfo.LOG_LV_ALL)
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_VERBOSE) != 0
                && logInfo.getLogLV().startsWith("V"))
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_DEBUG) != 0
                && logInfo.getLogLV().startsWith("D"))
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_INFO) != 0
                && logInfo.getLogLV().startsWith("I"))
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_WARN) != 0
                && logInfo.getLogLV().startsWith("W"))
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_ERROR) != 0
                && logInfo.getLogLV().startsWith("E"))
            return true;
        return (m_nFilterLogLV & LogInfo.LOG_LV_FATAL) != 0
                && logInfo.getLogLV().startsWith("F");
    }

    boolean checkPidFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterShowPid().length() <= 0)
            return true;
        return checkFilterContains(getLogTable().GetFilterShowPid(), logInfo.getPid());
    }

    boolean checkTidFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterShowTid().length() <= 0)
            return true;
        return checkFilterContains(getLogTable().GetFilterShowTid(), logInfo.getThread());
    }

    boolean checkFindFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterFind().length() <= 0)
            return true;
        return checkFilterContains(getLogTable().GetFilterFind(), logInfo.getMessage());
    }

    boolean checkRemoveFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterRemove().length() <= 0)
            return true;
        return checkFilterNotContains(getLogTable().GetFilterRemove(), logInfo.getMessage());
    }

    boolean checkShowTagFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterShowTag().length() <= 0)
            return true;
        return checkFilterContains(getLogTable().GetFilterShowTag(), logInfo.getTag());
    }

    boolean checkRemoveTagFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterRemoveTag().length() <= 0)
            return true;
        return checkFilterNotContains(getLogTable().GetFilterRemoveTag(), logInfo.getTag());
    }

    boolean checkBookmarkFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterBookmarkTag().length() <= 0 && logInfo.getBookmark().length() <= 0)
            return true;
        return checkFilterContains(getLogTable().GetFilterBookmarkTag(), logInfo.getBookmark());
    }


    boolean checkLogFlowFilter(LogInfo logInfo) {
        if (!getLogTable().isFilterLogFlow()) {
            return true;
        }
        List<LogFlowManager.FlowResultLine> flowResults = logInfo.getFlowResults();
        return flowResults != null && flowResults.size() > 0;
    }

    boolean checkFileNameFilter(LogInfo logInfo) {
        if (logInfo.getFileName() == null || logInfo.getFileName().length() <= 0) {
            return true;
        }
        if (getLogTable().GetFilterFileName() == null || getLogTable().GetFilterFileName().length() <= 0) {
            return true;
        }
        return logInfo.getFileName().startsWith(getLogTable().GetFilterFileName());
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

    private boolean checkFilterContains(String filter, String src) {
        Pattern pattern = Utils.findPatternOrCreate(filter);
        return pattern.matcher(src).find();
    }

    private boolean checkFilterNotContains(String src, String filter) {
        return !checkFilterContains(src, filter);
    }

    boolean checkUseFilter() {
        mFilterEnabled = m_ipIndicator.m_chBookmark.isSelected()
                || m_ipIndicator.m_chError.isSelected()
                || !checkLogLVFilter(new LogInfo())
                || (getLogTable().GetFilterShowPid().length() != 0 && m_chkEnableShowPid.isSelected())
                || (getLogTable().GetFilterShowTid().length() != 0 && m_chkEnableShowTid.isSelected())
                || (getLogTable().GetFilterShowTag().length() != 0 && m_chkEnableShowTag.isSelected())
                || (getLogTable().GetFilterRemoveTag().length() != 0 && m_chkEnableRemoveTag.isSelected())
                || (getLogTable().GetFilterBookmarkTag().length() != 0 && m_chkEnableBookmarkTag.isSelected())
                || (getLogTable().GetFilterFileName().length() != 0 && m_chkEnableFileNameFilter.isSelected())
                || (getLogTable().isFilterLogFlow() && m_chkEnableLogFlowTag.isSelected())
                || ((getLogTable().GetFilterFromTime() != -1l || getLogTable().GetFilterToTime() != -1l) && m_chkEnableTimeTag.isSelected())
                || (getLogTable().GetFilterFind().length() != 0 && m_chkEnableIncludeWord.isSelected())
                || (getLogTable().GetFilterRemove().length() != 0 && m_chkEnableExcludeWord.isSelected());
        return mFilterEnabled;
    }

    //////////////////////////////////////////////////////////////////////

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
                updateLogTable(-1, false);
            } else if (e.getSource().equals(m_btnRun)) {
                startLogcatParserProcess();
            } else if (e.getSource().equals(m_btnStop)) {
                stopLogcatParserProcess();
            } else if (e.getSource().equals(m_btnClear)) {
                boolean bBackup = m_bPauseADB;
                m_bPauseADB = true;
                clearData();
                updateLogTable(-1, false);
                m_bPauseADB = bBackup;
            } else if (e.getSource().equals(m_tbtnPause)) {
                pauseLogcatParserProcess();
            }
        }
    };

    @Override
    public void postEvent(Event param) {
        switch (param.type) {
            case EVENT_CLICK_BOOKMARK:
            case EVENT_CLICK_ERROR:
                mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
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
                m_tbLogTable.changeSelection(target);
            }
            break;
        }
    }

    void updateLogTable(int nRow, boolean bMove) {
        m_tmLogTableModel.fireTableDataChanged();
        m_logScrollVPane.validate();
        getLogTable().invalidate();
        getLogTable().repaint();
        if (nRow >= 0)
            getLogTable().changeSelection(nRow, 0, false, false, bMove);

        updateSubTable(-1);
    }

    void updateSubTable(int nRow) {
        m_tSubLogTableModel.fireTableDataChanged();
        m_subLogScrollVPane.validate();
        getSubTable().invalidate();
        getSubTable().repaint();
        if (nRow >= 0)
            getSubTable().showRowCenterIfNotInRect(nRow, true);
    }

    DocumentListener mFilterListener = new DocumentListener() {
        public void changedUpdate(DocumentEvent arg0) {
            try {
                if (arg0.getDocument().equals(m_tfIncludeWord.getDocument())
                        && m_chkEnableIncludeWord.isSelected()) {
                    getLogTable().setFilterFind(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument()
                        .equals(m_tfExcludeWord.getDocument())
                        && m_chkEnableExcludeWord.isSelected()) {
                    getLogTable().SetFilterRemove(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfShowPid.getDocument())
                        && m_chkEnableShowPid.isSelected()) {
                    getLogTable().SetFilterShowPid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfShowTid.getDocument())
                        && m_chkEnableShowTid.isSelected()) {
                    getLogTable().SetFilterShowTid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfShowTag.getDocument())
                        && m_chkEnableShowTag.isSelected()) {
                    getLogTable().SetFilterShowTag(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfRemoveTag.getDocument())
                        && m_chkEnableRemoveTag.isSelected()) {
                    getLogTable().SetFilterRemoveTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfBookmarkTag.getDocument())
                        && m_chkEnableBookmarkTag.isSelected()) {
                    getLogTable().SetFilterBookmarkTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfShowFileName.getDocument())
                        && m_chkEnableFileNameFilter.isSelected()) {
                    getLogTable().SetFilterFileName(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfHighlight.getDocument())
                        && m_chkEnableHighlight.isSelected()) {
                    getLogTable().SetHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getSubTable().SetHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfSearch.getDocument())) {
                    getLogTable().SetSearchHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getSubTable().SetSearchHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getLogTable().gotoNextSearchResult();
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfFromTimeTag.getDocument())) {
                    getLogTable().SetFilterFromTime(m_tfFromTimeTag.getText());
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfToTimeTag.getDocument())) {
                    getLogTable().SetFilterToTime(m_tfToTimeTag.getText());
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                }
            } catch (Exception e) {
                T.e(e);
            }
        }

        public void insertUpdate(DocumentEvent arg0) {
            try {
                if (arg0.getDocument().equals(m_tfIncludeWord.getDocument())
                        && m_chkEnableIncludeWord.isSelected()) {
                    getLogTable().setFilterFind(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument()
                        .equals(m_tfExcludeWord.getDocument())
                        && m_chkEnableExcludeWord.isSelected()) {
                    getLogTable().SetFilterRemove(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfShowPid.getDocument())
                        && m_chkEnableShowPid.isSelected()) {
                    getLogTable().SetFilterShowPid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfShowTid.getDocument())
                        && m_chkEnableShowTid.isSelected()) {
                    getLogTable().SetFilterShowTid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfShowTag.getDocument())
                        && m_chkEnableShowTag.isSelected()) {
                    getLogTable().SetFilterShowTag(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfRemoveTag.getDocument())
                        && m_chkEnableRemoveTag.isSelected()) {
                    getLogTable().SetFilterRemoveTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfBookmarkTag.getDocument())
                        && m_chkEnableBookmarkTag.isSelected()) {
                    getLogTable().SetFilterBookmarkTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfShowFileName.getDocument())
                        && m_chkEnableFileNameFilter.isSelected()) {
                    getLogTable().SetFilterFileName(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfHighlight.getDocument())
                        && m_chkEnableHighlight.isSelected()) {
                    getLogTable().SetHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getSubTable().SetHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfSearch.getDocument())) {
                    getLogTable().SetSearchHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getSubTable().SetSearchHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getLogTable().gotoNextSearchResult();
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfFromTimeTag.getDocument())) {
                    getLogTable().SetFilterFromTime(m_tfFromTimeTag.getText());
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfToTimeTag.getDocument())) {
                    getLogTable().SetFilterToTime(m_tfToTimeTag.getText());
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                }
            } catch (Exception e) {
                T.e(e);
            }
        }

        public void removeUpdate(DocumentEvent arg0) {
            try {
                if (arg0.getDocument().equals(m_tfIncludeWord.getDocument())
                        && m_chkEnableIncludeWord.isSelected()) {
                    getLogTable().setFilterFind(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument()
                        .equals(m_tfExcludeWord.getDocument())
                        && m_chkEnableExcludeWord.isSelected()) {
                    getLogTable().SetFilterRemove(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfShowPid.getDocument())
                        && m_chkEnableShowPid.isSelected()) {
                    getLogTable().SetFilterShowPid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfShowTid.getDocument())
                        && m_chkEnableShowTid.isSelected()) {
                    getLogTable().SetFilterShowTid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfShowTag.getDocument())
                        && m_chkEnableShowTag.isSelected()) {
                    getLogTable().SetFilterShowTag(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfRemoveTag.getDocument())
                        && m_chkEnableRemoveTag.isSelected()) {
                    getLogTable().SetFilterRemoveTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfShowFileName.getDocument())
                        && m_chkEnableFileNameFilter.isSelected()) {
                    getLogTable().SetFilterFileName(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfBookmarkTag.getDocument())
                        && m_chkEnableBookmarkTag.isSelected()) {
                    getLogTable().SetFilterBookmarkTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfHighlight.getDocument())
                        && m_chkEnableHighlight.isSelected()) {
                    getLogTable().SetHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getSubTable().SetHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfSearch.getDocument())) {
                    getLogTable().SetSearchHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getSubTable().SetSearchHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getLogTable().gotoNextSearchResult();
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfFromTimeTag.getDocument())) {
                    getLogTable().SetFilterFromTime(m_tfFromTimeTag.getText());
                    runFilter();
                } else if (arg0.getDocument().equals(m_tfToTimeTag.getDocument())) {
                    getLogTable().SetFilterToTime(m_tfToTimeTag.getText());
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING;
                    runFilter();
                }
            } catch (Exception e) {
                T.e(e);
            }
        }
    };


    private void loadTableColumnState() {
        for (int nIndex = 0; nIndex < m_colWidths.length; nIndex++) {
            LogFilterTableModel.setColumnWidth(nIndex, m_colWidths[nIndex]);
        }
        m_colWidths = LogFilterTableModel.ColWidth;
        // 不支持的column不能操作
        m_chkClmBookmark.setEnabled(
                getLogTable().showColumn(LogFilterTableModel.COLUMN_BOOKMARK,
                        m_chkClmBookmark.isSelected())
        );
        m_chkClmLine.setEnabled(
                getLogTable().showColumn(LogFilterTableModel.COLUMN_LINE,
                        m_chkClmLine.isSelected())
        );
        m_chkClmDate.setEnabled(
                getLogTable().showColumn(LogFilterTableModel.COLUMN_DATE,
                        m_chkClmDate.isSelected())
        );
        m_chkClmTime.setEnabled(
                getLogTable().showColumn(LogFilterTableModel.COLUMN_TIME,
                        m_chkClmTime.isSelected())
        );
        m_chkClmLogLV.setEnabled(
                getLogTable().showColumn(LogFilterTableModel.COLUMN_LOGLV,
                        m_chkClmLogLV.isSelected())
        );
        m_chkClmPid.setEnabled(
                getLogTable().showColumn(LogFilterTableModel.COLUMN_PID,
                        m_chkClmPid.isSelected())
        );
        m_chkClmThread.setEnabled(
                getLogTable().showColumn(LogFilterTableModel.COLUMN_THREAD,
                        m_chkClmThread.isSelected())
        );
        m_chkClmTag.setEnabled(
                getLogTable().showColumn(LogFilterTableModel.COLUMN_TAG,
                        m_chkClmTag.isSelected())
        );
        m_chkClmMessage.setEnabled(
                getLogTable().showColumn(LogFilterTableModel.COLUMN_MESSAGE,
                        m_chkClmMessage.isSelected())
        );
        m_chkClmFile.setEnabled(
                getLogTable().showColumn(LogFilterTableModel.COLUMN_FILE,
                        m_chkClmFile.isSelected())
        );

        getSubTable().showColumn(LogFilterTableModel.COLUMN_BOOKMARK,
                m_chkClmBookmark.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COLUMN_LINE,
                m_chkClmLine.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COLUMN_DATE,
                m_chkClmDate.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COLUMN_TIME,
                m_chkClmTime.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COLUMN_LOGLV,
                m_chkClmLogLV.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COLUMN_PID,
                m_chkClmPid.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COLUMN_THREAD,
                m_chkClmThread.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COLUMN_TAG,
                m_chkClmTag.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COLUMN_MESSAGE,
                m_chkClmMessage.isSelected());
        getSubTable().showColumn(LogFilterTableModel.COLUMN_FILE,
                m_chkClmFile.isSelected());
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
                getLogTable().showColumn(LogFilterTableModel.COLUMN_BOOKMARK,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COLUMN_BOOKMARK,
                        check.isSelected());
            } else if (check.equals(m_chkClmFile)) {
                getLogTable().showColumn(LogFilterTableModel.COLUMN_FILE,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COLUMN_FILE,
                        check.isSelected());
            } else if (check.equals(m_chkClmLine)) {
                getLogTable().showColumn(LogFilterTableModel.COLUMN_LINE,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COLUMN_LINE,
                        check.isSelected());
            } else if (check.equals(m_chkClmDate)) {
                getLogTable().showColumn(LogFilterTableModel.COLUMN_DATE,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COLUMN_DATE,
                        check.isSelected());
            } else if (check.equals(m_chkClmTime)) {
                getLogTable().showColumn(LogFilterTableModel.COLUMN_TIME,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COLUMN_TIME,
                        check.isSelected());
            } else if (check.equals(m_chkClmLogLV)) {
                getLogTable().showColumn(LogFilterTableModel.COLUMN_LOGLV,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COLUMN_LOGLV,
                        check.isSelected());
            } else if (check.equals(m_chkClmPid)) {
                getLogTable().showColumn(LogFilterTableModel.COLUMN_PID,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COLUMN_PID,
                        check.isSelected());
            } else if (check.equals(m_chkClmThread)) {
                getLogTable().showColumn(LogFilterTableModel.COLUMN_THREAD,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COLUMN_THREAD,
                        check.isSelected());
            } else if (check.equals(m_chkClmTag)) {
                getLogTable().showColumn(LogFilterTableModel.COLUMN_TAG,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COLUMN_TAG,
                        check.isSelected());
            } else if (check.equals(m_chkClmMessage)) {
                getLogTable().showColumn(LogFilterTableModel.COLUMN_MESSAGE,
                        check.isSelected());
                getSubTable().showColumn(LogFilterTableModel.COLUMN_MESSAGE,
                        check.isSelected());
            } else if (check.equals(m_chkEnableIncludeWord)
                    || check.equals(m_chkEnableExcludeWord)
                    || check.equals(m_chkEnableShowPid)
                    || check.equals(m_chkEnableShowTid)
                    || check.equals(m_chkEnableShowTag)
                    || check.equals(m_chkEnableRemoveTag)
                    || check.equals(m_chkEnableBookmarkTag)
                    || check.equals(m_chkEnableLogFlowTag)
                    || check.equals(m_chkEnableFileNameFilter)
                    || check.equals(m_chkEnableTimeTag)
                    || check.equals(m_chkEnableHighlight)) {
                useFilter(check);
            }
        }
    };

    ///////////////////////////////////热键///////////////////////////////////

    private final KeyEventDispatcher mKeyEventDispatcher = new KeyEventDispatcher() {
        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            if (!frameInfoProvider.isFrameFocused()) {
                return false;
            }

            // F2 上一个标签 F3 下一个标签
            // ctrl + F2 标记行（可多选）
            // ctrl + F 搜索关键词
            // F4 上一个搜索结果 F5 下一个搜索结果
            // ctrl + H 高亮关键词
            // ctrl + W 过滤msg关键词
            // ctrl + T 过滤tag关键词
            // ctrl + B 聚焦到log table
            // alt + 左箭头 上一个历史行 alt + 右箭头 下一个历史行
            boolean altPressed = Utils.isAltKeyPressed(e);
            boolean ctrlPressed = Utils.isControlKeyPressed(e);
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
                        showPanelAndSetSearchFocus();
                    }
                    break;
                case KeyEvent.VK_H:
                    if (e.getID() == KeyEvent.KEY_PRESSED && ctrlPressed) {
                        showPanelAndSetHighLightFocus();
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

    ///////////////////////////////////对话框///////////////////////////////////

    public void openFileBrowserToLoad(FileType type) {
        if (frameInfoProvider.getContainerFrame() == null) {
            return;
        }
        FileDialog fd = new FileDialog(frameInfoProvider.getContainerFrame(), "File open", FileDialog.LOAD);
        if (type == FileType.LOG) {
            fd.setDirectory(m_strLastDir);
        }
        fd.setMultipleMode(type == FileType.LOG);
        fd.setVisible(true);
        if (fd.getFile() != null) {
            switch (type) {
                case LOG:
                    parseLogFile(fd.getFiles()); // multi files
                    m_strLastDir = fd.getDirectory();
                    break;
                case MODE:
                    loadModeFile(new File(fd.getDirectory() + fd.getFile()));
                    break;
            }
        }
    }

    private void openPackagesView() {
        if (frameInfoProvider.getContainerFrame() == null) {
            return;
        }
        String title = "packages";
        String deviceID = null;
        if (m_selectedDevice != null) {
            title = m_selectedDevice.toString();
            deviceID = m_selectedDevice.code;
        }
        PackageViewDialog packageViewDialog = new PackageViewDialog(frameInfoProvider.getContainerFrame(), title, deviceID, new PackageViewDialog.PackageViewDialogListener() {

            @Override
            public void onFliterPidSelected(String value) {
                String pidShow = m_tbLogTable.GetFilterShowPid();
                if (pidShow.contains("|" + value)) {
                    pidShow = pidShow.replace("|" + value, "");
                } else if (pidShow.contains(value)) {
                    pidShow = pidShow.replace(value, "");
                } else {
                    pidShow += "|" + value;
                }
                m_tbLogTable.SetFilterShowPid(pidShow);
                LogFilterComponent.this.postEvent(new Event(EventBus.TYPE.EVENT_CHANGE_FILTER_SHOW_PID));
            }
        });
        packageViewDialog.setModal(false);
        packageViewDialog.setVisible(true);
    }

    private void openDumpsysView(String cmd) {
        if (frameInfoProvider.getContainerFrame() == null) {
            return;
        }
        String title = "Running Tasks";
        String deviceID = null;
        if (m_selectedDevice != null) {
            title = m_selectedDevice.toString();
            deviceID = m_selectedDevice.code;
        }
        DumpsysViewDialog dumpsysViewDialog = new DumpsysViewDialog(frameInfoProvider.getContainerFrame(), title, deviceID, cmd, new DumpsysViewDialog.DumpsysViewDialogListener() {


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

    private void openShowRowContentDialog(String content) {
        if (frameInfoProvider.getContainerFrame() == null) {
            return;
        }
        if (content == null || content.length() <= 0) {
            return;
        }
        String title = "Selected Rows";
        TextContentDialog contentDialog = new TextContentDialog(frameInfoProvider.getContainerFrame(), title, content);
        contentDialog.setModal(false);
        contentDialog.setVisible(true);
    }

    private void openFileBrowserToSave(FileType type) {
        if (frameInfoProvider.getContainerFrame() == null) {
            return;
        }
        FileDialog fd = new FileDialog(frameInfoProvider.getContainerFrame(), "File save", FileDialog.SAVE);
        if (type != FileType.MODE) {
            return;
        }
        fd.setVisible(true);
        if (fd.getFile() != null) {
            switch (type) {
                case MODE:
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
        mUIStateSaver.save(new UIStateSaver.DefaultPersistenceHelper(file.getAbsolutePath()));
    }

    private void loadModeFile(File file) {
        if (file == null) {
            T.e("mode file == null");
            return;
        }
        mUIStateSaver.load(new UIStateSaver.DefaultPersistenceHelper(file.getAbsolutePath()));
    }

    ///////////////////////////////////diff///////////////////////////////////

    @Override
    public void refreshDiffMenuBar() {
        if (mDiffService.getDiffServiceType() == DiffService.DiffServiceType.AS_SERVER) {
            if (mDiffService.isDiffConnected()) {
                mConnectDiffMenuItem.setEnabled(false);
                mDisconnectDiffMenuItem.setEnabled(true);
            } else {
                mConnectDiffMenuItem.setEnabled(true);
                mDisconnectDiffMenuItem.setEnabled(false);
            }
        }
    }

    @Override
    public void refreshUIWithDiffState() {
        if (!mDiffService.isDiffConnected()) {
            mSyncScrollCheckBox.setEnabled(false);
            mSyncSelectedCheckBox.setEnabled(false);
            mSyncScrollCheckBox.setVisible(false);
            mSyncSelectedCheckBox.setVisible(false);
            m_tfDiffState.setVisible(false);
            m_tfDiffState.setBackground(null);
            m_tfDiffState.setText("disconnected");
        } else {
            mSyncScrollCheckBox.setEnabled(true);
            mSyncSelectedCheckBox.setEnabled(true);
            mSyncScrollCheckBox.setVisible(true);
            mSyncSelectedCheckBox.setVisible(true);
            m_tfDiffState.setVisible(true);
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

    ///////////////////////////////////log flow///////////////////////////////////

    // 当前LogFlow运行到哪一行
    private int mLastProcessFlowLine = -1;

    @FieldSaveState
    private boolean mShowFlowInLogTable;

    private void initLogFlow() {
        File confDir = new File(Constant.LOG_FLOW_CONFIG_DIR);
        if (!confDir.exists()) {
            confDir.mkdirs();
            T.d("create log flow config directory: " + confDir.getAbsolutePath());
        }
        logFlowManager.init(confDir);

        m_tbLogTable.setShowLogFlowResult(mShowFlowInLogTable);
        m_tSublogTable.setShowLogFlowResult(mShowFlowInLogTable);

        // test
//        LogInfo logInfo1 = new LogInfo();
//        logInfo1.setLine(1);
//        logInfo1.setTag("RoomProXLog");
//        logInfo1.setMessage("enterRoom");
//
//        LogInfo logInfo2 = new LogInfo();
//        logInfo2.setLine(2);
//        logInfo2.setTag("RoomProXLog");
//        logInfo2.setMessage("leaveRoom");
//
//        LogInfo logInfo3 = new LogInfo();
//        logInfo3.setLine(3);
//        logInfo3.setTag("RoomProXLog");
//        logInfo3.setMessage("enterRoom");
//
//        LogInfo logInfo4 = new LogInfo();
//        logInfo4.setLine(4);
//        logInfo4.setTag("RoomProXLog");
//        logInfo4.setMessage("enterRoom");
//
//        LogInfo logInfo5 = new LogInfo();
//        logInfo5.setLine(5);
//        logInfo5.setTag("RoomProXLog");
//        logInfo5.setMessage("leaveRoom");
//
//        LogInfo logInfo6 = new LogInfo();
//        logInfo6.setLine(6);
//        logInfo6.setTag("RoomProXLog");
//        logInfo6.setMessage("leaveRoom");
//
//        LogFlowManager.getInstance().check(logInfo1);
//        LogFlowManager.getInstance().check(logInfo2);
//        LogFlowManager.getInstance().check(logInfo3);
//        LogFlowManager.getInstance().check(logInfo4);
//        LogFlowManager.getInstance().check(logInfo5);
//        LogFlowManager.getInstance().check(logInfo6);
//
//        List<LogFlowManager.FlowResult> currentResult = LogFlowManager.getInstance().getCurrentResult();
//        T.d(currentResult);
    }

    private void showAllFlow() {
        synchronized (FILTER_LOCK) {
            appendAllFlowLogAndSetLogState();
            Map<String, List<LogFlowManager.FlowResult>> flowResults = logFlowManager.getCurrentResult();
            if (flowResults.size() > 0) {
                LogFlowDialog dialog = new LogFlowDialog(logFlowManager, flowResults);
                dialog.setListener(new LogFlowDialog.Listener() {
                    @Override
                    public void onOkButtonClicked(LogFlowDialog dialog) {
                        dialog.hide();
                    }

                    @Override
                    public void onItemSelected(LogFlowDialog dialog, LogFlowDialog.ResultItem result) {
                        // jump to result line
                        m_tbLogTable.changeSelection(result.logInfo);
                    }

                    @Override
                    public void onMarkItem(LogFlowDialog logFlowDialog, LogFlowDialog.ResultItem resultItem) {
                        if (resultItem != null) {
                            LogInfo logInfo = resultItem.logInfo;
                            markLogInfo(0, logInfo.getLine() - 1, !logInfo.isMarked());
                        }
                    }
                });
                dialog.show();
            }
        }
    }

    /*
    把LogInfoAll全部推倒log flow manager里
     */
    private void appendAllFlowLogAndSetLogState() {
        if (mLastProcessFlowLine <= 0) {
            logFlowManager.reset();
        }
        if (mLastProcessFlowLine < m_arLogInfoAll.size() - 1) {
            List<LogInfo> logInfos = new ArrayList<>(m_arLogInfoAll).subList(mLastProcessFlowLine + 1, m_arLogInfoAll.size());
            for (LogInfo logInfo : logInfos) {
                appendFlowLogAndSetLogState(logInfo);
            }
        }
    }

    /*
    添加一条log到flow中
     */
    private boolean appendFlowLogAndSetLogState(LogInfo logInfo) {
        mLastProcessFlowLine = logInfo.getLine();
        Map<String, LogFlowManager.FlowResultLine> checkResult = logFlowManager.check(logInfo);
        if (checkResult != null && checkResult.size() > 0) {
            logInfo.setFlowResults(new ArrayList<>(checkResult.values()));
            return true;
        }
        return false;
    }

    /*
    是否在logtable显示flow result
     */
    private void handleShowFlowInLogTableStateChanged(boolean showInLogTable) {
        if (mShowFlowInLogTable != showInLogTable) {
            mShowFlowInLogTable = showInLogTable;
            m_tbLogTable.setShowLogFlowResult(mShowFlowInLogTable);
            m_tSublogTable.setShowLogFlowResult(mShowFlowInLogTable);
            // refresh
            appendAllFlowLogAndSetLogState();

            // 关掉的时候disable logflow filter
            if (!mShowFlowInLogTable) {
                m_chkEnableLogFlowTag.setSelected(false);
                m_chkEnableLogFlowTag.setEnabled(false);
            } else {
                m_chkEnableLogFlowTag.setEnabled(true);
            }

            ((AbstractTableModel) m_tbLogTable.getModel()).fireTableDataChanged();
            ((AbstractTableModel) m_tSublogTable.getModel()).fireTableDataChanged();
        }
    }

    //////////////////////////////////////////////////////////////////////

    public LogTable getLogTable() {
        return m_tbLogTable;
    }

    public File[] getLastParseredFiles() {
        return mLastParseredFiles;
    }

    public SubLogTable getSubTable() {
        return m_tSublogTable;
    }

    @Override
    public void searchSimilar(String cmd) {
        m_tbLogTable.searchSimilarForward(cmd);
    }

    @Override
    public void searchTimestamp(String cmd) {
        try {
            long timestamp = Long.parseLong(cmd);
            m_tbLogTable.selectTargetLogInfoInTimestamp(timestamp);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void compareWithSelectedRows(String targetRows) {
        String fmtRows = m_tbLogTable.getFormatSelectedRows(
                new int[]{LogFilterTableModel.COLUMN_LINE, LogFilterTableModel.COLUMN_TIME}
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

            Utils.runCmd(new String[]{Constant.DIFF_PROGRAM_PATH, tempFile1.getAbsolutePath(), tempFile2.getAbsolutePath()});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int mLastVBarValue = 0;
    private final AdjustmentListener mScrollListener = new AdjustmentListener() {
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

    @Override
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

    @Override
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

    @Override
    public void handleSelectedForwardSyncEvent(String cmd) {
        m_tbLogTable.searchSimilarForward(cmd);
    }

    @Override
    public void handleSelectedBackwardSyncEvent(String cmd) {
        m_tbLogTable.searchSimilarBackward(cmd);
    }


    ///////////////////////////////////logParser///////////////////////////////////

    private void switchToLogParser(int parserType) {
        m_iLogParser = sTypeToParserMap.get(parserType);
        m_tfParserType.setText(sTypeToParserNameMap.get(parserType));
        if (parserType == m_parserType) {
            return;
        }
        m_parserType = parserType;
        loadTableColumnState();

        if (parserType != Constant.PARSER_TYPE_LOGCAT
                && m_arLogInfoAll.size() > 0
                && mLastParseredFiles != null
                && mLastParseredFiles.length > 0
        ) {
            parseLogFile(mLastParseredFiles);
        }
    }

    /**
     * 自己从tabpane中被移除
     *
     * @param index
     */
    @Override
    public void onCloseTab(int index) {
        for (FloatingFrameInfo frameInfo : mFloatingFrameInfos) {
            if (!frameInfo.isRemoved) {
                frameInfo.frame.dispatchEvent(
                        new WindowEvent(frameInfo.frame, WindowEvent.WINDOW_CLOSING)
                );
            }
        }
        mFloatingFrameInfos.clear();
        debouncer.shutdown();
    }

    ///////////////////////////////////interface///////////////////////////////////

    enum FileType {
        LOG, MODE
    }

    enum LoadingState {
        IDLE,
        LOADING,
    }

    private static class TargetDevice {
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

    private interface FloatingTagetProvider {
        Component getTarget();
    }
}


