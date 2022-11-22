package com.faisal.wavevisualizer.utils;

public final class AudioUtils {
    public static int calculateAudioLength(int samplesCount, int sampleRate, int channelCount) {
        return ((samplesCount / channelCount) * 1000) / sampleRate;
    }
}