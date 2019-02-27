package org.jossing.wifihelper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 封装了与 Wi-Fi 相关的一些换算的支持类
 *
 * @author jossing
 * @date 2018/12/27
 */
public final class WifiSupport {
    private static final String TAG = "WifiSupport";

    /**
     * Wi-Fi 操作所必须的危险权限
     */
    public static final String[] LOCATION_PERMISSIONS = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION };

    /**
     * @see ScanResult#CHANNEL_WIDTH_20MHZ
     */
    public static final String DES_CHANNEL_WIDTH_20MHZ = "20MHz";

    /**
     * @see ScanResult#CHANNEL_WIDTH_40MHZ
     */
    public static final String DES_CHANNEL_WIDTH_40MHZ = "40MHz";

    /**
     * @see ScanResult#CHANNEL_WIDTH_80MHZ
     */
    public static final String DES_CHANNEL_WIDTH_80MHZ = "80MHz";

    /**
     * @see ScanResult#CHANNEL_WIDTH_160MHZ
     */
    public static final String DES_CHANNEL_WIDTH_160MHZ = "160MHz";

    /**
     * @see ScanResult#CHANNEL_WIDTH_80MHZ_PLUS_MHZ
     */
    public static final String DES_CHANNEL_WIDTH_80MHZ_PLUS_MHZ = "Double 80MHz";


    private WifiSupport() {}

    static boolean isOverApi18() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    static boolean isOverApi19() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    static boolean isOverApi21() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    static boolean isOverApi23() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    static boolean isOverApi28() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    /**
     * 获取 {@link WifiManager} 的实例
     */
    @NonNull
    public static WifiManager getWifiManager(@NonNull final Context context) {
        return (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * 获取 {@link ConnectivityManager} 的实例
     */
    @NonNull
    public static ConnectivityManager getConnectivityManager(@NonNull final Context context) {
        return (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * 检查位置信息服务是否已启用
     *
     * @return true 位置信息服务已启用
     */
    public static boolean isLocationServiceEnabled(@NonNull final Context context) {
        final Context appContext = context.getApplicationContext();
        boolean enabled;
        if (isOverApi28()) {
            // Android 9.0 以上，通过 LocationManager.isLocationEnabled() 判断就可以了
            final LocationManager locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
            enabled = locationManager.isLocationEnabled();
        } else if (isOverApi19()) {
            // Android 4.4 到 Android 9.0 之间
            // 通过判断 Settings.Secure.LOCATION_MODE 是否为 Settings.Secure.LOCATION_MODE_OFF 来确定位置信息服务的开关状态
            try {
                enabled = Settings.Secure.getInt(appContext.getContentResolver(), Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_OFF;
            } catch (Throwable tr) {
                enabled = false;
            }
        } else {
            // Android 4.4 以下版本采用判断 允许的位置提供者列表是否为空 的方式来确定位置信息服务的开关状态
            enabled = !TextUtils.isEmpty(Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED));
        }
        return enabled;
    }

    /**
     * 启动位置信息服务设置界面
     */
    public static void startLocationServiceSetting(@NonNull final Activity activity, final int requestCode) {
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 是否已授权定位权限
     */
    public static boolean isLocationPermissionGranted(@NonNull final Context context) {
        boolean isGranted = true;
        for (final String locationPermission : LOCATION_PERMISSIONS) {
            final int checkResult = ContextCompat.checkSelfPermission(context, locationPermission);
            if (checkResult != PackageManager.PERMISSION_GRANTED) {
                isGranted = false;
                break;
            }
        }
        return isGranted;
    }

    /**
     * 是否总是允许扫描 Wi-Fi，例如在 Wi-Fi 未开启时
     */
    public static boolean isScanAlwaysAvailable(@NonNull final Context context) {
        if (!isOverApi18()) {
            return false;
        }
        return getWifiManager(context).isScanAlwaysAvailable();
    }

    /**
     * 获取 Wi-Fi 列表
     */
    @NonNull
    static List<Wifi> getWifiList(@NonNull final Context context) {
        // 首先检查有权限没有
        if (!isLocationServiceEnabled(context) || !isLocationPermissionGranted(context)) {
            return new ArrayList<>();
        }
        final WifiManager wifiManager = getWifiManager(context);
        final List<ScanResult> scanResults = wifiManager.getScanResults();
        final Map<String, WifiConfiguration> wifiConfigurationMap = getConfigurationNetworks(wifiManager);
        final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        final List<Wifi> wifiList = new ArrayList<>();
        if (scanResults == null) {
            return wifiList;
        }
        // 同名 Wi-Fi 进行合并，同时把没有名字的 Wi-Fi 去掉
        final Map<String, Wifi> wifiMap = new ArrayMap<>();
        for (final ScanResult scanResult : scanResults) {
            if (TextUtils.isEmpty(scanResult.SSID)) {
                continue;
            }
            final Wifi wifiAdded = wifiMap.get(scanResult.SSID);
            if (wifiAdded == null) {
                wifiMap.put(scanResult.SSID, Wifi.from(scanResult)
                        .configuration(wifiConfigurationMap.get(scanResult.SSID))
                        .wifiInfo(wifiInfo)
                        .create());
                continue;
            }
            final boolean success = wifiAdded.merge(scanResult);
            if (!success) {
                Log.w(TAG, "getWifiList -> 忽略：" + scanResult);
            }
        }
        wifiList.addAll(wifiMap.values());
        Collections.sort(wifiList);
        return wifiList;
    }

    /**
     * 使用 SSID 为 key，将已配置的的网络列表包装在 Map 中返回
     *
     * @see WifiManager#getConfiguredNetworks()
     */
    @NonNull
    private static Map<String, WifiConfiguration> getConfigurationNetworks(@NonNull final WifiManager wifiManager) {
        final Map<String, WifiConfiguration> wifiConfigurationMap = new ArrayMap<>();
        final List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if (configuredNetworks == null) {
            return wifiConfigurationMap;
        }
        for (final WifiConfiguration configuration : configuredNetworks) {
            wifiConfigurationMap.put(getRealSSID(configuration.SSID), configuration);
        }
        return wifiConfigurationMap;
    }

    /**
     * 将用 int 存储的 ip 地址格式化为字符串
     */
    public static String getIpAddressString(final int ipAddress) {
        try {
            return String.valueOf(ipAddress & 0xFF) + "." +
                    (ipAddress >> 8 & 0xFF) + "." +
                    (ipAddress >> 16 & 0xFF) + "." +
                    (ipAddress >> 24 & 0xFF);
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }

    /**
     * 鉴于 {@link WifiConfiguration} 和 {@link NetworkInfo} 对 SSID 的存储特征，可以使用此方法提取不带引号的 SSID
     */
    @NonNull
    public static String getRealSSID(@NonNull final String SSID) {
        if (TextUtils.isEmpty(SSID)) {
            return "";
        }
        if (SSID.startsWith("\"") && SSID.endsWith("\"")) {
            return SSID.substring(1, SSID.length() - 1);
        }
        return SSID;
    }

    /**
     * @see #isNeedPassword(String)
     */
    public static boolean isNeedPassword(@NonNull final ScanResult scanResult) {
        return isNeedPassword(scanResult.capabilities);
    }

    /**
     * 判断某个 Wi-Fi 是否需要密码
     */
    static boolean isNeedPassword(@NonNull final String capabilities) {
        return !TextUtils.isEmpty(capabilities) && !"[ESS]".equalsIgnoreCase(capabilities);
    }

    /**
     * @see WifiManager#calculateSignalLevel(int, int)
     */
    public static int calculateSignalLevel(final int rssi, final int numLevels) {
        return WifiManager.calculateSignalLevel(rssi, numLevels);
    }

    /**
     * @return true Wi-Fi 频率是 2.4GHz
     */
    public static boolean is24GHz(final int frequency) {
        return frequency > 2400 && frequency < 2500;
    }

    /**
     * @return true Wi-Fi 频率是 5GHz
     */
    public static boolean is5GHz(final int frequency) {
        return frequency > 4900 && frequency < 5900;
    }

    /**
     * 返回信道带宽的文字描述
     *
     * @param channelBandWidth 信道带宽
     */
    @NonNull
    public static String getChannelBandWidthDescription(final int channelBandWidth) {
        switch (channelBandWidth) {
            case ScanResult.CHANNEL_WIDTH_20MHZ: return DES_CHANNEL_WIDTH_20MHZ;
            case ScanResult.CHANNEL_WIDTH_40MHZ: return DES_CHANNEL_WIDTH_40MHZ;
            case ScanResult.CHANNEL_WIDTH_80MHZ: return DES_CHANNEL_WIDTH_80MHZ;
            case ScanResult.CHANNEL_WIDTH_160MHZ: return DES_CHANNEL_WIDTH_160MHZ;
            case ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ: return DES_CHANNEL_WIDTH_80MHZ_PLUS_MHZ;
            default: return "";
        }
    }

    /**
     * 检查一个 {@link WifiConfiguration} 是否有效
     */
    public static boolean isConfigurationValid(final WifiConfiguration configuration) {
        return configuration != null && configuration.networkId >= 0;
    }

    /**
     * 检查一个 {@link WifiConfiguration} 是否已不再使用。例如配置的密码已经过期
     */
    public static boolean isConfigurationDisabled(final WifiConfiguration configuration) {
        return configuration != null && configuration.status == WifiConfiguration.Status.DISABLED;
    }

    /**
     * 创建或者获取已有的 Wi-Fi 连接配置
     * @param password Wi-Fi 密码，传 null 则不设置密码
     */
    @NonNull
    public static WifiConfiguration getWifiConfiguration(@NonNull final Wifi wifi, @Nullable final String password) {
        @NonNull
        final WifiConfiguration wifiConfig;
        if (wifi.isSaved()) {
            wifiConfig = wifi.configuration;
            if (wifi.isConfigDisabled()) {
                wifiPwdConfig(wifiConfig, wifi, password);
            }
        } else {
            wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = "\"" + wifi.SSID + "\"";
            wifiPwdConfig(wifiConfig, wifi, password);
        }
        return wifiConfig;
    }

    public static void wifiPwdConfig(@NonNull final WifiConfiguration wifiConfig, @NonNull final Wifi wifi, @Nullable final String password) {
        wifiConfig.allowedAuthAlgorithms.clear();
        wifiConfig.allowedGroupCiphers.clear();
        wifiConfig.allowedKeyManagement.clear();
        wifiConfig.allowedPairwiseCiphers.clear();
        wifiConfig.allowedProtocols.clear();

        final String upCaseCap = wifi.capabilities.toUpperCase();
        if (!wifi.isNeedPassword()) {
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.NONE);
        } else if (upCaseCap.contains("WEP")) {
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
            wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            if (password != null) {
                wifiConfig.wepKeys[0] = "\"" + password + "\"";
                wifiConfig.wepTxKeyIndex = 0;
            }
        } else {
            if (upCaseCap.contains("PSK")) {
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            }
            if (upCaseCap.contains("EAP")) {
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.LEAP);
            }
            if (upCaseCap.contains("WPA2")) {
                wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            } else if (upCaseCap.contains("WPA")) {
                wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            }
            if (upCaseCap.contains("TKIP")) {
                wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            }
            if (upCaseCap.contains("CCMP")) {
                wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            }
            if (password != null) {
                wifiConfig.preSharedKey = "\"" + password + "\"";
            }
        }
    }

}
