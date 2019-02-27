package org.jossing.wifihelper;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.Objects;

/**
 * Wi-Fi 网络变化的相关回调。
 * 先留着吧。暂时不用这个回调，因为这里面拿到的 NetworkInfo 的状态全是 connected。。。
 *
 * @author jossing
 * @date 2018/12/28
 */
@Deprecated
@RequiresApi(21)
class WifiNetworkCallback extends ConnectivityManager.NetworkCallback {
    private static final String TAG = "WifiNetworkCallback";

    private Activity mActivity;

    public WifiNetworkCallback(@NonNull final Activity activity) {
        mActivity = Objects.requireNonNull(activity);
    }

    @Override
    public void onAvailable(Network network) {

        Log.w(TAG, "onAvailable(" + WifiSupport.getConnectivityManager(mActivity).getNetworkInfo(network) + ")");
    }

    @Override
    public void onLosing(Network network, int maxMsToLive) {
        Log.w(TAG, "onLosing(" + WifiSupport.getConnectivityManager(mActivity).getNetworkInfo(network) + ", " + maxMsToLive + ")");
    }

    @Override
    public void onLost(Network network) {
        Log.w(TAG, "onLost(" + WifiSupport.getConnectivityManager(mActivity).getNetworkInfo(network) + ")");
    }

    @Override
    public void onUnavailable() {
        Log.w(TAG, "onUnavailable()");
    }

    @Override
    public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
        Log.w(TAG, "onCapabilitiesChanged(" + WifiSupport.getConnectivityManager(mActivity).getNetworkInfo(network) + ", " + networkCapabilities.toString() + ")");
    }

    @Override
    public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
        Log.w(TAG, "onLinkPropertiesChanged(" + WifiSupport.getConnectivityManager(mActivity).getNetworkInfo(network) + ", " + linkProperties.toString() + ")");
    }

    public void destroy() {
        mActivity = null;
    }

    public static NetworkRequest getNetworkRequest() {
        return new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
    }
}
