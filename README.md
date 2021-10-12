# GPUVideo-android

<img src="Logotype primary horizontal.png" width="60%" height="60%" />


[![Platform](https://img.shields.io/badge/platform-android-green.svg)](http://developer.android.com/index.html)
<img src="https://img.shields.io/badge/license-MIT-green.svg?style=flat">
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)

This library apply video filter on generate an Mp4 and on ExoPlayer video and Video Recording with Camera2.<br>
Android MediaCodec API is used this library.

# Features
* apply video filter, scale, and rotate Mp4.
* apply video filter on ExoPlayer video.
* apply video filter on Video Recording with Camera2.



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

## Gradle
Step 1. Add the JitPack repository to your build file
```groovy
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```
Step 2. Add the dependency
```groovy
dependencies {
        implementation 'com.github.MasayukiSuda:GPUVideo-android:v0.1.2'
        // if apply video filter on ExoPlayer video
        implementation 'com.google.android.exoplayer:exoplayer-core:2.15.1'
}
```

## Sample Usage apply video filter on generate an Mp4
```
    new GPUMp4Composer(srcMp4Path, destMp4Path)
            .rotation(Rotation.ROTATION_90)
            .size((width) 540, (height) 960)
            .fillMode(FillMode.PRESERVE_ASPECT_FIT)
            .filter(new GlFilterGroup(new GlMonochromeFilter(), new GlVignetteFilter()))
            .listener(new GPUMp4Composer.Listener() {
                @Override
                public void onProgress(double progress) {
                    Log.d(TAG, "onProgress = " + progress);
                }

                @Override
                public void onCompleted() {
                    Log.d(TAG, "onCompleted()");
                    runOnUiThread(() -> {
                        Toast.makeText(context, "codec complete path =" + destPath, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onCanceled() {
                    Log.d(TAG, "onCanceled");
                }

                @Override
                public void onFailed(Exception exception) {
                    Log.e(TAG, "onFailed()", exception);
                }
            })
            .start();
```
#### Builder Method
| method | description |
|:---|:---|
| rotation | Rotation of the movie, default Rotation.NORMAL |
| size | Resolution of the movie, default same resolution of src movie |
| fillMode | Options for scaling the bounds of an movie. PRESERVE_ASPECT_FIT is fit center. PRESERVE_ASPECT_CROP is center crop , default PRESERVE_ASPECT_FIT |
| filter | This filter is OpenGL Shaders to apply effects on video. Custom filters can be created by inheriting <a href="https://github.com/MasayukiSuda/GPUVideo-android/blob/master/gpuv/src/main/java/com/daasuu/gpuv/egl/filter/GlFilter.java">GlFilter.java</a>. , default GlFilter(No filter). Filters is <a href="https://github.com/MasayukiSuda/GPUVideo-android/blob/master/gpuv/src/main/java/com/daasuu/gpuv/egl/filter">here</a>. |
| videoBitrate | Set Video Bitrate, default video bitrate is 0.25 * 30 * outputWidth * outputHeight |
| mute | Mute audio track on exported video. Default `mute = false`. |
| flipVertical | Flip Vertical on exported video. Default `flipVertical = false`. |
| flipHorizontal | Flip Horizontal on exported video. Default `flipHorizontal = false`. |  


## Sample Usage apply video filter on ExoPlayer video  
#### STEP 1
Create [SimpleExoPlayer](https://google.github.io/ExoPlayer/guide.html#creating-the-player) instance. 
In this case, play MP4 file. <br>
Read [this](https://google.github.io/ExoPlayer/guide.html#add-exoplayer-as-a-dependency) if you want to play other video formats. <br>
```JAVA
    TrackSelector trackSelector = new DefaultTrackSelector();

    // Measures bandwidth during playback. Can be null if not required.
    DefaultBandwidthMeter defaultBandwidthMeter = new DefaultBandwidthMeter();
    // Produces DataSource instances through which media data is loaded.
    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "yourApplicationName"), defaultBandwidthMeter);
    // This is the MediaSource representing the media to be played.
    MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(MP4_URL));

    // SimpleExoPlayer
    player = ExoPlayerFactory.newSimpleInstance(context, mediaSource);
    // Prepare the player with the source.
    player.prepare(videoSource);
    player.setPlayWhenReady(true);

```


#### STEP 2
Create [GPUPlayerView](https://github.com/MasayukiSuda/GPUVideo-android/blob/master/gpuv/src/main/java/com/daasuu/gpuv/player/GPUPlayerView.java) and set SimpleExoPlayer to GPUPlayerView.

```JAVA
    gpuPlayerView = new GPUPlayerView(this);
    // set SimpleExoPlayer
    gpuPlayerView.setSimpleExoPlayer(player);
    gpuPlayerView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    // add gpuPlayerView to WrapperView
    ((MovieWrapperView) findViewById(R.id.layout_movie_wrapper)).addView(gpuPlayerView);
    gpuPlayerView.onResume();
```

#### STEP 3
Set Filter. Filters is [here](https://github.com/MasayukiSuda/GPUVideo-android/blob/master/gpuv/src/main/java/com/daasuu/gpuv/egl/filter).<br>
Custom filters can be created by inheriting [GlFilter.java](https://github.com/MasayukiSuda/GPUVideo-android/blob/master/gpuv/src/main/java/com/daasuu/gpuv/egl/filter/GlFilter.java).
```JAVA
    gpuPlayerView.setGlFilter(new GlSepiaFilter());
```

## Sample Usage  apply video filter on Video Recording with Camera2.

SetUp on onResume method.
```JAVA  
  sampleGLView = new GLSurfaceView(getApplicationContext());
  FrameLayout frameLayout = findViewById(R.id.wrap_view);
  frameLayout.addView(sampleGLView);
  
  gpuCameraRecorder = new GPUCameraRecorderBuilder(activity, sampleGLView)
    .lensFacing(LensFacing.BACK)
    .build();
```
Release on onPause method.
```JAVA
  sampleGLView.onPause();      

  gpuCameraRecorder.stop();
  gpuCameraRecorder.release();
  gpuCameraRecorder = null;

  ((FrameLayout) findViewById(R.id.wrap_view)).removeView(sampleGLView);
  sampleGLView = null;
```
Start and Stop Video record.
```JAVA
  // record start.
  gpuCameraRecorder.start(filepath);
  // record stop.
  gpuCameraRecorder.stop();
```
This filter is OpenGL Shaders to apply effects on camera preview. Custom filters can be created by inheriting <a href="https://github.com/MasayukiSuda/GPUVideo-android/blob/master/gpuv/src/main/java/com/daasuu/gpuv/egl/filter/GlFilter.java">GlFilter.java</a>. , default GlFilter(No filter). Filters is <a href="https://github.com/MasayukiSuda/GPUVideo-android/blob/master/gpuv/src/main/java/com/daasuu/gpuv/egl/filter">here</a>. 
```JAVA
  gpuCameraRecorder.setFilter(GlFilter);
```
Other methods.
```JAVA
  // if flash enable, turn on or off camera flash. 
  gpuCameraRecorder.switchFlashMode();
  // autofocus change.
  gpuCameraRecorder.changeAutoFocus();
  // set focus point at manual.
  gpuCameraRecorder.changeManualFocusPoint(float eventX, float eventY, int viewWidth, int viewHeight); 
  // scale camera preview
  gpuCameraRecorder.setGestureScale(float scale);
```

#### Builder Method
| method | description |
|:---|:---|
| cameraRecordListener | onGetFlashSupport, onRecordComplete, onError, and onCameraThreadFinish. Detail is <a href="https://github.com/MasayukiSuda/GPUVideo-android/blob/master/gpuv/src/main/java/com/daasuu/gpuv/camerarecorder/CameraRecordListener.java">here</a>. |
| filter | This filter is OpenGL Shaders to apply effects on camera preview. Custom filters can be created by inheriting <a href="https://github.com/MasayukiSuda/GPUVideo-android/blob/master/gpuv/src/main/java/com/daasuu/gpuv/egl/filter/GlFilter.java">GlFilter.java</a>. , default GlFilter(No filter). Filters is <a href="https://github.com/MasayukiSuda/GPUVideo-android/blob/master/gpuv/src/main/java/com/daasuu/gpuv/egl/filter">here</a>. |
| videoSize | Resolution of the movie, default `width=720, height=1280`. |
| cameraSize | Preview size. |
| lensFacing | Select back or front camera. Default `LensFacing.FRONT`.  |
| flipHorizontal | Flip Horizontal on recorded video. Default `flipHorizontal = false`. |
| flipVertical | Flip Vertical on recorded video. Default `flipVertical = false`. |
| mute | Mute audio track on recorded video. Default `mute = false`. |
| recordNoFilter | No Filter on recorded video although preview apply a filter. Default `recordNoFilter = false`. |



## Filters
 - Bilateral		
 - BoxBlur		
 - Brightness		
 - BulgeDistortion		
 - CGAColorspace		
 - Contrast		
 - Crosshatch		
 - Exposure		
 - FilterGroup		
 - Gamma		
 - GaussianBlur		
 - GrayScale		
 - Halftone		
 - Haze		
 - HighlightShadow		
 - Hue		
 - Invert		
 - LookUpTable		
 - Luminance		
 - LuminanceThreshold		
 - Monochrome		
 - Opacity		
 - Overlay		
 - Pixelation		
 - Posterize		
 - RGB		
 - Saturation		
 - Sepia		
 - Sharpen		
 - Solarize		
 - SphereRefraction		
 - Swirl			
 - ToneCurve		
 - Tone		
 - Vibrance		
 - Vignette		
 - Watermark		
 - WeakPixelInclusion		
 - WhiteBalance		
 - ZoomBlur



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
