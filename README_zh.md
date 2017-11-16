# 泼辣修图 Android SDK

泼辣修图SDK的体积仅仅为 (<200kb)。 本SDK使用Android原生OpenGL框架开发。目前用于泼辣修图中[泼辣修图](https://play.google.com/store/apps/details?id=photo.editor.polarr). 

本SDK包含了一个示例工程 (co.polarr.polarrrenderdemo) 用于调试以及开发对接。本示例程序演示了SDK中的所有功能 

最低版本限制 Android API Level 14 (4.0.3)

## 版权限制
包含本SDK在内的所有版本库中的内容，属于Polarr, Inc.版权所有。未经允许均不得用于商业目的。当前版本的示例SDK失效时间为2017年12月31日。如需要获取完整授权等更多相关信息，请联系我们[info@polarr.co](mailto:info@polarr.co)

## 功能模块
本SDK包含了泼辣修图App里面的全局调整功能。以下是泼辣修图的全局调整面板：

![sdk](https://user-images.githubusercontent.com/5923363/28428260-6f90ca4c-6dab-11e7-8136-67498e369665.png)

示例工程<br>
![示例工程](https://user-images.githubusercontent.com/5923363/28439929-bcdd097a-6dd6-11e7-8456-beef54bfaac8.gif)

## 效果展示

泼辣修图SDK的参数调整效果超过主流大量应用的调整效果，具体对比请见
https://www.dropbox.com/sh/5idxxl9g8hq7171/AAAL3ctgl6o_cnhn8lxxd2Hca?dl=0

# 易用性

几行代码即可接入本SDK

## 增加 dependencies 到 Gradle文件
### 必须的
```groovy
// render sdk
compile (name: 'renderer-release', ext: 'aar')
```
### 可选的
```groovy
// face detection
compile(name: 'dlib-release', ext: 'aar')
// qr code scanner and decoder
compile (name: 'qrcode-release', ext: 'aar')
// qr code
compile 'com.google.zxing:core:3.2.1'
```
## 在GL线程中初始化 PolarrRender
```java
PolarrRender polarrRender = new PolarrRender();
@Override
public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    // call in gl thread
    polarrRender.initRender(getResources(), getWidth(), getHeight(), null);
}
```
## 创建或传入Texture
### 创建Texture
```java
// 只需要调用一次
polarrRender.createInputTexture();
// bind a bitmap to sdk
int inputTexture = polarrRender.getTextureId();
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture);
GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap, 0);

// 输入Texture变化后需要调用
polarrRender.updateInputTexture();
```
### 传入一个Texture
```java
polarrRender.setInputTexture(inputTexture);

// 输入Texture变化后需要调用
polarrRender.updateInputTexture();
```
## 更新渲染尺寸。更新后需要更新输入Texture
```java
// call in gl thread
polarrRender.updateSize(width, height);
```
## 全局调整
调整单个属性的数值，取值范围从 -1.0f 到 +1.0f.
更多属性描述, 请参考 [基本全局调整属性](#基本全局调整属性)
### 更新调整参数，通过Map对象
```java
String label = "contrast";
float adjustmentValue = 0.5f;
Map<String,Object> stateMap = new HashMap<>();
stateMap.put(label, adjustmentValue);

// call in gl thread
polarrRender.updateStates(stateMap);
```
### 更新调整参数，通过json
```java
String stateJson = "{\"contrast\" : 0.5}";

// call in gl thread
polarrRender.updateStates(stateJson);
```
## 渲染
```java
@Override
public void onDrawFrame(GL10 gl) {
    // call in GL thread
    polarrRender.drawFrame();
}
```
## 自动增强
### 全局自动增强
返回值为全局自动增强后需要改变的调整值
```java
// call in gl thread
Map<String, Float> changedStates = polarrRender.autoEnhanceGlobal();
```
### 面部自动增强
进行面部自动增强前需要先[进行人脸识别](##人脸识别)
```java
//包含人脸识别后人脸信息的数据。这里可以是只包含人脸信息的数据，也可以是全部调整数据。
Map<String, Object> faceStates;
// 需要自动识别人脸的索引
int faceIndex = 0;
// 进行面部自动增强，并将增强后的参数设置给传入的map call in gl thread
polarrRender.autoEnhanceFace(faceStates, faceIndex);
// 更新渲染数据 call in gl thread
polarrRender.updateStates(faceStates);
```
## 面部调整
调整面部属性及渲染参数，需要先[进行人脸识别](##人脸识别)
### 面部渲染参数调整
```java
// 识别人脸并返回渲染所需的人脸参数
Map<String, Object> faceStates;
// 获取面部参数信息
List<FaceItem> faces = (List<FaceItem>) faceStates.get("faces");
FaceItem faceItem = faces.get(index);
FaceState faceAdjustments = faceItem.adjustments;
 
faceAdjustments.skin_smoothness = 0; // 皮肤平滑 (-1f,+1f)
faceAdjustments.skin_tone = 0; // 皮肤光泽 (-1f,+1f)
faceAdjustments.skin_hue = 0; // 皮肤色相 (-1f,+1f)
faceAdjustments.skin_saturation = 0;  // 皮肤饱和度 (-1f,+1f)
faceAdjustments.skin_shadows = 0; // 皮肤阴影 (-1f,+1f)
faceAdjustments.skin_highlights = 0; // 皮肤高光 (-1f,+1f)
faceAdjustments.teeth_whitening = 0; // 牙齿美白 (0f,+1f)
faceAdjustments.teeth_brightness = 0; // 牙齿亮度 (0f,+1f)
faceAdjustments.eyes_brightness = 0; // 眼睛亮度 (0f,+1f)
faceAdjustments.eyes_contrast = 0; // 眼睛对比度 (0f,+1f)
faceAdjustments.eyes_clarity = 0; // 眼睛清晰度 (0f,+1f)
faceAdjustments.lips_brightness = 0; // 嘴唇亮度 (0f,+1f)
faceAdjustments.lips_saturation = 0; // 嘴唇饱和度 (-1f,+1f)
```
### 面部尺寸属性调整
```java
// 识别人脸并返回渲染所需的人脸参数
Map<String, Object> faceStates;
// 获取面部参数信息
List<FaceFeaturesState> faceFeaturesStates = (List<FaceFeaturesState>) faceStates.get("face_features");
FaceFeaturesState featureSate = faceFeaturesStates.get(index);
 
featureSate.eye_size = {0, 0};  // 眼睛大小 {(-1f,+1f),(-1f,+1f)}
featureSate.face_width = 0; // 脸宽 (-1f,+1f)
featureSate.forehead_height = 0; // 前额高度 (-1f,+1f)
featureSate.chin_height = 0; // 下巴高度 (-1f,+1f)
featureSate.nose_width = 0; // 鼻子宽度 (-1f,+1f)
featureSate.nose_height = 0; // 鼻子长度 (-1f,+1f)
featureSate.mouth_width = 0; // 嘴宽度 (-1f,+1f)
featureSate.mouth_height = 0; // 嘴高度 (-1f,+1f)
featureSate.smile = 0; // 微笑程度 (-1f,+1f)
```
## 区域蒙版
```java
// 增加一个区域蒙版
Adjustment localMask = new Adjustment();
```
### 区域蒙版的色彩调整属性
```java
LocalState maskAdjustment = localMask.adjustments;
 
maskAdjustment.blur = 0.5f; // 模糊程度 (0f, +1.5f)
maskAdjustment.exposure = 0.5f; // 曝光度 (-1f, +1f)
maskAdjustment.gamma = 0; // 伽马值 (-1f, +1f)
maskAdjustment.temperature = 0.5f; // 色温 (-1f, +1f)
maskAdjustment.tint = 0; // 色调 (-1f, +1f)
maskAdjustment.saturation = 0; // 饱和度 (-1f, +1f)
maskAdjustment.vibrance = 0; // 自然饱和度 (-1f, +1f)
maskAdjustment.contrast = 0.3f; // 对比度 (-1f, +1f)
maskAdjustment.highlights = 0; // 高光 (-1f, +1f)
maskAdjustment.shadows = -0.8f; // 阴影 (-1f, +1f)
maskAdjustment.clarity = 1f; // 清晰度 (-1f, +1f)
maskAdjustment.mosaic_size = 0.2f; // 马赛克 (0, +1f)
maskAdjustment.mosaic_pattern = "square";// 马赛克类型 "square","hexagon","dot","triangle","diamond"
maskAdjustment.shadows_hue = 0; // 阴影色调，用于改变选中区域颜色 (0, +1f)
maskAdjustment.shadows_saturation = 0; // 阴影饱和度，用于改变选中区域颜色 (0, +1f)
maskAdjustment.dehaze = -0.2f; // 去雾 (-1f, +1f)
```
### 圆形蒙版
```java
Adjustment radialMask = new Adjustment();
 
radialMask.type = "radial"; // 蒙版类型为圆形蒙版
radialMask.position = new float[]{0f, 0f}; // 中心位置 (-0.5f, +0.5f) from center of photo
radialMask.size = new float[]{0.608f, 0.45f}; // 大小 (0f, +1f) width, height
radialMask.feather = 0.1f;  // 边缘羽化值 (0, +1f)
radialMask.invert = true; // 反转
 
radialMask.disabled = false; // 是否禁用，禁用后将不应用该蒙版
 
// 对蒙版设置色彩调整属性
LocalState maskAdjustment = radialMask.adjustments;
maskAdjustment.blur = 0.5f;
...

```
### 渐变蒙版
```java
Adjustment gradientMask = new Adjustment();
 
gradientMask.type = "gradient"; // 蒙版类型为渐变蒙版
gradientMask.startPoint = new float[]{0.12f, -0.36f}; // 起点 (-0.5f, +0.5f) from center
gradientMask.endPoint = new float[]{-0.096f, 0.26f}; // 终点 (-0.5f, +0.5f) from center
gradientMask.reflect = true; // 镜像
gradientMask.invert = false; // 反转
 
gradientMask.disabled = false; // 是否禁用，禁用后将不应用该蒙版
 
// 对蒙版设置色彩调整属性
LocalState maskAdjustment = gradientMask.adjustments;
maskAdjustment.blur = 0.5f;
...
```
### 笔刷蒙版
#### 蒙版笔刷
```java
Adjustment brushMask = new Adjustment();
 
brushMask.type = "brush"; // 蒙版类型为笔刷蒙版
brushMask.brush_mode = "mask"; // 蒙版笔刷 mask, 贴图笔刷 paint
 // 创建笔刷对象并增加到蒙版中
BrushItem brushItem = new BrushItem();
brushMask.brush.add(brushItem);
 
brushItem.mode = "mask"; // 蒙版笔刷 mask, 贴图笔刷 paint
brushItem.blend = false; // 混合模式
brushItem.erase = false; // 是否为橡皮
brushItem.channel = new float[]{1f, 0f, 0f, 0f}; // 笔刷的色彩通道，需要和蒙版的色彩通道保持一致 rgba
brushItem.flow = 0.5f; // 笔刷流量 (0, +1f)
brushItem.hardness = 0.5f; // 笔刷硬度 (0, +1f)
brushItem.size = 0.5f; // 笔刷大小 (0, +1f)
// 笔刷由多个点组成，每个点由3个浮点数组成，{x坐标，y坐标，笔刷压感}
Float[] points = {
        0.097f, 0.68f, 0.5f, 0.1045f, 0.6665f, 0.5f, 0.1125f, 0.653f, 0.5f, 
};
brushItem.points = Arrays.asList(points); // point: {x, y, z} (0, +1f), 'z' means pressure
 
brushMask.channel = new float[]{1f, 0f, 0f, 0f}; // 蒙版的色彩通道，和笔刷的色彩通道保持一致 rgba
brushMask.invert = false; // 反转
 
brushMask.disabled = false; // 是否禁用，禁用后将不应用该蒙版
 
// 对蒙版设置色彩调整属性
LocalState maskAdjustment = brushMask.adjustments;
maskAdjustment.exposure = 0.6f; // (-1f, +1f)
maskAdjustment.temperature = -0.8f; // (-1f, +1f)
maskAdjustment.mosaic_size = 0.05f; // (0, +1f)
maskAdjustment.mosaic_pattern = "dot";// "square","hexagon","dot","triangle","diamond"
...
```
#### 贴图笔刷
```java
Adjustment brushMask = new Adjustment();
 
brushMask.type = "brush";
brushMask.brush_mode = "paint"; // 蒙版笔刷 mask, 贴图笔刷 paint
BrushItem brushItem = new BrushItem();
brushMask.brush.add(brushItem);

brushItem.flow = 0.8f; // 笔刷流量 (0, +1f)
brushItem.size = 0.5f; // 笔刷大小 (0, +1f)
brushItem.mode = "paint"; // 蒙版笔刷 mask, 贴图笔刷 paint
brushItem.texture = "stroke_1"; // 笔刷贴图 "stroke_1","stroke_2","stroke_3","stroke_4","dot","speckles","chalk"

// 笔刷由多个点组成，每个点由4个浮点数组成，{x坐标，y坐标，笔刷压感，方向}，其中'方向'的取值范围为(-π，+π)
Float[] points = {
        0.189f, 0.8005f, 0.5f, 2.255f, 0.181f, 0.793f, 0.5f, 2.255f, 0.1725f, 0.7855f, 0.5f, 2.255f, 
};
brushItem.points = Arrays.asList(points); // point: {x, y, p, d} (0, +1f), 'p' means pressure, 'd' means direction (-π，+π)
 
brushMask.disabled = false; // 是否禁用，禁用后将不应用该蒙版
 
// 对蒙版设置色彩调整属性
LocalState maskAdjustment = brushMask.adjustments;
maskAdjustment.exposure = -0.6f; // (-1f, +1f)
...
```
### 设置并渲染蒙版
```java
// 前面创建好的蒙版
Adjustment localMask;
List<Adjustment> localMasks = new ArrayList<>();
localMasks.add(localMask);
localStateMap.put("local_adjustments", localMasks);

renderView.updateStates(localStateMap);
```
## 消除笔
消除区域图片要求：
1. 同原始图片大小一致
2. 背景为纯黑色 RGB (#000000)
3. 消除区域为纯白色 RGB (#FFFFFF)
```java
// 消除区域图片
Bitmap mask = BitmapFactory.decodeResource(getResources(), maskRid);
polarrRender.magicEarser(mask);
```
## 重置图片
重置图片为原始状态
```java
stateMap.clear();
// 如果需要重置人脸信息
FaceUtil.ResetFaceStates(faceStates);

// call in gl thread
polarrRender.updateStates(stateMap);
```
## 获取输入的Texture
```java
int out = polarrRender.getOutputId();
```
## 释放资源
```java
// call in GL thread
polarrRender.release();
```
## 基本全局调整属性

| 属性 | 取值范围 | 描述 |
|-----|:-------:|-----:|
| exposure | -1, +1 | [曝光](http://polaxiong.com/wiki/hou-qi-shu-yu/pu-guang.html) |
| gamma | -1, +1 | [亮度](http://polaxiong.com/wiki/hou-qi-shu-yu/liang-du.html) |
| contrast | -1, +1 | [对比度](http://polaxiong.com/wiki/hou-qi-shu-yu/dui-bi-du.html)|
| saturation | -1, +1 | [饱和度](http://polaxiong.com/wiki/hou-qi-shu-yu/bao-he-du.html)|
| vibrance | -1, +1 | [自然饱和度](http://polaxiong.com/wiki/hou-qi-shu-yu/zi-ran-bao-he-du.html)|
| distortion_horizontal | -1, +1 | [水平透视](http://polaxiong.com/wiki/hou-qi-shu-yu/shui-ping-tou-shi.html)|
| distortion_vertical | -1, +1 | [垂直透视](http://polaxiong.com/wiki/hou-qi-shu-yu/chui-zhi-tou-shi.html)|
| distortion_amount | -1, +1 | [镜头扭曲](http://polaxiong.com/wiki/hou-qi-shu-yu/jing-tou-niu-qu.html)|
| fringing | -1, +1 | [色差](http://polaxiong.com/wiki/hou-qi-shu-yu/se-cha.html)|
| color_denoise | 0, +1 | [降噪色彩](http://polaxiong.com/wiki/hou-qi-shu-yu/jiang-zao-se-cai.html)|
| luminance_denoise | 0, +1 | [降噪明度](http://polaxiong.com/wiki/hou-qi-shu-yu/jiang-zao-ming-du.html)|
| dehaze | -1, +1 | [去雾](http://polaxiong.com/wiki/hou-qi-shu-yu/qu-wu.html)|
| diffuse | 0, +1 | [眩光](http://polaxiong.com/wiki/hou-qi-shu-yu/xuan-guang.html)|
| temperature | -1, +1 | [色温](http://polaxiong.com/wiki/hou-qi-shu-yu/se-wen.html)|
| tint | -1, +1 | [色调](http://polaxiong.com/wiki/hou-qi-shu-yu/se-tiao.html)|
| highlights | -1, +1 | [高光](http://polaxiong.com/wiki/hou-qi-shu-yu/gao-guang.html)|
| shadows | -1, +1 | [阴影](http://polaxiong.com/wiki/hou-qi-shu-yu/yin-ying.html)|
| whites | -1, +1 | [白色色阶](http://polaxiong.com/wiki/hou-qi-shu-yu/bai-se-se-jie.html)|
| blacks | -1, +1 | [黑色色阶](http://polaxiong.com/wiki/hou-qi-shu-yu/hei-se-se-jie.html)|
| clarity | -1, +1 | [清晰度](http://polaxiong.com/wiki/hou-qi-shu-yu/qing-xi-du.html)|
| sharpen | 0, +1 | [锐化](http://polaxiong.com/wiki/hou-qi-shu-yu/rui-hua.html)
| highlights_hue | 0, +1 | [色调高光色相](http://polaxiong.com/wiki/hou-qi-shu-yu/se-tiao-gao-guang.html)|
| highlights_saturation | 0, +1 | [色调高光饱和度](http://polaxiong.com/wiki/hou-qi-shu-yu/se-tiao-gao-guang.html)|
| shadows_hue | 0, +1 | [色调阴影色相](http://polaxiong.com/wiki/hou-qi-shu-yu/se-tiao-yin-ying.html)|
| shadows_saturation | 0, +1 | [色调阴影饱和度](http://polaxiong.com/wiki/hou-qi-shu-yu/se-tiao-yin-ying.html)|
| balance | -1, +1 | [色调平衡](http://polaxiong.com/wiki/hou-qi-shu-yu/se-tiao-ping-heng.html)|
|  |  |  |
| hue_red | -1, +1 | [HSL色相红色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| hue_orange | -1, +1 | [HSL色相橘色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| hue_yellow | -1, +1 | [HSL色相黄色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| hue_green | -1, +1 | [HSL色相绿色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| hue_aqua | -1, +1 | [HSL色相青色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| hue_blue | -1, +1 | [HSL色相蓝色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| hue_purple | -1, +1 | [HSL色相紫色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| hue_magenta | -1, +1 | [HSL色相品红](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)
|  |  |  |
| saturation_red | -1, +1 | [HSL饱和度红色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| saturation_orange | -1, +1 | [HSL饱和度橘色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| saturation_yellow | -1, +1 | [HSL饱和度黄色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| saturation_green | -1, +1 | [HSL饱和度绿色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| saturation_aqua | -1, +1 | [HSL饱和度青色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| saturation_blue | -1, +1 | [HSL饱和度蓝色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| saturation_purple | -1, +1 | [HSL饱和度紫色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| saturation_magenta | -1, +1 | [HSL饱和度品红](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
|  |  |  |
| luminance_red | -1, +1 | [HSL明度红色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| luminance_orange | -1, +1 | [HSL明度橘色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| luminance_yellow | -1, +1 | [HSL明度黄色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| luminance_green | -1, +1 | [HSL明度绿色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| luminance_aqua | -1, +1 | [HSL明度青色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| luminance_blue | -1, +1 | [HSL明度蓝色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| luminance_purple | -1, +1 | [HSL明度紫色](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
| luminance_magenta | -1, +1 | [HSL明度品红](http://polaxiong.com/wiki/hou-qi-shu-yu/hsl.html)|
|  |  |  |
| grain_amount | 0, +1 | [噪点程度](http://polaxiong.com/wiki/hou-qi-shu-yu/zao-dian-cheng-du.html)|
| grain_size | 0, +1 | [噪点大小](http://polaxiong.com/wiki/hou-qi-shu-yu/zao-dian-da-xiao.html)|
|  |  |
| mosaic_size | 0, +1 | [像素化](http://polaxiong.com/wiki/hou-qi-shu-yu/xiang-su-hua.html)
| mosaic_pattern | "square","hexagon","dot","triangle","diamond" | [像素化](http://polaxiong.com/wiki/hou-qi-shu-yu/xiang-su-hua.html)
## 人脸识别
```groovy
dependencies {
    // face detection
    compile(name: 'dlib-release', ext: 'aar')
}
```
### 识别人脸并返回渲染所需数据
图片配置为ARGB8888、宽高不超过500像素可以获得较好结果和性能。由于人脸识别开销较大，需要在异步线程执行
```java
Bitmap scaledBitmap; //图片配置为ARGB8888、宽高不超过500像素可以获得较好结果和性能
// 初始化识别工具
FaceUtil.InitFaceUtil(context);
// 识别人脸并返回渲染所需的人脸参数
Map<String, Object> faces = FaceUtil.DetectFace(scaledBitmap);
// 释放识别工具（一次初始化支持多次人脸识别）
FaceUtil.Release();
 
// 将人脸参数写入本地属性数组，并设置给渲染引擎
localStateMap.putAll(faces);
renderView.updateStates(localStateMap);
```
### 重置人脸信息
```java
// 不需要初始化识别工具
FaceUtil.ResetFaceStates(faceStates);
```
## 滤镜工具
SDK 内置了泼辣修图的滤镜包，滤镜包数据内置于renderer module中。
### 获取滤镜列表
```java
// 获取滤镜包
List<FilterPackage> packages = FilterPackageUtil.GetAllFilters(getResources());
// 获取滤镜
FilterItem filterItem = filterPackage.filters.get(0);
```
### 设置滤镜参数
```java
renderView.updateStates(filterItem.state);
```
### 调整滤镜程度
```java
float adjustmentValue = 0.5f; // 滤镜程度 (0f, 1f)
Map<String, Object> interpolateStates = FilterPackageUtil.GetInterpolateValue(filterItem.state, adjustmentValue);
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
