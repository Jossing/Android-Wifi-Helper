package org.jossing.wifihelper.annotation;

import android.support.annotation.IntDef;

import org.jossing.wifihelper.enumerate.WifiOperating;

/**
 * 请求扫描 Wi-Fi 的操作结果
 *
 * @author jossing
 * @date 2018/12/29
 */
@IntDef({
        WifiOperating.RESULT_SUCCESS,
        WifiOperating.ERROR_INTERNAL,
        WifiOperating.REQUIRE_LOCATION_PERMISSION,
        WifiOperating.LOCATION_SERVICE_DISABLED,
        WifiOperating.WIFI_NOT_ENABLED,
})
public @interface ScanResult { }