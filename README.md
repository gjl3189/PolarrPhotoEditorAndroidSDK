# Polarr Photo Editor Android SDK

The Polarr Photo Editor Android SDK is an extremely portable (<200kb) library that exposes a subset of the native OpenGL rendering APIs used by [Polarr Photo Editor](https://play.google.com/store/apps/details?id=photo.editor.polarr). 

This SDK includes a starter project (co.polarr.polarrrenderdemo) that calls the Android SDK and users can try out the available editing tools. This repo is intended to demonstrate a subset of the capabilities of the full Android SDK.

The minimum Android API Level is 14 (4.0.3).

## License
The SDK included in this repository must not be used for any commercial purposes without the direct written consent of Polarr, Inc. The current version of the SDK expires on September 30, 2017. For pricing and more info regarding the full license SDK, please email [info@polarr.co](mailto:info@polarr.co).


## Init GLRenderView
```xml
<co.polarr.renderer.render.GLRenderView
  android:id="@+id/render_view"
  android:layout_width="match_parent"
  android:layout_height="match_parent"/>
```
```java
GLRenderView renderView = (GLRenderView) findViewById(R.id.render_view);
```
## Import image
```java
renderView.importImage(BitmapFactory.decodeResource(getResources(), R.mipmap.demo));
```
## Global adjustments
Adjust a specific adjustment with value. The value `from -1.0f to +1.0f`.
More adjustment lables, see [Basic global adjustments](#basic-global-adjustments)
```java
String label = "contrast";
float adjustmentValue = 0.5f;
renderView.updateShader(label, adjustmentValue);
renderView.requestRender();
```
## Show original image
```java
boolean isOriginal = true;
renderView.showOriginal(isOriginal);
```
## Reset image
Reset image to original.
```java
renderView.resetAll();
```
## Export image
```java
// save byte array to a file with PNG format. If need a bitmap, set needImage to true.
boolean needImage = true;
renderView.exportImageData(needImage, new OnExportCallback() {
    @Override
    public void onExport(Bitmap bitmap, byte[] array) {
        // save array to a file
    }
});
```
## Basic global adjustments
```
"exposure",
"contrast",
"saturation",
"distortion_horizontal",
"distortion_vertical",
"fringing",
"color_denoise",
"luminance_denoise",
"dehaze",
"diffuse",
"temperature",
"tint",
"gamma",
"highlights",
"shadows",
"whites",
"blacks",
"clarity",
"vibrance",
"highlights_hue",
"highlights_saturation",
"shadows_hue",
"shadows_saturation",
"balance",
"sharpen",
"hue_red",
"hue_orange",
"hue_yellow",
"hue_green",
"hue_aqua",
"hue_blue",
"hue_purple",
"hue_magenta",
"saturation_red",
"saturation_orange",
"saturation_yellow",
"saturation_green",
"saturation_aqua",
"saturation_blue",
"saturation_purple",
"saturation_magenta",
"luminance_red",
"luminance_orange",
"luminance_yellow",
"luminance_green",
"luminance_aqua",
"luminance_blue",
"luminance_purple",
"luminance_magenta",
```
## Render view movements
Change position
```java
float offsetX = 10.0f;
float offsetY = 5.5f;
renderView.setPosition(offsetX, offsetY);
```
Get current zoom
```java
float mZoom = renderView.getZoom();
```
Get minimum zoom. If current zoom less than minimum zoom, the image will be too small to fit the view size.
```java
float minZoom = renderView.getMinZoom();
```
Set zoom
```java
float mZoom = 1.2f;
renderView.setZoom(mZoom);
```
