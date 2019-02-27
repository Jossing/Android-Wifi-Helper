package org.jossing.wifihelper.enumerate;

import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.support.annotation.NonNull;

import org.jossing.wifihelper.annotation.ConnectionState;

/**
 * @author jossing
 * @date 2018/12/29
 */
public final class WifiConnection {

    private WifiConnection() {}

    public static final int UNKNOWN = -1;
    public static final int SEARCHING = 0;
    public static final int CONNECTING = 1;
    public static final int AUTHENTICATING = 2;
    public static final int OBTAINING_IPADDR = 3;
    public static final int CONNECTED = 4;
    public static final int SUSPENDED = 5;
    public static final int DISCONNECTED = 6;

    @ConnectionState
    public static int from(@NonNull final NetworkInfo networkInfo) {
        final int connectionState;
        switch (networkInfo.getDetailedState()) {
            case SCANNING:
                connectionState = WifiConnection.SEARCHING;
                break;
            case AUTHENTICATING:
                connectionState = WifiConnection.AUTHENTICATING;
                break;
            case OBTAINING_IPADDR:
                connectionState = WifiConnection.OBTAINING_IPADDR;
                break;
            default: switch (networkInfo.getState()) {
                case CONNECTING:
                    connectionState = WifiConnection.CONNECTING;
                    break;
                case CONNECTED:
                    connectionState = WifiConnection.CONNECTED;
                    break;
                case SUSPENDED:
                    connectionState = WifiConnection.SUSPENDED;
                    break;
                case DISCONNECTING:
                case DISCONNECTED:
                    connectionState = WifiConnection.DISCONNECTED;
                    break;
                case UNKNOWN:
                default:
                    connectionState = WifiConnection.UNKNOWN;
            }
        }
        return connectionState;
    }

    @ConnectionState
    public static int from(@NonNull final WifiInfo wifiInfo) {
        final int connectionState;
        switch (wifiInfo.getSupplicantState()) {
            case SCANNING:
                connectionState = WifiConnection.SEARCHING;
                break;
            case COMPLETED:
                connectionState = WifiConnection.CONNECTED;
                break;
            case AUTHENTICATING:
            case ASSOCIATING:
            case ASSOCIATED:
                connectionState = WifiConnection.AUTHENTICATING;
                break;
            case FOUR_WAY_HANDSHAKE:
            case GROUP_HANDSHAKE:
                connectionState = WifiConnection.AUTHENTICATING;
                break;
            case DISCONNECTED:
            case INTERFACE_DISABLED:
            case INACTIVE:
            case DORMANT:
            case UNINITIALIZED:
            case INVALID:
                connectionState = WifiConnection.DISCONNECTED;
                break;
            default:
                connectionState = WifiConnection.UNKNOWN;
        }
        return connectionState;
    }
}
