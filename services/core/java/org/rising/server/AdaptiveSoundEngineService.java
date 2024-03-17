/*
 * Copyright (C) 2023 The RisingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rising.server;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.RemoteControlClient;
import android.media.RemoteController;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

import com.android.server.SystemService;

public final class AdaptiveSoundEngineService extends SystemService {

    private final static int EFFECT_FADE_DURATION_MS = 1000;

    private final AudioManager mAudioManager;

    private static final String TAG = "AdaptiveSoundEngineService";
    private static final String AUDIO_EFFECT_MODE = "audio_effect_mode";
    private static final String AUDIO_EFFECT_MODE_ENABLED = "audio_effect_mode_enabled";
    private static final String BASS_BOOST_STRENGTH = "bass_boost_strength";
    private static final int USER_ALL = UserHandle.USER_ALL;

    private final AudioEffectsUtils audioEffectsUtils;
    private final Context mContext;
    private final SettingsObserver mSettingsObserver;
    
    private boolean isServiceEnabled = false;
    private boolean isEqInitialized = false;

    public AdaptiveSoundEngineService(Context context) {
        super(context);
        mContext = context;
        audioEffectsUtils = new AudioEffectsUtils(mContext);
        mSettingsObserver = new SettingsObserver(new Handler());
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onStart() {
        mSettingsObserver.observe();
        updateAudioEffectSettings();
    }

    private int getSetting(String settingName) {
        try {
            return Settings.System.getIntForUser(
                    getContentResolver(),
                    settingName, 0, ActivityManager.getCurrentUser());
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving setting: " + settingName, e);
            return 0;
        }
    }

    private ContentResolver getContentResolver() {
        return mContext.getContentResolver();
    }

    private void updateAudioEffectSettings() {
        pausePlayback();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                disableAllEffects();
                final int mode = getSetting(AUDIO_EFFECT_MODE);
                final boolean isEffectsEnabled = mode != 0 
                    && getSetting(AUDIO_EFFECT_MODE_ENABLED) != 0;
                if (isEffectsEnabled && !isEqInitialized) {
                    audioEffectsUtils.initializeAudioEffects();
                    audioEffectsUtils.enableEqualizer(true);
                }
                audioEffectsUtils.enableDynamicMode(isEffectsEnabled, mode);
            }
        }, 1000);
    }

    private void disableAllEffects() {
        if (isEqInitialized) {
            audioEffectsUtils.enableEqualizer(false);
            isEqInitialized = false;
        }
        audioEffectsUtils.releaseAudioEffects();
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }
        void observe() {
            getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(AUDIO_EFFECT_MODE), false, this, USER_ALL);
            getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(AUDIO_EFFECT_MODE_ENABLED), false, this, USER_ALL);
        }
        @Override
        public void onChange(boolean selfChange) {
            updateAudioEffectSettings();
        }
    }

    private void pausePlayback() {
        if (mAudioManager != null) {
            KeyEvent keyEventDown = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE);
            mAudioManager.dispatchMediaKeyEvent(keyEventDown);
            KeyEvent keyEventUp = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE);
            mAudioManager.dispatchMediaKeyEvent(keyEventUp);
        }
    }
}
