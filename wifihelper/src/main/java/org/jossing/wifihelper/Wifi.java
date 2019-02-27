package org.jossing.wifihelper;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.jossing.wifihelper.annotation.ConnectionState;
import org.jossing.wifihelper.enumerate.WifiConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 封装了与 Wi-Fi 相关参数的类
 *
 * @author jossing
 * @date 2018/12/27
 */
public class Wifi implements Comparable<Wifi> {
    private static final String TAG = "Wifi";

    public final static int UNSPECIFIED = -1;

    /**
     * @see android.net.wifi.ScanResult#SSID
     */
    public final String SSID;

    /**
     * @see android.net.wifi.ScanResult#BSSID
     */
    private String BSSID;

    /**
     * @see android.net.wifi.ScanResult#capabilities
     */
    public final String capabilities;

    /**
     * @see android.net.wifi.ScanResult#level
     */
    private int level;

    /**
     * 是否是 2.4GHz 的 Wi-Fi
     */
    private boolean freq24GHz = false;

    /**
     * 是否是 5GHz 的 Wi-Fi
     */
    private boolean freq5GHz = false;

    /**
     * 低于 API 23 时总是为 {@link #UNSPECIFIED}
     *
     * @see android.net.wifi.ScanResult#channelWidth
     */
    private final List<Integer> mChannelWidths = new ArrayList<>();

    /**
     * 此网络已保存的配置信息。<br/>
     * 如果此 Wi-Fi 网络已保存，则此属性不为 null。
     */
    public final WifiConfiguration configuration;

    /**
     * 仅当此 Wi-Fi 已连接，则此属性才可用
     */
    public final WifiInfo wifiInfo;

    /**
     * Wi-Fi 的连接状态。<br/>
     * 仅当此 Wi-Fi 已连接，则此属性才可用。
     */
    @ConnectionState
    private int mConnectionState = WifiConnection.UNKNOWN;

    /**
     * 由 {@link ScanResult} 的实例生成此类的实例
     */
    public static WifiFactory from(@NonNull final ScanResult scanResult) {
        return new WifiFactory(scanResult);
    }

    protected Wifi(@NonNull final ScanResult scanResult, @Nullable final WifiConfiguration configuration, @Nullable final WifiInfo wifiInfo) {
        SSID = scanResult.SSID;
        BSSID = scanResult.BSSID;
        capabilities = scanResult.capabilities;
        level = scanResult.level;
        freq24GHz = WifiSupport.is24GHz(scanResult.frequency);
        freq5GHz = WifiSupport.is5GHz(scanResult.frequency);
        mChannelWidths.add(WifiSupport.isOverApi23() ? scanResult.channelWidth : UNSPECIFIED);
        // 检查该配置有效性，和是否是此网络的配置
        if (WifiSupport.isConfigurationValid(configuration) &&
                TextUtils.equals(SSID, WifiSupport.getRealSSID(configuration.SSID))) {
            this.configuration = configuration;
        } else {
            this.configuration = null;
        }
        if (wifiInfo != null && TextUtils.equals(SSID, WifiSupport.getRealSSID(wifiInfo.getSSID()))) {
            this.wifiInfo = wifiInfo;
            mConnectionState = WifiConnection.from(wifiInfo);
        } else {
            this.wifiInfo = null;
        }
    }

    /**
     * @see ScanResult#BSSID
     */
    public String getBSSID() {
        return BSSID;
    }

    /**
     * @see ScanResult#level
     */
    public int getLevel() {
        return level;
    }

    /**
     * 如果 {@link #isCurrent()} == false，那么此方法没有意义。
     * @see WifiInfo#getIpAddress()
     */
    public int getIpAddress() {
        return wifiInfo == null ? 0 : wifiInfo.getIpAddress();
    }

    /**
     * @see WifiSupport#isNeedPassword(String)
     */
    public final boolean isNeedPassword() {
        return WifiSupport.isNeedPassword(capabilities);
    }

    /**
     * @see WifiSupport#calculateSignalLevel(int, int)
     */
    public final int getSignalLevel(final int numLevels) {
        return WifiSupport.calculateSignalLevel(level, numLevels);
    }

    /**
     * @see WifiSupport#is24GHz(int)
     */
    public boolean is24GHz() {
        return freq24GHz;
    }

    /**
     * @see WifiSupport#is5GHz(int)
     */
    public boolean is5GHz() {
        return freq5GHz;
    }

    /**
     * 返回此 Wi-Fi 支持的带宽的数量
     */
    public int countChannelWidths() {
        return mChannelWidths.size();
    }

    /**
     * 获取指定索引处的信道带宽
     */
    public int getChannelBandWidth(final int index) {
        if (index < 0 || index >= mChannelWidths.size()) {
            return UNSPECIFIED;
        }
        return mChannelWidths.get(index);
    }

    /**
     * @see WifiSupport#getChannelBandWidthDescription(int)
     */
    @NonNull
    public String getChannelBandWidthDescription() {
        final StringBuilder sb = new StringBuilder();
        for (final int channelWidth : mChannelWidths) {
            sb.append("[").append(WifiSupport.getChannelBandWidthDescription(channelWidth)).append("]");
        }
        return sb.toString();
    }

    /**
     * 是否是已保存的网络
     * @return true 网络已保存
     */
    public boolean isSaved() {
        return configuration != null;
    }

    /**
     * 已保存的配置是否已过期。例如原来的密码已经失效了。<br/>
     * 如果 {@link #isSaved()} == false，则此方法会返回 false。
     * @return true 配置已过期
     */
    public boolean isConfigDisabled() {
        return isSaved() && configuration.status == WifiConfiguration.Status.DISABLED;
    }

    /**
     * 是否是当前已连接的 Wi-Fi
     */
    public boolean isCurrent() {
        return wifiInfo != null && mConnectionState != WifiConnection.DISCONNECTED &&
                mConnectionState != WifiConnection.UNKNOWN;
    }

    @ConnectionState
    public int getConnectionState() {
        return mConnectionState;
    }

    /**
     * @return true 如果状态已改变
     */
    boolean setConnectionState(@ConnectionState final int connectionState) {
        if (mConnectionState != connectionState) {
            mConnectionState = connectionState;
            return true;
        }
        return false;
    }

    /**
     * 与另一个 {@link ScanResult} 对象合并
     *
     * @return true 合并成功
     */
    boolean merge(@NonNull final ScanResult target) {
        if (!TextUtils.equals(SSID, target.SSID) || !TextUtils.equals(capabilities, target.capabilities)) {
            return false;
        }
        // 保留信号好的那个
        if (level < target.level) {
            BSSID = target.BSSID;
            level = target.level;
        }
        freq24GHz = is24GHz() || WifiSupport.is24GHz(target.frequency);
        freq5GHz = is5GHz() || WifiSupport.is5GHz(target.frequency);
        final int targetChannelWidth = WifiSupport.isOverApi23() ? target.channelWidth : UNSPECIFIED;
        if (targetChannelWidth >= 0 && !mChannelWidths.contains(targetChannelWidth)) {
            mChannelWidths.add(targetChannelWidth);
        }
        Collections.sort(mChannelWidths, Integer::compareTo);
        return true;
    }

    /**
     * 排序优先级为：<br/>
     * <ol>
     *     <li>已连接</li>
     *     <li>已保存</li>
     *     <li>信号好</li>
     * </ol>
     */
    @Override
    public int compareTo(Wifi another) {
        if (another == null) {
            return -1;
        }
        // 已连接的网络要排在最前面，然后是已保存的
        if (isSaved()) {
            if (another.isSaved()) {
                final boolean isCurrent = isCurrent();
                final boolean isAnotherCurrent = another.isCurrent();
                if (isCurrent && !isAnotherCurrent) {
                    return -1;
                }
                if (!isCurrent && isAnotherCurrent) {
                    return 1;
                }
            } else {
                return -1;
            }
        } else if (another.isSaved()) {
            return 1;
        }
        // 最后再根据信号强度排序
        return Integer.compare(another.level, level);
    }

    @NonNull
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final String none = "<none>";
        sb.append("SSID: ").append(SSID == null ? none : SSID);
        sb.append(", BSSID: ").append(BSSID == null ? none : BSSID);
        sb.append(", capabilities: ").append(capabilities == null ? none : capabilities);
        sb.append(", level: ").append(level).append("dBm");
        sb.append(", frequency:").append(is24GHz() ? " 2.4Ghz" : "").append(is5GHz() ? " 5Ghz" : "");
        sb.append(", ChannelBandwidth: ").append(getChannelBandWidthDescription());
        return sb.toString();
    }


    static class WifiFactory {
        private final ScanResult mScanResult;
        private WifiConfiguration mConfiguration = null;
        private WifiInfo mWifiInfo = null;

        WifiFactory(@NonNull final ScanResult scanResult) {
            mScanResult = scanResult;
        }

        WifiFactory configuration(@Nullable final WifiConfiguration configuration) {
            mConfiguration = configuration;
            return this;
        }

        WifiFactory wifiInfo(@Nullable final WifiInfo wifiInfo) {
            mWifiInfo = wifiInfo;
            return this;
        }

        Wifi create() {
            return new Wifi(mScanResult, mConfiguration, mWifiInfo);
        }
    }
}
