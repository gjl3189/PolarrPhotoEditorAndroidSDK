package co.polarr.polarrrenderdemo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import co.polarr.qrcode.QRUtils;
import co.polarr.renderer.FilterPackageUtil;
import co.polarr.renderer.PolarrRenderThread;
import co.polarr.renderer.RenderCallback;
import co.polarr.renderer.entities.Adjustment;
import co.polarr.renderer.entities.BrushItem;
import co.polarr.renderer.entities.FilterItem;
import co.polarr.renderer.entities.FilterPackage;
import co.polarr.renderer.utils.QRCodeUtil;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMPORT_PHOTO = 1;
    private static final int REQUEST_IMPORT_QR_PHOTO = 2;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int ACTIVITY_RESULT_QR_SCANNER = 3;

    private static final int POINT_BRUSH_MOSIC = 1;
    private static final int POINT_BRUSH_BLUR = 2;
    private static final int POINT_BRUSH_PAINT = 3;
    private String brushType;
    private int currentPointState;
    private List<PointF> currentPoints;
    private BrushItem paintBrushItem;


    private AppCompatSeekBar seekbar;
    private TextView labelTv;

    /**
     * Render View
     */
    private DemoView renderView;
    /**
     * adjustment container
     */
    private View sliderCon;
    /**
     * save adjustment values
     */
    private Map<String, Object> localStateMap = new HashMap<>();
    private Map<String, Object> faceStates = new HashMap<>();
    private FilterItem mCurrentFilter;

    private List<FilterItem> mFilters;
    private PolarrRenderThread polarrRenderThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init render view
        renderView = (DemoView) findViewById(R.id.render_view);
        renderView.setAlpha(0);

        sliderCon = findViewById(R.id.slider);
        sliderCon.setVisibility(View.INVISIBLE);

        labelTv = (TextView) findViewById(R.id.label_tv);
        seekbar = (AppCompatSeekBar) findViewById(R.id.seekbar);

        polarrRenderThread = new PolarrRenderThread(getResources());
        polarrRenderThread.start();


        currentPointState = 0;
        currentPoints = new ArrayList<>();
        renderView.setClickable(true);
        renderView.setOnTouchListener(demoViewTouchListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.navigation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        hideAll();
        switch (item.getItemId()) {
            case R.id.navigation_import_image:
                importImage();
                break;
            case R.id.navigation_import_demo:
                importImageDemo();
                break;
            case R.id.navigation_reset:
                reset();
                break;
            case R.id.navigation_qr_scan:
                showQRScan();
                break;
            case R.id.navigation_import_qr:
                importQrImage();
                break;
            case R.id.navigation_bitmap:
                final ImageView demoIV = (ImageView) findViewById(R.id.demo_iv);
                demoIV.setImageBitmap(null);

                if (mFilters == null) {
                    List<FilterPackage> packages = FilterPackageUtil.GetAllFilters(getResources());
                    mFilters = new ArrayList<>();
                    for (FilterPackage filterPackage : packages) {
                        mFilters.addAll(filterPackage.filters);
                    }
                }

                final Bitmap imageBm = BitmapFactory.decodeResource(getResources(), R.mipmap.person);
                Map<String, Object> randomFilterStates = mFilters.get((int) (Math.random() * mFilters.size())).state;

                BenchmarkUtil.TimeStart("renderBitmap");
                polarrRenderThread.renderBitmap(imageBm, randomFilterStates, new RenderCallback() {
                    @Override
                    public void onRenderBitmap(final Bitmap bitmap) {
                        imageBm.recycle();
                        BenchmarkUtil.TimeEnd("renderBitmap");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                demoIV.setImageBitmap(bitmap);
                            }
                        });
                    }
                });

                break;
        }

        return true;
    }

    private void startBrush(int brushState) {
        currentPointState = brushState;
        currentPoints.clear();
        updateBrush(null);
        if (currentPointState == POINT_BRUSH_PAINT) {
            setBrushPaint(brushType);
        }
    }

    private void endTouch() {
        currentPoints.clear();
        if (currentPointState == POINT_BRUSH_PAINT) {
            if (paintBrushItem != null) {
                paintBrushItem = null;
            }
        }
    }

    private void stopBrush() {
        currentPointState = 0;
    }

    private void updateBrush(PointF point) {
        switch (currentPointState) {
            case POINT_BRUSH_MOSIC:
                setBrushMask("square");
                break;
            case POINT_BRUSH_BLUR:
                setBrushBlurMask();
                break;
            case POINT_BRUSH_PAINT:
                if (point != null) {
                    if(paintBrushItem == null) {
                        setBrushPaint(brushType);
                        addBrushPaintPoint(point);
                    }
                    else {
                        addBrushPaintPoint(point);
                    }
                }
                break;
        }
    }

    private View.OnTouchListener demoViewTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (currentPointState <= 0) {
                return false;
            } else {
                PointF touchPoint = new PointF(event.getX() / v.getWidth(), event.getY() / v.getHeight());
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        currentPoints.add(touchPoint);
                        updateBrush(touchPoint);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        endTouch();
                        break;
                }
                return true;
            }
        }
    };

    public void btnClicked(View view) {
        hideAll();
        switch (view.getId()) {
            case R.id.tv_desc:
                importImageDemo();
                break;
            case R.id.btn_addjustment:
                showList();
                break;
            case R.id.btn_auto:
                renderView.autoEnhance(localStateMap);
                break;
            case R.id.btn_auto_face:
                renderView.autoEnhanceFace0(localStateMap);
                break;
            case R.id.btn_add_radial:
                setRadialMask();
                break;
            case R.id.btn_add_gradient:
                setGradientMask();
                break;
            case R.id.btn_mosaic_brush: {
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                final CharSequence items[] = {
                        "square",
                        "hexagon",
                        "dot",
                        "triangle",
                        "diamond",
                };
                adb.setItems(items, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int n) {
                        String mosaicType = items[n].toString();
                        setBrushMask(mosaicType);
                        dialog.dismiss();
                    }

                });
                adb.setNegativeButton("Cancel", null);
                adb.setTitle("Choose a mosaic:");
                adb.show();
            }

            break;
            case R.id.btn_paint_brush: {
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                final CharSequence items[] = {
                        "meitu_1", "meitu_2", "meitu_3", "meitu_4"
                };
                adb.setItems(items, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int n) {
                        switch (n) {
                            case 0:
                                brushType = "stroke_5";
                                startBrush(POINT_BRUSH_PAINT);
                                break;
                            case 1:
                                startBrush(POINT_BRUSH_MOSIC);
                                break;
                            case 2:
                                startBrush(POINT_BRUSH_BLUR);
                                break;
                            case 3:
                                brushType = "stroke_6";
                                startBrush(POINT_BRUSH_PAINT);
                                break;
                        }
                        dialog.dismiss();
                    }

                });
                adb.setNegativeButton("Cancel", null);
                adb.setTitle("Choose a paint:");
                adb.show();
            }

            break;
            case R.id.btn_filters:
                showFilters();
                break;
            case R.id.btn_eraser:
                setEraser(R.mipmap.person);

                break;
//                AlertDialog.Builder adb = new AlertDialog.Builder(this);
//                final CharSequence items[] = {
//                        "tatoo",
//                        "animal",
//                        "bird",
//                        "rocks",
//                };
//                adb.setItems(items, new DialogInterface.OnClickListener() {
//
//                    @Override
//                    public void onClick(DialogInterface dialog, int n) {
//                        switch (n) {
//                            case 0:
//                                setEraser(R.mipmap.person);
//                                break;
//                            case 1:
//                                setEraser(R.mipmap.b1);
//                                break;
//                            case 2:
//                                setEraser(R.mipmap.c1);
//                                break;
//                            case 3:
//                                setEraser(R.mipmap.rocks_small);
//                                break;
//                        }
//                    }
//
//                });
//                adb.setNegativeButton("Cancel", null);
//                adb.setTitle("Choose a demo photo:");
//                adb.show();
//
//                break;
        }
    }

    @Override
    protected void onDestroy() {
        polarrRenderThread.interrupt();
        renderView.releaseRender();
        super.onDestroy();
    }

    private void importImageDemo() {
        final Bitmap imageBm = BitmapFactory.decodeResource(getResources(), R.mipmap.demo_1);

        new Thread() {
            @Override
            public void run() {
                float perfectSize = 500f;
                float minScale = Math.min(perfectSize / imageBm.getWidth(), perfectSize / imageBm.getHeight());
                minScale = Math.min(minScale, 1f);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(imageBm, (int) (minScale * imageBm.getWidth()), (int) (minScale * imageBm.getHeight()), true);
                FaceUtil.InitFaceUtil(MainActivity.this);
                Map<String, Object> faces = FaceUtil.DetectFace(scaledBitmap);
                FaceUtil.Release();
                scaledBitmap.recycle();

                faceStates = faces;
                localStateMap.putAll(faceStates);

                renderView.updateStates(localStateMap);
            }
        }.start();

        renderView.importImage(imageBm);
        renderView.setAlpha(1);
        findViewById(R.id.tv_desc).setVisibility(View.GONE);
    }

    private void importImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMPORT_PHOTO);
    }

    private void importQrImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMPORT_QR_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_IMPORT_PHOTO == requestCode) {
            if (data != null) {
                Uri uri = data.getData();
                final Bitmap imageBm = decodeBitmapFromUri(this, uri);
                renderView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        renderView.importImage(imageBm);
                        renderView.setAlpha(1);
                    }
                }, 1000);
            }
        } else if (REQUEST_IMPORT_QR_PHOTO == requestCode && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                File file = new File(uri.toString());


                final String qrCodeData = QRUtils.decodeImageQRCode(this, file.getPath());
                if (qrCodeData != null) {

                    new Thread() {
                        @Override
                        public void run() {
                            String statesString = QRCodeUtil.requestQRJson(qrCodeData);
                            updateQrStates(statesString);
                        }
                    }.start();
                }
            }
        } else if (ACTIVITY_RESULT_QR_SCANNER == requestCode && resultCode == RESULT_OK) {
            if (data == null || data.getStringExtra("value") == null) {
                return;
            }
            final String urlString = data.getStringExtra("value");
            new Thread() {
                @Override
                public void run() {
                    String statesString = QRCodeUtil.requestQRJson(urlString);
                    updateQrStates(statesString);
                }
            }.start();
        }
    }

    private static Bitmap decodeBitmapFromUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap decodedBm = BitmapFactory.decodeStream(inputStream);
            Bitmap formatedBm = decodedBm.copy(Bitmap.Config.ARGB_8888, false);
            decodedBm.recycle();
            return formatedBm;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showQRScan() {
        Intent intent = new Intent(this, QRScannerActivity.class);
        startActivityForResult(intent, ACTIVITY_RESULT_QR_SCANNER);
    }

    private void updateQrStates(String statesString) {
        // reset to default
        renderView.updateStatesWithJson(statesString);
    }

    private void reset() {
        localStateMap.clear();
        localStateMap.putAll(faceStates);
        FaceUtil.ResetFaceStates(faceStates);

        renderView.updateStates(localStateMap);
    }

    private void releaseRender() {
        renderView.releaseRender();
    }

    private void setEraser(final int srcRid) {
        final BitmapFactory.Options option = new BitmapFactory.Options();
        option.inScaled = false;
        Bitmap imageBm = BitmapFactory.decodeResource(getResources(), srcRid, option);
        renderView.importImage(imageBm);
        renderView.setAlpha(1);

        Toast.makeText(this, "Start processing in 2 sec...", Toast.LENGTH_SHORT).show();
        renderView.postDelayed(new Runnable() {
            @Override
            public void run() {
                List<PointF> maskPoints = new ArrayList<>();
                maskPoints.add(new PointF(0.41f, .61f));
                maskPoints.add(new PointF(0.41f, .68f));
                renderView.renderMagicEraser(maskPoints, 50);

            }
        }, 2000);
    }

    private void setRadialMask() {
        Adjustment radialMask = new Adjustment();
        co.polarr.renderer.entities.Context.LocalState maskAdjustment = radialMask.adjustments;

        maskAdjustment.blur = 0.5f; // (0f, +1.5f)
        maskAdjustment.exposure = 0.5f; // (-1f, +1f)
        maskAdjustment.gamma = 0; // (-1f, +1f)
        maskAdjustment.temperature = 0.5f; // (-1f, +1f)
        maskAdjustment.tint = 0; // (-1f, +1f)
        maskAdjustment.saturation = 0; // (-1f, +1f)
        maskAdjustment.vibrance = 0; // (-1f, +1f)
        maskAdjustment.contrast = 0.3f; // (-1f, +1f)
        maskAdjustment.highlights = 0; // (-1f, +1f)
        maskAdjustment.shadows = -0.8f; // (-1f, +1f)
        maskAdjustment.clarity = 1f; // (-1f, +1f)
        maskAdjustment.mosaic_size = 0.2f; // (0, +1f)
        maskAdjustment.mosaic_pattern = "square";// "square","hexagon","dot","triangle","diamond",
        maskAdjustment.shadows_hue = 0; // (0, +1f)
        maskAdjustment.shadows_saturation = 0; // (0, +1f)
        maskAdjustment.dehaze = -0.2f; // (-1f, +1f)

        radialMask.type = "radial";
        radialMask.position = new float[]{0f, 0f}; // (-0.5f, +0.5f) from center of photo
        radialMask.size = new float[]{0.608f, 0.45f}; // (0f, +1f) width, height
        radialMask.feather = 0.1f;  // (0, +1f)
        radialMask.invert = true;

        radialMask.disabled = false;

        List<Adjustment> localMasks = new ArrayList<>();
        localMasks.add(radialMask);
        localStateMap.put("local_adjustments", localMasks);

        renderView.updateStates(localStateMap);
    }

    private void setGradientMask() {
        Adjustment gradientMask = new Adjustment();
        co.polarr.renderer.entities.Context.LocalState maskAdjustment = gradientMask.adjustments;

        maskAdjustment.blur = 0f; // (0f, +1.5f)
        maskAdjustment.exposure = -0.4f; // (-1f, +1f)
        maskAdjustment.gamma = 0; // (-1f, +1f)
        maskAdjustment.temperature = 0f; // (-1f, +1f)
        maskAdjustment.tint = 0; // (-1f, +1f)
        maskAdjustment.saturation = -1; // (-1f, +1f)
        maskAdjustment.vibrance = 0; // (-1f, +1f)
        maskAdjustment.contrast = -0.9f; // (-1f, +1f)
        maskAdjustment.highlights = 0; // (-1f, +1f)
        maskAdjustment.shadows = 0f; // (-1f, +1f)
        maskAdjustment.clarity = -1f; // (-1f, +1f)
        maskAdjustment.mosaic_size = 0f; // (0, +1f)
        maskAdjustment.shadows_hue = 0.6f; // (0, +1f)
        maskAdjustment.shadows_saturation = 0.5f; // (0, +1f)
        maskAdjustment.dehaze = 0f; // (-1f, +1f)

        gradientMask.type = "gradient";
        gradientMask.startPoint = new float[]{0.12f, -0.36f}; // (-0.5f, +0.5f) from center
        gradientMask.endPoint = new float[]{-0.096f, 0.26f}; // (-0.5f, +0.5f) from center
        gradientMask.reflect = true;
        gradientMask.invert = false;

        gradientMask.disabled = false;

        List<Adjustment> localMasks = new ArrayList<>();
        localMasks.add(gradientMask);
        localStateMap.put("local_adjustments", localMasks);

        renderView.updateStates(localStateMap);
    }

    private void setBrushMask(String mosaicType) {
        if(currentPoints.isEmpty()) {
            return;
        }
        Adjustment brushMask = new Adjustment();
        co.polarr.renderer.entities.Context.LocalState maskAdjustment = brushMask.adjustments;

//        maskAdjustment.exposure = 0.6f; // (-1f, +1f)
//        maskAdjustment.temperature = -0.8f; // (-1f, +1f)
        maskAdjustment.mosaic_size = 0.05f; // (0, +1f)
        maskAdjustment.mosaic_pattern = mosaicType;// "square","hexagon","dot","triangle","diamond",

        brushMask.type = "brush";
        brushMask.brush_mode = "mask"; // mask, paint
        BrushItem brushItem = new BrushItem();
        brushMask.brush.add(brushItem);

        brushItem.mode = "mask";  // mask, paint
        brushItem.blend = false;
        brushItem.erase = false;
        brushItem.channel = new float[]{1f, 0f, 0f, 0f}; //rgba
        brushItem.flow = 0.7f; // (0, +1f)
        brushItem.size = 0.3f; // (0, +1f)
        brushItem.spacing = 0.5f;
        brushItem.hardness = 1f;

        brushItem.touchPoints.addAll(currentPoints);

        brushMask.channel = new float[]{1f, 0f, 0f, 0f}; //rgba
        brushMask.invert = false;

        brushMask.disabled = false;

        List<Adjustment> localMasks = new ArrayList<>();
        localMasks.add(brushMask);
        localStateMap.put("local_adjustments", localMasks);
        renderView.updateBrushPoints(brushItem);
        renderView.updateStates(localStateMap);
    }

    private void setBrushBlurMask() {
        if(currentPoints.isEmpty()) {
            return;
        }
        Adjustment brushMask = new Adjustment();
        co.polarr.renderer.entities.Context.LocalState maskAdjustment = brushMask.adjustments;

//        maskAdjustment.exposure = 0.6f; // (-1f, +1f)
//        maskAdjustment.temperature = -0.8f; // (-1f, +1f)
        maskAdjustment.blur = 0.5f; // (0, +1f)

        brushMask.type = "brush";
        brushMask.brush_mode = "mask"; // mask, paint
        BrushItem brushItem = new BrushItem();
        brushMask.brush.add(brushItem);

        brushItem.mode = "mask";  // mask, paint
        brushItem.blend = false;
        brushItem.erase = false;
        brushItem.channel = new float[]{1f, 0f, 0f, 0f}; //rgba
        brushItem.flow = 0.7f; // (0, +1f)
        brushItem.size = 0.3f; // (0, +1f)
        brushItem.spacing = 0.5f;
        brushItem.hardness = 1f;
//        Float[] points = {
//                0.097f, 0.68f, 0.5f, 0.1045f, 0.6665f, 0.5f, 0.1125f, 0.653f, 0.5f, 0.122f, 0.6405f, 0.5f, 0.1315f, 0.6275f, 0.5f, 0.141f, 0.6145f, 0.5f, 0.15f, 0.6015f, 0.5f, 0.1595f, 0.589f, 0.5f, 0.169f, 0.576f, 0.5f, 0.179f, 0.5635f, 0.5f, 0.189f, 0.551f, 0.5f, 0.199f, 0.538f, 0.5f, 0.208f, 0.525f, 0.5f, 0.217f, 0.512f, 0.5f, 0.2265f, 0.4995f, 0.5f, 0.2365f, 0.4865f, 0.5f, 0.246f, 0.474f, 0.5f, 0.256f, 0.4615f, 0.5f, 0.2675f, 0.4495f, 0.5f, 0.277f, 0.4365f, 0.5f, 0.2865f, 0.424f, 0.5f, 0.297f, 0.4115f, 0.5f, 0.3075f, 0.399f, 0.5f, 0.3185f, 0.387f, 0.5f, 0.3295f, 0.375f, 0.5f, 0.34f, 0.363f, 0.5f, 0.351f, 0.3505f, 0.5f, 0.3615f, 0.338f, 0.5f, 0.371f, 0.3255f, 0.5f, 0.3805f, 0.3125f, 0.5f, 0.3885f, 0.299f, 0.5f, 0.397f, 0.286f, 0.5f, 0.4065f, 0.273f, 0.5f, 0.4165f, 0.2605f, 0.5f, 0.426f, 0.2475f, 0.5f, 0.435f, 0.235f, 0.5f, 0.447f, 0.223f, 0.5f, 0.4585f, 0.2115f, 0.5f, 0.4715f, 0.2005f, 0.5f, 0.4845f, 0.1895f, 0.5f, 0.4975f, 0.1785f, 0.5f, 0.5115f, 0.168f, 0.5f, 0.527f, 0.1595f, 0.5f, 0.543f, 0.151f, 0.5f, 0.556f, 0.14f, 0.5f, 0.5675f, 0.152f, 0.5f, 0.576f, 0.1655f, 0.5f, 0.5855f, 0.178f, 0.5f, 0.595f, 0.191f, 0.5f, 0.6045f, 0.204f, 0.5f, 0.6135f, 0.2165f, 0.5f, 0.6235f, 0.2295f, 0.5f, 0.633f, 0.242f, 0.5f, 0.6425f, 0.255f, 0.5f, 0.652f, 0.268f, 0.5f, 0.6615f, 0.2805f, 0.5f, 0.671f, 0.2935f, 0.5f, 0.6805f, 0.3065f, 0.5f, 0.6895f, 0.319f, 0.5f, 0.6995f, 0.332f, 0.5f, 0.71f, 0.3445f, 0.5f, 0.7215f, 0.356f, 0.5f, 0.733f, 0.368f, 0.5f, 0.744f, 0.38f, 0.5f, 0.754f, 0.3925f, 0.5f, 0.7645f, 0.405f, 0.5f, 0.773f, 0.4185f, 0.5f, 0.781f, 0.4315f, 0.5f, 0.7905f, 0.4445f, 0.5f, 0.8f, 0.457f, 0.5f, 0.811f, 0.4695f, 0.5f, 0.824f, 0.48f, 0.5f, 0.838f, 0.4905f, 0.5f, 0.8525f, 0.5005f, 0.5f, 0.866f, 0.511f, 0.5f, 0.8785f, 0.5225f, 0.5f, 0.8885f, 0.535f, 0.5f, 0.8965f, 0.548f, 0.5f, 0.887f, 0.561f, 0.5f, 0.869f, 0.567f, 0.5f, 0.8645f, 0.581f, 0.5f, 0.869f, 0.5955f, 0.5f, 0.876f, 0.609f, 0.5f, 0.8795f, 0.6235f, 0.5f, 0.879f, 0.638f, 0.5f, 0.874f, 0.6525f, 0.5f, 0.8695f, 0.6665f, 0.5f, 0.865f, 0.681f, 0.5f, 0.8585f, 0.6945f, 0.5f, 0.8515f, 0.7085f, 0.5f, 0.8445f, 0.722f, 0.5f, 0.8375f, 0.7355f, 0.5f, 0.83f, 0.7495f, 0.5f, 0.82f, 0.762f, 0.5f, 0.81f, 0.7745f, 0.5f, 0.799f, 0.7865f, 0.5f, 0.788f, 0.7985f, 0.5f, 0.7755f, 0.81f, 0.5f, 0.7635f, 0.8215f, 0.5f, 0.7505f, 0.8325f, 0.5f, 0.7355f, 0.842f, 0.5f, 0.7205f, 0.8515f, 0.5f, 0.7035f, 0.858f, 0.5f, 0.6855f, 0.8645f, 0.5f, 0.667f, 0.8685f, 0.5f, 0.648f, 0.873f, 0.5f, 0.6295f, 0.8765f, 0.5f, 0.61f, 0.8795f, 0.5f, 0.591f, 0.882f, 0.5f, 0.5715f, 0.884f, 0.5f, 0.552f, 0.8845f, 0.5f, 0.5325f, 0.885f, 0.5f, 0.513f, 0.8845f, 0.5f, 0.4935f, 0.8835f, 0.5f, 0.474f, 0.882f, 0.5f, 0.4545f, 0.88f, 0.5f, 0.435f, 0.8785f, 0.5f, 0.416f, 0.8765f, 0.5f, 0.397f, 0.873f, 0.5f, 0.378f, 0.869f, 0.5f, 0.359f, 0.8655f, 0.5f, 0.3405f, 0.8605f, 0.5f, 0.3225f, 0.8555f, 0.5f, 0.304f, 0.851f, 0.5f, 0.2865f, 0.844f, 0.5f, 0.2695f, 0.837f, 0.5f, 0.253f, 0.829f, 0.5f, 0.236f, 0.8215f, 0.5f, 0.2195f, 0.814f, 0.5f, 0.204f, 0.805f, 0.5f, 0.1885f, 0.796f, 0.5f, 0.1725f, 0.7875f, 0.5f, 0.158f, 0.7775f, 0.5f, 0.1435f, 0.768f, 0.5f, 0.129f, 0.7585f, 0.5f, 0.1165f, 0.747f, 0.5f, 0.1045f, 0.7355f, 0.5f, 0.096f, 0.722f, 0.5f, 0.0905f, 0.708f, 0.5f, 0.085f, 0.694f, 0.5f, 0.085f, 0.6795f, 0.5f, 0.0905f, 0.6655f, 0.5f, 0.0955f, 0.651f, 0.5f, 0.1075f, 0.6395f, 0.5f
//        };

        brushItem.touchPoints.addAll(currentPoints);

//        for (int i = 0; i < points.length; i += 3) {
//            brushItem.touchPoints.add(new PointF(points[i], points[i + 1]));
//        }


        brushMask.channel = new float[]{1f, 0f, 0f, 0f}; //rgba
        brushMask.invert = false;

        brushMask.disabled = false;

        List<Adjustment> localMasks = new ArrayList<>();
        localMasks.add(brushMask);
        localStateMap.put("local_adjustments", localMasks);

        renderView.updateBrushPoints(brushItem);
        renderView.updateStates(localStateMap);
    }

    private void setBrushPaint(String paintType) {
        Adjustment brushMask = new Adjustment();

//        co.polarr.renderer.entities.Context.LocalState maskAdjustment = brushMask.adjustments;
//        maskAdjustment.exposure = -0.6f; // (-1f, +1f)

        brushMask.type = "brush";
        brushMask.brush_mode = "paint"; // mask, paint
        BrushItem brushItem = new BrushItem();
        brushMask.brush.add(brushItem);

        brushItem.flow = 0.7f; // (0, +1f)
        brushItem.size = 0.7f; // (0, +1f)
        brushItem.interpolate = false;
        brushItem.mode = "paint"; // mask, paint
        brushItem.randomize = 0.25f;
        brushItem.spacing = 0.5f;
        brushItem.hardness = 1f;
        brushItem.texture = paintType; // "stroke_5","stroke_6"

        brushMask.disabled = false;

        List<Adjustment> localMasks = new ArrayList<>();
        localMasks.add(brushMask);
        localStateMap.put("local_adjustments", localMasks);

        renderView.updateStates(localStateMap);

        paintBrushItem = brushItem;
    }

    private void addBrushPaintPoint(PointF point) {
        if (paintBrushItem != null) {
            renderView.addBrushPathPoint(paintBrushItem, point);
            renderView.updateStates(localStateMap);
        }
    }

    private void showFilters() {
        if (mFilters == null) {
            List<FilterPackage> packages = FilterPackageUtil.GetAllFilters(getResources());
            mFilters = new ArrayList<>();
            for (FilterPackage filterPackage : packages) {
                mFilters.addAll(filterPackage.filters);
            }
        }
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        final CharSequence items[] = new CharSequence[mFilters.size()];
        for (int i = 0; i < mFilters.size(); i++) {
            items[i] = mFilters.get(i).filterName("zh");
        }
        adb.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int n) {
                sliderCon.setVisibility(View.VISIBLE);
                FilterItem filterItem = mFilters.get(n);

                localStateMap.clear();
                localStateMap.putAll(faceStates);
                localStateMap.putAll(filterItem.state);
                mCurrentFilter = filterItem;

                renderView.updateStates(filterItem.state);

                final String label = "Filter:" + filterItem.filterName("zh");
                labelTv.setText(label);
                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        float adjustmentValue = (float) progress / 100f;
//                        localStateMap.put(label.toString(), adjustmentValue);

                        if (mCurrentFilter != null) {
                            localStateMap.clear();
                            localStateMap.putAll(faceStates);
                            Map<String, Object> interpolateStates = FilterPackageUtil.GetInterpolateValue(mCurrentFilter.state, adjustmentValue);
                            localStateMap.putAll(interpolateStates);

                            renderView.updateStates(interpolateStates);
                        }

                        labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", label, adjustmentValue));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                seekbar.setProgress(100);

                dialog.dismiss();
            }

        });
        adb.setNegativeButton("Cancel", null);
        adb.setTitle("Choose a filter:");
        adb.show();
    }

    private void showList() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        final CharSequence items[] = new CharSequence[]{
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
                "grain_amount",
                "grain_size",
                "mosaic_square",
                "mosaic_hexagon",
                "mosaic_dot",
                "mosaic_triangle",
                "mosaic_diamond",
        };

        adb.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int n) {
                sliderCon.setVisibility(View.VISIBLE);
                CharSequence label = items[n];
                if (label.toString().startsWith("mosaic_")) {
                    String type = label.toString().substring("mosaic_".length());
                    localStateMap.put("mosaic_pattern", type);
                    renderView.updateStates(localStateMap);

                    label = "mosaic_size";
                    Toast.makeText(MainActivity.this, "Mosaic type: " + type + ", try to adjust 'mosaic_size'", Toast.LENGTH_LONG).show();
                }

                labelTv.setText(label);
                final CharSequence finalLabel = label;
                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        float adjustmentValue = (float) progress / 100f * 2f - 1f;
                        localStateMap.put(finalLabel.toString(), adjustmentValue);

                        labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", finalLabel, adjustmentValue));

                        renderView.updateStates(localStateMap);
                        renderView.requestRender();
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                if (localStateMap.containsKey(label.toString())) {
                    float adjustmentValue = (float) localStateMap.get(label.toString());
                    seekbar.setProgress((int) ((adjustmentValue + 1) / 2 * 100));
                    labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", label, adjustmentValue));
                } else {
                    seekbar.setProgress(50);
                }

                dialog.dismiss();
            }

        });
        adb.setNegativeButton("Cancel", null);
        adb.setTitle("Choose a type:");
        adb.show();
    }

    private void hideList() {
    }

    private void hideAll() {
        stopBrush();
        sliderCon.setVisibility(View.INVISIBLE);

        hideList();
    }
}