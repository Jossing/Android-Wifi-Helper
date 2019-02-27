package org.jossing.wifihelper.annotation;

import android.support.annotation.IntDef;

import org.jossing.wifihelper.enumerate.WifiOperating;

/**
 * @author jossing
 * @date 2018/12/29
 */
@IntDef({
        WifiOperating.RESULT_SUCCESS,
        WifiOperating.REQUIRE_LOCATION_PERMISSION,
        WifiOperating.LOCATION_SERVICE_DISABLED,
})
public @interface WifiListState {
}
