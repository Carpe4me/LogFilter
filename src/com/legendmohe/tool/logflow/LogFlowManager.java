package com.legendmohe.tool.logflow;

import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.Utils;
import com.legendmohe.tool.thirdparty.json.JSONArray;
import com.legendmohe.tool.thirdparty.json.JSONException;
import com.legendmohe.tool.thirdparty.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 日志逻辑流检测
 * <p>
 * 1. 读取配置文件
 * 2. 逐行检测，通过匹配规则驱动状态机变化
 * 3. 外围逻辑导出结果，显示ui
 * <p>
 * Created by hexinyu on 2019/3/29.
 */
public class LogFlowManager {

    private FlowPatternChecker mPatternChecker = new FlowPatternChecker();

    private static class LazyHolder {
        private static final LogFlowManager INSTANCE = new LogFlowManager();
    }

    public static LogFlowManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    //////////////////////////////////////////////////////////////////////

    public void init(File configFolder) {
        if (configFolder == null || !configFolder.exists() || !configFolder.isDirectory()) {
            throw new IllegalArgumentException("invalid config folder:" + configFolder);
        }

        reset();

        List<LogStateMachineHolder> holders = new ArrayList<>();
        List<File> configFiles = Utils.listFiles(configFolder);
        for (File configFile : configFiles) {
            LogStateMachineHolder holder = loadConfigFile(configFile);
            if (holder != null) {
                holders.add(holder);
            }
        }
        mPatternChecker.init(holders);
    }

    public void reset() {
        mPatternChecker.reset();
    }

    public int check(LogInfo logInfo) {
        return mPatternChecker.check(logInfo);
    }

    public Map<String, List<FlowResult>> getCurrentResult() {
        Map<String, List<FlowResult>> results = new HashMap<>();
        for (LogStateMachineHolder holder : mPatternChecker.holders) {
            List<FlowResult> resultList = new ArrayList<>();
            resultList.addAll(holder.results);
            if (holder.currentResult != null && holder.currentResult.isValid()) {
                resultList.add(holder.currentResult);
            }
            if (resultList.size() > 0) {
                results.put(holder.name, resultList);
            }
        }
        return results;
    }

    ///////////////////////////////////private///////////////////////////////////

    private LogStateMachineHolder loadConfigFile(File configFile) {
        try {
            String content = Utils.fileContent2String(configFile);
            JSONObject configJson = new JSONObject(content);

            LogStateMachineHolder holder = new LogStateMachineHolder();
            boolean enable = configJson.optBoolean("enable", true);
            if (!enable) {
                return null;
            }
            holder.name = configJson.getString("name");
            holder.desc = configJson.getString("desc");

            // msg
            JSONObject msgObject = configJson.getJSONObject("msg");
            for (String msgName : msgObject.keySet()) {
                JSONObject msgItem = msgObject.getJSONObject(msgName);

                String tag = msgItem.getString("tag");
                String matchType = msgItem.getString("match_type");
                String pattern = msgItem.getString("pattern");
                String desc = msgItem.getString("desc");

                LogMassage logMassage = new LogMassage();
                logMassage.tag = tag;
                logMassage.matchType = matchType;
                logMassage.pattern = pattern;
                logMassage.desc = desc;
                logMassage.name = msgName;
                holder.mMassageMap.put(msgName, logMassage);
            }

            // state
            JSONArray stateObject = configJson.getJSONArray("state");
            for (Object stateObj : stateObject) {
                JSONObject stateItem = ((JSONObject) stateObj);

                String type = stateItem.optString("type", "normal");
                Object nameObj = stateItem.get("name");
                if (nameObj instanceof JSONArray) {
                    JSONArray nameArray = (JSONArray) nameObj;
                    for (Object name : nameArray) {
                        LogState logState = new LogState();
                        logState.type = LogState.typeMap.get(type);
                        logState.name = (String) name;
                        holder.mStateMap.put((String) name, logState);
                    }
                } else if (nameObj instanceof String) {
                    String name = (String) nameObj;
                    LogState logState = new LogState();
                    logState.type = LogState.typeMap.get(type);
                    logState.name = name;
                    holder.mStateMap.put(name, logState);
                }
            }

            // link
            JSONArray linkObject = configJson.getJSONArray("link");
            for (Object linkObj : linkObject) {
                JSONObject linkItem = ((JSONObject) linkObj);

                JSONArray fromArray = linkItem.getJSONArray("from");
                String to = linkItem.getString("to");
                String msg = linkItem.getString("msg");
                String desc = linkItem.optString("desc", "unknown");

                for (int i = 0; i < fromArray.length(); i++) {
                    String from = fromArray.getString(i);

                    LogStateLink linkState = new LogStateLink();
                    linkState.from = from;
                    linkState.to = to;
                    linkState.msg = msg;
                    linkState.desc = desc;
                    holder.mStateLinks.add(linkState);
                }
            }

            // init state machine
            holder.initStateMachine();
            return holder;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    ///////////////////////////////////flow stack///////////////////////////////////

    private static class FlowPatternChecker {

        private List<LogStateMachineHolder> holders = new ArrayList<>();

        int check(LogInfo logInfo) {
            int successCount = 0;
            for (LogStateMachineHolder holder : holders) {
                if (holder.check(logInfo)) {
                    successCount++;
                }
            }
            return successCount > 0 ? successCount : -1;
        }

        void reset() {
            for (LogStateMachineHolder holder : holders) {
                holder.reset();
            }
        }

        void init(List<LogStateMachineHolder> holders) {
            this.holders = holders;
        }
    }

    public static class LogStateMachineHolder {
        public String name;
        public String desc;

        List<FlowResult> results = new ArrayList<>();
        FlowResult currentResult = new FlowResult();

        StateMachine mStateMachine;

        Map<String, LogMassage> mMassageMap = new HashMap<>();
        Map<String, LogState> mStateMap = new HashMap<>();
        Map<String, Map<String, Map<String, String>>> mLinkDescMap = new HashMap<>();
        List<LogStateLink> mStateLinks = new ArrayList<>();

        void initStateMachine() {
            mStateMachine = StateMachine.createSync();
            // 初始化状态机，转换state实现
            Map<String, StateMachine.State> internalStateMap = new HashMap<>();
            for (String stateName : mStateMap.keySet()) {
                LogState logState = mStateMap.get(stateName);
                StateMachine.State internalState = new InternalState(logState) {
                    @Override
                    public void onEnter(StateMachine.State fromState, Object event, Object data) {
                        handleStateOnEnter(((InternalState) fromState).myState, myState, (LogMassage) event, (LogInfo) data);
                    }
                };
                internalStateMap.put(stateName, internalState);
                mStateMachine.addState(internalState);

                if (logState.type == LogState.TYPE.START) {
                    mStateMachine.setInitState(internalState);
                }
            }

            // 状态转移
            for (LogStateLink link : mStateLinks) {
                Map<String, Map<String, String>> toDescMap = mLinkDescMap.get(link.from);
                if (toDescMap == null) {
                    toDescMap = new HashMap<>();
                    mLinkDescMap.put(link.from, toDescMap);
                }
                Map<String, String> msgDescMap = toDescMap.get(link.to);
                if (msgDescMap == null) {
                    msgDescMap = new HashMap<>();
                    toDescMap.put(link.to, msgDescMap);
                }
                msgDescMap.put(link.msg, link.desc);

                StateMachine.State fromState = internalStateMap.get(link.from);
                StateMachine.State toState = internalStateMap.get(link.to);
                fromState.linkTo(toState, mMassageMap.get(link.msg));
            }

            mStateMachine.start();
        }

        boolean check(LogInfo logInfo) {
            // 只匹配可处理的event
            List<Object> events = mStateMachine.listAcceptableEvent();
            for (Object event : events) {
                LogMassage logMsg = (LogMassage) event;
                if (logMsg.match(logInfo)) {
                    mStateMachine.postEvent(logMsg, logInfo);
                    return true;
                }
            }
            return false;
        }

        void reset() {
            results.clear();
            currentResult = new FlowResult();
            mStateMachine.reset(0);
        }

        /*
        处理状态变化
         */
        private void handleStateOnEnter(LogState fromState, LogState inState, LogMassage msg, LogInfo info) {
            // 是否后开始第一个状态
            if (fromState.type == LogState.TYPE.START) {
                currentResult = new FlowResult();
                currentResult.name = name;
                currentResult.desc = desc;
            }

            // 触发当前状态转换的描述（连线的备注）
            String linkDesc = mLinkDescMap.get(fromState.name).get(inState.name).get(msg.name);
            // 记录info轨迹
            if (currentResult != null) {
                FlowResultLine resultLine = new FlowResultLine();
                resultLine.desc = msg.desc;
                resultLine.linkDesc = linkDesc;
                resultLine.logInfo = info;
                resultLine.isStartLine = fromState.type == LogState.TYPE.START;
                currentResult.resultLines.add(resultLine);
            }

            // 当前是否结束态
            if (inState.type == LogState.TYPE.END
                    || inState.type == LogState.TYPE.ERROR) {
                boolean isError = inState.type == LogState.TYPE.ERROR;

                if (currentResult != null) {
                    currentResult.errorCause = isError ? linkDesc : null;
                }
                results.add(currentResult);
                handleResultCollected(currentResult);
                currentResult = null;
                // 结束后reset一下，重新开始检测
                mStateMachine.reset(0);
            }
        }

        private void handleResultCollected(FlowResult result) {
        }
    }

    private static class InternalState extends StateMachine.State {

        LogState myState;

        public InternalState(LogState state) {
            super(state.name);
            this.myState = state;
        }
    }

    public static class LogMassage {
        public String tag;
        public String matchType;
        public String pattern;
        public String desc;
        public String name;

        private Matcher<String> mMatcher;

        void init() {
            if (matchType.equalsIgnoreCase("regex")) {
                Pattern r = Pattern.compile(pattern);

                mMatcher = new Matcher<String>() {
                    @Override
                    public boolean match(String o1) {
                        return r.matcher(o1).find();
                    }
                };
            } else if (matchType.equalsIgnoreCase("contains")) {
                mMatcher = new Matcher<String>() {
                    @Override
                    public boolean match(String o1) {
                        return o1.contains(pattern);
                    }
                };
            } else if (matchType.equalsIgnoreCase("startsWith")) {
                mMatcher = new Matcher<String>() {
                    @Override
                    public boolean match(String o1) {
                        return o1.startsWith(pattern);
                    }
                };
            } else {
                throw new IllegalArgumentException("unknown match type:" + matchType);
            }
        }

        boolean match(LogInfo logInfo) {
            if (!logInfo.getTag().equalsIgnoreCase(tag)) {
                return false;
            }
            if (mMatcher == null) {
                init();
            }
            return mMatcher.match(logInfo.getMessage());
        }
    }

    private interface Matcher<T> {
        boolean match(T o1);
    }

    public static class LogState {

        enum TYPE {
            NORMAL,
            START,
            END,
            ERROR,
        }

        static final Map<String, TYPE> typeMap = new HashMap<>();

        static {
            typeMap.put("normal", TYPE.NORMAL);
            typeMap.put("start", TYPE.START);
            typeMap.put("end", TYPE.END);
            typeMap.put("error", TYPE.ERROR);
        }

        public TYPE type = TYPE.NORMAL;
        public String name;
    }

    public static class LogStateLink {

        public String from;
        public String to;
        public String msg;
        public String desc;
    }

    //////////////////////////////////////////////////////////////////////

    public static class FlowResult {
        public String name;
        public String desc;
        public String errorCause = "not complete";

        public List<FlowResultLine> resultLines = new ArrayList<>();

        FlowResult() {
        }

        boolean isValid() {
            return name != null && name.length() > 0;
        }

        @Override
        public String toString() {
            return "FlowResult{" +
                    "name='" + name + '\'' +
                    ", desc='" + desc + '\'' +
                    ", errorCause='" + errorCause + '\'' +
                    ", resultLines=" + resultLines +
                    '}';
        }
    }

    public static class FlowResultLine {
        public LogInfo logInfo;
        public String desc;
        public String linkDesc;
        public boolean isStartLine;

        @Override
        public String toString() {
            return "FlowResultLine{" +
                    "logInfo line=" + logInfo.getLine() +
                    ", desc='" + desc + '\'' +
                    '}';
        }
    }
}
