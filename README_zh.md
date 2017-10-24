# 泼辣修图 Android SDK

泼辣修图SDK的体积仅仅为 (<200kb)。 本SDK使用Android原生OpenGL框架开发。目前用于泼辣修图中[泼辣修图](https://play.google.com/store/apps/details?id=photo.editor.polarr). 

本SDK包含了一个示例工程 (co.polarr.polarrrenderdemo) 用于调试以及开发对接。本示例程序演示了SDK中的所有功能

最低版本限制 Android API Level 14 (4.0.3)

## 版权限制
包含本SDK在内的所有版本库中的内容，属于Polarr, Inc.版权所有。未经允许均不得用于商业目的。当前版本的示例SDK失效时间为2017年9月30日。如需要获取完整授权等更多相关信息，请联系我们[info@polarr.co](mailto:info@polarr.co)

## 功能模块
本SDK包含了泼辣修图App里面的全局调整功能。以下是泼辣修图的全局调整面板：

![sdk](https://user-images.githubusercontent.com/5923363/28428260-6f90ca4c-6dab-11e7-8136-67498e369665.png)

## 效果展示

泼辣修图SDK的参数调整效果超过主流大量英勇地调整效果，具体对比请见
https://www.dropbox.com/sh/5idxxl9g8hq7171/AAAL3ctgl6o_cnhn8lxxd2Hca?dl=0

示例工程<br>
![示例工程](https://user-images.githubusercontent.com/5923363/28439929-bcdd097a-6dd6-11e7-8456-beef54bfaac8.gif)

# 易用性

几行代码即可接入本SDK

## 增加 dependencies 到 Gradle文件
### 必须的
```groovy
// render sdk
compile (name: 'renderer-release', ext: 'aar')
  
// render utils
compile (name: 'utils-release', ext: 'aar')
    
// fast json decoder used by render sdk
compile 'com.alibaba:fastjson:1.1.55.android'
```
### 可选的（仅仅用于二维码识别）
```groovy
// qr code scanner and decoder
compile (name: 'qrcode-release', ext: 'aar')
 
// qr code
compile 'com.google.zxing:core:3.2.1'
```
## 初始化 GLRenderView
```xml
<co.polarr.renderer.render.GLRenderView
  android:id="@+id/render_view"
  android:layout_width="match_parent"
  android:layout_height="match_parent"/>
```
```java
GLRenderView renderView = (GLRenderView) findViewById(R.id.render_view);
```
## 导入图片
```java
renderView.importImage(BitmapFactory.decodeResource(getResources(), R.mipmap.demo));
```
## 全局调整
调整单个属性的数值，取值范围从 -1.0f 到 +1.0f.
更多属性描述, 请参考 [基本全局调整属性](#基本全局调整属性)
```java
String label = "contrast";
float adjustmentValue = 0.5f;
renderView.updateShader(label, adjustmentValue);
renderView.requestRender();
```
## 显示原始图片
```java
boolean isOriginal = true;
renderView.showOriginal(isOriginal);
```
## 重置图片
重置图片为原始状态
```java
renderView.resetAll();
```
## 导出图片
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
## 基本全局调整属性
"exposure" [曝光](http://polaxiong.com/wiki/hou-qi-shu-yu/pu-guang.html)<br>
"gamma" [亮度](http://polaxiong.com/wiki/hou-qi-shu-yu/liang-du.html)<br>
"contrast" [对比度](http://polaxiong.com/wiki/hou-qi-shu-yu/dui-bi-du.html)<br>
"saturation" [饱和度](http://polaxiong.com/wiki/hou-qi-shu-yu/bao-he-du.html)<br>
"vibrance" [自然饱和度](http://polaxiong.com/wiki/hou-qi-shu-yu/zi-ran-bao-he-du.html)<br>
"distortion_horizontal" [水平透视](http://polaxiong.com/wiki/hou-qi-shu-yu/shui-ping-tou-shi.html)<br>
"distortion_vertical" [垂直透视](http://polaxiong.com/wiki/hou-qi-shu-yu/chui-zhi-tou-shi.html)<br>
"fringing" [色差](http://polaxiong.com/wiki/hou-qi-shu-yu/se-cha.html)<br>
"color_denoise" [降噪色彩](http://polaxiong.com/wiki/hou-qi-shu-yu/jiang-zao-se-cai.html)<br>
"luminance_denoise" [降噪明度](http://polaxiong.com/wiki/hou-qi-shu-yu/jiang-zao-ming-du.html)<br>
"dehaze" [去雾](http://polaxiong.com/wiki/hou-qi-shu-yu/qu-wu.html)<br>
"diffuse" [眩光](http://polaxiong.com/wiki/hou-qi-shu-yu/xuan-guang.html)<br>
"temperature" [色温](http://polaxiong.com/wiki/hou-qi-shu-yu/se-wen.html)<br>
"tint" [色调](http://polaxiong.com/wiki/hou-qi-shu-yu/se-tiao.html)<br>
"highlights" [高光](http://polaxiong.com/wiki/hou-qi-shu-yu/gao-guang.html)<br>
"shadows" [阴影](http://polaxiong.com/wiki/hou-qi-shu-yu/yin-ying.html)<br>
"whites" [白色色阶](http://polaxiong.com/wiki/hou-qi-shu-yu/bai-se-se-jie.html)<br>
"blacks" [黑色色阶](http://polaxiong.com/wiki/hou-qi-shu-yu/hei-se-se-jie.html)<br>
"clarity" [清晰度](http://polaxiong.com/wiki/hou-qi-shu-yu/qing-xi-du.html)<br>
"highlights_hue" [色调高光色相](http://polaxiong.com/wiki/hou-qi-shu-yu/se-tiao-gao-guang.html)<br>
"highlights_saturation" [色调高光饱和度](http://polaxiong.com/wiki/hou-qi-shu-yu/se-tiao-gao-guang.html)<br>
"shadows_hue" [色调阴影色相](http://polaxiong.com/wiki/hou-qi-shu-yu/se-tiao-yin-ying.html)<br>
"shadows_saturation" [色调阴影饱和度](http://polaxiong.com/wiki/hou-qi-shu-yu/se-tiao-yin-ying.html)<br>
"balance" [色调平衡](http://polaxiong.com/wiki/hou-qi-shu-yu/se-tiao-ping-heng.html)<br>
"sharpen" [锐化](http://polaxiong.com/wiki/hou-qi-shu-yu/rui-hua.html)

"hue_red" [HSL色相红色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"hue_orange" [HSL色相橘色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"hue_yellow" [HSL色相黄色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"hue_green" [HSL色相绿色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"hue_aqua" [HSL色相青色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"hue_blue" [HSL色相蓝色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"hue_purple" [HSL色相紫色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"hue_magenta" [HSL色相品红](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)

"saturation_red" [HSL饱和度红色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"saturation_orange" [HSL饱和度橘色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"saturation_yellow" [HSL饱和度黄色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"saturation_green" [HSL饱和度绿色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"saturation_aqua" [HSL饱和度青色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"saturation_blue" [HSL饱和度蓝色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"saturation_purple" [HSL饱和度紫色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"saturation_magenta" [HSL饱和度品红](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>

"luminance_red" [HSL明度红色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"luminance_orange" [HSL明度橘色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"luminance_yellow" [HSL明度黄色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"luminance_green" [HSL明度绿色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"luminance_aqua" [HSL明度青色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"luminance_blue" [HSL明度蓝色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"luminance_purple" [HSL明度紫色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>
"luminance_magenta" [HSL明度品红](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)<br>

"grain_amount" [噪点程度](http://polaxiong.com/wiki/hou-qi-shu-yu/zao-dian-cheng-du.html)<br>
"grain_size" [噪点大小](http://polaxiong.com/wiki/hou-qi-shu-yu/zao-dian-da-xiao.html)

## 视图调整
移动位置
```java
float offsetX = 10.0f;
float offsetY = 5.5f;
renderView.setPosition(offsetX, offsetY);
```
获取视图缩放
```java
float mZoom = renderView.getZoom();
```
获取最佳适配屏幕缩放数值
```java
float minZoom = renderView.getMinZoom();
```
设置视图缩放
```java
float mZoom = 1.2f;
renderView.setZoom(mZoom);
```

## 滤镜二维码
### 通过url请求滤镜信息
```java
// run on asyncronized thread
String statesString = QRCodeUtil.requestQRJson("http://www.polaxiong.com/users/custom_filter/1557497");
renderView.updateShaderWithStatesJson(statesString);
```
### 从照片导入滤镜二维码
```java
String qrImagePath;
String qrCodeData = QRUtils.decodeImageQRCode(context, qrImagePath);
  
// run on asyncronized thread
String statesString = QRCodeUtil.requestQRJson(qrCodeData);
renderView.updateShaderWithStatesJson(statesString);
```
### 滤镜二维码扫描与读取
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