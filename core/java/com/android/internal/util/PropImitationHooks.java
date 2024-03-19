/*
 * Copyright (C) 2022 Paranoid Android
 * Copyright (C) 2022 StatiXOS
 * Copyright (C) 2024 the RisingOS Android Project
 *           (C) 2023 ArrowOS
 *           (C) 2023 The LibreMobileOS Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util;

import android.app.ActivityManager;
import android.app.Application;
import android.app.TaskStackListener;
import android.content.Context;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Binder;
import android.os.Process;
import android.os.Build.VERSION;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.android.internal.R;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PropImitationHooks {

    private static final String TAG = "PropImitationHooks";
    private static final boolean DEBUG = SystemProperties.getBoolean("persist.sys.pihooks.debug", false);
    private static final String PIHOOKS_PROP = "persist.sys.pihooks.";
    
    private static final String PRODUCT_DEVICE = "ro.product.device";

    private static final String SPOOF_PIXEL_GAPPS = "persist.sys.pixelprops.gapps";
    private static final String SPOOF_PIXEL_GMS = "persist.sys.pixelprops.gms";
    private static final String SPOOF_PIXEL_GAMES = "persist.sys.pixelprops.games";
    private static final String SPOOF_PIXEL_GPHOTOS = "persist.sys.pixelprops.gphotos";
    private static final String SPOOF_PIXEL_NETFLIX = "persist.sys.pixelprops.netflix";

    private static final String sMainFP = SystemProperties.get(PIHOOKS_PROP + "spoof_fingerprint");
    private static final String sMainModel = SystemProperties.get(PIHOOKS_PROP + "spoof_model");
    private static final String sMainFpTablet = SystemProperties.get(PIHOOKS_PROP + "spoof_tablet_fingerprint");
    private static final String sMainModelTablet = SystemProperties.get(PIHOOKS_PROP + "spoof_tablet_model");
    private static final String sSecondaryFP = SystemProperties.get(PIHOOKS_PROP + "spoof_secondary_fingerprint");
    private static final String sSecondaryModel = SystemProperties.get(PIHOOKS_PROP + "spoof_secondary_model");
    private static final String sStockFp = SystemProperties.get("ro.vendor.build.fingerprint");

    private static final String PACKAGE_ARCORE = "com.google.ar.core";
    private static final String PACKAGE_ASI = "com.google.android.as";
    private static final String PACKAGE_COMPUTE_SERVICES = "com.google.android.as.oss";
    private static final String PACKAGE_EXT_SERVICES = "com.google.android.ext.services";
    private static final String PACKAGE_FINSKY = "com.android.vending";
    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PROCESS_GMS_PERSISTENT = PACKAGE_GMS + ".persistent";
    private static final String PROCESS_GMS_UI = PACKAGE_GMS + ".ui";
    private static final String PROCESS_GMS_UNSTABLE = PACKAGE_GMS + ".unstable";

    private static final String PACKAGE_AIAI = "com.google.android.apps.miphone.aiai.AiaiApplication";
    private static final String PACKAGE_GASSIST = "com.google.android.apps.googleassistant";
    private static final String PACKAGE_GCAM = "com.google.android.GoogleCamera";
    private static final String PACKAGE_GPHOTOS = "com.google.android.apps.photos";
    private static final String PACKAGE_SUBSCRIPTION_RED = "com.google.android.apps.subscriptions.red";
    private static final String PACKAGE_TURBO = "com.google.android.apps.turbo";
    private static final String PACKAGE_VELVET = "com.google.android.googlequicksearchbox";
    private static final String PACKAGE_GBOARD = "com.google.android.inputmethod.latin";
    private static final String PACKAGE_SETIINGS_INTELLIGENCE = "com.google.android.settings.intelligence";
    private static final String PACKAGE_SETUPWIZARD = "com.google.android.setupwizard";
    private static final String PACKAGE_EMOJI_WALLPAPER = "com.google.android.apps.emojiwallpaper";
    private static final String PACKAGE_CINEMATIC_PHOTOS = "com.google.android.wallpaper.effects";
    private static final String PACKAGE_GOOGLE_WALLPAPERS = "com.google.android.wallpaper";
    private static final String PACKAGE_SNAPCHAT = "com.snapchat.android";

    private static final ComponentName GMS_ADD_ACCOUNT_ACTIVITY = ComponentName.unflattenFromString(
            "com.google.android.gms/.auth.uiflows.minutemaid.MinuteMaidActivity");

    private static final Map<String, Object> asusROG6Props = createGameProps("ASUS_AI2201", "asus");
    private static final Map<String, Object> xperia5Props = createGameProps("SO-52A", "Sony");
    private static final Map<String, Object> op8ProProps = createGameProps("IN2020", "OnePlus");
    private static final Map<String, Object> op9ProProps = createGameProps("LE2123", "OnePlus");
    private static final Map<String, Object> xmMi11TProProps = createGameProps("2107113SI", "Xiaomi");
    private static final Map<String, Object> xmMi13ProProps = createGameProps("2210132C", "Xiaomi");
    private static final Map<String, Object> xmF5Props = createGameProps("23049PCD8G", "Xiaomi");
    private static final Map<String, Object> bs4Props = createGameProps("2SM-X706B", "blackshark");

    private static Map<String, Object> createGameProps(String model, String manufacturer) {
        Map<String, Object> props = new HashMap<>();
        props.put("BRAND", manufacturer);
        props.put("MODEL", model);
        props.put("MANUFACTURER", manufacturer);
        return props;
    }

    private static Map<String, Object> createGoogleSpoofProps(String model, String fingerprint) {
        Map<String, Object> props = new HashMap<>();
        props.put("BRAND", "google");
        props.put("MANUFACTURER", "Google");
        props.put("ID", getBuildID(fingerprint));
        props.put("DEVICE", getDeviceName(fingerprint));
        props.put("PRODUCT", getDeviceName(fingerprint));
        props.put("MODEL", model);
        props.put("FINGERPRINT", fingerprint);
        props.put("TYPE", "user");
        props.put("TAGS", "release-keys");
        return props;
    }

    private static String getBuildID(String fingerprint) {
        Pattern pattern = Pattern.compile("([A-Za-z0-9]+\\.\\d+\\.\\d+\\.\\w+)");
        Matcher matcher = pattern.matcher(fingerprint);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static String getDeviceName(String fingerprint) {
        String[] parts = fingerprint.split("/");
        if (parts.length >= 2) {
            return parts[1];
        } else {
            return "";
        }
    }

    private static final Set<String> extraPackageToSpoof = new HashSet<>(Arrays.asList(
            "com.android.chrome",
            "com.nhs.online.nhsonline",
            "com.netflix.mediaclient"
    ));
    
    private static final Set<String> secondarySpoof = new HashSet<>(Arrays.asList(
            "com.google.android.tts",
            "com.google.android.as"
    ));

    private static final Set<String> packagesToChangeROG6 = new HashSet<>(Arrays.asList(
            "com.activision.callofduty.shooter",
    	    "com.ea.gp.fifamobile",
            "com.gameloft.android.ANMP.GloftA9HM",
            "com.madfingergames.legends",
            "com.pearlabyss.blackdesertm",
            "com.pearlabyss.blackdesertm.gl"
    ));

    private static final Set<String> packagesToChangeXP5 = new HashSet<>(Arrays.asList(
            "com.garena.game.codm",
            "com.tencent.tmgp.kr.codm",
            "com.vng.codmvn"
    ));

    private static final Set<String> packagesToChangeOP8P = new HashSet<>(Arrays.asList(
            "com.netease.lztgglobal",
            "com.pubg.imobile",
            "com.pubg.krmobile",
            "com.rekoo.pubgm",
            "com.riotgames.league.wildrift",
            "com.riotgames.league.wildrifttw",
            "com.riotgames.league.wildriftvn",
            "com.riotgames.league.teamfighttactics",
            "com.riotgames.league.teamfighttacticstw",
            "com.riotgames.league.teamfighttacticsvn",
            "com.tencent.ig",
            "com.tencent.tmgp.pubgmhd",
            "com.vng.pubgmobile"
    ));

    private static final Set<String> packagesToChangeOP9P = new HashSet<>(Arrays.asList(
            "com.epicgames.fortnite",
            "com.epicgames.portal",
            "com.tencent.lolm"
    ));

    private static final Set<String> packagesToChange11TP = new HashSet<>(Arrays.asList(
            "com.ea.gp.apexlegendsmobilefps",
            "com.levelinfinite.hotta.gp",
            "com.supercell.clashofclans",
            "com.vng.mlbbvn"
    ));

    private static final Set<String> packagesToChange13P = new HashSet<>(Arrays.asList(
            "com.levelinfinite.sgameGlobal",
            "com.tencent.tmgp.sgame"
    ));

    private static final Set<String> packagesToChangeF5 = new HashSet<>(Arrays.asList(
            "com.dts.freefiremax",
            "com.dts.freefireth",
            "com.mobile.legends"
    ));

    private static final Set<String> packagesToChangeBS4 = new HashSet<>(Arrays.asList(
          "com.proximabeta.mf.uamo"
    ));

    private static volatile boolean sIsGms, sIsGmsUi, sIsGmsPersist, sIsFinsky, sisGoogleApp, sIsGoogleProcess, sIsExcluded;
    private static volatile String sProcessName;

    public static void setProps(Context appContext) {
        if (appContext == null) return;
        final String packageName = appContext.getPackageName();
        if (packageName == null) return;
        ActivityManager manager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return;
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = null;
        try {
            runningProcesses = manager.getRunningAppProcesses();
        } catch (Exception e) {
            runningProcesses = null;
        }
        if (runningProcesses == null) return;
        String processName = null;
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
            if (processInfo.pid == android.os.Process.myPid()) {
                processName = processInfo.processName;
                break;
            }
        }
        if (processName == null) return;
        final Resources res = appContext.getResources();
        if (res == null) return;
        final String[] EXCLUDED_PACKAGES = res.getStringArray(R.array.config_pihooks_exclude_list);
        final boolean sIsTablet = isDeviceTablet(appContext);
        final String sMainModelSpoof = sIsTablet ? sMainModelTablet : sMainModel;
        final String sMainFpSpoof = sIsTablet ? sMainFpTablet : sMainFP;
        final Map<String, Object> sMainSpoofProps = createGoogleSpoofProps(sMainModelSpoof, sMainFpSpoof);
        final Map<String, Object> sSecondarySpoofProps = createGoogleSpoofProps(sSecondaryModel, sSecondaryFP);
        sProcessName = processName;
        sIsGms = packageName.equals(PACKAGE_GMS) && processName.equals(PROCESS_GMS_UNSTABLE);
        sIsGmsUi = packageName.equals(PACKAGE_GMS) && processName.equals(PROCESS_GMS_UI);
        sIsGmsPersist = packageName.equals(PACKAGE_GMS) && processName.equals(PROCESS_GMS_PERSISTENT);
        sIsFinsky = packageName.equals(PACKAGE_FINSKY);
        sisGoogleApp = packageName.toLowerCase().contains("google");
        sIsGoogleProcess = processName.toLowerCase().contains("google");
        sIsExcluded = Arrays.asList(EXCLUDED_PACKAGES).contains(packageName);
        final boolean sIsExtra = extraPackageToSpoof.contains(packageName);
        final boolean sIsSecondary = secondarySpoof.contains(packageName);
        if (((sisGoogleApp || sIsGoogleProcess) && !sIsExcluded || sIsExtra) && !sIsSecondary) {
            dlog("Spoofing build for Google Services");
            setPropValue("TIME", System.currentTimeMillis());
            if (packageName.equals("com.netflix.mediaclient") && 
                        !SystemProperties.getBoolean(SPOOF_PIXEL_NETFLIX, false)) {
                return;
            }
            sMainSpoofProps.forEach((k, v) -> setPropValue(k, v));
        } else if (sIsSecondary) {
            sSecondarySpoofProps.forEach((k, v) -> setPropValue(k, v));
        } else {
            switch (packageName) {
                case PACKAGE_SUBSCRIPTION_RED:
                case PACKAGE_TURBO:
                case PACKAGE_VELVET:
                case PACKAGE_GBOARD:
                case PACKAGE_SETUPWIZARD:
                case PACKAGE_GMS:
                    sMainSpoofProps.forEach((k, v) -> setPropValue(k, v));
                    break;
                case PACKAGE_ARCORE:
                    dlog("Setting stock fingerprint for: " + packageName);
                    setPropValue("FINGERPRINT", sStockFp);
                    break;
                default:
                    spoofGameProps(packageName);
                    break;
            }
            if (packageName.equals(PACKAGE_GPHOTOS)) {
                if (SystemProperties.getBoolean(SPOOF_PIXEL_GPHOTOS, true)) {
                    dlog("Spoofing as Pixel XL for: " + packageName);
                    spoofBuildPhotos();
                }
            }
            if (sIsGms || sIsGmsPersist || sIsGmsUi) {
                if (SystemProperties.getBoolean("persist.sys.pixelprops.gms", true)) {
                    dlog("Spoofing GMS to pass integrity");
                    setPropValue("TIME", System.currentTimeMillis());
                    spoofBuildGms();
                }
            }
        }
    }

    private static void spoofGameProps(String packageName) {
        if (SystemProperties.getBoolean(SPOOF_PIXEL_GAMES, true)) {
            Map<String, Object> gamePropsToSpoof = null;
            if (packagesToChangeROG6.contains(packageName)) {
                dlog("Spoofing as Asus ROG 6 for: " + packageName);
                gamePropsToSpoof = asusROG6Props;
            } else if (packagesToChangeXP5.contains(packageName)) {
                dlog("Spoofing as Sony Xperia 5 for: " + packageName);
                gamePropsToSpoof = xperia5Props;
            } else if (packagesToChangeOP8P.contains(packageName)) {
                dlog("Spoofing as Oneplus 8 Pro for: " + packageName);
                gamePropsToSpoof = op8ProProps;
            } else if (packagesToChangeOP9P.contains(packageName)) {
                dlog("Spoofing as Oneplus 9 Pro for: " + packageName);
                gamePropsToSpoof = op9ProProps;
            } else if (packagesToChange11TP.contains(packageName)) {
                dlog("Spoofing as Xiaomi Mi 11T Pro for: " + packageName);
                gamePropsToSpoof = xmMi11TProProps;
            } else if (packagesToChange13P.contains(packageName)) {
                dlog("Spoofing as Xiaomi Mi 13 Pro for: " + packageName);
                gamePropsToSpoof = xmMi13ProProps;
            } else if (packagesToChangeF5.contains(packageName)) {
                dlog("Spoofing as Xiaomi F5 for: " + packageName);
                gamePropsToSpoof = xmF5Props;
            } else if (packagesToChangeBS4.contains(packageName)) {
                dlog("Spoofing as Black Shark 4 for: " + packageName);
                gamePropsToSpoof = bs4Props;
            }
            if (gamePropsToSpoof != null) {
                gamePropsToSpoof.forEach((k, v) -> setPropValue(k, v));
            }
        }
    }

    private static boolean isDeviceTablet(Context context) {
        if (context == null) {
            return false;
        }
        Configuration configuration = context.getResources().getConfiguration();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        }
        return (configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE
                || displayMetrics.densityDpi == DisplayMetrics.DENSITY_XHIGH
                || displayMetrics.densityDpi == DisplayMetrics.DENSITY_XXHIGH
                || displayMetrics.densityDpi == DisplayMetrics.DENSITY_XXXHIGH;
    }

    private static void setPropValue(String key, Object value) {
        try {
            if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                dlog(TAG + " Skipping setting empty value for key: " + key);
                return;
            }
            dlog(TAG + " Setting property for key: " + key + ", value: " + value.toString());
            Field field;
            Class<?> targetClass;
            try {
                targetClass = Build.class;
                field = targetClass.getDeclaredField(key);
            } catch (NoSuchFieldException e) {
                targetClass = Build.VERSION.class;
                field = targetClass.getDeclaredField(key);
            }
            if (field != null) {
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                if (fieldType == int.class || fieldType == Integer.class) {
                    if (value instanceof Integer) {
                        field.set(null, value);
                    } else if (value instanceof String) {
                        int convertedValue = Integer.parseInt((String) value);
                        field.set(null, convertedValue);
                        dlog(TAG + " Converted value for key " + key + ": " + convertedValue);
                    }
                } else if (fieldType == String.class) {
                    field.set(null, String.valueOf(value));
                }
                field.setAccessible(false);
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            dlog(TAG + " Failed to set prop " + key);
        } catch (NumberFormatException e) {
            dlog(TAG + " Failed to parse value for field " + key);
        }
    }

    public static boolean shouldBypassTaskPermission(Context context) {
        // GMS doesn't have MANAGE_ACTIVITY_TASKS permission
        final int callingUid = Binder.getCallingUid();
        final String callingPackage = context.getPackageManager().getNameForUid(callingUid);
        dlog("shouldBypassTaskPermission: callingPackage:" + callingPackage);
        return callingPackage != null && callingPackage.toLowerCase().contains("google");
    }

    private static void spoofBuildPhotos() {
        // Pixel XL Props
        setPropValue("BRAND", "google");
        setPropValue("MANUFACTURER", "Google");
        setPropValue("DEVICE", "marlin");
        setPropValue("HARDWARE", "marlin");
        setPropValue("ID", "QP1A.191005.007.A3");
        setPropValue("FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys");
        setPropValue("MODEL", "Pixel XL");
        setPropValue("PRODUCT", "marlin");
    }

    private static void spoofBuildGms() {
        // Alter build parameters to avoid hardware attestation enforcement
        setPropValue("BRAND", SystemProperties.get(PIHOOKS_PROP + "brand"));
        setPropValue("MANUFACTURER", SystemProperties.get(PIHOOKS_PROP + "manufacturer"));
        setPropValue("DEVICE", SystemProperties.get(PIHOOKS_PROP + "product_device"));
        setPropValue("ID", SystemProperties.get(PIHOOKS_PROP + "build_id"));
        setPropValue("FINGERPRINT", SystemProperties.get(PIHOOKS_PROP + "build_fingerprint"));
        setPropValue("MODEL", SystemProperties.get(PIHOOKS_PROP + "product_model"));
        setPropValue("PRODUCT", SystemProperties.get(PIHOOKS_PROP + "product_name"));
    }

    private static boolean isCallerSafetyNet() {
        return sIsGms && Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
    }

    public static void onEngineGetCertificateChain() {
        if ((isCallerSafetyNet() || sIsFinsky) && !sIsExcluded) {
            dlog("Blocked key attestation sIsGms=" + sIsGms + " sIsFinsky=" + sIsFinsky);
            throw new UnsupportedOperationException();
        }
    }

    public static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, "[" + sProcessName + "] " + msg);
    }
}
