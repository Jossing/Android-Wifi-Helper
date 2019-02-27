package org.jossing.wifisample;

import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import org.jossing.wifihelper.Wifi;
import org.jossing.wifihelper.WifiHelper;
import org.jossing.wifihelper.WifiSupport;
import org.jossing.wifihelper.annotation.WifiState;
import org.jossing.wifihelper.enumerate.WifiOperating;

/**
 * 扫描 Wi-Fi，连接 Wi-Fi 示例代码
 *
 * @author jossing
 * @date 2018/12/20
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Switch mSwitchWifi;
    private ImageButton mBtnScanWifi;
    private RecyclerView mRvWifiList;
    private WifiAdapter mWifiAdapter;

    private WifiHelper mWifiHelper;

    private static final int REQUEST_CODE_MUST_PERMISSION = 776;
    private static final int REQUEST_CODE_LOCATION_SERVICE = 762;
    private static final int REQUEST_CODE_PASSWORD = 712;

    /** 输入密码后要连接的 Wi-Fi */
    private Wifi mPendingWifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSwitchWifi = findViewById(R.id.switch_wifi);
        mBtnScanWifi = findViewById(R.id.btn_scan_wifi);
        mRvWifiList = findViewById(R.id.rv_wifi_list);

        if (mWifiHelper == null) {
            mWifiHelper = new WifiHelper(this);
        }

        setupView();

        mWifiHelper.setWifiListCallback((success, wifiList) -> {
            mWifiAdapter.setWifiList(wifiList);
        });
    }

    private void setupView() {
        mWifiHelper.setWifiStateCallback(state -> {
            mSwitchWifi.setText(getWifiStateDes(state));
            mSwitchWifi.setChecked(state == WifiManager.WIFI_STATE_ENABLED ||
                    state == WifiManager.WIFI_STATE_ENABLING);
            mSwitchWifi.setEnabled(state == WifiManager.WIFI_STATE_ENABLED ||
                    state == WifiManager.WIFI_STATE_DISABLED);
        });

        mSwitchWifi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSwitchWifi.setEnabled(false);
                mWifiHelper.switchWifi(isChecked, success -> {
                    if (!success) {
                        mSwitchWifi.setOnCheckedChangeListener(null);
                        mSwitchWifi.toggle();
                        mSwitchWifi.setOnCheckedChangeListener(this);
                    }
                    mSwitchWifi.setEnabled(true);
                });
            }
        });
        mBtnScanWifi.setOnClickListener(v -> startScanWifi(null));
        mRvWifiList.setLayoutManager(new LinearLayoutManager(this));
        mRvWifiList.setItemAnimator(null);
        mRvWifiList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mRvWifiList.setAdapter(mWifiAdapter = new WifiAdapter());
        mWifiAdapter.onWifiClick(this::onWifiClick);
        mWifiAdapter.onWifiLongClick(this::onWifiLongClick);
    }

    private String getWifiStateDes(@WifiState final int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_DISABLING:
                return "正在关闭…";
            case WifiManager.WIFI_STATE_DISABLED:
                return "Wi-Fi 已关闭";
            case WifiManager.WIFI_STATE_ENABLING:
                return "正在打开…";
            case WifiManager.WIFI_STATE_ENABLED:
                return "Wi-Fi 已打开";
            case WifiManager.WIFI_STATE_UNKNOWN:
            default:
                return "";
        }
    }

    /**
     * 启动 Wi-Fi 扫描
     */
    private void startScanWifi(@Nullable final Boolean permissionGranted) {
        if (permissionGranted == null) {
            // Android 6.0 以上需要动态申请定位权限
            ActivityCompat.requestPermissions(this, WifiSupport.LOCATION_PERMISSIONS, REQUEST_CODE_MUST_PERMISSION);
            return;
        }
        switch (mWifiHelper.scanWifi()) {
            case WifiOperating.RESULT_SUCCESS:
                Toast.makeText(this, "启动扫描成功", Toast.LENGTH_LONG).show();
                break;
            case WifiOperating.ERROR_INTERNAL:
                mWifiAdapter.setWifiList(mWifiHelper.getWifiList());
                Toast.makeText(this, "启动扫描失败，只能使用较旧的结果", Toast.LENGTH_LONG).show();
                break;
            case WifiOperating.REQUIRE_LOCATION_PERMISSION:
                Toast.makeText(this, "定位权限是必须的", Toast.LENGTH_LONG).show();
                break;
            case WifiOperating.LOCATION_SERVICE_DISABLED:
                Toast.makeText(this, "需要打开位置信息服务", Toast.LENGTH_LONG).show();
                WifiSupport.startLocationServiceSetting(this, REQUEST_CODE_LOCATION_SERVICE);
                break;
            case WifiOperating.WIFI_NOT_ENABLED:
                Toast.makeText(this, "请先打开 Wi-Fi", Toast.LENGTH_LONG).show();
                break;
            default:
        }
    }

    private void requestPassword(@NonNull final Wifi wifi) {
        mPendingWifi = wifi;
        final Intent intent = new Intent(this, PasswordActivity.class);
        startActivityForResult(intent, REQUEST_CODE_PASSWORD);
    }

    private void onWifiClick(@NonNull final Wifi wifi) {
        if (wifi.isCurrent()) {
            WifiDialog.showConnection(this, wifi);
        } else if (wifi.isSaved()) {
            if (wifi.isConfigDisabled()) {
                requestPassword(wifi);
            } else {
                mWifiHelper.connectWifi(wifi, null, success -> {
                    if (success) {
                        final DhcpInfo dhcpInfo = mWifiHelper.getDhcpInfo();
                        Toast.makeText(MainActivity.this, "IP: " + WifiSupport.getIpAddressString(dhcpInfo.ipAddress), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else {
            if (wifi.isNeedPassword()) {
                requestPassword(wifi);
            } else {
                mWifiHelper.connectWifi(wifi, "", success -> {
                    if (success) {
                        final DhcpInfo dhcpInfo = mWifiHelper.getDhcpInfo();
                        Toast.makeText(MainActivity.this, "IP: " + WifiSupport.getIpAddressString(dhcpInfo.ipAddress), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private boolean onWifiLongClick(@NonNull final Wifi wifi) {
        WifiDialog.showActions(this, wifi, new WifiDialog.ActionCallback() {
            @Override
            public void connect(@NonNull Wifi wifi) {
                onWifiClick(wifi);
            }

            @Override
            public void remove(@NonNull Wifi wifi) {
                final boolean success = mWifiHelper.removeWifiConfig(wifi);
                if (!success) {
                    Toast.makeText(MainActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void update(@NonNull Wifi wifi) {
                Toast.makeText(MainActivity.this, "敬请期待", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void info(@NonNull Wifi wifi) {
                WifiDialog.showDetail(MainActivity.this, wifi);
            }
        });
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_MUST_PERMISSION) {
            startScanWifi(WifiSupport.isLocationPermissionGranted(this));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LOCATION_SERVICE) {
            if (WifiSupport.isLocationServiceEnabled(this)) {
                startScanWifi(WifiSupport.isLocationPermissionGranted(this));
            } else {
                Toast.makeText(this, "位置信息服务未开启", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_CODE_PASSWORD) {
            if (resultCode == RESULT_OK && data != null && mPendingWifi != null) {
                final String password = data.getStringExtra(PasswordActivity.EXTRA_PASSWORD);
                mWifiHelper.connectWifi(mPendingWifi, password, success -> {
                    if (success) {
                        final DhcpInfo dhcpInfo = mWifiHelper.getDhcpInfo();
                        Toast.makeText(MainActivity.this, "IP: " + WifiSupport.getIpAddressString(dhcpInfo.ipAddress), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            mPendingWifi = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (mWifiHelper != null) {
            mWifiHelper.destroy();
            mWifiHelper = null;
        }
        super.onDestroy();
    }
}
