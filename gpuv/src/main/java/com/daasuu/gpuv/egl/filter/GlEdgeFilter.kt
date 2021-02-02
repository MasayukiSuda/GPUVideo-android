package com.daasuu.gpuv.egl.filter

private val FRAGMENT_SHADER_EDGE = """precision highp float;
varying vec2 textureCoordinate;
 varying vec2 leftTextureCoordinate;
 varying vec2 rightTextureCoordinate;

 varying vec2 topTextureCoordinate;
 varying vec2 topLeftTextureCoordinate;
 varying vec2 topRightTextureCoordinate;

 varying vec2 bottomTextureCoordinate;
 varying vec2 bottomLeftTextureCoordinate;
 varying vec2 bottomRightTextureCoordinate;

 uniform sampler2D sTexture;

 void main()
 {
    float i00   = texture2D(sTexture, textureCoordinate).r;
    float im1m1 = texture2D(sTexture, bottomLeftTextureCoordinate).r;
    float ip1p1 = texture2D(sTexture, topRightTextureCoordinate).r;
    float im1p1 = texture2D(sTexture, topLeftTextureCoordinate).r;
    float ip1m1 = texture2D(sTexture, bottomRightTextureCoordinate).r;
    float im10 = texture2D(sTexture, leftTextureCoordinate).r;
    float ip10 = texture2D(sTexture, rightTextureCoordinate).r;
    float i0m1 = texture2D(sTexture, bottomTextureCoordinate).r;
    float i0p1 = texture2D(sTexture, topTextureCoordinate).r;
    float h = -im1p1 - 2.0 * i0p1 - ip1p1 + im1m1 + 2.0 * i0m1 + ip1m1;
    float v = -im1m1 - 2.0 * im10 - im1p1 + ip1m1 + 2.0 * ip10 + ip1p1;

    float mag = length(vec2(h, v));

    gl_FragColor = vec4(vec3(mag), 1.0);
 }"""

public class GlEdgeFilter: GlThreex3TextureSamplingFilter(FRAGMENT_SHADER_EDGE)