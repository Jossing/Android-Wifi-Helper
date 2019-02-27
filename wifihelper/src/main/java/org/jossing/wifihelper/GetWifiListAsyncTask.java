package org.jossing.wifihelper;

import android.content.Context;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.jossing.wifihelper.enumerate.WifiConnection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 用于获取 Wi-Fi 列表的异步任务
 *
 * @author jossing
 * @date 2019/1/2
 */
final class GetWifiListAsyncTask extends AsyncTask<Object, Void, List<Wifi>> {

    private final WifiListCallback mWifiListCallback;

    private GetWifiListAsyncTask(@NonNull final WifiListCallback callback) {
        mWifiListCallback = callback;
    }

    @Override
    protected List<Wifi> doInBackground(Object... objects) {
        if (objects[0] == null) {
            return new ArrayList<>();
        }
        final Context appContext = ((Context) objects[0]).getApplicationContext();
        final NetworkInfo networkInfo = (NetworkInfo) objects[1];
        // 获取 Wi-Fi 列表
        final List<Wifi> wifiList = WifiSupport.getWifiList(appContext);
        // 更新 Wi-Fi 列表中，当前 Wi-Fi 的连接状态
        if (networkInfo != null) {
            Wifi currentWifi = null;
            for (final Wifi wifi : wifiList) {
                if (wifi.isCurrent()) {
                    currentWifi = wifi;
                    break;
                }
            }
            final String changedFromSSID = WifiSupport.getRealSSID(networkInfo.getExtraInfo());
            if (currentWifi != null && TextUtils.equals(currentWifi.SSID, changedFromSSID)) {
                currentWifi.setConnectionState(WifiConnection.from(networkInfo));
            }
        }
        return wifiList;
    }

    @Override
    protected void onPostExecute(List<Wifi> wifiList) {
        mWifiListCallback.invoke(wifiList != null ? wifiList : new ArrayList<>());
    }

    static void execute(@NonNull final Context context,
                        @Nullable final NetworkInfo networkInfo,
                        @NonNull final WifiListCallback callback) {
        new GetWifiListAsyncTask(callback).executeOnExecutor(sDefaultExecutor, context, networkInfo);
    }

    private static final Executor SERIAL_EXECUTOR = new SerialExecutor();

    private static volatile Executor sDefaultExecutor = SERIAL_EXECUTOR;

    private static class SerialExecutor implements Executor {
        final ArrayDeque<Runnable> mTasks = new ArrayDeque<>();
        Runnable mActive;

        @Override
        public synchronized void execute(final Runnable r) {
            mTasks.offer(() -> {
                try {
                    r.run();
                } finally {
                    scheduleNext();
                }
            });
            if (mActive == null) {
                scheduleNext();
            }
        }

        private synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(mActive);
            }
        }
    }

    interface WifiListCallback {
        void invoke(@NonNull final List<Wifi> wifiList);
    }
}
