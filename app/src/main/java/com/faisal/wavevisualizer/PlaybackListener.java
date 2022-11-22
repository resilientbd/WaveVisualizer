package com.faisal.wavevisualizer;

public interface PlaybackListener {
    void onProgress(int progress);
    void onCompletion();
}
