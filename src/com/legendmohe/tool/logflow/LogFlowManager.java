package com.legendmohe.tool.logflow;

import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.Utils;
import com.legendmohe.tool.thirdparty.json.JSONArray;
import com.legendmohe.tool.thirdparty.json.JSONException;
import com.legendmohe.tool.thirdparty.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

import javafx.util.Pair;

/**
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

        List<FlowPatternHolder> holders = new ArrayList<>();
        List<File> configFiles = Utils.listFiles(configFolder);
        for (File configFile : configFiles) {
            FlowPatternHolder holder = loadConfigFile(configFile);
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

    public List<FlowResult> getCurrentResult() {
        List<FlowResult> results = new ArrayList<>();
        for (FlowStack flowStack : mPatternChecker.mFlowStacks) {
            results.addAll(flowStack.flowResults);
            results.addAll(flowStack.curFlowResult);
        }
        return results;
    }

    ///////////////////////////////////private///////////////////////////////////

    private FlowPatternHolder loadConfigFile(File configFile) {
        try {
            String content = Utils.fileContent2String(configFile);
            JSONObject configJson = new JSONObject(content);

            FlowPatternHolder holder = new FlowPatternHolder();
            holder.name = configJson.getString("name");
            holder.desc = configJson.getString("desc");
            holder.supportRecursion = configJson.getBoolean("recursive");

            JSONArray flow = configJson.getJSONArray("flow");
            for (int i = 0; i < flow.length(); i++) {
                JSONObject flowItem = flow.getJSONObject(i);
                String tag = flowItem.getString("tag");
                String matchType = flowItem.getString("match_type");
                String pattern = flowItem.getString("pattern");
                String desc = flowItem.getString("desc");

                holder.addPatternItem(new FlowPatternItem(tag, matchType, pattern, desc));
            }
            return holder;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    ///////////////////////////////////flow stack///////////////////////////////////

    private static class FlowPatternChecker {

        List<FlowPatternHolder> mPatternLists;

        List<FlowStack> mFlowStacks;

        int check(LogInfo logInfo) {
            for (int i = 0; i < mPatternLists.size(); i++) {
                FlowStack flowStack = mFlowStacks.get(i);
                FlowPatternHolder patternHolder = mPatternLists.get(i);

                Pair<Integer, LogInfo> infoPair = flowStack.peek();
                FlowPatternItem flowPatternItem = null;
                int idx = -1;
                if (infoPair != null) { // 如果stack上已经有匹配
                    idx = infoPair.getKey() + 1;
                    flowPatternItem = patternHolder.patterns.get(idx);
                }
                if (flowPatternItem != null && logInfo != null && flowPatternItem.match(logInfo)) {
                    flowStack.push(new Pair<>(idx, logInfo));
                    flowStack.markCurrentFlowResults(idx, logInfo, patternHolder);
                    if (idx + 1 >= patternHolder.patterns.size()) {
                        // 已经匹配完了, stack回退，添加result
                        flowStack.collectFlowResults(patternHolder.patterns.size());
                    }
                    return i;
                } else if (patternHolder.matchFirst(logInfo)) {
                    if (flowPatternItem != null && !patternHolder.supportRecursion) {
                        // 不支持嵌套匹配，这里标记上一个isCompleted为false，然后继续
                        flowStack.breakResultStack();
                    }
                    flowStack.push(new Pair<>(0, logInfo));
                    flowStack.markCurrentFlowResults(0, logInfo, patternHolder);
                    if (patternHolder.patterns.size() == 1) {
                        // 已经匹配完了, stack回退，添加result
                        flowStack.collectFlowResults(patternHolder.patterns.size());
                    }
                    return 0;
                }
            }
            return -1;
        }

        void reset() {
            if (mFlowStacks != null) {
                for (FlowStack flowStack : mFlowStacks) {
                    flowStack.reset();
                }
            }
        }

        void init(List<FlowPatternHolder> holders) {
            mPatternLists = holders;
            mFlowStacks = new ArrayList<>(mPatternLists.size());
            for (int i = 0; i < mPatternLists.size(); i++) {
                mFlowStacks.add(new FlowStack());
            }
        }
    }

    private static class FlowStack {

        List<FlowResult> flowResults = new ArrayList<>();

        Stack<FlowResult> curFlowResult = new Stack();

        Stack<Pair<Integer, LogInfo>> mIdxStack = new Stack<>();

        Pair<Integer, LogInfo> pop() {
            return mIdxStack.size() > 0 ? mIdxStack.pop() : null;
        }

        Pair<Integer, LogInfo> peek() {
            return mIdxStack.size() > 0 ? mIdxStack.peek() : null;
        }

        void push(Pair<Integer, LogInfo> idx) {
            mIdxStack.push(idx);
            // 第一个匹配
            if (idx.getKey() == 0) {
                curFlowResult.push(new FlowResult());
            }
        }

        void collectFlowResults(int patternSize) {
            for (int i = 0; i < patternSize; i++) {
                pop();
            }
            if (!curFlowResult.empty()) {
                FlowResult pop = curFlowResult.pop();
                pop.isCompleted = true;
                flowResults.add(pop);
            }
        }

        void breakResultStack() {
            while (peek() != null && peek().getKey() >= 0) {
                pop();
            }
            if (!curFlowResult.empty()) {
                FlowResult pop = curFlowResult.pop();
                pop.isCompleted = false;
                flowResults.add(pop);
            }
        }

        void markCurrentFlowResults(int idx, LogInfo info, FlowPatternHolder holder) {
            curFlowResult.peek().name = holder.name;
            curFlowResult.peek().desc = holder.desc;
            curFlowResult.peek().infoPair.add(
                    new Pair<>(
                            info,
                            holder.patterns.get(idx)
                    )
            );
        }

        void reset() {
            curFlowResult.clear();
            flowResults = new ArrayList<>();
        }
    }

    public static class FlowPatternHolder {
        public String name;
        public String desc;
        public boolean supportRecursion;

        List<FlowPatternItem> patterns = new ArrayList<>();

        void addPatternItem(FlowPatternItem item) {
            if (item != null) {
                item.patternHolder = this;
                patterns.add(item);
            }
        }

        boolean matchFirst(LogInfo logInfo) {
            return patterns.get(0).match(logInfo);
        }

        @Override
        public String toString() {
            return "FlowPatternHolder{" +
                    "name='" + name + '\'' +
                    ", desc='" + desc + '\'' +
                    ", supportRecursion=" + supportRecursion +
                    ", patterns=" + patterns +
                    '}';
        }
    }

    public static class FlowPatternItem {
        public FlowPatternHolder patternHolder;

        public String tag;
        public String matchType;
        public String pattern;
        public String desc;

        private Matcher<String> mMatcher;

        FlowPatternItem(String tag, String matchType, String pattern, String desc) {
            this.tag = tag;
            this.matchType = matchType;
            this.pattern = pattern;
            this.desc = desc;
            this.init();
        }

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
            } else {
                throw new IllegalArgumentException("unknown match type:" + matchType);
            }
        }

        boolean match(LogInfo logInfo) {
            if (!logInfo.getTag().equalsIgnoreCase(tag)) {
                return false;
            }
            return mMatcher.match(logInfo.getMessage());
        }

        @Override
        public String toString() {
            return "{" +
                    "tag='" + tag + '\'' +
                    ", matchType='" + matchType + '\'' +
                    ", pattern='" + pattern + '\'' +
                    ", desc='" + desc + '\'' +
                    '}';
        }
    }

    private interface Matcher<T> {
        boolean match(T o1);
    }

    public static class FlowResult {
        public String name;
        public String desc;
        public boolean isCompleted;

        public List<Pair<LogInfo, FlowPatternItem>> infoPair = new ArrayList<>();

        FlowResult() {
        }

        @Override
        public String toString() {
            return "FlowResult{" +
                    "isCompleted=" + isCompleted +
                    ", infoPair=" + infoPair +
                    '}';
        }
    }
}
