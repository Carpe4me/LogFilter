package com.legendmohe.tool.util; /**
 *
 */

/**
 *
 */
public interface EventBus {

    enum TYPE {
        EVENT_CLICK_BOOKMARK,
        EVENT_CLICK_ERROR,
        EVENT_CHANGE_FILTER_SHOW_PID,
        EVENT_CHANGE_FILTER_SHOW_TAG,
        EVENT_CHANGE_FILTER_REMOVE_TAG,
        EVENT_CHANGE_FILTER_FIND_WORD,
        EVENT_CHANGE_FILTER_REMOVE_WORD,
        EVENT_CHANGE_FILTER_FROM_TIME,
        EVENT_CHANGE_FILTER_TO_TIME,
        EVENT_CHANGE_SELECTION,
    }

    void postEvent(Event param);

    class Event {
        public TYPE type;
        public Object param1;
        public Object param2;
        public Object param3;

        public Event(TYPE type) {
            this(type, null, null, null);
        }

        public Event(TYPE type, Object param1) {
            this(type, param1, null, null);
        }

        public Event(TYPE type, Object param1, Object param2) {
            this(type, param1, param2, null);
        }

        public Event(TYPE type, Object param1, Object param2, Object param3) {
            this.type = type;
            this.param1 = param1;
            this.param2 = param2;
            this.param3 = param3;
        }
    }
}
