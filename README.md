# Polarr Photo Editor Android SDK

The Polarr Photo Editor Android SDK is an extremely portable (<200kb) library that exposes a subset of the native OpenGL rendering APIs used by [Polarr Photo Editor](https://play.google.com/store/apps/details?id=photo.editor.polarr). 

This SDK includes a starter project (co.polarr.polarrrenderdemo) that calls the Android SDK and users can try out the available editing tools. This repo is intended to demonstrate a subset of the capabilities of the full Android SDK.

The minimum Android API Level is 14 (4.0.3).

## License
The SDK included in this repository must not be used for any commercial purposes without the direct written consent of Polarr, Inc. The current version of the SDK expires on December 31, 2017. For pricing and more info regarding the full license SDK, please email [info@polarr.co](mailto:info@polarr.co).

## Functionalities
The current SDK includes everything as seen in Polarr Photo Editor's global adjustment panel

![sdk](https://user-images.githubusercontent.com/806199/27817613-efbf57be-6046-11e7-915c-7d8a48c4a716.jpg)

Starter project<br>
![Starter project](https://user-images.githubusercontent.com/5923363/28439929-bcdd097a-6dd6-11e7-8456-beef54bfaac8.gif)

# Sample Usage

Below are code samples and function calls to use the SDK

## Add dependencies to Gradle
### Required
```groovy
// render sdk
compile (name: 'renderer-release', ext: 'aar')
```
### Optional
```groovy
// qr code scanner and decoder
compile (name: 'qrcode-release', ext: 'aar')
 
// qr code
compile 'com.google.zxing:core:3.2.1'
```
## Init PolarrRender in GL thread
```java
PolarrRender polarrRender = new PolarrRender();
@Override
public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    // call in gl thread
    polarrRender.initRender(getResources(), getWidth(), getHeight(), null);
}
```
## Create or Set an input texture
### Create a texture and bind
```java
// only need call one time.
polarrRender.createInputTexture();
// bind a bitmap to sdk
int inputTexture = polarrRender.getTextureId();
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture);
GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap, 0);

// call after input texture changed
polarrRender.updateInputTexture();
```
### Set an input texture and bind
```java
polarrRender.setInputTexture(inputTexture);

// call after input texture changed
polarrRender.updateInputTexture();
```
## Update render size, need rebind input texture.
```java
// call in gl thread
polarrRender.updateSize(width, height);
```
## Global adjustments
Adjust a specific adjustment with value. The value `from -1.0f to +1.0f`.
More adjustment lables, see [Basic global adjustments](#basic-global-adjustments)
### Update by a state map
```java
String label = "contrast";
float adjustmentValue = 0.5f;
Map<String,Object> stateMap = new HashMap<>();
stateMap.put(label, adjustmentValue);

// call in gl thread
polarrRender.updateStates(stateMap);
```
### Update from a state json
```java
String stateJson = "{\"contrast\" : 0.5}";

// call in gl thread
polarrRender.updateStates(stateJson);
```
## Draw frame
```java
@Override
public void onDrawFrame(GL10 gl) {
    // call in GL thread
    polarrRender.drawFrame();
}
```
## Reset all state
Reset image to original.
```java
stateMap.clear();
// call in gl thread
polarrRender.updateStates(stateMap);
```
## Get output texture
```java
int out = polarrRender.getOutputId();
```
## Release all resource
```java
// call in GL thread
polarrRender.release();
```
## Basic global adjustments

| Properties | Range |
|-----|:-------:|
| exposure | -1, +1 |
| gamma | -1, +1 |
| contrast | -1, +1 |
| saturation | -1, +1 |
| vibrance | -1, +1 |
| distortion_horizontal | -1, +1 |
| distortion_vertical | -1, +1 |
| distortion_amount | -1, +1 |
| fringing | -1, +1 |
| color_denoise | 0, +1 |
| luminance_denoise | 0, +1 |
| dehaze | -1, +1 |
| diffuse | 0, +1 |
| temperature | -1, +1 |
| tint | -1, +1 |
| highlights | -1, +1 |
| shadows | -1, +1 |
| whites | -1, +1 |
| blacks | -1, +1 |
| clarity | -1, +1 |
| sharpen | 0, +1 |
| highlights_hue | 0, +1 |
| highlights_saturation | 0, +1 |
| shadows_hue | 0, +1 |
| shadows_saturation | 0, +1 |
| balance | -1, +1 |\
|  |  |
| hue_red | -1, +1 |
| hue_orange | -1, +1 |
| hue_yellow | -1, +1 |
| hue_green | -1, +1 |
| hue_aqua | -1, +1 |
| hue_blue | -1, +1 |
| hue_purple | -1, +1 |
| hue_magenta | -1, +1 |
|  |  |
| saturation_red | -1, +1 |
| saturation_orange | -1, +1 |
| saturation_yellow | -1, +1 |
| saturation_green | -1, +1 |
| saturation_aqua | -1, +1 |
| saturation_blue | -1, +1 |
| saturation_purple | -1, +1 |
| saturation_magenta | -1, +1 |
|  |  |
| luminance_red | -1, +1 |
| luminance_orange | -1, +1 |
| luminance_yellow | -1, +1 |
| luminance_green | -1, +1 |
| luminance_aqua | -1, +1 |
| luminance_blue | -1, +1 |
| luminance_purple | -1, +1 |
| luminance_magenta | -1, +1 |
|  |  |
| grain_amount | 0, +1 |
| grain_size | 0, +1 |

## QR code
### QR code request from a url
```java
// run on asyncronized thread
String statesString = QRCodeUtil.requestQRJson("http://www.polaxiong.com/users/custom_filter/1557497");
renderView.updateShaderWithStatesJson(statesString);
```
### QR code import from a image
```java
String qrImagePath;
String qrCodeData = QRUtils.decodeImageQRCode(context, qrImagePath);
  
// run on asyncronized thread
String statesString = QRCodeUtil.requestQRJson(qrCodeData);
renderView.updateShaderWithStatesJson(statesString);
```
### QR code scan and request
```java
Intent intent = new Intent(this, QRScannerActivity.class);
startActivityForResult(intent, ACTIVITY_RESULT_QR_SCANNER);
 
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (ACTIVITY_RESULT_QR_SCANNER == requestCode && resultCode == RESULT_OK) {
        if (data == null || data.getStringExtra("value") == null) {
            return;
        }
        final String urlString = data.getStringExtra("value");

        ThreadManager.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                String statesString = QRCodeUtil.requestQRJson(urlString);
                renderView.updateShaderWithStatesJson(statesString);
            }
        });
    }
}
```