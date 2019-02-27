package org.jossing.wifisample;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import org.jossing.wifihelper.Wifi;
import org.jossing.wifihelper.WifiSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * 显示 Wi-Fi 信息的 Dialog
 *
 * @author jossing
 * @date 2018/12/20
 */
public final class WifiDialog {

    public interface ActionCallback {
        void connect(@NonNull final Wifi wifi);
        void remove(@NonNull final Wifi wifi);
        void update(@NonNull final Wifi wifi);
        void info(@NonNull final Wifi wifi);
    }

    public static void confirm(@NonNull final Activity activity, @NonNull final CharSequence msg, final DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(activity)
                .setTitle("提示")
                .setMessage(msg)
                .setNegativeButton("取消", (dialog, which) -> { })
                .setPositiveButton("确定", listener)
                .setCancelable(true)
                .create()
                .show();
    }

    public static void showDetail(@NonNull final Activity activity, @NonNull final Wifi wifi) {
        new AlertDialog.Builder(activity)
                .setTitle("Wi-Fi 信息")
                .setMessage(wifi.toString().replaceAll(", ", "\n"))
                .setPositiveButton("关闭", (dialog, which) -> { })
                .setCancelable(true)
                .create()
                .show();
    }

    public static void showConnection(@NonNull final Activity activity, @NonNull final Wifi wifi) {
        final StringBuilder messageBuilder = new StringBuilder();
        if (wifi.isCurrent()) {
            messageBuilder.append(wifi.wifiInfo.toString().replaceAll(", ", "\n"))
                    .append("\nIP: ")
                    .append(WifiSupport.getIpAddressString(wifi.getIpAddress()));
        }
        new AlertDialog.Builder(activity)
                .setTitle("网络详情")
                .setMessage(messageBuilder)
                .setPositiveButton("关闭", (dialog, which) -> { })
                .setCancelable(true)
                .create()
                .show();
    }

    public static void showActions(@NonNull final Activity activity, @NonNull final Wifi wifi, final ActionCallback callback) {
        final List<String> actions = new ArrayList<>();
        if (!wifi.isCurrent()) {
            actions.add("连接到网络");
        }
        if (wifi.isSaved()) {
            actions.add("取消保存网络");
            actions.add("修改网络");
        }
        actions.add("Wi-Fi 信息");
        new AlertDialog.Builder(activity)
                .setTitle(wifi.SSID)
                .setItems(actions.toArray(new String[actions.size()]), (dialog, which) -> {
                    if (callback == null) {
                        return;
                    }
                    final String actionName = actions.get(which);
                    switch (actionName) {
                        case "连接到网络":
                            callback.connect(wifi);
                            break;
                        case "取消保存网络":
                            callback.remove(wifi);
                            break;
                        case "修改网络":
                            callback.update(wifi);
                            break;
                        case "Wi-Fi 信息":
                            callback.info(wifi);
                            break;
                        default:
                    }
                })
                .setCancelable(true)
                .create()
                .show();
    }
}
