package org.jossing.wifisample;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author jossing
 * @date 2018/12/20
 */
public final class WifiViewHolder extends RecyclerView.ViewHolder {

    public final TextView mTvWifiName;
    public final TextView mTvWifiStatus;
    public final ImageView mIvLevelSignalWifi;

    public WifiViewHolder(ViewGroup parent) {
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item_wifi, parent, false));
        mTvWifiName = itemView.findViewById(R.id.tv_wifi_name);
        mTvWifiStatus = itemView.findViewById(R.id.tv_wifi_status);
        mIvLevelSignalWifi = itemView.findViewById(R.id.iv_level_signal_wifi);
    }
}
