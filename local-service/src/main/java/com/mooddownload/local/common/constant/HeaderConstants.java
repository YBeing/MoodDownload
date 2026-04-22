package com.mooddownload.local.common.constant;

/**
 * HTTP 请求头常量。
 */
public final class HeaderConstants {

    public static final String REQUEST_ID = "X-Request-Id";

    public static final String LOCAL_TOKEN = "X-Local-Token";

    public static final String CLIENT_TYPE = "X-Client-Type";

    public static final String CLIENT_TYPE_NATIVE_HOST = "native-host";

    public static final String CLIENT_TYPE_BROWSER_EXTENSION = "browser-extension";

    public static final String CLIENT_TYPE_DESKTOP_APP = "desktop-app";

    private HeaderConstants() {
    }
}
