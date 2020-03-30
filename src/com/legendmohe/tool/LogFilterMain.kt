package com.legendmohe.tool

import com.legendmohe.tool.annotation.CheckBoxSaveState
import com.legendmohe.tool.annotation.FieldSaveState
import com.legendmohe.tool.annotation.TextFieldSaveState
import com.legendmohe.tool.annotation.UIStateSaver
import com.legendmohe.tool.config.Constant
import com.legendmohe.tool.diff.DiffService
import com.legendmohe.tool.diff.DiffService.DiffServiceType
import com.legendmohe.tool.logflow.LogFlowManager
import com.legendmohe.tool.logtable.BaseLogTable.BaseLogTableListener
import com.legendmohe.tool.logtable.LogTable
import com.legendmohe.tool.logtable.SubLogTable
import com.legendmohe.tool.logtable.model.LogFilterTableModel
import com.legendmohe.tool.parser.*
import com.legendmohe.tool.thirdparty.util.OsCheck
import com.legendmohe.tool.view.*
import com.legendmohe.tool.view.DumpsysViewDialog.DumpsysViewDialogListener
import com.legendmohe.tool.view.LogFlowDialog.ResultItem
import com.legendmohe.tool.view.PackageViewDialog.PackageViewDialogListener
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import java.awt.event.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.swing.*
import javax.swing.border.Border
import javax.swing.border.EmptyBorder
import javax.swing.border.EtchedBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.MenuEvent
import javax.swing.event.MenuListener
import javax.swing.plaf.FontUIResource
import javax.swing.table.AbstractTableModel
import javax.swing.text.DefaultEditorKit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class LogFilterMain private constructor() : JFrame(), EventBus, BaseLogTableListener, IDiffCmdHandler {
    var m_arLogInfoAll: ArrayList<LogInfo> = ArrayList()
    var m_arLogInfoFiltered: ArrayList<LogInfo> = ArrayList()
    var m_hmMarkedInfoAll: HashMap<Int, Int> = HashMap()
    var m_hmMarkedInfoFiltered: HashMap<Int, Int> = HashMap()
    var m_hmErrorAll: ConcurrentHashMap<Int, Int> = ConcurrentHashMap()
    var m_hmErrorFiltered: ConcurrentHashMap<Int, Int> = ConcurrentHashMap()
    lateinit var m_iLogParser: ILogParser

    //////////////////////////////////////////////////////////////////////
    lateinit var m_tfStatus: JLabel
    lateinit var m_ipIndicator: IndicatorPanel
    lateinit var logTable: LogTable

    lateinit var m_logScrollVPane: JScrollPane
    lateinit var m_tmLogTableModel: LogFilterTableModel
    var mFilterEnabled = false

    // Word Filter, tag filter
    lateinit var m_tfSearch: JTextField
    lateinit var m_tfHighlight: JTextField

    @TextFieldSaveState
    lateinit var m_tfIncludeWord: JTextField

    @TextFieldSaveState
    lateinit var m_tfExcludeWord: JTextField

    @TextFieldSaveState
    lateinit var m_tfShowTag: JTextField

    @TextFieldSaveState
    lateinit var m_tfRemoveTag: JTextField

    @TextFieldSaveState
    lateinit var m_tfShowPid: JTextField

    @TextFieldSaveState
    lateinit var m_tfShowTid: JTextField

    @TextFieldSaveState
    lateinit var m_tfBookmarkTag: JTextField

    @TextFieldSaveState
    lateinit var m_tfFontSize: JTextField

    @TextFieldSaveState
    lateinit var m_tfShowFileName: JTextField
    private lateinit var m_tfFromTimeTag: JTextField
    private lateinit var m_tfToTimeTag: JTextField
    private lateinit var m_tfGoto: JTextField

    // Device
    lateinit var m_btnDevice: JButton
    lateinit var m_lDeviceList: JList<TargetDevice>
    lateinit var m_comboDeviceCmd: JComboBox<String>
    lateinit var m_comboCmd: JComboBox<String>
    lateinit var m_btnSetFont: JButton

    // Log filter enable/disable
    @CheckBoxSaveState
    lateinit var m_chkEnableIncludeWord: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkEnableExcludeWord: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkEnableShowTag: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkEnableRemoveTag: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkEnableShowPid: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkEnableShowTid: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkEnableHighlight: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkEnableBookmarkTag: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkEnableLogFlowTag: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkEnableFileNameFilter: JCheckBox
    private lateinit var m_chkEnableTimeTag: JCheckBox

    // Log filter
    @CheckBoxSaveState
    lateinit var m_chkVerbose: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkDebug: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkInfo: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkWarn: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkError: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkFatal: JCheckBox

    // Show column
    @CheckBoxSaveState
    lateinit var m_chkClmBookmark: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkClmLine: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkClmDate: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkClmTime: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkClmLogLV: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkClmPid: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkClmThread: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkClmTag: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkClmMessage: JCheckBox

    @CheckBoxSaveState
    lateinit var m_chkClmFile: JCheckBox
    lateinit var m_comboEncode: JComboBox<String>

    //    JComboBox m_jcFontType;
    lateinit var m_btnRun: JButton
    lateinit var m_btnClear: JButton
    lateinit var m_tbtnPause: JToggleButton
    lateinit var m_btnStop: JButton
    var m_strLogFileName: String? = null
    var m_selectedDevice: TargetDevice? = null

    // String m_strProcessCmd;
    var m_Process: Process? = null
    var m_thProcess: Thread? = null
    var m_thWatchFile: Thread? = null
    var m_thFilterParse: Thread? = null
    var m_bPauseADB = false
    var FILE_LOCK: Object = Object()
    var FILTER_LOCK: Object = Object()

    @JvmField
    @Volatile
    var mLogParsingState = Constant.PARSING_STATUS_READY
    var m_nFilterLogLV = LogInfo.LOG_LV_ALL

    @FieldSaveState
    var m_nWinWidth = Constant.DEFAULT_WIDTH

    @FieldSaveState
    var m_nWinHeight = Constant.DEFAULT_HEIGHT
    var m_nLastWidth = 0
    var m_nLastHeight = 0

    @FieldSaveState
    var mWindowState = 0
    var mRecentMenu: RecentFileMenu? = null

    @FieldSaveState
    var m_parserType = Constant.PARSER_TYPE_DEFAULT_LOG

    @FieldSaveState
    var m_colWidths = LogFilterTableModel.DEFAULT_WIDTH

    @FieldSaveState
    private var m_strLastDir: String? = null
    private lateinit var mDisconnectDiffMenuItem: JMenuItem
    private lateinit var mConnectDiffMenuItem: JMenuItem
    var mDiffService: DiffService? = null
    private lateinit var m_tfDiffPort: JLabel
    private lateinit var m_tfDiffState: JLabel
    private lateinit var m_tfParserType: JLabel
    private lateinit var mSyncScrollCheckBox: JCheckBox
    private lateinit var mSyncSelectedCheckBox: JCheckBox
    private var mSyncScrollEnable = false
    private var mSyncScrollSelected = false
    private val mUIStateSaver: UIStateSaver
    private lateinit var mSplitPane: ExpandableSplitPane

    @FieldSaveState
    private var mLogSplitPaneDividerLocation = -1
    private lateinit var mMainSplitPane: ExpandableSplitPane

    @FieldSaveState
    private var mMainSplitPaneDividerLocation = -1
    var subTable: SubLogTable? = null
        private set
    private lateinit var m_tSubLogTableModel: LogFilterTableModel
    private lateinit var m_subLogScrollVPane: JScrollPane

    var m_arSubLogInfoAll: ArrayList<LogInfo> = ArrayList()
    private lateinit var mSearchPanel: JPanel

    @FieldSaveState
    private val mFilterTagHistory: MutableSet<String> = HashSet()

    // 当前处理的文件集合
    private var mLastParseredFiles: Array<File?>? = null
    private val mRecentlyInputHistory: MutableMap<Component?, MutableList<String>> = HashMap()

    ///////////////////////////////////setup process///////////////////////////////////
    private fun setupMenuBar() {
        val menuBar = JMenuBar()
        val fileMenu = JMenu("File")
        fileMenu.mnemonic = KeyEvent.VK_F
        val fileOpen = JMenuItem("Open")
        fileOpen.mnemonic = KeyEvent.VK_O
        fileOpen.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_O,
                ActionEvent.ALT_MASK)
        fileOpen.toolTipText = "Open log file"
        fileOpen.addActionListener { openFileBrowserToLoad(FileType.LOG) }
        val modeMenu = JMenu("Mode")
        val modeOpen = JMenuItem("Open Mode File")
        modeOpen.toolTipText = "Open .mode file"
        modeOpen.addActionListener { openFileBrowserToLoad(FileType.MODE) }
        val modeSave = JMenuItem("Save Mode File")
        modeSave.toolTipText = "Save .mode file"
        modeSave.addActionListener { openFileBrowserToSave(FileType.MODE) }
        modeMenu.add(modeOpen)
        modeMenu.add(modeSave)
        mRecentMenu = object : RecentFileMenu("RecentFile", 10) {
            override fun onSelectFile(filePath: String) {
                val files = filePath.split("\\|".toRegex()).toTypedArray()
                val recentFiles = arrayOfNulls<File>(files.size)
                for (i in files.indices) {
                    recentFiles[i] = File(files[i])
                }
                parseLogFile(recentFiles)
            }
        }
        fileMenu.add(fileOpen)
        fileMenu.add(modeMenu)
        fileMenu.add(mRecentMenu)
        val toolsMenu = JMenu("Tools")
        val viewMenu = initAndGetViewMenu(this)
        toolsMenu.add(viewMenu)
        val diffMenu = JMenu("Diff Service")
        mDisconnectDiffMenuItem = JMenuItem("disconnect")
        mDisconnectDiffMenuItem.isEnabled = false
        mDisconnectDiffMenuItem.addActionListener {
            mDiffService!!.disconnectDiffClient()
            mConnectDiffMenuItem.isEnabled = true
            mDisconnectDiffMenuItem.isEnabled = false
        }
        mConnectDiffMenuItem = JMenuItem("connect to diff server")
        mConnectDiffMenuItem.addActionListener {
            val serverPort = JOptionPane.showInputDialog(
                    this@LogFilterMain,
                    "Enter Server Port",
                    "",
                    JOptionPane.QUESTION_MESSAGE
            )
            if (serverPort != null && serverPort.length != 0) {
                if (mDiffService!!.setupDiffClient(serverPort)) {
                    mConnectDiffMenuItem.isEnabled = false
                    mDisconnectDiffMenuItem.isEnabled = true
                }
            }
        }
        diffMenu.add(mConnectDiffMenuItem)
        diffMenu.add(mDisconnectDiffMenuItem)
        toolsMenu.add(diffMenu)
        val converterItem = JMenuItem("text converter")
        converterItem.toolTipText = "convert all kinds of log msg"
        converterItem.addActionListener { TextConverterDialog().show() }
        toolsMenu.add(converterItem)
        val parserMenu = JMenu("Parser")
        parserMenu.addMenuListener(object : MenuListener {
            override fun menuSelected(e: MenuEvent) {
                val menuItem = parserMenu.getItem(m_parserType)
                menuItem.isSelected = true
            }

            override fun menuDeselected(e: MenuEvent) {}
            override fun menuCanceled(e: MenuEvent) {}
        })
        val defaultLogParserMenu = JRadioButtonMenuItem("DefaultLog Parser", m_parserType == Constant.PARSER_TYPE_DEFAULT_LOG)
        defaultLogParserMenu.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                switchToLogParser(Constant.PARSER_TYPE_DEFAULT_LOG)
            }
        }
        parserMenu.add(defaultLogParserMenu)
        val logcatParserMenu = JRadioButtonMenuItem("Logcat Parser", m_parserType == Constant.PARSER_TYPE_LOGCAT)
        logcatParserMenu.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                switchToLogParser(Constant.PARSER_TYPE_LOGCAT)
            }
        }
        parserMenu.add(logcatParserMenu)
        val bigoParserMenu = JRadioButtonMenuItem("BigoDevLog Parser", m_parserType == Constant.PARSER_TYPE_BIGO_DEV_LOG)
        bigoParserMenu.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                switchToLogParser(Constant.PARSER_TYPE_BIGO_DEV_LOG)
            }
        }
        parserMenu.add(bigoParserMenu)
        val bigoXLogParserMenu = JRadioButtonMenuItem("BigoXLog Parser", m_parserType == Constant.PARSER_TYPE_BIGO_XLOG)
        bigoXLogParserMenu.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                switchToLogParser(Constant.PARSER_TYPE_BIGO_XLOG)
            }
        }
        parserMenu.add(bigoXLogParserMenu)
        val imoDevLogParserMenu = JRadioButtonMenuItem("IMODevLog Parser", m_parserType == Constant.PARSER_TYPE_IMO_DEV_LOG)
        imoDevLogParserMenu.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                switchToLogParser(Constant.PARSER_TYPE_IMO_DEV_LOG)
            }
        }
        parserMenu.add(imoDevLogParserMenu)

        // 就这样放进去就可以了。。。
        val parserBG = ButtonGroup()
        parserBG.add(defaultLogParserMenu)
        parserBG.add(logcatParserMenu)
        parserBG.add(bigoParserMenu)
        parserBG.add(bigoXLogParserMenu)
        parserBG.add(imoDevLogParserMenu)
        val flowMenu = JMenu("Flow")
        val showAllFlow = JMenuItem("show all log flow")
        showAllFlow.addActionListener { showAllFlow() }
        val showFlowInLogTable = JCheckBoxMenuItem("show log flow in line")
        showFlowInLogTable.addItemListener { handleShowFlowInLogTableStateChanged(showFlowInLogTable.state) }
        showFlowInLogTable.state = mShowFlowInLogTable
        flowMenu.add(showFlowInLogTable)
        flowMenu.add(showAllFlow)
        menuBar.add(fileMenu)
        menuBar.add(toolsMenu)
        menuBar.add(parserMenu)
        menuBar.add(flowMenu)
        this.jMenuBar = menuBar
    }

    private fun initAndGetViewMenu(mainFrame: LogFilterMain): JMenu {
        val viewMenu = JMenu("View")
        val packagesMenuItem = JMenuItem("Show Running Packages")
        packagesMenuItem.toolTipText = "show running packages on current android device"
        packagesMenuItem.addActionListener { mainFrame.openPackagesView() }
        viewMenu.add(packagesMenuItem)
        val ActivitiesMenuItem = JMenuItem("Show Running Activities")
        ActivitiesMenuItem.toolTipText = "show running activities on current android device"
        ActivitiesMenuItem.addActionListener { mainFrame.openDumpsysView("dumpsys activity activities") }
        viewMenu.add(ActivitiesMenuItem)
        val pendingAlarmMenuItem = JMenuItem("Show Pending Alarms")
        pendingAlarmMenuItem.toolTipText = "show pending alarms on current android device"
        pendingAlarmMenuItem.addActionListener { mainFrame.openDumpsysView("dumpsys alarm") }
        viewMenu.add(pendingAlarmMenuItem)
        val pendingIntentMenuItem = JMenuItem("Show Pending Intents")
        pendingIntentMenuItem.toolTipText = "show pending intents on current android device"
        pendingIntentMenuItem.addActionListener { mainFrame.openDumpsysView("dumpsys activity intents") }
        viewMenu.add(pendingIntentMenuItem)
        val meminfoMenuItem = JMenuItem("Show Memory Info")
        meminfoMenuItem.toolTipText = "show memory info on current android device"
        meminfoMenuItem.addActionListener { mainFrame.openDumpsysView("dumpsys meminfo") }
        viewMenu.add(meminfoMenuItem)
        val cpuinfoMenuItem = JMenuItem("Show CPU Info")
        cpuinfoMenuItem.toolTipText = "show CPU info on current android device"
        cpuinfoMenuItem.addActionListener { mainFrame.openDumpsysView("dumpsys cpuinfo") }
        viewMenu.add(cpuinfoMenuItem)
        val customMenu = JMenu("Custom Info")
        loadCustomMenu(mainFrame, customMenu)
        viewMenu.add(customMenu)
        return viewMenu
    }

    private fun loadCustomMenu(mainFrame: LogFilterMain, customMenu: JMenu) {
        val p = Properties()
        try {
            p.load(FileInputStream(Constant.INI_FILE_DUMP_SYS))
        } catch (e: FileNotFoundException) {
            T.d(Constant.INI_FILE_DUMP_SYS + " not exist!")
            return
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }
        val names = p.propertyNames()
        while (names.hasMoreElements()) {
            val name = names.nextElement() as String
            val cmd = p.getProperty(name)
            if (cmd != null) {
                val newMenuItem = JMenuItem(name)
                newMenuItem.addActionListener { mainFrame.openDumpsysView(cmd) }
                customMenu.add(newMenuItem)
            }
        }
    }

    private fun restoreSplitPane() {
        mSplitPane.resizeWeight = 1.0
        mSplitPane.isOneTouchExpandable = true
        if (mLogSplitPaneDividerLocation > 0) {
            mSplitPane.dividerLocation = mLogSplitPaneDividerLocation
        }
        mMainSplitPane.resizeWeight = 1.0
        mMainSplitPane.isOneTouchExpandable = true
        if (mMainSplitPaneDividerLocation > 0) {
            mMainSplitPane.dividerLocation = mMainSplitPaneDividerLocation
        }
    }

    private fun makeFilename(): String {
        val now = Date()
        val format = SimpleDateFormat("yyyyMMdd_HHmmss")
        return Constant.OUTPUT_LOG_DIR + File.separator + "LogFilter_" + format.format(now) + ".txt"
    }

    private fun exit() {
        if (m_Process != null) m_Process!!.destroy()
        if (m_thProcess != null) m_thProcess!!.interrupt()
        if (m_thWatchFile != null) m_thWatchFile!!.interrupt()
        if (m_thFilterParse != null) m_thFilterParse!!.interrupt()
        saveColor()
        m_nWinWidth = size.width
        m_nWinHeight = size.height
        mLogSplitPaneDividerLocation = mSplitPane.dividerLocation
        mMainSplitPaneDividerLocation = mMainSplitPane.dividerLocation
        mUIStateSaver.save()
        System.exit(0)
    }

    private fun loadUI() {
        minimumSize = Dimension(Constant.MIN_WIDTH, Constant.MIN_HEIGHT)
        extendedState = mWindowState
        preferredSize = Dimension(m_nWinWidth, m_nWinHeight)
        loadTableColumnState()
        logTable.setFontSize(m_tfFontSize
                .text?.toInt() ?: 12)
        subTable!!.setFontSize(m_tfFontSize
                .text?.toInt() ?: 12)
        updateLogTable(-1, false)
    }

    fun loadCmd() {
        try {
            val p = Properties()
            try {
                p.load(FileInputStream(Constant.INI_FILE_CMD))
            } catch (e: FileNotFoundException) {
                T.d(Constant.INI_FILE_CMD + " not exist!")
            }
            if (p.getProperty(Constant.INI_CMD_COUNT) == null) {
                p.setProperty(Constant.INI_CMD + "0", Constant.ANDROID_THREAD_CMD)
                p.setProperty(Constant.INI_CMD + "1", Constant.ANDROID_DEFAULT_CMD)
                p.setProperty(Constant.INI_CMD + "2", Constant.ANDROID_RADIO_CMD)
                p.setProperty(Constant.INI_CMD + "3", Constant.ANDROID_EVENT_CMD)
                p.setProperty(Constant.INI_CMD + "4", Constant.ANDROID_CUSTOM_CMD)
                p.setProperty(Constant.INI_CMD_COUNT, "5")
                p.store(FileOutputStream(Constant.INI_FILE_CMD), null)
            }
            T.d("p.getProperty(INI_CMD_COUNT) = "
                    + p.getProperty(Constant.INI_CMD_COUNT))
            val nCount = p.getProperty(Constant.INI_CMD_COUNT).toInt()
            T.d("nCount = $nCount")
            for (nIndex in 0 until nCount) {
                T.d("CMD = " + Constant.INI_CMD + nIndex)
                m_comboCmd.addItem(p.getProperty(Constant.INI_CMD + nIndex))
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    private fun loadParser() {
        switchToLogParser(m_parserType)
    }

    fun loadColor() {
        try {
            val p = Properties()
            p.load(FileInputStream(Constant.INI_FILE_COLOR))
            Constant.COLOR_0 = p.getProperty(Constant.INI_COLOR_0)
                    .replace("0x", "").toInt(16)
            Constant.COLOR_1 = p.getProperty(Constant.INI_COLOR_1)
                    .replace("0x", "").toInt(16)
            Constant.COLOR_2 = p.getProperty(Constant.INI_COLOR_2)
                    .replace("0x", "").toInt(16)
            Constant.COLOR_3 = p
                    .getProperty(Constant.INI_COLOR_3).replace("0x", "").toInt(16)
            Constant.COLOR_ERROR = Constant.COLOR_3
            Constant.COLOR_4 = p
                    .getProperty(Constant.INI_COLOR_4).replace("0x", "").toInt(16)
            Constant.COLOR_WARN = Constant.COLOR_4
            Constant.COLOR_5 = p.getProperty(Constant.INI_COLOR_5)
                    .replace("0x", "").toInt(16)
            Constant.COLOR_6 = p
                    .getProperty(Constant.INI_COLOR_6).replace("0x", "").toInt(16)
            Constant.COLOR_INFO = Constant.COLOR_6
            Constant.COLOR_7 = p
                    .getProperty(Constant.INI_COLOR_7).replace("0x", "").toInt(16)
            Constant.COLOR_DEBUG = Constant.COLOR_7
            Constant.COLOR_8 = p
                    .getProperty(Constant.INI_COLOR_8).replace("0x", "").toInt(16)
            Constant.COLOR_FATAL = Constant.COLOR_8
            val nCount = p.getProperty(Constant.INI_HIGILIGHT_COUNT,
                    "0").toInt()
            if (nCount > 0) {
                Constant.COLOR_HIGHLIGHT = arrayOfNulls(nCount)
                for (nIndex in 0 until nCount) Constant.COLOR_HIGHLIGHT[nIndex] = p.getProperty(
                        Constant.INI_HIGILIGHT_ + nIndex).replace("0x", "")
            } else {
                Constant.COLOR_HIGHLIGHT = arrayOfNulls(1)
                Constant.COLOR_HIGHLIGHT[0] = "ffff"
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    fun saveColor() {
        try {
            val p = Properties()
            p.setProperty(Constant.INI_COLOR_0,
                    "0x" + Integer.toHexString(Constant.COLOR_0).toUpperCase())
            p.setProperty(Constant.INI_COLOR_1,
                    "0x" + Integer.toHexString(Constant.COLOR_1).toUpperCase())
            p.setProperty(Constant.INI_COLOR_2,
                    "0x" + Integer.toHexString(Constant.COLOR_2).toUpperCase())
            p.setProperty(Constant.INI_COLOR_3,
                    "0x" + Integer.toHexString(Constant.COLOR_3).toUpperCase())
            p.setProperty(Constant.INI_COLOR_4,
                    "0x" + Integer.toHexString(Constant.COLOR_4).toUpperCase())
            p.setProperty(Constant.INI_COLOR_5,
                    "0x" + Integer.toHexString(Constant.COLOR_5).toUpperCase())
            p.setProperty(Constant.INI_COLOR_6,
                    "0x" + Integer.toHexString(Constant.COLOR_6).toUpperCase())
            p.setProperty(Constant.INI_COLOR_7,
                    "0x" + Integer.toHexString(Constant.COLOR_7).toUpperCase())
            p.setProperty(Constant.INI_COLOR_8,
                    "0x" + Integer.toHexString(Constant.COLOR_8).toUpperCase())
            if (Constant.COLOR_HIGHLIGHT != null) {
                p.setProperty(Constant.INI_HIGILIGHT_COUNT, ""
                        + Constant.COLOR_HIGHLIGHT.size)
                for (nIndex in Constant.COLOR_HIGHLIGHT.indices) p.setProperty(Constant.INI_HIGILIGHT_ + nIndex, "0x"
                        + Constant.COLOR_HIGHLIGHT[nIndex].toUpperCase())
            }
            p.store(FileOutputStream(Constant.INI_FILE_COLOR), "done.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
    fun addDesc() {
        appendDescToTable(Constant.VERSION)
        appendDescToTable("")
        appendDescToTable("Xinyu.he fork from https://github.com/iookill/LogFilter")
        appendDescToTable("")
        appendDescToTable("<Hot key>")
        appendDescToTable("F2 上一个标签 F3 下一个标签")
        appendDescToTable("ctrl + F2 标记行（可多选）")
        appendDescToTable("ctrl + F 搜索关键词")
        appendDescToTable("F4 上一个搜索结果 F5 下一个搜索结果")
        appendDescToTable("trl + H 高亮关键词")
        appendDescToTable("ctrl + W 过滤msg关键词")
        appendDescToTable("ctrl + T 过滤tag关键词")
        appendDescToTable("ctrl + B 聚焦到log table")
        appendDescToTable("alt + 左箭头 上一个历史行 alt + 右箭头 下一个历史行")
    }

    private fun appendDescToTable(strMessage: String) {
        val logInfo = LogInfo()
        logInfo.line = m_arLogInfoAll.size + 1
        logInfo.message = strMessage
        m_arLogInfoAll.add(logInfo)
    }

    override fun markLogInfo(nIndex: Int, line: Int, isMark: Boolean) {
        synchronized(FILTER_LOCK) {
            val logInfo = m_arLogInfoAll[line]
            logInfo.isMarked = isMark
            m_arLogInfoAll[line] = logInfo
            if (logInfo.isMarked) {
                m_arSubLogInfoAll.add(logInfo)
                m_hmMarkedInfoAll[line] = line
                if (mFilterEnabled) m_hmMarkedInfoFiltered[line] = m_arLogInfoFiltered.size
            } else {
                m_arSubLogInfoAll.remove(logInfo)
                m_hmMarkedInfoAll.remove(line)
                if (mFilterEnabled) m_hmMarkedInfoFiltered.remove(line)
            }
        }
        m_ipIndicator.repaint()
        m_arSubLogInfoAll.sortWith(Comparator { o1, o2 -> o1.line - o2.line })
        updateSubTable(-1)
    }

    override fun showInMarkTable(selectedRow: Int, line: Int) {
        synchronized(FILTER_LOCK) {
            val target = m_arLogInfoAll[line]
            subTable!!.changeSelection(target)
        }
    }

    override fun showRowsContent(content: String) {
        if (content.isNotEmpty()) {
            openShowRowContentDialog(content)
        }
    }

    override fun getSupportedColumns(): IntArray {
        if (!::m_iLogParser.isInitialized)
            return AbstractLogParser.gDefColumns
        return m_iLogParser.supportedColumns
    }

    fun clearData() {
        m_arSubLogInfoAll.clear()
        m_arLogInfoAll.clear()
        m_arLogInfoFiltered.clear()
        m_hmMarkedInfoAll.clear()
        m_hmMarkedInfoFiltered.clear()
        m_hmErrorAll.clear()
        m_hmErrorFiltered.clear()
        mLastProcessFlowLine = -1
        LogFlowManager.getInstance().reset()
    }

    fun getIndicatorPanel(): Component {
        val jp = JPanel()
        jp.layout = BorderLayout()
        m_ipIndicator = IndicatorPanel(this)
        m_ipIndicator.setData(m_arLogInfoAll, m_hmMarkedInfoAll, m_hmErrorAll)
        jp.add(m_ipIndicator, BorderLayout.CENTER)
        return jp
    }

    fun getDevicePanel(): Component {
        val jpOptionDevice = JPanel()
        jpOptionDevice.border = BorderFactory
                .createTitledBorder("Device select")
        jpOptionDevice.layout = BorderLayout()
        val jpCmd = JPanel()
        m_comboDeviceCmd = JComboBox()
        m_comboDeviceCmd.addItem(Constant.COMBO_ANDROID)
        m_comboDeviceCmd.addItemListener(ItemListener { e ->
            if (e.stateChange != ItemEvent.SELECTED) return@ItemListener
            val listModel = m_lDeviceList
                    .model as DefaultListModel<*>
            listModel.clear()
            if (e.item == Constant.COMBO_CUSTOM_COMMAND) {
                m_comboDeviceCmd.setEditable(true)
            } else {
                m_comboDeviceCmd.setEditable(false)
            }
        })
        m_btnDevice = JButton("OK")
        m_btnDevice.margin = Insets(0, 0, 0, 0)
        m_btnDevice.addActionListener(m_alButtonListener)
        jpCmd.add(m_comboDeviceCmd)
        jpCmd.add(m_btnDevice)
        jpOptionDevice.add(jpCmd, BorderLayout.NORTH)
        val listModel = DefaultListModel<TargetDevice>()
        m_lDeviceList = JList(listModel)
        val vbar = JScrollPane(m_lDeviceList)
        vbar.preferredSize = Dimension(100, 50)
        m_lDeviceList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        m_lDeviceList.addListSelectionListener { e ->
            val deviceList = e.source as JList<*>
            m_selectedDevice = deviceList.selectedValue as TargetDevice
        }
        jpOptionDevice.add(vbar, BorderLayout.CENTER)
        val cmdPanel = JPanel(BorderLayout())
        val funcPanel = JPanel(GridLayout(1, 4))
        funcPanel.preferredSize = Dimension(100, 20)
        m_btnClear = JButton("Clear")
        m_btnClear.margin = Insets(0, 0, 0, 0)
        m_btnClear.isEnabled = false
        m_btnRun = JButton("Run")
        m_btnRun.margin = Insets(0, 0, 0, 0)
        m_tbtnPause = JToggleButton("Pause")
        m_tbtnPause.margin = Insets(0, 0, 0, 0)
        m_tbtnPause.isEnabled = false
        m_btnStop = JButton("Stop")
        m_btnStop.margin = Insets(0, 0, 0, 0)
        m_btnStop.isEnabled = false
        m_btnRun.addActionListener(m_alButtonListener)
        m_btnStop.addActionListener(m_alButtonListener)
        m_btnClear.addActionListener(m_alButtonListener)
        m_tbtnPause.addActionListener(m_alButtonListener)
        funcPanel.add(m_btnClear)
        funcPanel.add(m_btnRun)
        funcPanel.add(m_btnStop)
        funcPanel.add(m_tbtnPause)
        cmdPanel.add(funcPanel, BorderLayout.SOUTH)
        val adbPanel = JPanel()
        val jlProcessCmd = JLabel("Cmd : ")
        m_comboCmd = JComboBox()
        m_comboCmd.preferredSize = Dimension(180, 25)
        adbPanel.add(jlProcessCmd)
        adbPanel.add(m_comboCmd)
        cmdPanel.add(adbPanel)
        jpOptionDevice.add(cmdPanel, BorderLayout.SOUTH)
        return jpOptionDevice
    }

    fun addLogInfo(logInfo: LogInfo) {
        synchronized(FILTER_LOCK) {
            m_arLogInfoAll.add(logInfo)
            // 实时显示log flow
            if (mShowFlowInLogTable) {
                appendFlowLogAndSetLogState(logInfo)
            }
            if (logInfo.logLV == "E" || logInfo.logLV == "ERROR") m_hmErrorAll[logInfo.line - 1] = logInfo.line - 1
            if (mFilterEnabled) {
                if (m_ipIndicator.m_chBookmark.isSelected
                        || m_ipIndicator.m_chError.isSelected) {
                    var bAddFilteredArray = false
                    if (logInfo.isMarked
                            && m_ipIndicator.m_chBookmark.isSelected) {
                        bAddFilteredArray = true
                        m_hmMarkedInfoFiltered[logInfo.line - 1] = m_arLogInfoFiltered.size
                        if (logInfo.logLV == "E" || logInfo.logLV == "ERROR") m_hmErrorFiltered[logInfo.line - 1] = m_arLogInfoFiltered.size
                    }
                    if ((logInfo.logLV == "E" || (logInfo.logLV
                                    == "ERROR"))
                            && m_ipIndicator.m_chError.isSelected) {
                        bAddFilteredArray = true
                        m_hmErrorFiltered[logInfo.line - 1] = m_arLogInfoFiltered.size
                        if (logInfo.isMarked) m_hmMarkedInfoFiltered[logInfo.line - 1] = m_arLogInfoFiltered.size
                    }
                    if (bAddFilteredArray) m_arLogInfoFiltered.add(logInfo)
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
                        && checkFileNameFilter(logInfo)) {
                    m_arLogInfoFiltered.add(logInfo)
                    if (logInfo.isMarked) m_hmMarkedInfoFiltered[logInfo.line - 1] = m_arLogInfoFiltered.size
                    if (logInfo.logLV === "E"
                            || logInfo.logLV === "ERROR") if (logInfo.logLV == "E" || logInfo.logLV == "ERROR") m_hmErrorFiltered[logInfo.line - 1] = m_arLogInfoFiltered.size
                }
            }
        }
    }

    fun addChangeListener() {
        m_tfSearch.document.addDocumentListener(mFilterListener)
        m_tfHighlight.document.addDocumentListener(mFilterListener)
        m_tfIncludeWord.document.addDocumentListener(mFilterListener)
        m_tfExcludeWord.document.addDocumentListener(mFilterListener)
        m_tfShowTag.document.addDocumentListener(mFilterListener)
        m_tfRemoveTag.document.addDocumentListener(mFilterListener)
        m_tfBookmarkTag.document.addDocumentListener(mFilterListener)
        m_tfShowPid.document.addDocumentListener(mFilterListener)
        m_tfShowTid.document.addDocumentListener(mFilterListener)
        m_tfShowFileName.document.addDocumentListener(mFilterListener)
        m_tfFromTimeTag.document.addDocumentListener(mFilterListener)
        m_tfToTimeTag.document.addDocumentListener(mFilterListener)
        m_chkEnableIncludeWord.addItemListener(m_itemListener)
        m_chkEnableExcludeWord.addItemListener(m_itemListener)
        m_chkEnableShowPid.addItemListener(m_itemListener)
        m_chkEnableShowTid.addItemListener(m_itemListener)
        m_chkEnableShowTag.addItemListener(m_itemListener)
        m_chkEnableRemoveTag.addItemListener(m_itemListener)
        m_chkEnableBookmarkTag.addItemListener(m_itemListener)
        m_chkEnableLogFlowTag.addItemListener(m_itemListener)
        m_chkEnableFileNameFilter.addItemListener(m_itemListener)
        m_chkEnableHighlight.addItemListener(m_itemListener)
        m_chkEnableTimeTag.addItemListener(m_itemListener)
        m_chkVerbose.addItemListener(m_itemListener)
        m_chkDebug.addItemListener(m_itemListener)
        m_chkInfo.addItemListener(m_itemListener)
        m_chkWarn.addItemListener(m_itemListener)
        m_chkError.addItemListener(m_itemListener)
        m_chkFatal.addItemListener(m_itemListener)
        m_chkClmBookmark.addItemListener(m_itemListener)
        m_chkClmLine.addItemListener(m_itemListener)
        m_chkClmDate.addItemListener(m_itemListener)
        m_chkClmTime.addItemListener(m_itemListener)
        m_chkClmLogLV.addItemListener(m_itemListener)
        m_chkClmPid.addItemListener(m_itemListener)
        m_chkClmThread.addItemListener(m_itemListener)
        m_chkClmTag.addItemListener(m_itemListener)
        m_chkClmMessage.addItemListener(m_itemListener)
        m_chkClmFile.addItemListener(m_itemListener)
        m_logScrollVPane.viewport.addChangeListener { // m_ipIndicator.m_bDrawFull = false;
            if (extendedState != MAXIMIZED_BOTH) {
                m_nLastWidth = width
                m_nLastHeight = height
            }
            m_ipIndicator.repaint()
        }
    }

    private fun addUndoListener() {
        Utils.makeUndoable(m_tfSearch)
        Utils.makeUndoable(m_tfHighlight)
        Utils.makeUndoable(m_tfIncludeWord)
        Utils.makeUndoable(m_tfExcludeWord)
        Utils.makeUndoable(m_tfShowTag)
        Utils.makeUndoable(m_tfRemoveTag)
        Utils.makeUndoable(m_tfShowPid)
        Utils.makeUndoable(m_tfShowTid)
        Utils.makeUndoable(m_tfBookmarkTag)
        Utils.makeUndoable(m_tfFontSize)
        Utils.makeUndoable(m_tfShowFileName)
        Utils.makeUndoable(m_tfGoto)
        Utils.makeUndoable(m_tfToTimeTag)
        Utils.makeUndoable(m_tfFromTimeTag)
    }

    private fun bindRecentlyPopup() {
        bindHistoryInput(m_tfSearch)
        bindHistoryInput(m_tfHighlight)
        bindHistoryInput(m_tfIncludeWord)
        bindHistoryInput(m_tfExcludeWord)
        bindHistoryInput(m_tfShowTag)
        bindHistoryInput(m_tfRemoveTag)
        bindHistoryInput(m_tfShowPid)
        bindHistoryInput(m_tfShowTid)
        bindHistoryInput(m_tfBookmarkTag)
        bindHistoryInput(m_tfFontSize)
        bindHistoryInput(m_tfShowFileName)
        bindHistoryInput(m_tfGoto)
        bindHistoryInput(m_tfToTimeTag)
        bindHistoryInput(m_tfFromTimeTag)
    }

    /*
    添加输入历史功能
     */
    private fun bindHistoryInput(textField: JTextField?) {
        if (!mRecentlyInputHistory.containsKey(textField)) {
            mRecentlyInputHistory[textField] = ArrayList()
        }
        // build poup menu
        val popup = JPopupMenu()
        textField!!.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (!popup.isVisible) {
                    val historyInputList = mRecentlyInputHistory[textField]
                    if (historyInputList == null || historyInputList.size <= 0) return
                    popup.removeAll()
                    for (his in historyInputList) {
                        // New project menu item
                        val menuItem = JMenuItem(his)
                        menuItem.addActionListener {
                            historyInputList.remove(his)
                            historyInputList.add(0, his)
                            textField.text = his
                            popup.isVisible = false
                        }
                        popup.add(menuItem)
                    }
                    popup.show(textField,
                            0, textField.height)
                    textField.requestFocus()
                }
            }
        })
        textField.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {}
            override fun focusLost(e: FocusEvent) {
                val historyInputList = mRecentlyInputHistory[textField] ?: return
                if (historyInputList.size > 0 && !popup.isVisible) {
                    return
                }
                val newContent = textField.text
                if (newContent != null && newContent.length > 0 && !historyInputList.contains(newContent)) {
                    historyInputList.remove(newContent)
                    historyInputList.add(0, newContent)
                }
            }
        })
    }

    fun createFilterPanel(): Component {
        m_chkEnableIncludeWord = JCheckBox()
        m_chkEnableExcludeWord = JCheckBox()
        m_chkEnableShowTag = JCheckBox()
        m_chkEnableRemoveTag = JCheckBox()
        m_chkEnableShowPid = JCheckBox()
        m_chkEnableShowTid = JCheckBox()
        m_chkEnableBookmarkTag = JCheckBox()
        m_chkEnableLogFlowTag = JCheckBox()
        m_chkEnableFileNameFilter = JCheckBox()
        m_chkEnableTimeTag = JCheckBox()
        m_chkEnableIncludeWord.isSelected = true
        m_chkEnableExcludeWord.isSelected = true
        m_chkEnableShowTag.isSelected = true
        m_chkEnableRemoveTag.isSelected = true
        m_chkEnableShowPid.isSelected = true
        m_chkEnableShowTid.isSelected = true
        m_chkEnableBookmarkTag.isSelected = false
        m_chkEnableLogFlowTag.isSelected = false
        m_chkEnableFileNameFilter.isSelected = false
        m_chkEnableTimeTag.isSelected = true
        m_tfIncludeWord = JTextField()
        m_tfExcludeWord = JTextField()
        m_tfShowTag = JTextField()
        m_tfRemoveTag = JTextField()
        m_tfShowPid = JTextField()
        m_tfShowTid = JTextField()
        m_tfShowFileName = JTextField()
        m_tfBookmarkTag = JTextField()
        m_tfFromTimeTag = JTextField()
        m_tfToTimeTag = JTextField()
        val jpMain = JPanel(BorderLayout())
        val jpWordFilter = JPanel()
        jpWordFilter.border = BorderFactory.createTitledBorder("Word filter")
        jpWordFilter.layout = BoxLayout(jpWordFilter, BoxLayout.Y_AXIS)
        val jpInclide = JPanel(BorderLayout())
        val find = JLabel()
        find.text = "include:"
        jpInclide.add(find, BorderLayout.WEST)
        jpInclide.add(m_tfIncludeWord, BorderLayout.CENTER)
        jpInclide.add(m_chkEnableIncludeWord, BorderLayout.EAST)
        val jpExclude = JPanel(BorderLayout())
        val remove = JLabel()
        remove.text = "exclude:"
        jpExclude.add(remove, BorderLayout.WEST)
        jpExclude.add(m_tfExcludeWord, BorderLayout.CENTER)
        jpExclude.add(m_chkEnableExcludeWord, BorderLayout.EAST)
        val jpLFTag = JPanel(FlowLayout())
        val lfTag = JLabel()
        lfTag.text = "Filter LogFlow: "
        jpLFTag.add(lfTag)
        jpLFTag.add(m_chkEnableLogFlowTag)
        m_chkEnableLogFlowTag.isSelected = mShowFlowInLogTable
        jpWordFilter.add(jpInclide)
        jpWordFilter.add(jpExclude)
        jpWordFilter.add(jpLFTag)
        jpMain.add(jpWordFilter, BorderLayout.NORTH)
        val jpTagFilter = JPanel()
        jpTagFilter.layout = BoxLayout(jpTagFilter, BoxLayout.Y_AXIS)
        jpTagFilter.border = BorderFactory.createTitledBorder("Tag filter")
        val jpPidTid = JPanel(GridLayout(1, 3))
        val jpPid = JPanel(BorderLayout())
        val pid = JLabel()
        pid.text = "Pid : "
        jpPid.add(pid, BorderLayout.WEST)
        jpPid.add(m_tfShowPid, BorderLayout.CENTER)
        jpPid.add(m_chkEnableShowPid, BorderLayout.EAST)
        val jpTid = JPanel(BorderLayout())
        val tid = JLabel()
        tid.text = "Tid : "
        jpTid.add(tid, BorderLayout.WEST)
        jpTid.add(m_tfShowTid, BorderLayout.CENTER)
        jpTid.add(m_chkEnableShowTid, BorderLayout.EAST)
        jpPidTid.add(jpPid)
        jpPidTid.add(jpTid)
        val jpShow = JPanel(BorderLayout())
        val show = JLabel()
        show.text = "Tag Include: "
        jpShow.add(show, BorderLayout.WEST)
        jpShow.add(m_tfShowTag, BorderLayout.CENTER)
        val tagIncludeExtPanel = JPanel(BorderLayout())
        val tagIncludeExtBtn = createExtDialogButton(Constant.EXT_DIALOG_TYPE_INCLUDE_TAG)
        tagIncludeExtPanel.add(tagIncludeExtBtn, BorderLayout.WEST)
        tagIncludeExtPanel.add(m_chkEnableShowTag, BorderLayout.EAST)
        jpShow.add(tagIncludeExtPanel, BorderLayout.EAST)
        val jpRemoveTag = JPanel(BorderLayout())
        val removeTag = JLabel()
        removeTag.text = "Tag Exclude: "
        jpRemoveTag.add(removeTag, BorderLayout.WEST)
        jpRemoveTag.add(m_tfRemoveTag, BorderLayout.CENTER)
        val tagExcludeExtPanel = JPanel(BorderLayout())
        val tagExcludeExtBtn = createExtDialogButton(Constant.EXT_DIALOG_TYPE_EXCLUDE_TAG)
        tagExcludeExtPanel.add(tagExcludeExtBtn, BorderLayout.WEST)
        tagExcludeExtPanel.add(m_chkEnableRemoveTag, BorderLayout.EAST)
        jpRemoveTag.add(tagExcludeExtPanel, BorderLayout.EAST)
        val jpBmTag = JPanel(BorderLayout())
        val bkTag = JLabel()
        bkTag.text = "Bookmark: "
        jpBmTag.add(bkTag, BorderLayout.WEST)
        jpBmTag.add(m_tfBookmarkTag, BorderLayout.CENTER)
        jpBmTag.add(m_chkEnableBookmarkTag, BorderLayout.EAST)
        val jpFromTimeTag = JPanel(BorderLayout())
        val jlFromTimeTag = JLabel()
        jlFromTimeTag.text = "from time"
        jpFromTimeTag.add(jlFromTimeTag, BorderLayout.WEST)
        jpFromTimeTag.add(m_tfFromTimeTag, BorderLayout.CENTER)
        val jpToTimeTag = JPanel(BorderLayout())
        val jlToTimeTag = JLabel()
        jlToTimeTag.text = "to time"
        jpToTimeTag.add(jlToTimeTag, BorderLayout.WEST)
        jpToTimeTag.add(m_tfToTimeTag, BorderLayout.CENTER)
        val jpTimeTag = JPanel(GridLayout(2, 1))
        jpTimeTag.add(jpFromTimeTag)
        jpTimeTag.add(jpToTimeTag)
        val jpTimeMainTag = JPanel(BorderLayout())
        jpTimeMainTag.add(jpTimeTag, BorderLayout.CENTER)
        jpTimeMainTag.add(m_chkEnableTimeTag, BorderLayout.EAST)
        val jpFile = JPanel(BorderLayout())
        val jLFile = JLabel()
        jLFile.text = "File : "
        jpFile.add(jLFile, BorderLayout.WEST)
        jpFile.add(m_tfShowFileName, BorderLayout.CENTER)
        jpFile.add(m_chkEnableFileNameFilter, BorderLayout.EAST)
        jpTagFilter.add(jpPidTid)
        jpTagFilter.add(jpShow)
        jpTagFilter.add(jpRemoveTag)
        jpTagFilter.add(jpBmTag)
        jpTagFilter.add(jpTimeMainTag)
        jpTagFilter.add(jpFile)
        jpMain.add(jpTagFilter, BorderLayout.CENTER)
        return jpMain
    }

    private fun createExtDialogButton(type: Int): JButton {
        val tagIncludeExtBtn = JButton("...")
        tagIncludeExtBtn.border = EmptyBorder(3, 3, 3, 3)
        tagIncludeExtBtn.isBorderPainted = false
        tagIncludeExtBtn.isContentAreaFilled = false
        tagIncludeExtBtn.isOpaque = false
        tagIncludeExtBtn.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                val tagSet = getAllInfoContent(LogFilterTableModel.COLUMN_TAG)
                if (tagSet.size <= 0) {
                    return
                }
                // 字典序
                val resultList: List<String> = ArrayList(tagSet)
                Collections.sort(resultList, Comparator { o1, o2 ->
                    val containsO1 = mFilterTagHistory.contains(o1)
                    val containsO2 = mFilterTagHistory.contains(o2)
                    if (containsO1 && containsO2) {
                        return@Comparator o1.compareTo(o2)
                    }
                    if (containsO1) {
                        return@Comparator -1
                    }
                    if (containsO2) {
                        1
                    } else o1.compareTo(o2)
                })
                val list: JList<*> = JList<Any?>(resultList.toTypedArray())
                val dialog = ListDialog("Please select an item in the list: ", list)
                dialog.setOnOk {
                    val selectedItems = dialog.selectedItems
                    for (selectedItem in selectedItems) {
                        val tag = selectedItem as String
                        when (type) {
                            Constant.EXT_DIALOG_TYPE_INCLUDE_TAG -> {
                                logTable.appendFilterShowTag(tag)
                            }
                            Constant.EXT_DIALOG_TYPE_EXCLUDE_TAG -> {
                                logTable.appendFilterRemoveTag(tag)
                            }
                        }
                        mFilterTagHistory.add(tag)
                    }
                    when (type) {
                        Constant.EXT_DIALOG_TYPE_INCLUDE_TAG -> {
                            postEvent(EventBus.Event(EventBus.TYPE.EVENT_CHANGE_FILTER_SHOW_TAG))
                        }
                        Constant.EXT_DIALOG_TYPE_EXCLUDE_TAG -> {
                            postEvent(EventBus.Event(EventBus.TYPE.EVENT_CHANGE_FILTER_REMOVE_TAG))
                        }
                    }
                }
                dialog.show()
            }

            private fun getAllInfoContent(column: Int): Set<String> {
                val tagSet: MutableSet<String> = HashSet()
                for (logInfo in m_arLogInfoAll) {
                    if (logInfo.tag != null && logInfo.tag.isNotEmpty()) {
                        tagSet.add(logInfo.getContentByColumn(column).toString())
                    }
                }
                return tagSet
            }
        })
        return tagIncludeExtBtn
    }

    fun createHighlightPanel(): Component {
        m_chkEnableHighlight = JCheckBox()
        m_chkEnableHighlight.isSelected = true
        m_tfHighlight = JTextField()
        val jpMain = JPanel(BorderLayout())
        jpMain.border = BorderFactory.createTitledBorder("Highlight")
        jpMain.add(m_tfHighlight)
        jpMain.add(m_chkEnableHighlight, BorderLayout.EAST)
        return jpMain
    }

    fun createSearchPanel(): Component {
        m_tfSearch = JTextField()
        m_tfSearch.addActionListener { logTable.gotoNextSearchResult() }
        val jpMain = JPanel(BorderLayout())
        jpMain.border = BorderFactory.createTitledBorder("Search")
        jpMain.add(m_tfSearch, BorderLayout.CENTER)
        val buttonPanel = JPanel(GridLayout(1, 2))
        val preButton = JButton()
        preButton.margin = Insets(0, 0, 0, 0)
        preButton.text = "<="
        preButton.addActionListener { logTable.gotoPreSearchResult() }
        buttonPanel.add(preButton)
        val nextButton = JButton()
        nextButton.margin = Insets(0, 0, 0, 0)
        nextButton.text = "=>"
        nextButton.addActionListener { logTable.gotoNextSearchResult() }
        buttonPanel.add(nextButton)
        jpMain.add(buttonPanel, BorderLayout.EAST)
        return jpMain
    }

    fun createCheckPanel(): Component {
        m_chkVerbose = JCheckBox()
        m_chkDebug = JCheckBox()
        m_chkInfo = JCheckBox()
        m_chkWarn = JCheckBox()
        m_chkError = JCheckBox()
        m_chkFatal = JCheckBox()
        m_chkClmBookmark = JCheckBox()
        m_chkClmLine = JCheckBox()
        m_chkClmDate = JCheckBox()
        m_chkClmTime = JCheckBox()
        m_chkClmLogLV = JCheckBox()
        m_chkClmPid = JCheckBox()
        m_chkClmThread = JCheckBox()
        m_chkClmTag = JCheckBox()
        m_chkClmMessage = JCheckBox()
        m_chkClmFile = JCheckBox()
        val jpMain = JPanel()
        jpMain.layout = BoxLayout(jpMain, BoxLayout.Y_AXIS)
        val jpLogFilter = JPanel()
        jpLogFilter.layout = GridLayout(3, 2)
        jpLogFilter.border = BorderFactory.createTitledBorder("Log filter")
        m_chkVerbose.text = "Verbose"
        m_chkVerbose.isSelected = true
        m_chkDebug.text = "Debug"
        m_chkDebug.isSelected = true
        m_chkInfo.text = "Info"
        m_chkInfo.isSelected = true
        m_chkWarn.text = "Warn"
        m_chkWarn.isSelected = true
        m_chkError.text = "Error"
        m_chkError.isSelected = true
        m_chkFatal.text = "Fatal"
        m_chkFatal.isSelected = true
        jpLogFilter.add(m_chkVerbose)
        jpLogFilter.add(m_chkDebug)
        jpLogFilter.add(m_chkInfo)
        jpLogFilter.add(m_chkWarn)
        jpLogFilter.add(m_chkError)
        jpLogFilter.add(m_chkFatal)
        val jpShowColumn = JPanel()
        jpShowColumn.layout = GridLayout(5, 2)
        jpShowColumn.border = BorderFactory.createTitledBorder("Show column")
        m_chkClmBookmark.text = "Mark"
        m_chkClmBookmark.toolTipText = "Bookmark"
        m_chkClmLine.text = "Line"
        m_chkClmLine.isSelected = true
        m_chkClmDate.text = "Date"
        m_chkClmDate.isSelected = true
        m_chkClmTime.text = "Time"
        m_chkClmTime.isSelected = true
        m_chkClmLogLV.text = "LogLV"
        m_chkClmLogLV.isSelected = true
        m_chkClmPid.text = "Pid"
        m_chkClmPid.isSelected = true
        m_chkClmThread.text = "Thread"
        m_chkClmThread.isSelected = true
        m_chkClmTag.text = "Tag"
        m_chkClmTag.isSelected = true
        m_chkClmMessage.text = "Msg"
        m_chkClmMessage.isSelected = true
        m_chkClmFile.text = "File"
        m_chkClmFile.isSelected = true
        jpShowColumn.add(m_chkClmLine)
        jpShowColumn.add(m_chkClmDate)
        jpShowColumn.add(m_chkClmTime)
        jpShowColumn.add(m_chkClmLogLV)
        jpShowColumn.add(m_chkClmPid)
        jpShowColumn.add(m_chkClmThread)
        jpShowColumn.add(m_chkClmTag)
        jpShowColumn.add(m_chkClmBookmark)
        jpShowColumn.add(m_chkClmMessage)
        jpShowColumn.add(m_chkClmFile)
        jpMain.add(jpLogFilter)
        jpMain.add(jpShowColumn)
        return jpMain
    }

    fun createOptionFilter(): Component {
        val optionFilter = JPanel()
        optionFilter.layout = BoxLayout(optionFilter, BoxLayout.Y_AXIS)
        optionFilter.add(getDevicePanel())
        optionFilter.add(createFilterPanel())
        optionFilter.add(createCheckPanel())
        return optionFilter
    }

    fun createOptionMenu(): Component {
        val optionMenu = JPanel(BorderLayout())
        val optionWest = JPanel(FlowLayout(FlowLayout.LEADING))
        val jlFont = JLabel("Font Size : ")
        m_tfFontSize = JTextField(2)
        m_tfFontSize.horizontalAlignment = SwingConstants.RIGHT
        m_tfFontSize.text = "12"
        m_btnSetFont = JButton("OK")
        m_btnSetFont.margin = Insets(0, 0, 0, 0)
        m_btnSetFont.addActionListener(m_alButtonListener)
        val jlEncode = JLabel("Text Encode : ")
        m_comboEncode = JComboBox()
        m_comboEncode.addItem("UTF-8")
        m_comboEncode.addItem("Local")
        val jlGoto = JLabel("Goto : ")
        m_tfGoto = JTextField(6)
        m_tfGoto.horizontalAlignment = SwingConstants.RIGHT
        m_tfGoto.addCaretListener {
            try {
                val nIndex = m_tfGoto.text.toInt() - 1
                logTable.showRowCenterIfNotInRect(nIndex, true)
            } catch (err: Exception) {
            }
        }
        mSyncScrollCheckBox = JCheckBox("sync scroll")
        mSyncScrollCheckBox.isEnabled = false
        mSyncScrollCheckBox.addItemListener { e ->
            val check = e.source as JCheckBox
            enableSyncScroll(check.isSelected)
        }
        mSyncSelectedCheckBox = JCheckBox("sync selected")
        mSyncSelectedCheckBox.isEnabled = false
        mSyncSelectedCheckBox.addItemListener { e ->
            val check = e.source as JCheckBox
            enableSyncSelected(check.isSelected)
        }
        val preHistoryButton = JButton("<")
        preHistoryButton.margin = Insets(0, 5, 0, 5)
        preHistoryButton.addActionListener { logTable.historyBack() }
        val nextHistoryButton = JButton(">")
        nextHistoryButton.margin = Insets(0, 5, 0, 5)
        nextHistoryButton.addActionListener { logTable.historyForward() }
        val jpActionPanel = JPanel(FlowLayout(FlowLayout.TRAILING))
        val clearFieldBtn = JButton("Clean Filter")
        clearFieldBtn.margin = Insets(0, 0, 0, 0)
        clearFieldBtn.addActionListener {
            m_tfIncludeWord.text = ""
            m_tfExcludeWord.text = ""
            m_tfShowTag.text = ""
            m_tfRemoveTag.text = ""
            m_tfShowPid.text = ""
            m_tfShowFileName.text = ""
            m_tfShowTid.text = ""
            m_tfBookmarkTag.text = ""
        }
        jpActionPanel.add(clearFieldBtn)
        val followBtn = JButton("Follow")
        followBtn.margin = Insets(0, 0, 0, 0)
        followBtn.addActionListener {
            val endLine = m_tmLogTableModel.rowCount
            updateLogTable(endLine - 1, true)
        }
        jpActionPanel.add(followBtn)
        val hideLeftBtn = JButton("Toggle Left Panel")
        hideLeftBtn.margin = Insets(0, 0, 0, 0)
        hideLeftBtn.addActionListener {
            mMainSplitPane.setOneSideHidden(
                    mMainSplitPane.leftComponent,
                    !mMainSplitPane.isSideHidden(mMainSplitPane.leftComponent)
            )
        }
        jpActionPanel.add(hideLeftBtn)
        val hideBottomBtn = JButton("Toggle Bottom Panel")
        hideBottomBtn.margin = Insets(0, 0, 0, 0)
        hideBottomBtn.addActionListener {
            mSplitPane.setOneSideHidden(
                    mSplitPane.rightComponent,
                    !mSplitPane.isSideHidden(mSplitPane.rightComponent)
            )
        }
        jpActionPanel.add(hideBottomBtn)
        optionWest.add(mSyncScrollCheckBox)
        optionWest.add(mSyncSelectedCheckBox)
        optionWest.add(jlFont)
        optionWest.add(m_tfFontSize)
        optionWest.add(m_btnSetFont)
        optionWest.add(jlEncode)
        optionWest.add(m_comboEncode)
        optionWest.add(jlGoto)
        optionWest.add(m_tfGoto)
        optionWest.add(preHistoryButton)
        optionWest.add(nextHistoryButton)
        optionWest.add(jpActionPanel)
        optionMenu.add(optionWest, BorderLayout.CENTER)
        mSearchPanel = JPanel(GridLayout(1, 2))
        mSearchPanel.add(createHighlightPanel())
        mSearchPanel.add(createSearchPanel())
        mSearchPanel.isVisible = false
        optionMenu.add(mSearchPanel, BorderLayout.SOUTH)
        return optionMenu
    }

    fun createMainSplitPane(): Component {
        mMainSplitPane = ExpandableSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                createOptionPanel(),
                createLogPanel()
        )
        mMainSplitPane.isContinuousLayout = true
        return mMainSplitPane
    }

    fun createOptionPanel(): Component = createOptionFilter()

    fun createStatusPanel(): Component {
        val mainP = JPanel(BorderLayout())
        val tfPanel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints()
        constraints.weightx = 1.0
        constraints.fill = GridBagConstraints.HORIZONTAL
        val border: Border = BorderFactory.createCompoundBorder(EmptyBorder(0, 4, 0, 0), EtchedBorder())
        m_tfStatus = JLabel("ready")
        m_tfStatus.border = border
        m_tfDiffPort = JLabel("not bind")
        m_tfDiffPort.border = border
        m_tfDiffState = JLabel("disconnected")
        m_tfDiffState.border = border
        m_tfParserType = JLabel("")
        m_tfParserType.border = border
        tfPanel.add(m_tfDiffState, constraints)
        tfPanel.add(m_tfDiffPort, constraints)
        tfPanel.add(m_tfParserType, constraints)
        tfPanel.add(m_tfStatus, constraints)
        mainP.add(tfPanel, BorderLayout.EAST)
        return mainP
    }

    fun createLogPanel(): Component {
        val mainLogPanel = JPanel(BorderLayout())
        mainLogPanel.add(createOptionMenu(), BorderLayout.NORTH)
        m_tmLogTableModel = LogFilterTableModel()
        m_tmLogTableModel.setData(m_arLogInfoAll)
        logTable = LogTable(m_tmLogTableModel, this)
        m_logScrollVPane = JScrollPane(logTable)
        val tablePanel = JPanel()
        tablePanel.layout = OverlayLayout(tablePanel)
        tablePanel.add(m_logScrollVPane)
        mainLogPanel.add(tablePanel, BorderLayout.CENTER)
        mainLogPanel.add(getIndicatorPanel(), BorderLayout.WEST)
        m_tSubLogTableModel = LogFilterTableModel()
        m_tSubLogTableModel.setData(m_arSubLogInfoAll)
        subTable = SubLogTable(m_tSubLogTableModel, this)
        m_subLogScrollVPane = JScrollPane(subTable)
        mSplitPane = ExpandableSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                mainLogPanel,
                m_subLogScrollVPane
        )
        return mSplitPane
    }

    fun initValue() {
        val confDir = File(Constant.CONFIG_BASE_DIR)
        if (!confDir.exists()) {
            confDir.mkdirs()
            T.d("create conf directory: " + confDir.absolutePath)
        }
        val outputDir = File(Constant.OUTPUT_LOG_DIR)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
            T.d("create log directory: " + outputDir.absolutePath)
        }
        m_strLogFileName = makeFilename()
        // m_strProcessCmd = ANDROID_DEFAULT_CMD + m_strLogFileName;
    }

    fun parseLogFile(files: Array<File?>?) {
        if (files == null) {
            T.e("files == null")
            return
        }
        if (files.size <= 0) {
            T.e("files size <= 0")
            return
        }
        val title = StringBuilder()
        val filePathBuilder = StringBuilder()
        for (file in files) {
            filePathBuilder.append(file!!.absolutePath).append("|")
            title.append(file.name).append(" | ")
        }
        mRecentMenu!!.addEntry(filePathBuilder.deleteCharAt(filePathBuilder.length - 1).toString())
        setTitle(title.deleteCharAt(title.length - 1).toString())
        // parsing
        Thread(Runnable {
            setStatus("Parsing")
            clearData()
            logTable.clearSelection()
            subTable!!.clearSelection()
            val newLogInfos: MutableList<LogInfo> = ArrayList()
            var fileIdx = 0
            for (file in files) {
                var fstream: FileInputStream? = null
                var `in`: DataInputStream? = null
                var br: BufferedReader? = null
                fileIdx++
                try {
                    fstream = FileInputStream(file)
                    `in` = DataInputStream(fstream)
                    br = if (m_comboEncode.selectedItem == "UTF-8") BufferedReader(InputStreamReader(`in`,
                            "UTF-8")) else BufferedReader(InputStreamReader(`in`))
                    var strLine: String
                    while (br.readLine().also { strLine = it } != null) {
                        if ("" != strLine.trim { it <= ' ' }) {
                            val logInfo = m_iLogParser.parseLog(strLine)
                            logInfo.type = LogInfo.TYPE.SYSTEM
                            // 处理空白行
                            if (logInfo.tag == null || logInfo.tag.length <= 0) {
                                if (newLogInfos.size > 1) {
                                    val oldInfo = newLogInfos[newLogInfos.size - 1]
                                    logInfo.timestamp = oldInfo.timestamp
                                } else {
                                    logInfo.timestamp = 0
                                }
                            }
                            logInfo.fileName = fileIdx.toString()
                            newLogInfos.add(logInfo)
                        }
                    }
                } catch (ioe: Exception) {
                    T.e(ioe)
                }
                try {
                    br?.close()
                    `in`?.close()
                    fstream?.close()
                } catch (e: Exception) {
                    T.e(e)
                }
            }
            // merge and sort
            if (files.size > 1) {
                newLogInfos.sortWith(Comparator { o1, o2 -> (o1.timestamp - o2.timestamp).toInt() })
            }
            var lineIdx = 1
            for (info in newLogInfos) {
                info.line = lineIdx++
                addLogInfo(info)
            }
            runFilter()
            mLastParseredFiles = files
            setStatus("Parse complete")
        }).start()
    }

    fun pauseLogcatParserProcess() {
        if (m_tbtnPause.isSelected) {
            m_bPauseADB = true
            m_tbtnPause.text = "Resume"
        } else {
            m_bPauseADB = false
            m_tbtnPause.text = "Pause"
        }
    }

    override fun onSetBookmark(nLine: Int, strBookmark: String) {
        val logInfo = m_arLogInfoAll[nLine]
        logInfo.bookmark = strBookmark
        m_arLogInfoAll[nLine] = logInfo
    }

    private val aDBValidCmd: Array<String>
        get() {
            val strCommand = Constant.DEVICES_CMD[m_comboDeviceCmd.selectedIndex]
            val cmd: Array<String>
            cmd = if (Utils.isWindows()) {
                arrayOf("cmd.exe", "/C", strCommand)
            } else {
                arrayOf("/bin/bash", "-l", "-c", strCommand)
            }
            return cmd
        }

    private fun addDevicesToListModelFromCmd(cmd: Array<String>, listModel: DefaultListModel<TargetDevice>) {
        try {
            listModel.clear()
            val processBuilder = ProcessBuilder(*cmd)
            processBuilder.redirectErrorStream(true)
            val oProcess = processBuilder.start()
            val stdOut = BufferedReader(InputStreamReader(
                    oProcess.inputStream))
            var s: String
            while (stdOut.readLine().also { s = it } != null) {
                if (!s.startsWith("List of devices attached") && s.length != 0) {
                    listModel.addElement(TargetDevice(s))
                }
            }
        } catch (e: Exception) {
            T.e("e = $e")
            listModel.addElement(TargetDevice(e.message))
        }
    }

    fun showPanelAndSetSearchFocus() {
        if (!mSearchPanel.isVisible || m_tfHighlight.hasFocus()) {
            mSearchPanel.isVisible = true
            m_tfSearch.requestFocus()
        } else {
            mSearchPanel.isVisible = false
        }
    }

    private fun showPanelAndSetHighLightFocus() {
        if (!mSearchPanel.isVisible || m_tfSearch.hasFocus()) {
            mSearchPanel.isVisible = true
            m_tfHighlight.requestFocus()
        } else {
            mSearchPanel.isVisible = false
        }
    }

    private fun setWordIncludeFocus() {
        m_tfIncludeWord.requestFocus()
    }

    private fun setTagIncludeFocus() {
        m_tfShowTag.requestFocus()
    }

    private fun setMainTableFocus() {
        logTable.requestFocus()
    }

    override fun searchKeyword(keyword: String) {
        m_tfSearch.text = keyword
    }

    fun setDnDListener() {
        DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE,
                object : DropTargetListener {
                    override fun dropActionChanged(dtde: DropTargetDragEvent) {}
                    override fun dragOver(dtde: DropTargetDragEvent) {}
                    override fun dragExit(dte: DropTargetEvent) {}
                    override fun dragEnter(event: DropTargetDragEvent) {}
                    override fun drop(event: DropTargetDropEvent) {
                        try {
                            event.acceptDrop(DnDConstants.ACTION_COPY)
                            val t = event.transferable
                            val list = t
                                    .getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                            val i = list.iterator()
                            val files: MutableList<File?> = ArrayList()
                            while (i.hasNext()) {
                                val file = i.next() as File
                                if (file.isFile) {
                                    files.add(file)
                                }
                            }
                            if (files.size > 0) {
                                stopLogcatParserProcess()
                                parseLogFile(files.toTypedArray())
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                })
    }

    fun setLogLV(nLogLV: Int, bChecked: Boolean) {
        m_nFilterLogLV = if (bChecked) m_nFilterLogLV or nLogLV else m_nFilterLogLV and nLogLV.inv()
        mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
        runFilter()
    }

    fun useFilter(checkBox: JCheckBox) {
        if (checkBox == m_chkEnableIncludeWord) {
            logTable.setFilterFind(if (checkBox.isSelected) m_tfIncludeWord
                    .text else "")
        }
        if (checkBox == m_chkEnableExcludeWord) {
            logTable.SetFilterRemove(if (checkBox.isSelected) m_tfExcludeWord
                    .text else "")
        }
        if (checkBox == m_chkEnableShowPid) {
            logTable.SetFilterShowPid(if (checkBox.isSelected) m_tfShowPid
                    .text else "")
        }
        if (checkBox == m_chkEnableShowTid) {
            logTable.SetFilterShowTid(if (checkBox.isSelected) m_tfShowTid
                    .text else "")
        }
        if (checkBox == m_chkEnableShowTag) {
            logTable.SetFilterShowTag(if (checkBox.isSelected) m_tfShowTag
                    .text else "")
        }
        if (checkBox == m_chkEnableRemoveTag) {
            logTable.SetFilterRemoveTag(if (checkBox.isSelected) m_tfRemoveTag
                    .text else "")
        }
        if (checkBox == m_chkEnableBookmarkTag) {
            logTable.SetFilterBookmarkTag(if (checkBox.isSelected) m_tfBookmarkTag
                    .text else "")
        }
        if (checkBox == m_chkEnableLogFlowTag) {
            logTable.SetFilterLogFlow(checkBox.isSelected)
        }
        if (checkBox == m_chkEnableFileNameFilter) {
            logTable.SetFilterFileName(if (checkBox.isSelected) m_tfShowFileName
                    .text else "")
        }
        if (checkBox == m_chkEnableHighlight) {
            logTable.SetHighlight(if (checkBox.isSelected) m_tfHighlight
                    .text else "")
            subTable!!.SetHighlight(if (checkBox.isSelected) m_tfHighlight
                    .text else "")
        }
        if (checkBox == m_chkEnableTimeTag) {
            if (checkBox.isSelected) {
                logTable.SetFilterFromTime(m_tfFromTimeTag.text)
                logTable.SetFilterToTime(m_tfToTimeTag.text)
            } else {
                logTable.SetFilterFromTime("")
                logTable.SetFilterToTime("")
            }
        }
        mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
        runFilter()
    }

    fun setProcessBtn(bStart: Boolean) {
        if (bStart) {
            m_btnRun.isEnabled = false
            m_btnStop.isEnabled = true
            m_btnClear.isEnabled = true
            m_tbtnPause.isEnabled = true
        } else {
            m_btnRun.isEnabled = true
            m_btnStop.isEnabled = false
            m_btnClear.isEnabled = false
            m_tbtnPause.isEnabled = false
            m_tbtnPause.isSelected = false
            m_tbtnPause.text = "Pause"
        }
    }

    private val processCmd: String
        get() = if (m_lDeviceList.selectedIndex < 0 || m_selectedDevice == null || m_selectedDevice!!.code.isEmpty())
            Constant.ANDROID_DEFAULT_CMD_FIRST + m_comboCmd.selectedItem
        else
            Constant.ANDROID_SELECTED_CMD_FIRST + m_selectedDevice!!.code + " " + m_comboCmd.selectedItem

    private fun setStatus(strText: String?) {
        m_tfStatus.text = strText
    }

    fun stopLogcatParserProcess() {
        setProcessBtn(false)
        if (m_Process != null) m_Process!!.destroy()
        if (m_thProcess != null) m_thProcess!!.interrupt()
        if (m_thWatchFile != null) m_thWatchFile!!.interrupt()
        m_Process = null
        m_thProcess = null
        m_thWatchFile = null
        m_bPauseADB = false
    }

    ///////////////////////////////////parser///////////////////////////////////
    private fun startLogcatParse() {
        m_thWatchFile = Thread(Runnable {
            var fstream: FileInputStream? = null
            var `in`: DataInputStream? = null
            var br: BufferedReader? = null
            try {
                fstream = FileInputStream(m_strLogFileName)
                `in` = DataInputStream(fstream)
                br = if (m_comboEncode.selectedItem == "UTF-8") BufferedReader(InputStreamReader(`in`,
                        "UTF-8")) else BufferedReader(InputStreamReader(`in`))
                var strLine: String? = null
                title = m_strLogFileName!!
                m_arLogInfoAll.clear()
                var bEndLine: Boolean
                var nSelectedIndex: Int
                var nAddCount: Int
                var nPreRowCount: Int
                var nEndLine: Int
                while (true) {
                    Thread.sleep(50)
                    if (mLogParsingState == Constant.PARSING_STATUS_CHANGE_PENDING
                            || mLogParsingState == Constant.PARSING_STATUS_PARSING) continue
                    if (m_bPauseADB) continue
                    bEndLine = false
                    nSelectedIndex = logTable.selectedRow
                    nPreRowCount = logTable.rowCount
                    nAddCount = 0
                    if (nSelectedIndex == -1
                            || nSelectedIndex == logTable.rowCount - 1) bEndLine = true
                    synchronized(FILE_LOCK) {
                        var nLine = m_arLogInfoAll.size + 1
                        while (!m_bPauseADB
                                && br.readLine().also { strLine = it } != null) {
                            if (strLine != null
                                    && "" != strLine!!.trim { it <= ' ' }) {
                                val logInfo = m_iLogParser
                                        .parseLog(strLine)
                                if (logInfo != null) {
                                    logInfo.line = nLine++
                                    logInfo.fileName = "logcat"
                                    addLogInfo(logInfo)
                                }
                                nAddCount++
                            }
                        }
                    }
                    if (nAddCount == 0) continue
                    synchronized(FILTER_LOCK) {
                        if (mFilterEnabled == false) {
                            m_tmLogTableModel.setData(m_arLogInfoAll)
                            m_ipIndicator.setData(m_arLogInfoAll,
                                    m_hmMarkedInfoAll, m_hmErrorAll)
                        } else {
                            m_tmLogTableModel.setData(m_arLogInfoFiltered)
                            m_ipIndicator
                                    .setData(m_arLogInfoFiltered,
                                            m_hmMarkedInfoFiltered,
                                            m_hmErrorFiltered)
                        }
                        nEndLine = m_tmLogTableModel.rowCount
                        if (nPreRowCount != nEndLine) {
                            if (bEndLine) updateLogTable(nEndLine - 1, true) else updateLogTable(nSelectedIndex, false)
                        }
                    }
                }
            } catch (e: InterruptedException) {
                T.e(e)
            } catch (e: Exception) {
                e.printStackTrace()
                T.e(e)
            }
            try {
                br?.close()
                `in`?.close()
                fstream?.close()
            } catch (e: Exception) {
                T.e(e)
            }
            println("End m_thWatchFile thread")
        })
        m_thWatchFile!!.start()
    }

    fun runFilter() {
        checkUseFilter()
        while (mLogParsingState == Constant.PARSING_STATUS_PARSING) try {
            Thread.sleep(100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        synchronized(FILTER_LOCK) { FILTER_LOCK.notify() }
    }

    fun startFilterParse() {
        m_thFilterParse = Thread(Runnable {
            try {
                while (true) {
                    synchronized(FILTER_LOCK) filterLoop@{
                        mLogParsingState = Constant.PARSING_STATUS_READY
                        FILTER_LOCK.wait()
                        mLogParsingState = Constant.PARSING_STATUS_PARSING
                        m_arLogInfoFiltered.clear()
                        m_hmMarkedInfoFiltered.clear()
                        m_hmErrorFiltered.clear()
                        logTable.clearSelection()
                        subTable!!.clearSelection()
                        if (!mFilterEnabled) {
                            m_tmLogTableModel.setData(m_arLogInfoAll)
                            m_ipIndicator.setData(m_arLogInfoAll,
                                    m_hmMarkedInfoAll, m_hmErrorAll)
                            val latestInfo = logTable.latestSelectedLogInfo
                            if (latestInfo != null) {
                                var i = 0
                                for (info in m_arLogInfoAll) {
                                    i++
                                    if (info.line >= latestInfo.line) {
                                        break
                                    }
                                }
                                updateLogTable(i - 1, true)
                            } else {
                                updateLogTable(m_arLogInfoFiltered.size - 1, true)
                            }
                            mLogParsingState = Constant.PARSING_STATUS_READY
                            return@filterLoop
                        }
                        m_tmLogTableModel.setData(m_arLogInfoFiltered)
                        m_ipIndicator.setData(m_arLogInfoFiltered,
                                m_hmMarkedInfoFiltered, m_hmErrorFiltered)
                        // updateTable(-1);
                        setStatus("Parsing")
                        val nRowCount = m_arLogInfoAll.size
                        var logInfo: LogInfo
                        var bAddFilteredArray: Boolean
                        for (nIndex in 0 until nRowCount) {
                            if (nIndex % 10000 == 0) Thread.sleep(1)
                            if (mLogParsingState == Constant.PARSING_STATUS_CHANGE_PENDING) {
                                break
                            }
                            logInfo = m_arLogInfoAll[nIndex]
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
                                    && checkFileNameFilter(logInfo)) {
                                if (m_ipIndicator.m_chBookmark.isSelected
                                        || m_ipIndicator.m_chError.isSelected) {
                                    bAddFilteredArray = false
                                    if (logInfo.isMarked
                                            && m_ipIndicator.m_chBookmark
                                                    .isSelected) {
                                        bAddFilteredArray = true
                                        m_hmMarkedInfoFiltered[logInfo.line - 1] = m_arLogInfoFiltered.size
                                        if (logInfo.logLV == "E" || (logInfo.logLV
                                                        == "ERROR")) m_hmErrorFiltered[logInfo.line - 1] = m_arLogInfoFiltered.size
                                    }
                                    if ((logInfo.logLV == "E" || logInfo.logLV == "ERROR")
                                            && m_ipIndicator.m_chError
                                                    .isSelected) {
                                        bAddFilteredArray = true
                                        m_hmErrorFiltered[logInfo.line - 1] = m_arLogInfoFiltered.size
                                        if (logInfo.isMarked) m_hmMarkedInfoFiltered[logInfo.line - 1] = m_arLogInfoFiltered.size
                                    }
                                    if (bAddFilteredArray) m_arLogInfoFiltered.add(logInfo)
                                } else {
                                    m_arLogInfoFiltered.add(logInfo)
                                    if (logInfo.isMarked) m_hmMarkedInfoFiltered[logInfo.line - 1] = m_arLogInfoFiltered.size
                                    if (logInfo.logLV == "E" || (logInfo.logLV
                                                    == "ERROR")) m_hmErrorFiltered[logInfo.line - 1] = m_arLogInfoFiltered.size
                                }
                            }
                        }
                        if (mLogParsingState == Constant.PARSING_STATUS_PARSING) {
                            mLogParsingState = Constant.PARSING_STATUS_READY
                            m_tmLogTableModel.setData(m_arLogInfoFiltered)
                            m_ipIndicator.setData(m_arLogInfoFiltered,
                                    m_hmMarkedInfoFiltered,
                                    m_hmErrorFiltered)
                            val latestInfo = logTable.latestSelectedLogInfo
                            if (latestInfo != null) {
                                var i = 0
                                for (info in m_arLogInfoFiltered) {
                                    i++
                                    if (info.line >= latestInfo.line) {
                                        break
                                    }
                                }
                                updateLogTable(i - 1, true)
                            } else {
                                updateLogTable(m_arLogInfoFiltered.size - 1, true)
                            }
                            setStatus("Complete")
                        }
                    }
                }
            } catch (e: InterruptedException) {
                println("m_thFilterParse exit normal")
            } catch (e: Exception) {
                e.printStackTrace()
                T.e(e)
            }
            println("End m_thFilterParse thread")
        })
        m_thFilterParse!!.start()
    }

    fun startLogcatParserProcess() {
        clearData()
        logTable.clearSelection()
        subTable!!.clearSelection()
        // 自动切换到logcatparser
        switchToLogParser(Constant.PARSER_TYPE_LOGCAT)
        m_thProcess = Thread(Runnable {
            try {
                var s: String?
                m_Process = null
                T.d("getProcessCmd() = $processCmd")
                m_Process = Runtime.getRuntime().exec(processCmd)
                val stdOut = BufferedReader(
                        InputStreamReader(m_Process!!.inputStream,
                                "UTF-8"))
                val fileOut: Writer = BufferedWriter(OutputStreamWriter(
                        FileOutputStream(m_strLogFileName), "UTF-8"))
                startLogcatParse()
                while (stdOut.readLine().also { s = it } != null) {
                    if (s != null && "" != s!!.trim { it <= ' ' }) {
                        synchronized(FILE_LOCK) {
                            fileOut.write(s)
                            fileOut.write("\r\n")
                            // fileOut.newLine();
                            fileOut.flush()
                        }
                    }
                }
                fileOut.close()
                // T.d("Exit Code: " + m_Process.exitValue());
            } catch (e: Exception) {
                T.e("e = $e")
            }
            stopLogcatParserProcess()
        })
        m_thProcess!!.start()
        setProcessBtn(true)
    }

    fun checkLogLVFilter(logInfo: LogInfo): Boolean {
        if (m_nFilterLogLV == LogInfo.LOG_LV_ALL) return true
        if (m_nFilterLogLV and LogInfo.LOG_LV_VERBOSE != 0
                && logInfo.logLV.startsWith("V")) return true
        if (m_nFilterLogLV and LogInfo.LOG_LV_DEBUG != 0
                && logInfo.logLV.startsWith("D")) return true
        if (m_nFilterLogLV and LogInfo.LOG_LV_INFO != 0
                && logInfo.logLV.startsWith("I")) return true
        if (m_nFilterLogLV and LogInfo.LOG_LV_WARN != 0
                && logInfo.logLV.startsWith("W")) return true
        if (m_nFilterLogLV and LogInfo.LOG_LV_ERROR != 0
                && logInfo.logLV.startsWith("E")) return true
        return (m_nFilterLogLV and LogInfo.LOG_LV_FATAL != 0
                && logInfo.logLV.startsWith("F"))
    }

    fun checkPidFilter(logInfo: LogInfo): Boolean {
        if (logTable.GetFilterShowPid().length <= 0) return true
        val stk = StringTokenizer(
                logTable.GetFilterShowPid(), "|", false)
        while (stk.hasMoreElements()) {
            if (logInfo.pid.toLowerCase().contains(
                            stk.nextToken().toLowerCase())) return true
        }
        return false
    }

    fun checkTidFilter(logInfo: LogInfo): Boolean {
        if (logTable.GetFilterShowTid().length <= 0) return true
        val stk = StringTokenizer(
                logTable.GetFilterShowTid(), "|", false)
        while (stk.hasMoreElements()) {
            if (logInfo.thread.toLowerCase().contains(
                            stk.nextToken().toLowerCase())) return true
        }
        return false
    }

    fun checkFindFilter(logInfo: LogInfo): Boolean {
        if (logTable.GetFilterFind().length <= 0) return true
        val stk = StringTokenizer(logTable.GetFilterFind(),
                "|", false)
        while (stk.hasMoreElements()) {
            if (logInfo.message.toLowerCase().contains(
                            stk.nextToken().toLowerCase())) return true
        }
        return false
    }

    fun checkRemoveFilter(logInfo: LogInfo): Boolean {
        if (logTable.GetFilterRemove().length <= 0) return true
        val stk = StringTokenizer(
                logTable.GetFilterRemove(), "|", false)
        while (stk.hasMoreElements()) {
            if (logInfo.message.toLowerCase().contains(
                            stk.nextToken().toLowerCase())) return false
        }
        return true
    }

    fun checkShowTagFilter(logInfo: LogInfo): Boolean {
        if (logTable.GetFilterShowTag().length <= 0) return true
        val stk = StringTokenizer(
                logTable.GetFilterShowTag(), "|", false)
        while (stk.hasMoreElements()) {
            if (logInfo.tag.toLowerCase().contains(
                            stk.nextToken().toLowerCase())) return true
        }
        return false
    }

    fun checkRemoveTagFilter(logInfo: LogInfo): Boolean {
        if (logTable.GetFilterRemoveTag().length <= 0) return true
        val stk = StringTokenizer(
                logTable.GetFilterRemoveTag(), "|", false)
        while (stk.hasMoreElements()) {
            if (logInfo.tag.toLowerCase().contains(
                            stk.nextToken().toLowerCase())) return false
        }
        return true
    }

    fun checkBookmarkFilter(logInfo: LogInfo): Boolean {
        if (logTable.GetFilterBookmarkTag().length <= 0 && logInfo.bookmark.length <= 0) return true
        val stk = StringTokenizer(
                logTable.GetFilterBookmarkTag(), "|", false)
        while (stk.hasMoreElements()) {
            if (logInfo.bookmark.toLowerCase().contains(
                            stk.nextToken().toLowerCase())) return true
        }
        return false
    }

    fun checkLogFlowFilter(logInfo: LogInfo): Boolean {
        if (!logTable.isFilterLogFlow) {
            return true
        }
        val flowResults = logInfo.flowResults
        return flowResults != null && flowResults.size > 0
    }

    fun checkFileNameFilter(logInfo: LogInfo): Boolean {
        if (logInfo.fileName == null || logInfo.fileName.length <= 0) {
            return true
        }
        return if (logTable.GetFilterFileName() == null || logTable.GetFilterFileName().length <= 0) {
            true
        } else logInfo.fileName.startsWith(logTable.GetFilterFileName())
    }

    fun checkToTimeFilter(logInfo: LogInfo): Boolean {
        if (logInfo.timestamp == -1L) return true
        return if (logTable.GetFilterToTime() == -1L) {
            true
        } else logInfo.timestamp <= logTable.GetFilterToTime()
        //        T.d("checkToTimeFilter:" + logInfo.getTime() + " | " + logInfo.getTimestamp() + " | " + getLogTable().GetFilterToTime());
    }

    fun checkFromTimeFilter(logInfo: LogInfo): Boolean {
        if (logInfo.timestamp == -1L) return true
        return if (logTable.GetFilterFromTime() == -1L) {
            true
        } else logInfo.timestamp >= logTable.GetFilterFromTime()

//        T.d("checkFromTimeFilter:" + logInfo.getTime() + " | " + logInfo.getTimestamp() + " | " + getLogTable().GetFilterFromTime());
    }

    fun checkUseFilter(): Boolean {
        mFilterEnabled = !(!m_ipIndicator.m_chBookmark.isSelected
                && !m_ipIndicator.m_chError.isSelected
                && checkLogLVFilter(LogInfo())
                && (logTable.GetFilterShowPid().length == 0 || !m_chkEnableShowPid.isSelected)
                && (logTable.GetFilterShowTid().length == 0 || !m_chkEnableShowTid.isSelected)
                && (logTable.GetFilterShowTag().length == 0 || !m_chkEnableShowTag.isSelected)
                && (logTable.GetFilterRemoveTag().length == 0 || !m_chkEnableRemoveTag.isSelected)
                && (logTable.GetFilterBookmarkTag().length == 0 || !m_chkEnableBookmarkTag.isSelected)
                && (logTable.GetFilterFileName().length == 0 || !m_chkEnableFileNameFilter.isSelected)
                && (!logTable.isFilterLogFlow || !m_chkEnableLogFlowTag.isSelected)
                && (logTable.GetFilterFromTime() == -1L && logTable.GetFilterToTime() == -1L || !m_chkEnableTimeTag.isSelected)
                && (logTable.GetFilterFind().length == 0 || !m_chkEnableIncludeWord.isSelected)
                && (logTable.GetFilterRemove().length == 0 || !m_chkEnableExcludeWord.isSelected))
        return mFilterEnabled
    }

    //////////////////////////////////////////////////////////////////////
    var m_alButtonListener = ActionListener { e ->
        if (e.source == m_btnDevice) {
            m_selectedDevice = null
            val cmd = aDBValidCmd
            addDevicesToListModelFromCmd(cmd, m_lDeviceList.model as DefaultListModel<TargetDevice>)
        } else if (e.source == m_btnSetFont) {
            logTable.setFontSize(m_tfFontSize
                    .text?.toInt() ?: 12)
            subTable!!.setFontSize(m_tfFontSize
                    .text?.toInt() ?: 12)
            updateLogTable(-1, false)
        } else if (e.source == m_btnRun) {
            startLogcatParserProcess()
        } else if (e.source == m_btnStop) {
            stopLogcatParserProcess()
        } else if (e.source == m_btnClear) {
            val bBackup = m_bPauseADB
            m_bPauseADB = true
            clearData()
            updateLogTable(-1, false)
            m_bPauseADB = bBackup
        } else if (e.source == m_tbtnPause) {
            pauseLogcatParserProcess()
        }
    }

    override fun postEvent(param: EventBus.Event) {
        when (param.type) {
            EventBus.TYPE.EVENT_CLICK_BOOKMARK, EventBus.TYPE.EVENT_CLICK_ERROR -> {
                mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                runFilter()
            }
            EventBus.TYPE.EVENT_CHANGE_FILTER_SHOW_PID -> m_tfShowPid.text = logTable.GetFilterShowPid()
            EventBus.TYPE.EVENT_CHANGE_FILTER_SHOW_TAG -> m_tfShowTag.text = logTable.GetFilterShowTag()
            EventBus.TYPE.EVENT_CHANGE_FILTER_REMOVE_TAG -> m_tfRemoveTag.text = logTable.GetFilterRemoveTag()
            EventBus.TYPE.EVENT_CHANGE_FILTER_FROM_TIME -> {
                val fromTimeStr = param.param1 as String
                m_tfFromTimeTag.text = fromTimeStr
            }
            EventBus.TYPE.EVENT_CHANGE_FILTER_TO_TIME -> {
                val toTimeStr = param.param1 as String
                m_tfToTimeTag.text = toTimeStr
            }
            EventBus.TYPE.EVENT_CHANGE_SELECTION -> {
                val target = param.param1 as LogInfo
                logTable.changeSelection(target)
            }
        }
    }

    fun updateLogTable(nRow: Int, bMove: Boolean) {
        // System.out.println("updateTable nRow:" + nRow + " | " + bMove);
        m_tmLogTableModel.fireTableDataChanged()
        m_logScrollVPane.validate()
        // if(nRow >= 0)
        // m_tbLogTable.changeSelection(nRow, 0, false, false);
        logTable.invalidate()
        logTable.repaint()
        if (nRow >= 0) logTable.changeSelection(nRow, 0, false, false, bMove)
        updateSubTable(-1)
    }

    fun updateSubTable(nRow: Int) {
//        System.out.println("updateSubTable nRow:" + nRow + " | " + bMove);
        m_tSubLogTableModel.fireTableDataChanged()
        m_subLogScrollVPane.validate()
        subTable!!.invalidate()
        subTable!!.repaint()
        if (nRow >= 0) subTable!!.showRowCenterIfNotInRect(nRow, true)
    }

    var mFilterListener: DocumentListener = object : DocumentListener {
        override fun changedUpdate(arg0: DocumentEvent) {
            try {
                if (arg0.document == m_tfIncludeWord.document && m_chkEnableIncludeWord.isSelected) {
                    logTable.setFilterFind(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if ((arg0.document
                                == m_tfExcludeWord.document) && m_chkEnableExcludeWord.isSelected) {
                    logTable.SetFilterRemove(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfShowPid.document && m_chkEnableShowPid.isSelected) {
                    logTable.SetFilterShowPid(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfShowTid.document && m_chkEnableShowTid.isSelected) {
                    logTable.SetFilterShowTid(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfShowTag.document && m_chkEnableShowTag.isSelected) {
                    logTable.SetFilterShowTag(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfRemoveTag.document && m_chkEnableRemoveTag.isSelected) {
                    logTable.SetFilterRemoveTag(arg0.document.getText(
                            0, arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfBookmarkTag.document && m_chkEnableBookmarkTag.isSelected) {
                    logTable.SetFilterBookmarkTag(arg0.document.getText(
                            0, arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfShowFileName.document && m_chkEnableFileNameFilter.isSelected) {
                    logTable.SetFilterFileName(arg0.document.getText(
                            0, arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfHighlight.document && m_chkEnableHighlight.isSelected) {
                    logTable.SetHighlight(arg0.document.getText(0,
                            arg0.document.length))
                    subTable!!.SetHighlight(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfSearch.document) {
                    logTable.SetSearchHighlight(arg0.document.getText(0,
                            arg0.document.length))
                    subTable!!.SetSearchHighlight(arg0.document.getText(0,
                            arg0.document.length))
                    logTable.gotoNextSearchResult()
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfFromTimeTag.document) {
                    logTable.SetFilterFromTime(m_tfFromTimeTag.text)
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfToTimeTag.document) {
                    logTable.SetFilterToTime(m_tfToTimeTag.text)
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                }
            } catch (e: Exception) {
                T.e(e)
            }
        }

        override fun insertUpdate(arg0: DocumentEvent) {
            try {
                if (arg0.document == m_tfIncludeWord.document && m_chkEnableIncludeWord.isSelected) {
                    logTable.setFilterFind(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if ((arg0.document
                                == m_tfExcludeWord.document) && m_chkEnableExcludeWord.isSelected) {
                    logTable.SetFilterRemove(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfShowPid.document && m_chkEnableShowPid.isSelected) {
                    logTable.SetFilterShowPid(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfShowTid.document && m_chkEnableShowTid.isSelected) {
                    logTable.SetFilterShowTid(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfShowTag.document && m_chkEnableShowTag.isSelected) {
                    logTable.SetFilterShowTag(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfRemoveTag.document && m_chkEnableRemoveTag.isSelected) {
                    logTable.SetFilterRemoveTag(arg0.document.getText(
                            0, arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfBookmarkTag.document && m_chkEnableBookmarkTag.isSelected) {
                    logTable.SetFilterBookmarkTag(arg0.document.getText(
                            0, arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfShowFileName.document && m_chkEnableFileNameFilter.isSelected) {
                    logTable.SetFilterFileName(arg0.document.getText(
                            0, arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfHighlight.document && m_chkEnableHighlight.isSelected) {
                    logTable.SetHighlight(arg0.document.getText(0,
                            arg0.document.length))
                    subTable!!.SetHighlight(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfSearch.document) {
                    logTable.SetSearchHighlight(arg0.document.getText(0,
                            arg0.document.length))
                    subTable!!.SetSearchHighlight(arg0.document.getText(0,
                            arg0.document.length))
                    logTable.gotoNextSearchResult()
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfFromTimeTag.document) {
                    logTable.SetFilterFromTime(m_tfFromTimeTag.text)
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfToTimeTag.document) {
                    logTable.SetFilterToTime(m_tfToTimeTag.text)
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                }
            } catch (e: Exception) {
                T.e(e)
            }
        }

        override fun removeUpdate(arg0: DocumentEvent) {
            try {
                if (arg0.document == m_tfIncludeWord.document && m_chkEnableIncludeWord.isSelected) {
                    logTable.setFilterFind(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if ((arg0.document
                                == m_tfExcludeWord.document) && m_chkEnableExcludeWord.isSelected) {
                    logTable.SetFilterRemove(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfShowPid.document && m_chkEnableShowPid.isSelected) {
                    logTable.SetFilterShowPid(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfShowTid.document && m_chkEnableShowTid.isSelected) {
                    logTable.SetFilterShowTid(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfShowTag.document && m_chkEnableShowTag.isSelected) {
                    logTable.SetFilterShowTag(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfRemoveTag.document && m_chkEnableRemoveTag.isSelected) {
                    logTable.SetFilterRemoveTag(arg0.document.getText(
                            0, arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfShowFileName.document && m_chkEnableFileNameFilter.isSelected) {
                    logTable.SetFilterFileName(arg0.document.getText(
                            0, arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfBookmarkTag.document && m_chkEnableBookmarkTag.isSelected) {
                    logTable.SetFilterBookmarkTag(arg0.document.getText(
                            0, arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfHighlight.document && m_chkEnableHighlight.isSelected) {
                    logTable.SetHighlight(arg0.document.getText(0,
                            arg0.document.length))
                    subTable!!.SetHighlight(arg0.document.getText(0,
                            arg0.document.length))
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfSearch.document) {
                    logTable.SetSearchHighlight(arg0.document.getText(0,
                            arg0.document.length))
                    subTable!!.SetSearchHighlight(arg0.document.getText(0,
                            arg0.document.length))
                    logTable.gotoNextSearchResult()
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                } else if (arg0.document == m_tfFromTimeTag.document) {
                    logTable.SetFilterFromTime(m_tfFromTimeTag.text)
                    runFilter()
                } else if (arg0.document == m_tfToTimeTag.document) {
                    logTable.SetFilterToTime(m_tfToTimeTag.text)
                    mLogParsingState = Constant.PARSING_STATUS_CHANGE_PENDING
                    runFilter()
                }
            } catch (e: Exception) {
                T.e(e)
            }
        }
    }

    private fun loadTableColumnState() {
        for (nIndex in m_colWidths.indices) {
            LogFilterTableModel.setColumnWidth(nIndex, m_colWidths[nIndex])
        }
        m_colWidths = LogFilterTableModel.ColWidth
        // 不支持的column不能操作
        m_chkClmBookmark.isEnabled = logTable.showColumn(LogFilterTableModel.COLUMN_BOOKMARK,
                m_chkClmBookmark.isSelected)
        m_chkClmLine.isEnabled = logTable.showColumn(LogFilterTableModel.COLUMN_LINE,
                m_chkClmLine.isSelected)
        m_chkClmDate.isEnabled = logTable.showColumn(LogFilterTableModel.COLUMN_DATE,
                m_chkClmDate.isSelected)
        m_chkClmTime.isEnabled = logTable.showColumn(LogFilterTableModel.COLUMN_TIME,
                m_chkClmTime.isSelected)
        m_chkClmLogLV.isEnabled = logTable.showColumn(LogFilterTableModel.COLUMN_LOGLV,
                m_chkClmLogLV.isSelected)
        m_chkClmPid.isEnabled = logTable.showColumn(LogFilterTableModel.COLUMN_PID,
                m_chkClmPid.isSelected)
        m_chkClmThread.isEnabled = logTable.showColumn(LogFilterTableModel.COLUMN_THREAD,
                m_chkClmThread.isSelected)
        m_chkClmTag.isEnabled = logTable.showColumn(LogFilterTableModel.COLUMN_TAG,
                m_chkClmTag.isSelected)
        m_chkClmMessage.isEnabled = logTable.showColumn(LogFilterTableModel.COLUMN_MESSAGE,
                m_chkClmMessage.isSelected)
        m_chkClmFile.isEnabled = logTable.showColumn(LogFilterTableModel.COLUMN_FILE,
                m_chkClmFile.isSelected)
        subTable!!.showColumn(LogFilterTableModel.COLUMN_BOOKMARK,
                m_chkClmBookmark.isSelected)
        subTable!!.showColumn(LogFilterTableModel.COLUMN_LINE,
                m_chkClmLine.isSelected)
        subTable!!.showColumn(LogFilterTableModel.COLUMN_DATE,
                m_chkClmDate.isSelected)
        subTable!!.showColumn(LogFilterTableModel.COLUMN_TIME,
                m_chkClmTime.isSelected)
        subTable!!.showColumn(LogFilterTableModel.COLUMN_LOGLV,
                m_chkClmLogLV.isSelected)
        subTable!!.showColumn(LogFilterTableModel.COLUMN_PID,
                m_chkClmPid.isSelected)
        subTable!!.showColumn(LogFilterTableModel.COLUMN_THREAD,
                m_chkClmThread.isSelected)
        subTable!!.showColumn(LogFilterTableModel.COLUMN_TAG,
                m_chkClmTag.isSelected)
        subTable!!.showColumn(LogFilterTableModel.COLUMN_MESSAGE,
                m_chkClmMessage.isSelected)
        subTable!!.showColumn(LogFilterTableModel.COLUMN_FILE,
                m_chkClmFile.isSelected)
    }

    var m_itemListener = ItemListener { itemEvent ->
        val check = itemEvent.source as JCheckBox
        if (check == m_chkVerbose) {
            setLogLV(LogInfo.LOG_LV_VERBOSE, check.isSelected)
        } else if (check == m_chkDebug) {
            setLogLV(LogInfo.LOG_LV_DEBUG, check.isSelected)
        } else if (check == m_chkInfo) {
            setLogLV(LogInfo.LOG_LV_INFO, check.isSelected)
        } else if (check == m_chkWarn) {
            setLogLV(LogInfo.LOG_LV_WARN, check.isSelected)
        } else if (check == m_chkError) {
            setLogLV(LogInfo.LOG_LV_ERROR, check.isSelected)
        } else if (check == m_chkFatal) {
            setLogLV(LogInfo.LOG_LV_FATAL, check.isSelected)
        } else if (check == m_chkClmBookmark) {
            logTable.showColumn(LogFilterTableModel.COLUMN_BOOKMARK,
                    check.isSelected)
            subTable!!.showColumn(LogFilterTableModel.COLUMN_BOOKMARK,
                    check.isSelected)
        } else if (check == m_chkClmFile) {
            logTable.showColumn(LogFilterTableModel.COLUMN_FILE,
                    check.isSelected)
            subTable!!.showColumn(LogFilterTableModel.COLUMN_FILE,
                    check.isSelected)
        } else if (check == m_chkClmLine) {
            logTable.showColumn(LogFilterTableModel.COLUMN_LINE,
                    check.isSelected)
            subTable!!.showColumn(LogFilterTableModel.COLUMN_LINE,
                    check.isSelected)
        } else if (check == m_chkClmDate) {
            logTable.showColumn(LogFilterTableModel.COLUMN_DATE,
                    check.isSelected)
            subTable!!.showColumn(LogFilterTableModel.COLUMN_DATE,
                    check.isSelected)
        } else if (check == m_chkClmTime) {
            logTable.showColumn(LogFilterTableModel.COLUMN_TIME,
                    check.isSelected)
            subTable!!.showColumn(LogFilterTableModel.COLUMN_TIME,
                    check.isSelected)
        } else if (check == m_chkClmLogLV) {
            logTable.showColumn(LogFilterTableModel.COLUMN_LOGLV,
                    check.isSelected)
            subTable!!.showColumn(LogFilterTableModel.COLUMN_LOGLV,
                    check.isSelected)
        } else if (check == m_chkClmPid) {
            logTable.showColumn(LogFilterTableModel.COLUMN_PID,
                    check.isSelected)
            subTable!!.showColumn(LogFilterTableModel.COLUMN_PID,
                    check.isSelected)
        } else if (check == m_chkClmThread) {
            logTable.showColumn(LogFilterTableModel.COLUMN_THREAD,
                    check.isSelected)
            subTable!!.showColumn(LogFilterTableModel.COLUMN_THREAD,
                    check.isSelected)
        } else if (check == m_chkClmTag) {
            logTable.showColumn(LogFilterTableModel.COLUMN_TAG,
                    check.isSelected)
            subTable!!.showColumn(LogFilterTableModel.COLUMN_TAG,
                    check.isSelected)
        } else if (check == m_chkClmMessage) {
            logTable.showColumn(LogFilterTableModel.COLUMN_MESSAGE,
                    check.isSelected)
            subTable!!.showColumn(LogFilterTableModel.COLUMN_MESSAGE,
                    check.isSelected)
        } else if (check == m_chkEnableIncludeWord || check == m_chkEnableExcludeWord || check == m_chkEnableShowPid || check == m_chkEnableShowTid || check == m_chkEnableShowTag || check == m_chkEnableRemoveTag || check == m_chkEnableBookmarkTag || check == m_chkEnableLogFlowTag || check == m_chkEnableFileNameFilter || check == m_chkEnableTimeTag || check == m_chkEnableHighlight) {
            useFilter(check)
        }
    }

    ///////////////////////////////////热键///////////////////////////////////
    private val mKeyEventDispatcher = KeyEventDispatcher { e ->
        if (!this@LogFilterMain.isFocused) {
            return@KeyEventDispatcher false
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
        val altPressed = Utils.isAltKeyPressed(e)
        val ctrlPressed = Utils.isControlKeyPressed(e)
        when (e.keyCode) {
            KeyEvent.VK_F2 -> if (e.isControlDown && e.id == KeyEvent.KEY_PRESSED) {
                val arSelectedRow = logTable.selectedRows
                for (nIndex in arSelectedRow) {
                    val logInfo = m_tmLogTableModel.getRow(nIndex)
                    logInfo.isMarked = !logInfo.isMarked
                    markLogInfo(nIndex, logInfo.line - 1, logInfo.isMarked)
                }
                logTable.repaint()
            } else if (!e.isControlDown && e.id == KeyEvent.KEY_PRESSED) logTable.gotoPreBookmark()
            KeyEvent.VK_F3 -> {
                if (e.id == KeyEvent.KEY_PRESSED) logTable.gotoNextBookmark()
                return@KeyEventDispatcher false
            }
            KeyEvent.VK_F -> if (e.id == KeyEvent.KEY_PRESSED && ctrlPressed) {
                showPanelAndSetSearchFocus()
            }
            KeyEvent.VK_H -> if (e.id == KeyEvent.KEY_PRESSED && ctrlPressed) {
                showPanelAndSetHighLightFocus()
            }
            KeyEvent.VK_W -> if (e.id == KeyEvent.KEY_PRESSED && ctrlPressed) {
                setWordIncludeFocus()
            }
            KeyEvent.VK_T -> if (e.id == KeyEvent.KEY_PRESSED && ctrlPressed) {
                setTagIncludeFocus()
            }
            KeyEvent.VK_B -> if (e.id == KeyEvent.KEY_PRESSED && ctrlPressed) {
                setMainTableFocus()
            }
            KeyEvent.VK_F5 -> if (e.id == KeyEvent.KEY_PRESSED) logTable.gotoNextSearchResult()
            KeyEvent.VK_F4 -> if (e.id == KeyEvent.KEY_PRESSED) logTable.gotoPreSearchResult()
            KeyEvent.VK_LEFT -> if (e.id == KeyEvent.KEY_PRESSED && altPressed) logTable.historyBack()
            KeyEvent.VK_RIGHT -> if (e.id == KeyEvent.KEY_PRESSED && altPressed) logTable.historyForward()
        }
        false
    }

    ///////////////////////////////////对话框///////////////////////////////////
    fun openFileBrowserToLoad(type: FileType) {
        val fd = FileDialog(this, "File open", FileDialog.LOAD)
        if (type == FileType.LOG) {
            fd.directory = m_strLastDir
        }
        fd.isMultipleMode = type == FileType.LOG
        fd.isVisible = true
        if (fd.file != null) {
            when (type) {
                FileType.LOG -> {
                    parseLogFile(fd.files) // multi files
                    m_strLastDir = fd.directory
                }
                FileType.MODE -> loadModeFile(File(fd.directory + fd.file))
            }
        }
    }

    private fun openPackagesView() {
        var title = "packages"
        var deviceID: String? = null
        if (m_selectedDevice != null) {
            title = m_selectedDevice.toString()
            deviceID = m_selectedDevice!!.code
        }
        val packageViewDialog = PackageViewDialog(this, title, deviceID, PackageViewDialogListener { value ->
            var pidShow = logTable.GetFilterShowPid()
            if (pidShow.contains("|$value")) {
                pidShow = pidShow.replace("|$value", "")
            } else if (pidShow.contains(value)) {
                pidShow = pidShow.replace(value, "")
            } else {
                pidShow += "|$value"
            }
            logTable.SetFilterShowPid(pidShow)
            this@LogFilterMain.postEvent(EventBus.Event(EventBus.TYPE.EVENT_CHANGE_FILTER_SHOW_PID))
        })
        packageViewDialog.isModal = false
        packageViewDialog.isVisible = true
    }

    private fun openDumpsysView(cmd: String) {
        var title = "Running Tasks"
        var deviceID: String? = null
        if (m_selectedDevice != null) {
            title = m_selectedDevice.toString()
            deviceID = m_selectedDevice!!.code
        }
        val dumpsysViewDialog = DumpsysViewDialog(this, title, deviceID, cmd, object : DumpsysViewDialogListener {
            override fun onRowSingleClick(value: String) {}
            override fun onRowDoubleClick(value: String) {}
        })
        dumpsysViewDialog.isModal = false
        dumpsysViewDialog.isVisible = true
    }

    private fun openShowRowContentDialog(content: String?) {
        if (content == null || content.length <= 0) {
            return
        }
        val title = "Selected Rows"
        val contentDialog = RowsContentDialog(this, title, content)
        contentDialog.isModal = false
        contentDialog.isVisible = true
    }

    private fun openFileBrowserToSave(type: FileType) {
        val fd = FileDialog(this, "File save", FileDialog.SAVE)
        if (type != FileType.MODE) {
            return
        }
        fd.isVisible = true
        if (fd.file != null) {
            when (type) {
                FileType.MODE -> saveModeFile(File(fd.directory + fd.file))
            }
        }
    }

    private fun saveModeFile(file: File?) {
        if (file == null) {
            T.e("mode file == null")
            return
        }
        mUIStateSaver.save(file.absolutePath)
    }

    private fun loadModeFile(file: File?) {
        if (file == null) {
            T.e("mode file == null")
            return
        }
        mUIStateSaver.load(file.absolutePath)
    }

    ///////////////////////////////////diff///////////////////////////////////
    override fun refreshDiffMenuBar() {
        if (mDiffService!!.diffServiceType == DiffServiceType.AS_SERVER) {
            if (mDiffService!!.isDiffConnected) {
                mConnectDiffMenuItem.isEnabled = false
                mDisconnectDiffMenuItem.isEnabled = true
            } else {
                mConnectDiffMenuItem.isEnabled = true
                mDisconnectDiffMenuItem.isEnabled = false
            }
        }
    }

    override fun refreshUIWithDiffState() {
        if (!mDiffService!!.isDiffConnected) {
            mSyncScrollCheckBox.isEnabled = false
            mSyncSelectedCheckBox.isEnabled = false
            m_tfDiffState.background = null
            m_tfDiffState.text = "disconnected"
        } else {
            mSyncScrollCheckBox.isEnabled = true
            mSyncSelectedCheckBox.isEnabled = true
            m_tfDiffState.background = Color.GREEN
            when (mDiffService!!.diffServiceType) {
                DiffServiceType.AS_CLIENT -> m_tfDiffState.text = "as client"
                DiffServiceType.AS_SERVER -> m_tfDiffState.text = "as server"
            }
        }
    }

    private fun initDiffService() {
        val port = 20000 + Random().nextInt(10000)
        m_tfDiffPort.text = "port: $port"
        mDiffService = DiffService(this, port)
        logTable.diffService = mDiffService
    }

    ///////////////////////////////////log flow///////////////////////////////////
    // 当前LogFlow运行到哪一行
    private var mLastProcessFlowLine = -1

    @FieldSaveState
    private var mShowFlowInLogTable = false
    private fun initLogFlow() {
        val confDir = File(Constant.LOG_FLOW_CONFIG_DIR)
        if (!confDir.exists()) {
            confDir.mkdirs()
            T.d("create log flow config directory: " + confDir.absolutePath)
        }
        LogFlowManager.getInstance().init(confDir)
        logTable.isShowLogFlowResult = mShowFlowInLogTable
        subTable!!.isShowLogFlowResult = mShowFlowInLogTable

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
//        System.out.println(currentResult);
    }

    private fun showAllFlow() {
        synchronized(FILTER_LOCK) {
            appendAllFlowLogAndSetLogState()
            val flowResults = LogFlowManager.getInstance().currentResult
            if (flowResults.size > 0) {
                val dialog = LogFlowDialog(flowResults)
                dialog.setListener(object : LogFlowDialog.Listener {
                    override fun onOkButtonClicked(dialog: LogFlowDialog) {
                        dialog.hide()
                    }

                    override fun onItemSelected(dialog: LogFlowDialog, result: ResultItem) {
                        // jump to result line
                        logTable.changeSelection(result.logInfo)
                    }

                    override fun onMarkItem(logFlowDialog: LogFlowDialog, resultItem: ResultItem) {
                        if (resultItem != null) {
                            val logInfo = resultItem.logInfo
                            markLogInfo(0, logInfo.line - 1, !logInfo.isMarked)
                        }
                    }
                })
                dialog.show()
            }
        }
    }

    /*
    把LogInfoAll全部推倒log flow manager里
     */
    private fun appendAllFlowLogAndSetLogState() {
        if (mLastProcessFlowLine <= 0) {
            LogFlowManager.getInstance().reset()
        }
        if (mLastProcessFlowLine < m_arLogInfoAll.size - 1) {
            val logInfos: List<LogInfo> = ArrayList(m_arLogInfoAll).subList(mLastProcessFlowLine + 1, m_arLogInfoAll.size)
            for (logInfo in logInfos) {
                appendFlowLogAndSetLogState(logInfo)
            }
        }
    }

    /*
    添加一条log到flow中
     */
    private fun appendFlowLogAndSetLogState(logInfo: LogInfo): Boolean {
        mLastProcessFlowLine = logInfo.line
        val checkResult = LogFlowManager.getInstance().check(logInfo)
        if (checkResult != null && checkResult.size > 0) {
            logInfo.flowResults = ArrayList(checkResult.values)
            return true
        }
        return false
    }

    /*
    是否在logtable显示flow result
     */
    private fun handleShowFlowInLogTableStateChanged(showInLogTable: Boolean) {
        if (mShowFlowInLogTable != showInLogTable) {
            mShowFlowInLogTable = showInLogTable
            logTable.isShowLogFlowResult = mShowFlowInLogTable
            subTable!!.isShowLogFlowResult = mShowFlowInLogTable
            // refresh
            appendAllFlowLogAndSetLogState()

            // 关掉的时候disable logflow filter
            if (!mShowFlowInLogTable) {
                m_chkEnableLogFlowTag.isSelected = false
                m_chkEnableLogFlowTag.isEnabled = false
            } else {
                m_chkEnableLogFlowTag.isEnabled = true
            }
            (logTable.model as AbstractTableModel).fireTableDataChanged()
            (subTable!!.model as AbstractTableModel).fireTableDataChanged()
        }
    }

    override fun searchSimilar(cmd: String) {
        logTable.searchSimilarForward(cmd)
    }

    override fun searchTimestamp(cmd: String) {
        try {
            val timestamp = cmd.toLong()
            logTable.selectTargetLogInfoInTimestamp(timestamp)
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
    }

    override fun compareWithSelectedRows(targetRows: String) {
        val fmtRows = logTable.getFormatSelectedRows(intArrayOf(LogFilterTableModel.COLUMN_LINE, LogFilterTableModel.COLUMN_TIME))
        if (fmtRows == null || fmtRows.length == 0) {
            return
        }
        try {
            val tempFile1 = File.createTempFile("target", ".txt")
            var bw = BufferedWriter(FileWriter(tempFile1))
            bw.write(targetRows)
            bw.close()
            val tempFile2 = File.createTempFile("src", ".txt")
            bw = BufferedWriter(FileWriter(tempFile2))
            bw.write(fmtRows)
            bw.close()
            Utils.runCmd(arrayOf(Constant.DIFF_PROGRAM_PATH, tempFile1.absolutePath, tempFile2.absolutePath))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private var mLastVBarValue = 0
    private val mScrollListener = AdjustmentListener { e ->
        val scrollBar = e.source as JScrollBar
        if (scrollBar === m_logScrollVPane.horizontalScrollBar) {
            T.d("HorizontalScrollBar: " + scrollBar.value)
        } else if (scrollBar === m_logScrollVPane.verticalScrollBar) {
            mDiffService!!.writeDiffCommand(
                    DiffService.DiffServiceCmdType.SYNC_SCROLL_V, (scrollBar.value - mLastVBarValue).toString())
            mLastVBarValue = scrollBar.value
        }
    }

    override fun enableSyncScroll(enable: Boolean) {
        mSyncScrollEnable = enable
        if (mSyncScrollEnable) {
//            m_logScrollVPane.getHorizontalScrollBar().addAdjustmentListener(mScrollListener);
            m_logScrollVPane.verticalScrollBar.addAdjustmentListener(mScrollListener)
        } else {
//            m_logScrollVPane.getHorizontalScrollBar().removeAdjustmentListener(mScrollListener);
            m_logScrollVPane.verticalScrollBar.removeAdjustmentListener(mScrollListener)
        }
    }

    private fun enableSyncSelected(enable: Boolean) {
        mSyncScrollSelected = enable
    }

    override fun handleScrollVSyncEvent(cmd: String) {
        val scrollBar = m_logScrollVPane.verticalScrollBar
        val scrollChanged = Integer.valueOf(cmd)
        val newValue = scrollBar.value + scrollChanged
        if (newValue >= scrollBar.minimum && newValue <= scrollBar.maximum) scrollBar.value = newValue
    }

    override fun onSelectedRowChanged(lastRowIndex: Int, rowIndex: Int, logInfo: LogInfo) {
        if (mSyncScrollSelected) {
            if (lastRowIndex > rowIndex) {
                mDiffService!!.writeDiffCommand(
                        DiffService.DiffServiceCmdType.SYNC_SELECTED_BACKWARD,
                        logInfo.message
                )
            } else {
                mDiffService!!.writeDiffCommand(
                        DiffService.DiffServiceCmdType.SYNC_SELECTED_FORWARD,
                        logInfo.message
                )
            }
        }
    }

    override fun handleSelectedForwardSyncEvent(cmd: String) {
        logTable.searchSimilarForward(cmd)
    }

    override fun handleSelectedBackwardSyncEvent(cmd: String) {
        logTable.searchSimilarBackward(cmd)
    }

    ///////////////////////////////////logParser///////////////////////////////////
    private fun switchToLogParser(parserType: Int) {
        val iLogParser = sTypeToParserMap[parserType] ?: return
        m_iLogParser = iLogParser
        m_parserType = parserType
        m_tfParserType.text = sTypeToParserNameMap[parserType]
        loadTableColumnState()
        if (parserType != Constant.PARSER_TYPE_LOGCAT && m_arLogInfoAll.size > 0 && mLastParseredFiles != null && mLastParseredFiles!!.size > 0) {
            parseLogFile(mLastParseredFiles)
        }
    }

    ///////////////////////////////////interface///////////////////////////////////
    enum class FileType {
        LOG, MODE
    }

    class TargetDevice(src: String?) {
        var code: String
        var product: String? = null
        var model: String? = null
        var device: String? = null
        override fun toString(): String {
            return "[" + model + "]" + code
        }

        init {
            var src = src
            src = src!!.replace("\t", " ")
            val codeIdx = src.indexOf(' ')
            if (codeIdx == -1) {
                code = src
            } else {
                code = src.substring(0, codeIdx)
                val infoIdx = src.indexOf("product:")
                if (infoIdx != -1) {
                    val infoStr = src.substring(infoIdx)
                    val infos = infoStr.split("\\s+".toRegex()).toTypedArray()
                    product = infos[0].substring("product:".length)
                    model = infos[1].substring("model:".length)
                    device = infos[2].substring("device:".length)
                }
            }
        }
    }

    companion object {
        private const val serialVersionUID = 1L
        private val sTypeToParserMap: MutableMap<Int, ILogParser> = HashMap()
        private val sTypeToParserNameMap: MutableMap<Int, String> = HashMap()

        ///////////////////////////////////main///////////////////////////////////
        @JvmStatic
        fun main(args: Array<String>) {
            // You should always work with UI inside Event Dispatch Thread (EDT)
            // That includes installing L&F, creating any Swing components etc.
            SwingUtilities.invokeLater {
                configByPlatform()
                val main = LogFilterMain()
                main.pack()
                main.addComponentListener(object : ComponentAdapter() {
                    override fun componentResized(e: ComponentEvent) {
                        main.restoreSplitPane()
                    }
                })
                main.restoreSplitPane()
                if (args != null && args.size > 0) {
                    EventQueue.invokeLater {
                        val files = arrayOfNulls<File>(args.size)
                        for (i in args.indices) {
                            files[i] = File(args[i]).absoluteFile
                        }
                        main.parseLogFile(files)
                    }
                }
            }
        }

        private fun configByPlatform() {
            if (OsCheck.getOperatingSystemType() == OsCheck.OSType.Windows) {
                setUIFont(FontUIResource("微软雅黑", Font.PLAIN, 12))
            } else {
                setUIFont(FontUIResource("Consoles", Font.PLAIN, 12))
            }
            try {
                UIManager.setLookAndFeel(
                        UIManager.getCrossPlatformLookAndFeelClassName())
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: InstantiationException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: UnsupportedLookAndFeelException) {
                e.printStackTrace()
            }
            if (OsCheck.getOperatingSystemType() == OsCheck.OSType.MacOS) {
                val im = UIManager.get("TextField.focusInputMap") as InputMap
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction)
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction)
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction)
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.META_DOWN_MASK), DefaultEditorKit.selectAllAction)
            }
        }

        fun setUIFont(f: FontUIResource?) {
            val keys: Enumeration<*> = UIManager.getDefaults().keys()
            while (keys.hasMoreElements()) {
                val key = keys.nextElement()
                val value = UIManager.get(key)
                if (value is FontUIResource) UIManager.put(key, f)
            }
        }
    }

    init {
        sTypeToParserMap[Constant.PARSER_TYPE_LOGCAT] = LogCatParser()
        sTypeToParserMap[Constant.PARSER_TYPE_BIGO_DEV_LOG] = BigoDevLogParser()
        sTypeToParserMap[Constant.PARSER_TYPE_BIGO_XLOG] = BigoXLogParser()
        sTypeToParserMap[Constant.PARSER_TYPE_IMO_DEV_LOG] = IMODevLogParser()
        sTypeToParserMap[Constant.PARSER_TYPE_DEFAULT_LOG] = DefaultLogParser()
        sTypeToParserNameMap[Constant.PARSER_TYPE_LOGCAT] = "logcat"
        sTypeToParserNameMap[Constant.PARSER_TYPE_BIGO_DEV_LOG] = "bigo dev log"
        sTypeToParserNameMap[Constant.PARSER_TYPE_BIGO_XLOG] = "bigo xlog"
        sTypeToParserNameMap[Constant.PARSER_TYPE_IMO_DEV_LOG] = "imo dev log"
        sTypeToParserNameMap[Constant.PARSER_TYPE_DEFAULT_LOG] = "default"
    }

    ///////////////////////////////////constructor///////////////////////////////////

    init {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                exit()
            }
        })
        initValue()
        val pane = contentPane
        pane.layout = BorderLayout()
        pane.add(createMainSplitPane(), BorderLayout.CENTER)
        pane.add(createStatusPanel(), BorderLayout.SOUTH)
        setDnDListener()
        addChangeListener()
        addUndoListener()
        bindRecentlyPopup()
        startFilterParse()

        // register state saver
        mUIStateSaver = UIStateSaver(this, Constant.INI_FILE_STATE)
        mUIStateSaver.load()
        loadUI()
        loadColor()
        loadCmd()
        initDiffService()
        initLogFlow()
        loadParser()
        addDesc()
        title = Constant.WINDOW_TITLE + " " + Constant.VERSION
        addWindowStateListener { e -> mWindowState = e.newState }
        setupMenuBar()
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(mKeyEventDispatcher)
        isVisible = true
    }
}