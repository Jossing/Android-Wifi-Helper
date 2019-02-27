package org.jossing.wifihelper;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.jossing.wifihelper.annotation.WifiListState;
import org.jossing.wifihelper.annotation.ScanResult;
import org.jossing.wifihelper.annotation.WifiState;
import org.jossing.wifihelper.enumerate.WifiConnection;
import org.jossing.wifihelper.enumerate.WifiOperating;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Wi-Fi 相关功能的帮助类
 *
 * @author jossing
 * @date 2018/12/28
 */
public final class WifiHelper {
    private static final String TAG = "WifiHelper";

    private Activity mActivity;
    private WifiManager mWifiManager;
    private WifiReceiver mWifiReceiver;

    @Nullable
    private WifiListCallback mWifiListCallback;
    @Nullable
    private WifiStateCallback mWifiStateCallback;

    /** Wi-Fi 列表的访问锁 */
    private final Object mLockWifiList = new Object();
    /** 扫描得到的 Wi-Fi 集合 */
    @NonNull
    private List<Wifi> mWifiList = new ArrayList<>();

    /** 当前 Wi-Fi 状态和上一个 Wi-Fi 状态的访问锁 */
    private final Object mLockWifiState = new Object();
    /** Wi-Fi 的上一个状态 */
    @WifiState
    private int mWifiPreState;

    /** 开 Wi-Fi 回调的访问锁 */
    private final Object mLockWifiSwitchOnCallback = new Object();
    /** 开 Wi-Fi 回调 */
    private WifiSwitchCallback mWifiSwitchOnCallback;

    /** 关 Wi-Fi 回调的访问锁 */
    private final Object mLockWifiSwitchOffCallback = new Object();
    /** 关 Wi-Fi 回调 */
    private WifiSwitchCallback mWifiSwitchOffCallback;

    /** 当前 Wi-Fi 网络连接信息的访问锁 */
    private final Object mLockCurWifiNetworkInfo = new Object();
    /** 当前 Wi-Fi 网络的连接信息 */
    private NetworkInfo mCurWifiNetworkInfo = null;

    /** 连接指定 Wi-Fi 回调的访问锁 */
    private final Object mLockWifiConnectCallback = new Object();
    /** 连接指定 Wi-Fi 的回调 */
    private WifiConnectCallback mWifiConnectCallback;


    public WifiHelper(@NonNull final Activity activity) {
        mActivity = Objects.requireNonNull(activity);
        mWifiManager = WifiSupport.getWifiManager(activity);
        mWifiPreState = WifiManager.WIFI_STATE_UNKNOWN;
        register();
        // 主动拿一下 Wi-Fi 列表
        GetWifiListAsyncTask.execute(activity, null, wifiList -> {
            synchronized (mLockWifiList) {
                mWifiList = wifiList;
                // 刚初始化出来的 wifiList 肯定是 empty，
                // 这时候主动从系统获取的 wifiList 如果还是 empty，足以说明 wifiList 没变了。。
                if (!wifiList.isEmpty()) {
                    invokeWifiListCallback(WifiOperating.RESULT_SUCCESS);
                }
            }
        });
    }

    public void setWifiListCallback(@Nullable final WifiListCallback wifiListCallback) {
        mWifiListCallback = wifiListCallback;
        final boolean shouldInvokeNow;
        synchronized (mLockWifiList) {
            shouldInvokeNow = !mWifiList.isEmpty();
        }
        if (shouldInvokeNow && wifiListCallback != null) {
            onScanResultsAvailable(true);
        }
    }

    private void invokeWifiListCallback(@WifiListState final int state) {
        final WifiListCallback wifiListCallback = mWifiListCallback;
        if (wifiListCallback != null) {
            wifiListCallback.onWifiListChanged(state, mWifiList);
        }
    }

    public void setWifiStateCallback(@Nullable final WifiStateCallback wifiStateCallback) {
        mWifiStateCallback = wifiStateCallback;
        if (wifiStateCallback != null) {
            wifiStateCallback.onWifiStateChanged(getWifiCurState());
        }
    }

    private void invokeWifiStateCallback(@WifiState final int state) {
        final WifiStateCallback wifiStateCallback = mWifiStateCallback;
        if (wifiStateCallback != null) {
            wifiStateCallback.onWifiStateChanged(state);
        }
    }

    /**
     * 注册 Wi-Fi 相关信息的监听器。<br/>
     * 仅在未注册时，此方法才会生效。
     * @see #unregister()
     */
    private void register() {
        if (mWifiReceiver == null) {
            final IntentFilter intentFilter = WifiReceiver.addFilterActions(new IntentFilter());
            mActivity.registerReceiver(mWifiReceiver = new WifiReceiver(mWifiReceiverCallback), intentFilter);
        }
    }

    /**
     * 反注册 Wi-Fi 相关信息的监听器。<br/>
     * 仅在已注册时，此方法才会生效。
     * @see #register()
     */
    private void unregister() {
        if (mWifiReceiver != null) {
            mActivity.unregisterReceiver(mWifiReceiver);
            mWifiReceiver = null;
        }
    }

    /**
     * 主动请求请求扫描 Wi-Fi
     */
    @ScanResult
    public int scanWifi() {
        // 检查定位服务是否已开启
        if (!WifiSupport.isLocationServiceEnabled(mActivity)) {
            return WifiOperating.LOCATION_SERVICE_DISABLED;
        }
        // 检查是否已授权定位权限
        if (!WifiSupport.isLocationPermissionGranted(mActivity)) {
            return WifiOperating.REQUIRE_LOCATION_PERMISSION;
        }
        if (isScanAlwaysAvailable() || mWifiManager.isWifiEnabled()) {
            return mWifiManager.startScan() ? WifiOperating.RESULT_SUCCESS : WifiOperating.ERROR_INTERNAL;
        }
        return WifiOperating.WIFI_NOT_ENABLED;
    }

    /**
     * 切换 Wi-Fi 开关。<br/>
     * 注意：只有最后一次调用此方法"开"或者"关"传入的 callback 能够收到回调。
     *
     * @param switchOn true 打开，false 关闭
     * @param callback 切换完成时会回调
     */
    public void switchWifi(final boolean switchOn, @NonNull final WifiSwitchCallback callback) {
        // 使用 Wi-Fi 状态访问锁，同步以下对 Wi-Fi 状态判断的代码块
        synchronized (mLockWifiState) {
            final int wifiCurState = mWifiManager.getWifiState();
            if (wifiCurState == WifiManager.WIFI_STATE_UNKNOWN) {
                // Wi-Fi 状态未知，无法操作
                callback.onSwitchDone(false);
                return;
            }
            if (switchOn) {
                // 用户要求打开
                if (wifiCurState == WifiManager.WIFI_STATE_ENABLED) {
                    // 已经打开
                    setWifiSwitchOnCallback(null);
                    callback.onSwitchDone(true);
                } else if (wifiCurState == WifiManager.WIFI_STATE_ENABLING) {
                    // 正在打开
                    setWifiSwitchOnCallback(callback);
                } else if (wifiCurState == WifiManager.WIFI_STATE_DISABLING) {
                    // 正在关闭，等关闭后再打开
                    setWifiSwitchOffCallback(off -> {
                        // 关闭成功才能执行打开操作
                        final boolean success = off && mWifiManager.setWifiEnabled(true);
                        callback.onSwitchDone(success);
                    });
                } else {
                    // 当前已关闭，直接打开
                    setWifiSwitchOnCallback(callback);
                    final boolean success = mWifiManager.setWifiEnabled(true);
                    if (!success) {
                        setWifiSwitchOnCallback(null);
                        callback.onSwitchDone(false);
                    }
                }
            } else {
                // 用户要求关闭
                if (wifiCurState == WifiManager.WIFI_STATE_DISABLED) {
                    // 已经关闭
                    setWifiSwitchOffCallback(null);
                    callback.onSwitchDone(true);
                } else if (wifiCurState == WifiManager.WIFI_STATE_DISABLING) {
                    // 正在关闭
                    setWifiSwitchOffCallback(callback);
                } else if (wifiCurState == WifiManager.WIFI_STATE_ENABLING) {
                    // 正在打开，等打开后再关闭
                    setWifiSwitchOnCallback(on -> {
                        // 打开成功才能执行关闭操作
                        final boolean success = on && mWifiManager.setWifiEnabled(false);
                        callback.onSwitchDone(success);
                    });
                } else {
                    // 当前已打开，直接关闭
                    setWifiSwitchOffCallback(callback);
                    final boolean success = mWifiManager.setWifiEnabled(false);
                    if (!success) {
                        setWifiSwitchOffCallback(null);
                        callback.onSwitchDone(false);
                    }
                }
            }
        }
    }

    /**
     * 不论如何，调用此方法后，{@link #mWifiSwitchOnCallback} 总是会被置为 null。
     */
    @Nullable
    private WifiSwitchCallback getWifiSwitchOnCallback() {
        synchronized (mLockWifiSwitchOnCallback) {
            final WifiSwitchCallback wifiSwitchOnCallback = mWifiSwitchOnCallback;
            mWifiSwitchOnCallback = null;
            return wifiSwitchOnCallback;
        }
    }

    /**
     * 以线程同步的方式给 {@link #mWifiSwitchOnCallback} 赋值。<br/>
     * 同步锁为 {@link #mLockWifiSwitchOnCallback} 对象。
     */
    private void setWifiSwitchOnCallback(final WifiSwitchCallback wifiSwitchOnCallback) {
        synchronized (mLockWifiSwitchOnCallback) {
            mWifiSwitchOnCallback = wifiSwitchOnCallback;
        }
    }

    /**
     * 不论如何，调用此方法后，{@link #mWifiSwitchOffCallback} 总是会被置为 null。
     */
    @Nullable
    private WifiSwitchCallback getWifiSwitchOffCallback() {
        synchronized (mLockWifiSwitchOffCallback) {
            final WifiSwitchCallback wifiSwitchOffCallback = mWifiSwitchOffCallback;
            mWifiSwitchOffCallback = null;
            return wifiSwitchOffCallback;
        }
    }

    /**
     * 以线程同步的方式给 {@link #mWifiSwitchOffCallback} 赋值。<br/>
     * 同步锁为 {@link #mLockWifiSwitchOffCallback} 对象。
     */
    private void setWifiSwitchOffCallback(final WifiSwitchCallback wifiSwitchOffCallback) {
        synchronized (mLockWifiSwitchOffCallback) {
            mWifiSwitchOffCallback = wifiSwitchOffCallback;
        }
    }

    /**
     * 获取上一次扫描得到的 Wi-Fi 列表
     */
    @NonNull
    public List<Wifi> getWifiList() {
        synchronized (mLockWifiList) {
            return mWifiList;
        }
    }

    @WifiState
    public int getWifiCurState() {
        return mWifiManager.getWifiState();
    }

    @WifiState
    public int getWifiPreState() {
        synchronized (mLockWifiState) {
            return mWifiPreState;
        }
    }

    private void setWifiPreState(int wifiPreState) {
        synchronized (mLockWifiState) {
            mWifiPreState = wifiPreState;
        }
    }

    /**
     * @see WifiSupport#isScanAlwaysAvailable(Context)
     */
    public boolean isScanAlwaysAvailable() {
        if (!WifiSupport.isOverApi18()) {
            return false;
        }
        return mWifiManager.isScanAlwaysAvailable();
    }

    /**
     * @see WifiManager#getConnectionInfo()
     */
    public WifiInfo getConnectionInfo() {
        return mWifiManager.getConnectionInfo();
    }

    /**
     * @see WifiManager#getDhcpInfo()
     */
    public DhcpInfo getDhcpInfo() {
        return mWifiManager.getDhcpInfo();
    }

    /**
     * 连接 Wi-Fi
     * @param password Wi-Fi 密码（如果需要）
     */
    public void connectWifi(@NonNull final Wifi wifi, @Nullable final String password, final WifiConnectCallback.Callback callback) {
        synchronized (mLockWifiConnectCallback) {
            if (wifi.isCurrent()) {
                callback.onConnected(true);
                return;
            }
            final WifiConfiguration wifiConfig;
            final int networkId;
            if (wifi.isSaved()) {
                wifiConfig = wifi.configuration;
                if (wifi.isConfigDisabled()) {
                    WifiSupport.wifiPwdConfig(wifiConfig, wifi, password);
                    networkId = mWifiManager.updateNetwork(wifiConfig);
                } else {
                    networkId = wifiConfig.networkId;
                }
            } else {
                wifiConfig = new WifiConfiguration();
                wifiConfig.SSID = "\"" + wifi.SSID + "\"";
                WifiSupport.wifiPwdConfig(wifiConfig, wifi, password);
                networkId = mWifiManager.addNetwork(wifiConfig);
            }
            mWifiConnectCallback = WifiConnectCallback.with(wifi.SSID, callback);
            final boolean success = mWifiManager.enableNetwork(networkId, true);;
            if (!success) {
                mWifiConnectCallback = null;
                onScanResultsAvailable(true);
                callback.onConnected(false);
            }
        }
    }

    /**
     * 删除一个 Wi-Fi 的网络配置。只能删除本 app 创建的配置。
     * @return true 删除成功，或该 Wi-Fi 本就没有配置。
     */
    public boolean removeWifiConfig(@NonNull final Wifi wifi) {
        final boolean success;
        if (wifi.isSaved()) {
            final int networkId = wifi.configuration.networkId;
            success = mWifiManager.removeNetwork(networkId);
        } else {
            success = false;
        }
        if (success) {
            onScanResultsAvailable(true);
        }
        return success;
    }

    /**
     * @see WifiReceiver.Callback#onScanResultsAvailable(boolean)
     */
    private boolean onScanResultsAvailable(final boolean isUpdated) {
        // Wi-Fi 列表已更新，或是 Wi-Fi 列表还未拿到，都要重新给 mWifiList 赋值
        if (isUpdated || mWifiList.isEmpty()) {
            synchronized (mLockCurWifiNetworkInfo) {
                final NetworkInfo networkInfo = mCurWifiNetworkInfo;
                GetWifiListAsyncTask.execute(mActivity, networkInfo, wifiList -> {
                    synchronized (mLockWifiList) {
                        // wifiList 没变，就不用回调了
                        if (mWifiList.isEmpty() && wifiList.isEmpty()) {
                            return;
                        }
                        mWifiList = wifiList;
                        final int state;
                        if (!WifiSupport.isLocationServiceEnabled(mActivity)) {
                            state = WifiOperating.LOCATION_SERVICE_DISABLED;
                        } else if (!WifiSupport.isLocationPermissionGranted(mActivity)) {
                            state = WifiOperating.REQUIRE_LOCATION_PERMISSION;
                        } else {
                            state = WifiOperating.RESULT_SUCCESS;
                        }
                        // 回调 Wi-Fi 列表
                        invokeWifiListCallback(state);
                    }
                });
            }
        }
        return true;
    }

    /**
     * @see WifiReceiver.Callback#onWifiStateChanged(int, int)
     */
    private boolean onWifiStateChanged(@WifiState final int curState, @WifiState final int previousState) {
        setWifiPreState(previousState);
        // 回调 Wi-Fi 状态
        invokeWifiStateCallback(curState);
        if (curState == WifiManager.WIFI_STATE_DISABLED || curState == WifiManager.WIFI_STATE_UNKNOWN) {
            // Wi-Fi 关闭后，如果 Wi-Fi 不允许关闭时扫描，则清空 Wi-Fi 列表
            if (!isScanAlwaysAvailable()) {
                synchronized (mLockWifiList) {
                    mWifiList.clear();
                    invokeWifiListCallback(WifiOperating.RESULT_SUCCESS);
                }
            }
            final WifiSwitchCallback wifiSwitchOffCallback = getWifiSwitchOffCallback();
            if (wifiSwitchOffCallback != null) {
                final boolean success = curState != WifiManager.WIFI_STATE_UNKNOWN;
                wifiSwitchOffCallback.onSwitchDone(success);
            }
        }
        if (curState == WifiManager.WIFI_STATE_ENABLED || curState == WifiManager.WIFI_STATE_UNKNOWN) {
            final WifiSwitchCallback wifiSwitchOnCallback = getWifiSwitchOnCallback();
            if (wifiSwitchOnCallback != null) {
                final boolean success = curState != WifiManager.WIFI_STATE_UNKNOWN;
                wifiSwitchOnCallback.onSwitchDone(success);
            }
        }
        return true;
    }

    /**
     * @see WifiReceiver.Callback#onWifiConnectionStateChanged(NetworkInfo)
     */
    private boolean onWifiConnectionStateChanged(@NonNull final NetworkInfo networkInfo) {
        synchronized (mLockCurWifiNetworkInfo) {
            mCurWifiNetworkInfo = networkInfo;
        }
        onScanResultsAvailable(true);

        synchronized (mLockWifiConnectCallback) {
            final WifiConnectCallback connectCallback = mWifiConnectCallback;
            boolean called = false;
            if (connectCallback != null) {
                final String connectingSSID;
                // 在 Android 9.0 以上，此处拿到的 NetworkInfo.getExtraInfo 可能为空，
                // 为保险起见，增加一个从 WifiInfo 中获取 SSID 的方法。
                if (!TextUtils.isEmpty(networkInfo.getExtraInfo())) {
                    connectingSSID = WifiSupport.getRealSSID(networkInfo.getExtraInfo());
                } else {
                    connectingSSID = WifiSupport.getRealSSID(getConnectionInfo().getSSID());
                }
                if (TextUtils.equals(connectCallback.mTargetSSID, connectingSSID)) {
                    final int connectionState = WifiConnection.from(networkInfo);
                    if (WifiConnection.CONNECTED == connectionState) {
                        // 连接成功
                        called = true;
                        connectCallback.invoke(true);
                    }
                    if (WifiConnection.DISCONNECTED == connectionState) {
                        // 连接失败
                        called = true;
                        connectCallback.invoke(false);
                    }
                }
            }
            if (called) {
                mWifiConnectCallback = null;
            }
        }
        return true;
    }

    /**
     * 销毁此对象，销毁后不能继续使用。<br/>
     * 内部会同步调用 {@link #unregister()}
     */
    public void destroy() {
        unregister();
        mActivity = null;
    }

    private final WifiReceiver.Callback mWifiReceiverCallback = new WifiReceiver.Callback() {
        @Override
        public boolean onScanResultsAvailable(boolean isUpdated) {
            return WifiHelper.this.onScanResultsAvailable(isUpdated);
        }

        @Override
        public boolean onWifiStateChanged(int curState, int previousState) {
            return WifiHelper.this.onWifiStateChanged(curState, previousState);
        }

        @Override
        public boolean onWifiConnectionStateChanged(@NonNull NetworkInfo networkInfo) {
            return WifiHelper.this.onWifiConnectionStateChanged(networkInfo);
        }
    };

    /**
     * Wi-Fi 开关回调
     */
    public interface WifiSwitchCallback {

        /**
         * 切换 Wi-Fi 开关完成
         * @param success true 操作成功
         */
        void onSwitchDone(final boolean success);
    }

    /**
     * Wi-Fi 列表回调
     */
    public interface WifiListCallback {

        /**
         * 当扫描得到新的 Wi-Fi 列表时调用
         *
         * @param state 获取 Wi-Fi 列表时发生的状态
         * @param wifiList /
         */
        void onWifiListChanged(@WifiListState final int state, @NonNull final List<Wifi> wifiList);
    }

    /**
     * Wi-Fi 状态回调
     */
    public interface WifiStateCallback {

        /**
         * 当 Wi-Fi 的状态变化时调用
         *
         * @param state Wi-Fi 状态
         */
        void onWifiStateChanged(@WifiState final int state);
    }

    /**
     * Wi-Fi 连接回调
     */
    public static class WifiConnectCallback {

        private final String mTargetSSID;
        private Callback mCallback;

        private WifiConnectCallback(@NonNull final String SSID, @NonNull final Callback callback) {
            mTargetSSID = SSID;
            mCallback = callback;
        }

        private boolean compareSSID(final String SSID) {
            return TextUtils.equals(mTargetSSID, SSID);
        }

        private void invoke(final boolean connected) {
            mCallback.onConnected(connected);
        }

        public static WifiConnectCallback with(@NonNull final String SSID, @NonNull final Callback callback) {
            return new WifiConnectCallback(SSID, callback);
        }

        public interface Callback {
            /**
             * 连接完成时调用
             * @param success true 连接指定 Wi-Fi 成功
             */
            void onConnected(final boolean success);
        }
    }
}
