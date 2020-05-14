package com.legendmohe.tool.logflow;

import com.legendmohe.tool.util.T;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 状态机实现
 * <p>
 * Created by hexinyu on 2018/2/27.
 */
public class StateMachine {
    private static final String TAG = "StateMachine";

    public static final boolean DEBUG = false;

    private String mLogTag = TAG;

    Set<State> mStates = new HashSet<>();
    private State mInitState;
    private State mCurrentState;
    private Object mHandleLock = new Object();

    private StateChangeHandler mHandler;

    private Logger mLogger;

    public static StateMachine createSync() {
        return new StateMachine();
    }

    public static StateMachine createAsync(StateChangeHandler handler) {
        return new StateMachine(handler);
    }

    private StateMachine(StateChangeHandler changeHandler) {
        mHandler = changeHandler;
    }

    private StateMachine() {
        mHandler = new StateChangeHandler();
    }

    public void setLogger(Logger logger) {
        mLogger = logger;
    }

    public void setInitState(State initState) {
        if (DEBUG) {
            T.d(mLogTag + " statemachine setInitState=" + initState);
        }
        if (mLogger != null) {
            mLogger.log(mLogTag, "statemachine setInitState=" + initState);
        }
        mInitState = initState;
    }

    public StateMachine addState(State state) {
        synchronized (this) {
            if (DEBUG) {
                T.d(mLogTag + " statemachine add state=" + state);
            }
            if (mLogger != null) {
                mLogger.log(mLogTag, "statemachine add state=" + state);
            }
            mStates.add(state);
            state.setStateMachine(this);
        }
        return this;
    }

    public void start() {
        synchronized (this) {
            if (DEBUG) {
                T.d(mLogTag + " statemachine start");
            }
            if (mLogger != null) {
                mLogger.log(mLogTag, "statemachine start");
            }
            for (State state : mStates) {
                state.onStart();
            }
            mCurrentState = mInitState;
        }
    }

    public void start(State initState) {
        setInitState(initState);
        start();
    }

    public void stop(int cause) {
        synchronized (this) {
            if (DEBUG) {
                T.d(mLogTag + " stop");
            }
            if (mLogger != null) {
                mLogger.log(mLogTag, "stop");
            }
            for (State state : mStates) {
                state.onStop(cause);
            }
        }
    }

    public void reset(int cause) {
        synchronized (this) {
            if (DEBUG) {
                T.d(mLogTag + " reset cause=" + cause);
            }
            if (mLogger != null) {
                mLogger.log(mLogTag, "reset cause=" + cause);
            }
            for (State state : mStates) {
                state.onReset(cause);
            }
            mCurrentState = mInitState;
        }
    }

    public void postEvent(Object event) {
        postEvent(event, null);
    }

    public void postEvent(final Object event, final Object data) {
        if (mHandler == null) {
            return;
        }
        if (DEBUG) {
            T.d(mLogTag + " postEvent event=" + event + " data=" + data);
        }
        if (mLogger != null) {
            mLogger.log(mLogTag, "postEvent event=" + event + " data=" + data);
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mHandleLock) {
                    State nextState = mCurrentState.mToStates.get(event);
                    if (nextState == null) {
                        mCurrentState.onUnhandleEvent(event, data);
                        return;
                    }

                    if (DEBUG) {
                        T.d(mLogTag + " state change from=" + mCurrentState + " to=" + nextState);
                    }
                    if (mLogger != null) {
                        mLogger.log(mLogTag, "state change from=" + mCurrentState + " to=" + nextState);
                    }

                    State prevState = mCurrentState;
                    mCurrentState.onLeave(nextState, event, data);
                    mCurrentState = nextState;
                    nextState.onEnter(prevState, event, data);
                }
            }
        });
    }

    public boolean canMoveTo(State toState) {
        if (toState == null) {
            return false;
        }
        synchronized (this) {
            HashMap<Object, State> states = mCurrentState.mToStates;
            for (Object event : states.keySet()) {
                if (states.get(event).equals(toState)) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean canAccept(Object event) {
        synchronized (this) {
            return mCurrentState.mToStates.containsKey(event);
        }
    }

    public List<Object> listAcceptableEvent() {
        synchronized (this) {
            return new ArrayList<>(mCurrentState.mToStates.keySet());
        }
    }

    public State getCurrentState() {
        return mCurrentState;
    }

    public boolean isInState(Class<? extends State> State) {
        return State != null && State.isInstance(mCurrentState);
    }

    //////////////////////////////////////////////////////////////////////

    public static abstract class State {

        HashMap<Object, State> mToStates = new HashMap<>();
        private StateMachine mStateMachine;

        @SuppressWarnings("unused")
        private String mName = "UNKNOWN";

        public State(String name) {
            mName = name;
        }

        public State linkTo(State toState, Object event) {
            if (toState == null) {
                throw new IllegalArgumentException("toState cannot be null");
            }
            mToStates.put(event, toState);
            return this;
        }

        public void onStart() {
        }

        public void onStop(int cause) {
        }

        public void onReset(int cause) {
        }

        public void onUnhandleEvent(Object event, Object data) {
        }

        public void onEnter(State fromState, Object event, Object data) {
        }

        public void onLeave(State toState, Object event, Object data) {
        }

        protected StateMachine getStateMachine() {
            return mStateMachine;
        }

        protected void setStateMachine(StateMachine stateMachine) {
            mStateMachine = stateMachine;
        }

        @Override
        public String toString() {
            return "State{" +
                    "mName='" + mName + '\'' +
                    '}';
        }

        public boolean equalToState(Class<? extends State> otherState) {
            return this.getClass().equals(otherState);
        }
    }

    //////////////////////////////////////////////////////////////////////

    public static class StateChangeHandler {

        public StateChangeHandler() {
        }

        public void post(Runnable runnable) {
            runnable.run();
        }
    }

    public interface Logger {
        void log(String tag, String msg);
    }
}
