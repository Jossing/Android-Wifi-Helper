package org.jossing.wifisample;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;

import org.jossing.wifihelper.Wifi;
import org.jossing.wifihelper.enumerate.WifiConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Wi-Fi 列表适配器
 *
 * @author jossing
 * @date 2018/12/20
 */
public final class WifiAdapter extends RecyclerView.Adapter<WifiViewHolder> {
    private static final String TAG = "WifiAdapter";

    private final List<Wifi> mWifiList = new ArrayList<>();
    private OnWifiClick mOnWifiClick;
    private OnWifiLongClick mOnWifiLongClick;

    public void setWifiList(List<Wifi> wifiList) {
        mWifiList.clear();
        if (wifiList != null) {
            mWifiList.addAll(wifiList);
        }
        notifyDataSetChanged();
    }

    public void onWifiClick(@Nullable final OnWifiClick listener) {
        mOnWifiClick = listener;
    }

    public void onWifiLongClick(@Nullable final OnWifiLongClick listener) {
        mOnWifiLongClick = listener;
    }

    @NonNull
    @Override
    public WifiViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new WifiViewHolder(viewGroup);
    }

    @Override
    public void onBindViewHolder(@NonNull WifiViewHolder wifiViewHolder, int position) {
        final Wifi wifi = mWifiList.get(position);
        final StringBuilder wifiFreqBuilder = new StringBuilder();
        if (wifi.is24GHz()) {
            wifiFreqBuilder.append("/2.4");
        }
        if (wifi.is5GHz()) {
            wifiFreqBuilder.append("/5");
        }
        if (wifiFreqBuilder.length() != 0) {
            wifiFreqBuilder.delete(0, 1);
            wifiFreqBuilder.append("GHz");
        }
        final SpannableStringBuilder wifiName = new SpannableStringBuilder(wifiFreqBuilder.toString());
        final ForegroundColorSpan freqColorSpan = new ForegroundColorSpan(Color.parseColor("#8A000000"));
        final AbsoluteSizeSpan freqSizeSpan = new AbsoluteSizeSpan(14, true);
        wifiName.setSpan(freqColorSpan, 0, wifiName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        wifiName.setSpan(freqSizeSpan, 0, wifiName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        wifiName.insert(0, " ");
        wifiName.insert(0, wifi.SSID);
        wifiViewHolder.mTvWifiName.setText(wifiName);
        wifiViewHolder.mTvWifiStatus.setText(getWifiStateDes(wifi));
        if (wifiViewHolder.mTvWifiStatus.length() != 0) {
            wifiViewHolder.mTvWifiStatus.setVisibility(View.VISIBLE);
        } else {
            wifiViewHolder.mTvWifiStatus.setVisibility(View.GONE);
        }
        wifiViewHolder.mIvLevelSignalWifi.setImageResource(wifi.isNeedPassword() ? R.drawable.level_signal_wifi_bar_lock_black : R.drawable.level_signal_wifi_bar_black);
        wifiViewHolder.mIvLevelSignalWifi.setImageLevel(wifi.getSignalLevel(5));
        wifiViewHolder.itemView.setOnClickListener(v -> {
            if (mOnWifiClick != null) {
                mOnWifiClick.onClick(wifi);
            }
        });
        wifiViewHolder.itemView.setOnLongClickListener(v -> {
            if (mOnWifiLongClick != null) {
                return mOnWifiLongClick.onClick(wifi);
            }
            return false;
        });
    }

    @NonNull
    private String getWifiStateDes(@NonNull final Wifi wifi) {
        final String state;
        if (wifi.isCurrent()) {
            switch (wifi.getConnectionState()) {
                case WifiConnection.SEARCHING:
                    state = "正在查找接入点…";
                    break;
                case WifiConnection.CONNECTING:
                    state = "正在连接…";
                    break;
                case WifiConnection.AUTHENTICATING:
                    state = "正在进行身份验证…";
                    break;
                case WifiConnection.OBTAINING_IPADDR:
                    state = "正在获取IP地址…";
                    break;
                case WifiConnection.CONNECTED:
                    state = "已连接";
                    break;
                case WifiConnection.SUSPENDED:
                    state = "流量已暂停";
                    break;
                case WifiConnection.DISCONNECTED:
                    state = "已断开";
                    break;
                case WifiConnection.UNKNOWN:
                default:
                    state = "";
            }
        } else if (wifi.isConfigDisabled()) {
            state = "请检查密码，然后重试";
        } else if (wifi.isSaved()) {
            state = "已保存";
        } else {
            state = "";
        }
        return state;
    }

    @Override
    public int getItemCount() {
        return mWifiList.size();
    }


    public interface OnWifiClick {

        void onClick(@NonNull final Wifi wifi);
    }

    public interface OnWifiLongClick {

        boolean onClick(@NonNull final Wifi wifi);
    }
}
