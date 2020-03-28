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

    public synchronized void init(File configFolder) {
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

    public synchronized void reset() {
        mPatternChecker.reset();
    }

    public synchronized Map<String, FlowResultLine> check(LogInfo logInfo) {
        return mPatternChecker.check(logInfo);
    }

    public synchronized Map<String, List<FlowResult>> getCurrentResult() {
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

    public synchronized int getResultIndex(String resultName) {
        for (LogStateMachineHolder holder : mPatternChecker.holders) {
            if (holder.name.equalsIgnoreCase(resultName)) {
                return holder.index;
            }
        }
        return -1;
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
            holder.index = configJson.optInt("index", Integer.MAX_VALUE);
            holder.name = configJson.getString("name");
            holder.desc = configJson.getString("desc");

            // parse msg
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

            // parse state
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

            // parse link
            JSONArray linkObject = configJson.getJSONArray("link");
            for (Object linkObj : linkObject) {
                if (linkObj instanceof JSONObject) {
                    parseLinkDefJSONObject((JSONObject) linkObj, holder);
                } else if (linkObj instanceof String) {
                    parseLinkDefString((String) linkObj, holder);
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

    /*
    json形式的状态转移解析
     */
    private void parseLinkDefJSONObject(JSONObject linkObj, LogStateMachineHolder holder) {
        JSONObject linkItem = linkObj;

        JSONArray msgList = new JSONArray();
        if (!(linkItem.get("msg") instanceof JSONArray)) {
            msgList.put(linkItem.getString("msg"));
        } else {
            msgList = linkItem.getJSONArray("msg");
        }
        JSONArray fromArray = linkItem.getJSONArray("from");
        String to = linkItem.getString("to");
        boolean dropIfError = linkItem.optBoolean("dropIfError", true);
        boolean addToResultIfError = linkItem.optBoolean("addToResultIfError", true);
        boolean removeResultIfError = linkItem.optBoolean("removeResultIfError", false);
        String desc = linkItem.optString("desc", "unknown");

        for (int i = 0; i < fromArray.length(); i++) {
            String from = fromArray.getString(i);
            for (Object msgObj : msgList) {
                String msg = (String) msgObj;
                LogStateLink stateLink = new LogStateLink();
                stateLink.from = from;
                stateLink.to = to;
                stateLink.msg = msg;
                stateLink.desc = desc;
                stateLink.dropIfError = dropIfError;
                stateLink.addToResultIfError = addToResultIfError;
                stateLink.removeResultIfError = removeResultIfError;
                holder.mStateLinks.add(stateLink);
            }
        }
    }

    /*
    字符串形式的状态转移解析
    eg. "START--disconnect-->DISCONNECTED:linkd断开(dropIfError=true;addToResultIfError=true)"
     */
    private void parseLinkDefString(String src, LogStateMachineHolder holder) {
        String[] tempSplitResult = src.split("--");
        String fromStateArray = tempSplitResult[0].trim();
        String[] fromStates = fromStateArray.split("\\|");
        String msgArray = tempSplitResult[1].trim();
        String[] msgs = msgArray.split("\\|");
        String toStateAndParams = tempSplitResult[2].trim();

        tempSplitResult = toStateAndParams.split(":");
        String toState = tempSplitResult[0].trim();
        if (toState.startsWith(">")) {
            toState = toState.substring(1, toState.length()).trim();
        }
        String descAndParams = tempSplitResult[1].trim();

        tempSplitResult = descAndParams.split("\\(");
        String desc = tempSplitResult[0].trim();
        Map<String, String> paramMap = new HashMap<>();
        if (tempSplitResult.length > 1) {
            String paramArray = tempSplitResult[1].trim();
            String[] params = paramArray.substring(0, paramArray.length() - 1).split(";");
            for (String paramKeyValue : params) {
                String[] keyValue = paramKeyValue.split("=");
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                paramMap.put(key, value);
            }
        }

        for (String fromState : fromStates) {
            for (String msg : msgs) {
                LogStateLink stateLink = new LogStateLink();
                stateLink.from = fromState;
                stateLink.to = toState;
                stateLink.msg = msg;
                stateLink.desc = desc;
                stateLink.dropIfError = Boolean.valueOf(paramMap.getOrDefault("dropIfError", "true"));
                stateLink.addToResultIfError = Boolean.valueOf(paramMap.getOrDefault("addToResultIfError", "true"));
                stateLink.removeResultIfError = Boolean.valueOf(paramMap.getOrDefault("removeResultIfError", "false"));
                holder.mStateLinks.add(stateLink);
            }
        }
    }

    ///////////////////////////////////flow stack///////////////////////////////////

    private static class FlowPatternChecker {

        private List<LogStateMachineHolder> holders = new ArrayList<>();

        Map<String, FlowResultLine> check(LogInfo logInfo) {
            Map<String, FlowResultLine> tempResult = new HashMap<>();
            for (LogStateMachineHolder holder : holders) {
                FlowResultLine result = holder.check(logInfo);
                if (result != null) {
                    tempResult.put(holder.name, result);
                }
            }
            return tempResult;
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
        public int index;
        public String name;
        public String desc;

        List<FlowResult> results = new ArrayList<>();
        FlowResult currentResult = new FlowResult();

        StateMachine mStateMachine;

        Map<String, LogMassage> mMassageMap = new HashMap<>();
        Map<String, LogState> mStateMap = new HashMap<>();
        Map<String, Map<String, Map<String, LineDesc>>> mLinkDescMap = new HashMap<>();
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
                Map<String, Map<String, LineDesc>> toDescMap = mLinkDescMap.get(link.from);
                if (toDescMap == null) {
                    toDescMap = new HashMap<>();
                    mLinkDescMap.put(link.from, toDescMap);
                }
                Map<String, LineDesc> msgDescMap = toDescMap.get(link.to);
                if (msgDescMap == null) {
                    msgDescMap = new HashMap<>();
                    toDescMap.put(link.to, msgDescMap);
                }
                msgDescMap.put(link.msg,
                        new LineDesc(link.desc, link.dropIfError, link.addToResultIfError, link.removeResultIfError)
                );

                StateMachine.State fromState = internalStateMap.get(link.from);
                StateMachine.State toState = internalStateMap.get(link.to);
                fromState.linkTo(toState, mMassageMap.get(link.msg));
            }

            mStateMachine.start();
        }

        FlowResultLine check(LogInfo logInfo) {
            // 只匹配可处理的event
            List<Object> events = mStateMachine.listAcceptableEvent();
            for (Object event : events) {
                LogMassage logMsg = (LogMassage) event;
                if (logMsg.match(logInfo)) {
                    mStateMachine.postEvent(logMsg, logInfo);
                    // 取出最后一个
                    FlowResult lastResult;
                    if (currentResult.isValid()) {
                        lastResult = currentResult;
                    } else {
                        if (results.size() == 0) {
                            return null;
                        }
                        lastResult = results.get(results.size() - 1);
                    }
                    return lastResult.resultLines.size() == 0 ? null : lastResult.resultLines.get(lastResult.resultLines.size() - 1);
                }
            }
            return null;
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
            LineDesc linkDesc = mLinkDescMap.get(fromState.name).get(inState.name).get(msg.name);
            // 记录info轨迹
            if (currentResult != null) {
                FlowResultLine resultLine = new FlowResultLine();
                resultLine.desc = msg.desc;
                resultLine.flowResult = currentResult;
                resultLine.linkDesc = linkDesc.desc;
                resultLine.logInfo = info;
                resultLine.isStartLine = fromState.type == LogState.TYPE.START;
                currentResult.resultLines.add(resultLine);
            }

            // 当前是否结束态
            if (inState.type == LogState.TYPE.END
                    || inState.type == LogState.TYPE.ERROR) {
                boolean isError = inState.type == LogState.TYPE.ERROR;

                if (currentResult != null) {
                    currentResult.errorCause = isError ? linkDesc.desc : null;
                }
                // 当错误发生时，addToResultIfError为false的话，不会把它添加到resultLines中
                if (isError && !linkDesc.addToResultIfError) {
                    // remove last
                    currentResult.resultLines.remove(currentResult.resultLines.size() - 1);
                }
                // 当错误发生时，removeResultIfError为true的话，不会把currentResult添加到results中
                if (!(isError && linkDesc.removeResultIfError)) {
                    results.add(currentResult);
                } else {
                    // 标记removeResultIfError为true的情况下，条目不标红
                    for (FlowResultLine resultLine : currentResult.resultLines) {
                        resultLine.isValid = false;
                    }
                }
                currentResult = new FlowResult();
                // 结束后reset一下，重新开始检测
                mStateMachine.reset(0);

                // dropifError为false表明当错误发生时，当前log重新推入状态机再判断一次（应对重复开始case）
                if (isError && !linkDesc.dropIfError) {
                    mStateMachine.postEvent(msg, info);
                }
            }
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
        private static Map<String, Pattern> sPatternCache = new HashMap<>();

        public String tag;
        public String matchType;
        public String pattern;
        public String desc;
        public String name;

        private Matcher<String> mMatcher;

        void init() {
            if (matchType.equalsIgnoreCase("regex")) {
                Pattern finalP = getCachePattern(this.pattern);
                mMatcher = o1 -> finalP.matcher(o1).find();
            } else if (matchType.equalsIgnoreCase("contains")) {
                mMatcher = o1 -> o1.contains(pattern);
            } else if (matchType.equalsIgnoreCase("startsWith")) {
                mMatcher = o1 -> o1.startsWith(pattern);
            } else {
                throw new IllegalArgumentException("unknown match type:" + matchType);
            }
        }

        private Pattern getCachePattern(String pattern) {
            Pattern p = sPatternCache.get(pattern);
            if (p == null) {
                sPatternCache.put(pattern, Pattern.compile(pattern));
            }
            p = sPatternCache.get(pattern);
            return p;
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
        public boolean dropIfError;
        public boolean addToResultIfError;
        public boolean removeResultIfError;
    }

    private static class LineDesc {
        String desc;
        private boolean dropIfError;
        boolean addToResultIfError;
        boolean removeResultIfError;

        LineDesc(String desc, boolean dropIfError, boolean addToResultIfError, boolean removeResultIfError) {
            this.desc = desc;
            this.dropIfError = dropIfError;
            this.addToResultIfError = addToResultIfError;
            this.removeResultIfError = removeResultIfError;
        }
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
        public FlowResult flowResult;
        public boolean isStartLine;
        public boolean isValid = true;

        @Override
        public String toString() {
            return "FlowResultLine{" +
                    "logInfo line=" + logInfo.getLine() +
                    ", desc='" + desc + '\'' +
                    '}';
        }
    }
}
