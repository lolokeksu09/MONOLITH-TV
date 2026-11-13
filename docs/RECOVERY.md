# Recovery · never remove stock HOME

A custom HOME app can leave the device inconvenient to navigate if it crashes. MONOLITH TV must not automatically change system HOME selections.

## Before selecting a default launcher

1. Install and open the preview as an ordinary TV app.
2. Verify physical remote navigation, app launch and BACK.
3. Confirm Google TV Launcher remains installed and enabled.
4. Confirm that Wi-Fi ADB remains accessible and record the current IP.

## Return to stock

On Android TV, open Settings > Apps > Default apps > Home app when this menu exists and select the stock launcher. Firmware menus vary.

If the preview is selected as HOME and fails, connect through an already authorized ADB session:

```bash
adb -s 192.168.0.160:5555 shell cmd package clear-default-apps dev.monolith.tv.preview
adb -s 192.168.0.160:5555 shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
```

Device-specific availability of `cmd package clear-default-apps` is not guaranteed; inspect `cmd package help` if this command fails. Alternative: launch the stock TV Settings activity over ADB, then clear launcher defaults through the UI.

As a last resort for **preview only**, uninstall its package:

```bash
adb -s 192.168.0.160:5555 uninstall dev.monolith.tv.preview
```

This removes preview preferences and app data. Never uninstall or disable `com.google.android.tvlauncher` as part of MONOLITH TV setup.

