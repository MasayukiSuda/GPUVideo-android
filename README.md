# GPUVideo-android

[![Platform](https://img.shields.io/badge/platform-android-green.svg)](http://developer.android.com/index.html)
<img src="https://img.shields.io/badge/license-MIT-green.svg?style=flat">
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)

This library apply video filter on generate an Mp4 and on ExoPlayer video and Video Recording with Camera2.

#### apply video filter on generate an Mp4 
<table>
    <td><img src="art/sample.gif"><br>Sample Video<br>No filter</td>
    <td><img src="art/grayscale.gif" ><br><a href="https://github.com/MasayukiSuda/GPUVideo-android/blob/master/gpuv/src/main/java/com/daasuu/gpuv/egl/filter/GlGrayScaleFilter.java">GlGlayScaleFilter</a><br> apply</td>
    <td><img src="art/monochrome.gif" ><br><a href="https://github.com/MasayukiSuda/GPUVideo-android/blob/master/gpuv/src/main/java/com/daasuu/gpuv/egl/filter/GlMonochromeFilter.java">GlMonochromeFilter</a><br> apply</td>
    <td><img src="art/watermark.gif" ><br><a href="https://github.com/MasayukiSuda/GPUVideo-android/blob/master/gpuv/src/main/java/com/daasuu/gpuv/egl/filter/GlWatermarkFilter.java">GlWatermarkFilter</a><br> apply</td>
</table>

#### apply video filter on ExoPlayer video
<img src="art/art.gif" width="33.33%">

#### apply video filter on Video Recording with Camera2.
<img src="art/camera.gif" width="33.33%">


## References And Special Thanks to
* [android-transcoder](https://github.com/ypresto/android-transcoder)
* [android-transcoder Japanese blog](http://qiita.com/yuya_presto/items/d48e29c89109b746d000)
* [android-gpuimage](https://github.com/CyberAgent/android-gpuimage)
* [Android MediaCodec stuff](http://bigflake.com/mediacodec/)
* [grafika](https://github.com/google/grafika)
* [libstagefright](https://android.googlesource.com/platform/frameworks/av/+/lollipop-release/media/libstagefright)
* [AudioVideoRecordingSample](https://github.com/saki4510t/AudioVideoRecordingSample)


## Sample Dependencies
* [glide](https://github.com/bumptech/glide)


## License
[MIT License](https://github.com/MasayukiSuda/GPUVideo-android/blob/master/LICENSE)

#### ExoPlayer and ExoPlayer demo.

    Copyright (C) 2014 The Android Open Source Project
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
