package org.jossing.wifihelper.annotation;

import android.net.wifi.WifiManager;
import android.support.annotation.IntDef;

/**
 * 限定 Wi-Fi 的状态
 *
 * @author jossing
 * @date 2018/12/29
 */
@IntDef({
        WifiManager.WIFI_STATE_DISABLING,
        WifiManager.WIFI_STATE_DISABLED,
        WifiManager.WIFI_STATE_ENABLING,
        WifiManager.WIFI_STATE_ENABLED,
        WifiManager.WIFI_STATE_UNKNOWN
})
public @interface WifiState { }