package org.jossing.wifihelper.annotation;

import android.support.annotation.IntDef;

import org.jossing.wifihelper.enumerate.WifiConnection;

/**
 * @author jossing
 * @date 2018/12/29
 */
@IntDef({
        WifiConnection.UNKNOWN,
        WifiConnection.SEARCHING,
        WifiConnection.CONNECTING,
        WifiConnection.AUTHENTICATING,
        WifiConnection.OBTAINING_IPADDR,
        WifiConnection.CONNECTED,
        WifiConnection.SUSPENDED,
        WifiConnection.DISCONNECTED,
})
public @interface ConnectionState {
}
