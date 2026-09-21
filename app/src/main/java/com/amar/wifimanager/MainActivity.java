package com.amar.wifimanager;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private static final int LOCATION_REQUEST = 100;

    private WifiManager wifiManager;
    private LinearLayout wifiList;
    private TextView statusText;

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

        wifiManager = (WifiManager)
                getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);

        wifiList = findViewById(R.id.wifiList);
        statusText = findViewById(R.id.statusText);

        findViewById(R.id.scanButton)
                .setOnClickListener(v -> startWifiScan());

        findViewById(R.id.myWifiButton)
                .setOnClickListener(v -> showMyWifi());

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

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
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

    private void startWifiScan() {

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

            checkPermission();
            return;
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

        if (!wifiManager.startScan()) {
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

            TextView item =
                    new TextView(this);

            item.setText(
                    "📶 " + ssid +
                    "\nSignal: " +
                    result.level +
                    " dBm" +
                    "\nStrength: " +
                    (WifiManager.calculateSignalLevel(
                            result.level, 5
                    ) + 1) +
                    "/5"
            );

            item.setTextSize(18);

            item.setPadding(
                    16,
                    20,
                    16,
                    20
            );

            wifiList.addView(item);
        }
    }

    private void showMyWifi() {

        String ssid = "";

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) {

            WifiInfo info =
                    wifiManager.getConnectionInfo();

            if (info != null &&
                    info.getSSID() != null) {

                ssid = info.getSSID()
                        .replace("\"", "");
            }
        }

        final EditText ssidInput =
                new EditText(this);

        ssidInput.setHint(
                "WiFi name (SSID)"
        );

        ssidInput.setText(ssid);

        final EditText passwordInput =
                new EditText(this);

        passwordInput.setHint(
                "अपने WiFi का password"
        );

        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                40,
                10,
                40,
                0
        );

        box.addView(ssidInput);
        box.addView(passwordInput);

        new AlertDialog.Builder(this)

                .setTitle(
                        "अपने WiFi का QR बनाएं"
                )

                .setMessage(
                        "अपने/authorized WiFi का password डालें।"
                )

                .setView(box)

                .setPositiveButton(
                        "GENERATE QR",
                        (dialog, which) ->
                                generateQr(
                                        ssidInput.getText().toString(),
                                        passwordInput.getText().toString()
                                )
                )

                .setNegativeButton(
                        "CANCEL",
                        null
                )

                .show();
    }

    private void generateQr(
            String ssid,
            String password
    ) {

        if (ssid.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "WiFi name डालें",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String wifiData =
                "WIFI:T:WPA;S:" +
                escapeWifi(ssid) +
                ";P:" +
                escapeWifi(password) +
                ";;";

        try {

            Map<EncodeHintType, Object> hints =
                    new HashMap<>();

            hints.put(
                    EncodeHintType.MARGIN,
                    2
            );

            BitMatrix matrix =
                    new MultiFormatWriter().encode(
                            wifiData,
                            BarcodeFormat.QR_CODE,
                            700,
                            700,
                            hints
                    );

            Bitmap bitmap =
                    Bitmap.createBitmap(
                            700,
                            700,
                            Bitmap.Config.RGB_565
                    );

            for (int x = 0; x < 700; x++) {

                for (int y = 0; y < 700; y++) {

                    bitmap.setPixel(
                            x,
                            y,
                            matrix.get(x, y)
                                    ? 0xFF000000
                                    : 0xFFFFFFFF
                    );
                }
            }

            ImageView imageView =
                    new ImageView(this);

            imageView.setImageBitmap(
                    bitmap
            );

            imageView.setPadding(
                    20,
                    10,
                    20,
                    10
            );

            new AlertDialog.Builder(this)

                    .setTitle(
                            "WiFi QR Code"
                    )

                    .setMessage(
                            "दूसरे फोन से इस QR को scan करें।"
                    )

                    .setView(imageView)

                    .setPositiveButton(
                            "DONE",
                            null
                    )

                    .show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "QR बनाने में समस्या हुई",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private String escapeWifi(String value) {

        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace(":", "\\:")
                .replace("\"", "\\\"");
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        try {

            unregisterReceiver(
                    wifiReceiver
            );

        } catch (Exception ignored) {
        }
    }
}
