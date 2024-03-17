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

import android.content.Context;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.media.AudioManager;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AudioEffectsUtils {

    private final AudioManager mAudioManager;
    private Equalizer equalizer;
    private BassBoost bassBoost;
    private PresetReverb presetReverb;
    private Virtualizer virtualizer;
    private ScheduledExecutorService dynamicModeScheduler;

    private boolean isBassBoostEnabled = false;
    private boolean isDynamicModeEnabled = false;
    private int currentMode = 0; 

    public AudioEffectsUtils(Context context) {
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void initializeAudioEffects() {
        final int audioSessionId = AudioManager.AUDIO_SESSION_ID_GENERATE;
        equalizer = new Equalizer(0, audioSessionId);
        bassBoost = new BassBoost(0, audioSessionId);
        presetReverb = new PresetReverb(0, audioSessionId);
        virtualizer = new Virtualizer(0, audioSessionId);
    }

    public void releaseAudioEffects() {
        if (equalizer != null) equalizer.release();
        if (bassBoost != null) bassBoost.release();
        if (presetReverb != null) presetReverb.release();
        if (virtualizer != null) virtualizer.release();
    }

    public void enableEqualizer(boolean enable) {
        if (equalizer != null) equalizer.setEnabled(enable);
    }

    public void enableDynamicMode(boolean enable, int mode) {
        isDynamicModeEnabled = enable;
        currentMode = mode;
        if (enable) {
            enableDynamicEqMode(mode);
        } else {
            disableDynamicMode();
        }
    }

    private void disableDynamicMode() {
        if (dynamicModeScheduler != null && !dynamicModeScheduler.isShutdown()) {
            dynamicModeScheduler.shutdownNow();
        }
    }

    private void enableDynamicEqMode(int mode) {
        dynamicModeScheduler = Executors.newSingleThreadScheduledExecutor();
        dynamicModeScheduler.scheduleAtFixedRate(() -> {
            if (!isDynamicModeEnabled) {
                dynamicModeScheduler.shutdownNow();
                return;
            }
            int currentVolume = getCurrentVolumeLevel();
            applyDynamicFrequencyResponse(currentVolume);
            if (mode == 1 || mode == 2 || mode == 4) {
                applyDynamicBassBoost(currentVolume);
            }
            applyDynamicSpatialAudio(currentVolume);
            if (mode != 1) {
                applyDynamicReverb(currentVolume);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    private int getCurrentVolumeLevel() {
        return (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) /
               mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    // Dynamic Frequency Response
    private void applyDynamicFrequencyResponse(int volumeLevel) {
        final short minEQLevel = equalizer.getBandLevelRange()[0];
        final short maxEQLevel = equalizer.getBandLevelRange()[1];
        short numberOfBands = equalizer.getNumberOfBands();
        for (short i = 0; i < numberOfBands; i++) {
            short newLevel = calculateBandLevel(i, numberOfBands, minEQLevel, maxEQLevel, volumeLevel);
            newLevel = adjustLevelForMode(newLevel, i, volumeLevel);
            short currentLevel = equalizer.getBandLevel(i);
            if (currentLevel != newLevel) {
                equalizer.setBandLevel(i, newLevel);
            }
        }
    }
    
    private short adjustLevelForMode(short baseLevel, short band, int volumeLevel) {
        switch (currentMode) {
            case 1: // Music
                return adjustForMusic(baseLevel, band, volumeLevel);
            case 2: // Game
                return adjustForGame(baseLevel, band, volumeLevel);
            case 3: // Theater
                return adjustForTheater(baseLevel, band, volumeLevel);
            case 4: // Smart
                return adjustForSmart(baseLevel, band, volumeLevel);
            default:
                return baseLevel;
        }
    }

    private short adjustForMusic(short baseLevel, short band, int volumeLevel) {
        return (band >= 2 && band <= 4) ? (short) (baseLevel * 1.05) : baseLevel;
    }

    private short adjustForGame(short baseLevel, short band, int volumeLevel) {
        return (band > 4) ? (short) (baseLevel * 1.1) : baseLevel;
    }

    private short adjustForTheater(short baseLevel, short band, int volumeLevel) {
        return (band == 3 || band == 4) ? (short) (baseLevel * 1.2) : baseLevel;
    }

    private short adjustForSmart(short baseLevel, short band, int volumeLevel) {
        return (short) (baseLevel * 1.1);
    }

    private short calculateBandLevel(short band, short numberOfBands, short minEQLevel, short maxEQLevel, int volumeLevel) {
        if (band < 0 || band >= numberOfBands) {
            return minEQLevel;
        }
        double lowFrequencyAdjustment = 1.1;
        double midHighFrequencyAdjustment = 1.2;
        double volumeAdjustment = 0.85 + (volumeLevel / 100.0 * 0.6);
        double adjustmentFactor = band < numberOfBands / 2 ? lowFrequencyAdjustment : midHighFrequencyAdjustment;
        return (short) Math.min(maxEQLevel, Math.max(minEQLevel,
                minEQLevel + (adjustmentFactor * volumeAdjustment * (maxEQLevel - minEQLevel))));
    }

    private short calculateOutputGain(int volumeLevel) {
        double gainFactor = 1.0 + (1.0 - volumeLevel / 100.0);
        short gain = (short) (gainFactor * (equalizer.getBandLevelRange()[1] - equalizer.getBandLevelRange()[0]) / 5);
        return (short) Math.min(gain, equalizer.getBandLevelRange()[1]);
    }

    private void applyDynamicBassBoost(int volumeLevel) {
        if (bassBoost != null) {
            short strength = (short) Math.max(0, 1000 - (volumeLevel * 5));
            bassBoost.setStrength(strength);
        }
    }

    private void applyDynamicReverb(int volumeLevel) {
        if (presetReverb != null) {
            short newPreset;
            if (volumeLevel < 33) {
                newPreset = PresetReverb.PRESET_SMALLROOM;
            } else if (volumeLevel < 66) {
                newPreset = PresetReverb.PRESET_MEDIUMROOM;
            } else {
                newPreset = PresetReverb.PRESET_LARGEROOM;
            }
            presetReverb.setPreset(newPreset);
        }
    }
    
    private void applyDynamicSpatialAudio(int volumeLevel) {
        virtualizer.setStrength((short) Math.min(1500, 800 + volumeLevel * 2));
    }
}
