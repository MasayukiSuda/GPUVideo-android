package com.daasuu.gpuvideoandroid.compose;

import java.util.List;

public interface VideoLoadListener {

    void onVideoLoaded(List<VideoItem> videoItems);

    void onFailed(Exception e);
}
