package org.jossing.wifihelper.enumerate;

/**
 * 枚举 Wi-Fi 相关操作结果的类
 *
 * @author jossing
 * @date 2018/12/29
 */
public final class WifiOperating {

    private WifiOperating() {}

    /**
     * 操作成功
     */
    public final static int RESULT_SUCCESS = 0;
    /**
     * 系统 API 内部返回的操作失败
     */
    public final static int ERROR_INTERNAL = 1;
    /**
     * 由于缺少定位权限引发的错误
     */
    public final static int REQUIRE_LOCATION_PERMISSION = 2;
    /**
     * 由于禁用了位置信息服务引发的错误
     */
    public final static int LOCATION_SERVICE_DISABLED = 3;
    /**
     * 由于 Wi-Fi 没有开启引发的错误
     */
    public final static int WIFI_NOT_ENABLED = 4;
}
