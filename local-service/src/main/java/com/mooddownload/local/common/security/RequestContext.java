package com.mooddownload.local.common.security;

/**
 * 请求上下文容器。
 */
public final class RequestContext {

    private static final ThreadLocal<State> HOLDER = new ThreadLocal<State>();

    private RequestContext() {
    }

    public static void set(String requestId, String clientType) {
        HOLDER.set(new State(requestId, clientType));
    }

    public static String getRequestId() {
        State state = HOLDER.get();
        return state == null ? null : state.requestId;
    }

    public static String getClientType() {
        State state = HOLDER.get();
        return state == null ? null : state.clientType;
    }

    public static void clear() {
        HOLDER.remove();
    }

    private static final class State {

        private final String requestId;

        private final String clientType;

        private State(String requestId, String clientType) {
            this.requestId = requestId;
            this.clientType = clientType;
        }
    }
}

