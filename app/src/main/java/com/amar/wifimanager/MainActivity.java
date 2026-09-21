package com.amar.wifimanager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_REQUEST = 100;
    private WifiManager wifiManager;
    private LinearLayout wifiList;
    private TextView statusText;
    private Button scanButton;

    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            showWifiNetworks();
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        wifiList = findViewById(R.id.wifiList);
        statusText = findViewById(R.id.statusText);
        scanButton = findViewById(R.id.scanButton);
        scanButton.setOnClickListener(v -> startWifiScan());

        IntentFilter filter =
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wifiReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(wifiReceiver, filter);
        }
        checkPermission();
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_REQUEST);
        }
    }

    private void startWifiScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            checkPermission();
            return;
        }
        wifiList.removeAllViews();
        statusText.setText("WiFi scan हो रहा है...");
        if (!wifiManager.startScan()) showWifiNetworks();
    }

    private void showWifiNetworks() {
        wifiList.removeAllViews();
        List<ScanResult> results = wifiManager.getScanResults();
        if (results == null || results.isEmpty()) {
            statusText.setText("कोई WiFi नहीं मिला। Location और WiFi ON रखें।");
            return;
        }
        statusText.setText("कुल " + results.size() + " WiFi मिले");
        for (ScanResult result : results) {
            String ssid = result.SSID;
            if (ssid == null || ssid.trim().isEmpty()) ssid = "Hidden Network";
            final String networkName = ssid;
            int level = WifiManager.calculateSignalLevel(result.level, 5);
            TextView item = new TextView(this);
            item.setText("📶 " + networkName +
                    "\nSignal: " + result.level + " dBm" +
                    "\nStrength: " + (level + 1) + "/5");
            item.setTextSize(18);
            item.setPadding(16, 20, 16, 20);
            item.setOnClickListener(v -> Toast.makeText(
                    MainActivity.this, "Network: " + networkName,
                    Toast.LENGTH_SHORT).show());
            wifiList.addView(item);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(wifiReceiver); } catch (Exception ignored) {}
    }
}
