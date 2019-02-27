package org.jossing.wifihelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jossing.wifihelper.annotation.WifiState;

import static org.jossing.wifihelper.WifiSupport.isOverApi23;

/**
 * Wi-Fi 相关状态的广播接收器
 *
 * @author jossing
 * @date 2018/12/28
 */
final class WifiReceiver extends BroadcastReceiver {
    private static final String TAG = "WifiReceiver";

    private Callback mCallback;

    public WifiReceiver(final Callback callback) {
        mCallback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        switch (intent.getAction()) {
            case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                final boolean isUpdated;
                if (isOverApi23()) {
                    isUpdated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                } else {
                    isUpdated = true;
                }
                onScanResultsAvailable(isUpdated);
                break;
            case WifiManager.WIFI_STATE_CHANGED_ACTION:
                final int curState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                final int previousState = intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                onWifiStateChanged(curState, previousState);
                break;
            case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                final NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if ("WIFI".equalsIgnoreCase(networkInfo.getTypeName())) {
                    onWifiConnectionStateChanged(networkInfo);
                }
                break;
            default:
        }
    }

    private void onScanResultsAvailable(final boolean isUpdated) {
        final boolean intercept = mCallback != null && mCallback.onScanResultsAvailable(isUpdated);
        if (!intercept) {
            Log.w(TAG, "onScanResultsAvailable(" + isUpdated + ")");
        }
    }

    private void onWifiStateChanged(final int curState, final int previousState) {
        final boolean intercept = mCallback != null && mCallback.onWifiStateChanged(curState, previousState);
        if (!intercept) {
            Log.w(TAG, "onWifiStateChanged(" + getWifiStateDes(curState) + ", " + getWifiStateDes(previousState) + ")");
        }
    }

    private String getWifiStateDes(@WifiState final int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_DISABLING:
                return "正在关闭…";
            case WifiManager.WIFI_STATE_DISABLED:
                return "已关闭";
            case WifiManager.WIFI_STATE_ENABLING:
                return "正在打开…";
            case WifiManager.WIFI_STATE_ENABLED:
                return "已打开";
            case WifiManager.WIFI_STATE_UNKNOWN:
            default:
                return "未知";
        }
    }

    private void onWifiConnectionStateChanged(@NonNull final NetworkInfo networkInfo) {
        final boolean intercept = mCallback != null && mCallback.onWifiConnectionStateChanged(networkInfo);
        if (!intercept) {
            Log.w(TAG, "onWifiConnectionStateChanged(" + networkInfo.getState().name() + ", " + networkInfo.getDetailedState().name() + ")");
        }
    }

    public interface Callback {

        /**
         * 当新一轮扫描完成时调用
         *
         * @param isUpdated true 扫描成功，结果是新的；false 扫描失败，结果是上一次扫描成功的。
         * @return true 事件被消耗了
         */
        boolean onScanResultsAvailable(final boolean isUpdated);

        /**
         * 当 Wi-Fi 状态变化时调用
         *
         * @param curState 当前（新的）状态
         * @param previousState 上一个状态
         * @return true 事件被消耗了
         */
        boolean onWifiStateChanged(@WifiState final int curState, @WifiState final int previousState);

        /**
         * 当 Wi-Fi 连接状态发生变化时调用
         *
         * @param networkInfo 新的网络信息
         * @return true 事件被消耗了
         */
        boolean onWifiConnectionStateChanged(@NonNull final NetworkInfo networkInfo);
    }

    /**
     * 返回已配置好此接收器需要的几个 Action 的 {@link IntentFilter}
     */
    public static IntentFilter addFilterActions(@NonNull final IntentFilter intentFilter) {
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        return intentFilter;
    }
}
