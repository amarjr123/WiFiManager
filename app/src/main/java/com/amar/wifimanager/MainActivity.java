package com.amar.wifimanager;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {

    private static final int LOCATION_REQUEST = 100;

    private WifiManager wifiManager;
    private LinearLayout wifiList;
    private TextView statusText;
    private Button scanButton;

    private final BroadcastReceiver wifiReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    showWifiNetworks();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        wifiManager =
                (WifiManager) getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);

        wifiList = findViewById(R.id.wifiList);
        statusText = findViewById(R.id.statusText);
        scanButton = findViewById(R.id.scanButton);

        scanButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startWifiScan();
                    }
                }
        );

        IntentFilter filter =
                new IntentFilter(
                        WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
                );

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                    wifiReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            registerReceiver(
                    wifiReceiver,
                    filter
            );
        }

        checkPermission();
    }

    private void checkPermission() {

        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        LOCATION_REQUEST
                );
            }
        }
    }

    private void startWifiScan() {

        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

                checkPermission();
                return;
            }
        }

        if (!wifiManager.isWifiEnabled()) {

            statusText.setText(
                    "पहले फोन का WiFi ON करें"
            );

            return;
        }

        wifiList.removeAllViews();

        statusText.setText(
                "WiFi scan हो रहा है..."
        );

        boolean started =
                wifiManager.startScan();

        if (!started) {
            showWifiNetworks();
        }
    }

    private void showWifiNetworks() {

        wifiList.removeAllViews();

        List<ScanResult> results =
                wifiManager.getScanResults();

        if (results == null || results.isEmpty()) {

            statusText.setText(
                    "कोई WiFi नहीं मिला। WiFi और Location ON रखें।"
            );

            return;
        }

        statusText.setText(
                "कुल " + results.size() + " WiFi मिले"
        );

        for (ScanResult result : results) {

            String ssid = result.SSID;

            if (ssid == null ||
                    ssid.trim().isEmpty()) {

                ssid = "Hidden Network";
            }

            final String networkName = ssid;

            int level =
                    WifiManager.calculateSignalLevel(
                            result.level,
                            5
                    );

            TextView wifiItem =
                    new TextView(this);

            wifiItem.setText(
                    "📶 " + networkName
                            + "\nSignal: "
                            + result.level
                            + " dBm"
                            + "\nStrength: "
                            + (level + 1)
                            + "/5"
            );

            wifiItem.setTextSize(18);

            wifiItem.setPadding(
                    16,
                    24,
                    16,
                    24
            );

            wifiItem.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Network: " + networkName,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

            wifiList.addView(wifiItem);
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        try {
            unregisterReceiver(wifiReceiver);
        } catch (Exception ignored) {
        }
    }
}
