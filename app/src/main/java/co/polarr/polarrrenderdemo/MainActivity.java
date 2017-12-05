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
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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

                checkInitFilters();

                final Bitmap imageBm = BitmapFactory.decodeResource(getResources(), R.mipmap.person);
                Map<String, Object> randomFilterStates = mFilters.get((int) (Math.random() * mFilters.size())).state;
                polarrRenderThread.renderBitmap(imageBm, randomFilterStates, new RenderCallback() {
                    @Override
                    public void onRenderBitmap(final Bitmap bitmap) {
                        imageBm.recycle();

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
                        "stroke_1", "stroke_2", "stroke_3", "stroke_4", "dot", "speckles", "chalk"
                };
                adb.setItems(items, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int n) {
                        String mosaicType = items[n].toString();
                        setBrushPaint(mosaicType);
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
        final int inputWidth = imageBm.getWidth();
        final int inputHeight = imageBm.getHeight();
        renderView.importImage(imageBm);
        renderView.setAlpha(1);

        Toast.makeText(this, "Start processing in 2 sec...", Toast.LENGTH_SHORT).show();
        renderView.postDelayed(new Runnable() {
            @Override
            public void run() {
                List<PointF> maskPoints = new ArrayList<>();
                maskPoints.add(new PointF(0.41f, .61f));
                maskPoints.add(new PointF(0.41f, .68f));

                renderView.renderMagicEraser(maskPoints, 50f);
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
        Adjustment brushMask = new Adjustment();
        co.polarr.renderer.entities.Context.LocalState maskAdjustment = brushMask.adjustments;

        maskAdjustment.exposure = 0.6f; // (-1f, +1f)
        maskAdjustment.temperature = -0.8f; // (-1f, +1f)
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
        brushItem.flow = 0.5f; // (0, +1f)
        brushItem.hardness = 0.5f; // (0, +1f)
        brushItem.size = 0.5f; // (0, +1f)
//        Float[] points = {
//                0.097f, 0.68f, 0.5f, 0.1045f, 0.6665f, 0.5f, 0.1125f, 0.653f, 0.5f, 0.122f, 0.6405f, 0.5f, 0.1315f, 0.6275f, 0.5f, 0.141f, 0.6145f, 0.5f, 0.15f, 0.6015f, 0.5f, 0.1595f, 0.589f, 0.5f, 0.169f, 0.576f, 0.5f, 0.179f, 0.5635f, 0.5f, 0.189f, 0.551f, 0.5f, 0.199f, 0.538f, 0.5f, 0.208f, 0.525f, 0.5f, 0.217f, 0.512f, 0.5f, 0.2265f, 0.4995f, 0.5f, 0.2365f, 0.4865f, 0.5f, 0.246f, 0.474f, 0.5f, 0.256f, 0.4615f, 0.5f, 0.2675f, 0.4495f, 0.5f, 0.277f, 0.4365f, 0.5f, 0.2865f, 0.424f, 0.5f, 0.297f, 0.4115f, 0.5f, 0.3075f, 0.399f, 0.5f, 0.3185f, 0.387f, 0.5f, 0.3295f, 0.375f, 0.5f, 0.34f, 0.363f, 0.5f, 0.351f, 0.3505f, 0.5f, 0.3615f, 0.338f, 0.5f, 0.371f, 0.3255f, 0.5f, 0.3805f, 0.3125f, 0.5f, 0.3885f, 0.299f, 0.5f, 0.397f, 0.286f, 0.5f, 0.4065f, 0.273f, 0.5f, 0.4165f, 0.2605f, 0.5f, 0.426f, 0.2475f, 0.5f, 0.435f, 0.235f, 0.5f, 0.447f, 0.223f, 0.5f, 0.4585f, 0.2115f, 0.5f, 0.4715f, 0.2005f, 0.5f, 0.4845f, 0.1895f, 0.5f, 0.4975f, 0.1785f, 0.5f, 0.5115f, 0.168f, 0.5f, 0.527f, 0.1595f, 0.5f, 0.543f, 0.151f, 0.5f, 0.556f, 0.14f, 0.5f, 0.5675f, 0.152f, 0.5f, 0.576f, 0.1655f, 0.5f, 0.5855f, 0.178f, 0.5f, 0.595f, 0.191f, 0.5f, 0.6045f, 0.204f, 0.5f, 0.6135f, 0.2165f, 0.5f, 0.6235f, 0.2295f, 0.5f, 0.633f, 0.242f, 0.5f, 0.6425f, 0.255f, 0.5f, 0.652f, 0.268f, 0.5f, 0.6615f, 0.2805f, 0.5f, 0.671f, 0.2935f, 0.5f, 0.6805f, 0.3065f, 0.5f, 0.6895f, 0.319f, 0.5f, 0.6995f, 0.332f, 0.5f, 0.71f, 0.3445f, 0.5f, 0.7215f, 0.356f, 0.5f, 0.733f, 0.368f, 0.5f, 0.744f, 0.38f, 0.5f, 0.754f, 0.3925f, 0.5f, 0.7645f, 0.405f, 0.5f, 0.773f, 0.4185f, 0.5f, 0.781f, 0.4315f, 0.5f, 0.7905f, 0.4445f, 0.5f, 0.8f, 0.457f, 0.5f, 0.811f, 0.4695f, 0.5f, 0.824f, 0.48f, 0.5f, 0.838f, 0.4905f, 0.5f, 0.8525f, 0.5005f, 0.5f, 0.866f, 0.511f, 0.5f, 0.8785f, 0.5225f, 0.5f, 0.8885f, 0.535f, 0.5f, 0.8965f, 0.548f, 0.5f, 0.887f, 0.561f, 0.5f, 0.869f, 0.567f, 0.5f, 0.8645f, 0.581f, 0.5f, 0.869f, 0.5955f, 0.5f, 0.876f, 0.609f, 0.5f, 0.8795f, 0.6235f, 0.5f, 0.879f, 0.638f, 0.5f, 0.874f, 0.6525f, 0.5f, 0.8695f, 0.6665f, 0.5f, 0.865f, 0.681f, 0.5f, 0.8585f, 0.6945f, 0.5f, 0.8515f, 0.7085f, 0.5f, 0.8445f, 0.722f, 0.5f, 0.8375f, 0.7355f, 0.5f, 0.83f, 0.7495f, 0.5f, 0.82f, 0.762f, 0.5f, 0.81f, 0.7745f, 0.5f, 0.799f, 0.7865f, 0.5f, 0.788f, 0.7985f, 0.5f, 0.7755f, 0.81f, 0.5f, 0.7635f, 0.8215f, 0.5f, 0.7505f, 0.8325f, 0.5f, 0.7355f, 0.842f, 0.5f, 0.7205f, 0.8515f, 0.5f, 0.7035f, 0.858f, 0.5f, 0.6855f, 0.8645f, 0.5f, 0.667f, 0.8685f, 0.5f, 0.648f, 0.873f, 0.5f, 0.6295f, 0.8765f, 0.5f, 0.61f, 0.8795f, 0.5f, 0.591f, 0.882f, 0.5f, 0.5715f, 0.884f, 0.5f, 0.552f, 0.8845f, 0.5f, 0.5325f, 0.885f, 0.5f, 0.513f, 0.8845f, 0.5f, 0.4935f, 0.8835f, 0.5f, 0.474f, 0.882f, 0.5f, 0.4545f, 0.88f, 0.5f, 0.435f, 0.8785f, 0.5f, 0.416f, 0.8765f, 0.5f, 0.397f, 0.873f, 0.5f, 0.378f, 0.869f, 0.5f, 0.359f, 0.8655f, 0.5f, 0.3405f, 0.8605f, 0.5f, 0.3225f, 0.8555f, 0.5f, 0.304f, 0.851f, 0.5f, 0.2865f, 0.844f, 0.5f, 0.2695f, 0.837f, 0.5f, 0.253f, 0.829f, 0.5f, 0.236f, 0.8215f, 0.5f, 0.2195f, 0.814f, 0.5f, 0.204f, 0.805f, 0.5f, 0.1885f, 0.796f, 0.5f, 0.1725f, 0.7875f, 0.5f, 0.158f, 0.7775f, 0.5f, 0.1435f, 0.768f, 0.5f, 0.129f, 0.7585f, 0.5f, 0.1165f, 0.747f, 0.5f, 0.1045f, 0.7355f, 0.5f, 0.096f, 0.722f, 0.5f, 0.0905f, 0.708f, 0.5f, 0.085f, 0.694f, 0.5f, 0.085f, 0.6795f, 0.5f, 0.0905f, 0.6655f, 0.5f, 0.0955f, 0.651f, 0.5f, 0.1075f, 0.6395f, 0.5f
//        };

        Float[] points = {
                0.097f, 0.38f, 0.5f, 0.1045f, 0.3665f, 0.5f, 0.1125f, 0.353f, 0.5f, 0.122f, 0.3405f, 0.5f, 0.1315f, 0.3275f, 0.5f};

        brushItem.points = Arrays.asList(points); // point: {x, y, z} (0, +1f), 'z' means pressure

        brushMask.channel = new float[]{1f, 0f, 0f, 0f}; //rgba
        brushMask.invert = false;

        brushMask.disabled = false;

        List<Adjustment> localMasks = new ArrayList<>();
        localMasks.add(brushMask);
        localStateMap.put("local_adjustments", localMasks);

        Map<String, Object> interpolateStates = FilterPackageUtil.GetInterpolateValue(localStateMap, 0.5f);

        renderView.updateStates(interpolateStates);
    }

    private void setBrushPaint(String paintType) {
        Adjustment brushMask = new Adjustment();
        co.polarr.renderer.entities.Context.LocalState maskAdjustment = brushMask.adjustments;

        maskAdjustment.exposure = -0.6f; // (-1f, +1f)

        brushMask.type = "brush";
        brushMask.brush_mode = "paint"; // mask, paint
        BrushItem brushItem = new BrushItem();
        brushMask.brush.add(brushItem);

        brushItem.flow = 0.8f; // (0, +1f)
        brushItem.size = 0.5f; // (0, +1f)
        brushItem.mode = "paint"; // mask, paint
        brushItem.texture = paintType; // "stroke_1","stroke_2","stroke_3","stroke_4","dot","speckles","chalk"

//        Float[] points = {
//                0.189f, 0.8005f, 0.5f, 2.255f, 0.181f, 0.793f, 0.5f, 2.255f, 0.1725f, 0.7855f, 0.5f, 2.255f, 0.1645f, 0.778f, 0.5f, 2.255f, 0.156f, 0.77f, 0.5f, 2.238f, 0.148f, 0.7625f, 0.5f, 2.238f, 0.1405f, 0.7545f, 0.5f, 2.223f, 0.1325f, 0.747f, 0.5f, 2.223f, 0.1245f, 0.739f, 0.5f, 2.223f, 0.1175f, 0.731f, 0.5f, 2.1405f, 0.1105f, 0.7225f, 0.5f, 2.1405f, 0.104f, 0.714f, 0.5f, 2.0865f, 0.0975f, 0.7055f, 0.5f, 2.0865f, 0.0915f, 0.697f, 0.5f, 2.0565f, 0.0865f, 0.688f, 0.5f, 1.9715f, 0.0825f, 0.6785f, 0.5f, 1.855f, 0.0955f, 0.68f, 0.5f, -0.1185f, 0.1085f, 0.682f, 0.5f, -0.2545f, 0.1205f, 0.6855f, 0.5f, -0.356f, 0.1325f, 0.689f, 0.5f, -0.356f, 0.1445f, 0.693f, 0.5f, -0.416f, 0.1565f, 0.697f, 0.5f, -0.416f, 0.1685f, 0.701f, 0.5f, -0.4345f, 0.18f, 0.705f, 0.5f, -0.4345f, 0.1925f, 0.7085f, 0.5f, -0.364f, 0.2045f, 0.712f, 0.5f, -0.364f, 0.217f, 0.7155f, 0.5f, -0.353f, 0.229f, 0.7185f, 0.5f, -0.312f, 0.242f, 0.721f, 0.5f, -0.235f, 0.236f, 0.712f, 0.5f, 2.046f, 0.2285f, 0.704f, 0.5f, 2.177f, 0.221f, 0.696f, 0.5f, 2.177f, 0.2135f, 0.688f, 0.5f, 2.211f, 0.2055f, 0.6805f, 0.5f, 2.211f, 0.198f, 0.6725f, 0.5f, 2.211f, 0.19f, 0.6645f, 0.5f, 2.2135f, 0.182f, 0.657f, 0.5f, 2.2135f, 0.1745f, 0.649f, 0.5f, 2.2135f, 0.167f, 0.641f, 0.5f, 2.182f, 0.1595f, 0.633f, 0.5f, 2.182f, 0.152f, 0.625f, 0.5f, 2.185f, 0.1445f, 0.617f, 0.5f, 2.185f, 0.137f, 0.609f, 0.5f, 2.185f, 0.13f, 0.6005f, 0.5f, 2.109f, 0.1235f, 0.5925f, 0.5f, 2.109f, 0.117f, 0.584f, 0.5f, 2.109f, 0.1125f, 0.575f, 0.5f, 1.9165f, 0.111f, 0.565f, 0.5f, 1.681f, 0.124f, 0.5645f, 0.5f, 0.0665f, 0.1365f, 0.5675f, 0.5f, -0.307f, 0.1485f, 0.5705f, 0.5f, -0.35f, 0.161f, 0.574f, 0.5f, -0.35f, 0.173f, 0.5775f, 0.5f, -0.344f, 0.1855f, 0.5805f, 0.5f, -0.344f, 0.198f, 0.5835f, 0.5f, -0.3215f, 0.21f, 0.587f, 0.5f, -0.3215f, 0.2225f, 0.5895f, 0.5f, -0.304f, 0.2355f, 0.59f, 0.5f, -0.024f, 0.2355f, 0.58f, 0.5f, 1.588f, 0.2275f, 0.5725f, 0.5f, 2.252f, 0.2185f, 0.5655f, 0.5f, 2.32f, 0.2095f, 0.5585f, 0.5f, 2.32f, 0.2005f, 0.551f, 0.5f, 2.32f, 0.192f, 0.544f, 0.5f, 2.317f, 0.183f, 0.537f, 0.5f, 2.317f, 0.1745f, 0.5295f, 0.5f, 2.3f, 0.1655f, 0.522f, 0.5f, 2.3f, 0.157f, 0.515f, 0.5f, 2.3f, 0.1485f, 0.5075f, 0.5f, 2.268f, 0.14f, 0.5f, 0.5f, 2.268f, 0.132f, 0.4925f, 0.5f, 2.268f, 0.124f, 0.4845f, 0.5f, 2.199f, 0.1165f, 0.4765f, 0.5f, 2.199f, 0.11f, 0.468f, 0.5f, 2.082f, 0.1055f, 0.459f, 0.5f, 1.916f, 0.1185f, 0.4565f, 0.5f, 0.223f, 0.131f, 0.4585f, 0.5f, -0.2045f, 0.1435f, 0.462f, 0.5f, -0.3205f, 0.156f, 0.465f, 0.5f, -0.3205f, 0.1685f, 0.468f, 0.5f, -0.3205f, 0.1805f, 0.4715f, 0.5f, -0.356f, 0.1925f, 0.4745f, 0.5f, -0.356f, 0.205f, 0.4775f, 0.5f, -0.2875f, 0.2175f, 0.4805f, 0.5f, -0.2875f, 0.2305f, 0.482f, 0.5f, -0.179f, 0.2435f, 0.4825f, 0.5f, -0.0575f, 0.2565f, 0.4815f, 0.5f, 0.1155f, 0.269f, 0.478f, 0.5f, 0.35f, 0.262f, 0.4695f, 0.5f, 2.103f, 0.254f, 0.462f, 0.5f, 2.235f, 0.246f, 0.4545f, 0.5f, 2.235f, 0.2375f, 0.447f, 0.5f, 2.2965f, 0.229f, 0.4395f, 0.5f, 2.2965f, 0.22f, 0.4325f, 0.5f, 2.2965f, 0.211f, 0.4255f, 0.5f, 2.353f, 0.202f, 0.4185f, 0.5f, 2.353f, 0.1925f, 0.4115f, 0.5f, 2.353f, 0.1835f, 0.4045f, 0.5f, 2.328f, 0.1745f, 0.3975f, 0.5f, 2.328f, 0.166f, 0.39f, 0.5f, 2.293f, 0.1575f, 0.3825f, 0.5f, 2.293f, 0.1495f, 0.375f, 0.5f, 2.232f, 0.1615f, 0.3715f, 0.5f, 0.378f, 0.1745f, 0.373f, 0.5f, -0.1785f, 0.187f, 0.375f, 0.5f, -0.1785f, 0.1995f, 0.378f, 0.5f, -0.309f, 0.212f, 0.3805f, 0.5f, -0.309f, 0.2245f, 0.3835f, 0.5f, -0.279f, 0.237f, 0.386f, 0.5f, -0.279f, 0.2495f, 0.389f, 0.5f, -0.279f, 0.2625f, 0.3905f, 0.5f, -0.1925f, 0.275f, 0.3925f, 0.5f, -0.1925f, 0.288f, 0.3935f, 0.5f, -0.093f, 0.301f, 0.3945f, 0.5f, -0.093f, 0.314f, 0.393f, 0.5f, 0.165f, 0.3195f, 0.384f, 0.5f, 1.151f, 0.3135f, 0.375f, 0.5f, 2.0575f, 0.3045f, 0.368f, 0.5f, 2.3175f, 0.295f, 0.361f, 0.5f, 2.3675f, 0.286f, 0.3545f, 0.5f, 2.3675f, 0.276f, 0.3475f, 0.5f, 2.4015f, 0.2665f, 0.341f, 0.5f, 2.4015f, 0.257f, 0.3345f, 0.5f, 2.4015f, 0.2475f, 0.3275f, 0.5f, 2.367f, 0.2385f, 0.321f, 0.5f, 2.367f, 0.229f, 0.314f, 0.5f, 2.342f, 0.22f, 0.307f, 0.5f, 2.342f, 0.211f, 0.2995f, 0.5f, 2.3345f, 0.202f, 0.2925f, 0.5f, 2.3345f, 0.1935f, 0.285f, 0.5f, 2.265f, 0.1855f, 0.2775f, 0.5f, 2.265f, 0.1885f, 0.268f, 0.5f, 1.333f, 0.2015f, 0.269f, 0.5f, -0.0955f, 0.214f, 0.2715f, 0.5f, -0.279f, 0.2265f, 0.2745f, 0.5f, -0.279f, 0.2385f, 0.278f, 0.5f, -0.3815f, 0.2505f, 0.2815f, 0.5f, -0.3815f, 0.263f, 0.2855f, 0.5f, -0.3865f, 0.275f, 0.289f, 0.5f, -0.3865f, 0.287f, 0.2925f, 0.5f, -0.339f, 0.2995f, 0.2955f, 0.5f, -0.339f, 0.312f, 0.2975f, 0.5f, -0.224f, 0.325f, 0.3f, 0.5f, -0.224f, 0.3365f, 0.2955f, 0.5f, 0.449f, 0.3305f, 0.287f, 0.5f, 2.067f, 0.3215f, 0.28f, 0.5f, 2.321f, 0.3125f, 0.2725f, 0.5f, 2.325f, 0.3035f, 0.2655f, 0.5f, 2.325f, 0.2945f, 0.2585f, 0.5f, 2.325f, 0.286f, 0.2515f, 0.5f, 2.3265f, 0.277f, 0.2445f, 0.5f, 2.3265f, 0.268f, 0.237f, 0.5f, 2.3265f, 0.2595f, 0.23f, 0.5f, 2.2975f, 0.2505f, 0.2225f, 0.5f, 2.2975f, 0.242f, 0.2155f, 0.5f, 2.2975f, 0.234f, 0.2075f, 0.5f, 2.254f, 0.2255f, 0.2f, 0.5f, 2.254f, 0.2175f, 0.1925f, 0.5f, 2.254f, 0.2095f, 0.185f, 0.5f, 2.2365f, 0.201f, 0.177f, 0.5f, 2.2365f, 0.193f, 0.1695f, 0.5f, 2.2365f, 0.186f, 0.161f, 0.5f, 2.1335f, 0.179f, 0.153f, 0.5f, 2.1335f, 0.1775f, 0.143f, 0.5f, 1.6945f, 0.19f, 0.1465f, 0.5f, -0.33f, 0.2025f, 0.1495f, 0.5f, -0.33f, 0.214f, 0.1535f, 0.5f, -0.423f, 0.226f, 0.1575f, 0.5f, -0.423f, 0.238f, 0.1615f, 0.5f, -0.395f, 0.25f, 0.165f, 0.5f, -0.395f, 0.2625f, 0.1685f, 0.5f, -0.364f, 0.2745f, 0.172f, 0.5f, -0.364f, 0.287f, 0.175f, 0.5f, -0.333f, 0.299f, 0.1785f, 0.5f, -0.333f, 0.312f, 0.1805f, 0.5f, -0.2395f, 0.3245f, 0.183f, 0.5f, -0.2395f, 0.3375f, 0.1835f, 0.5f, -0.0495f, 0.35f, 0.181f, 0.5f, 0.268f, 0.3425f, 0.173f, 0.5f, 2.1965f, 0.3345f, 0.165f, 0.5f, 2.2065f, 0.327f, 0.1575f, 0.5f, 2.2135f, 0.319f, 0.1495f, 0.5f, 2.2135f, 0.3115f, 0.1415f, 0.5f, 2.197f, 0.305f, 0.133f, 0.5f, 2.096f, 0.318f, 0.134f, 0.5f, -0.103f, 0.3305f, 0.136f, 0.5f, -0.2225f, 0.343f, 0.1385f, 0.5f, -0.2225f, 0.356f, 0.1405f, 0.5f, -0.237f, 0.3685f, 0.143f, 0.5f, -0.237f, 0.381f, 0.145f, 0.5f, -0.237f, 0.394f, 0.147f, 0.5f, -0.1835f, 0.407f, 0.149f, 0.5f, -0.1835f, 0.4195f, 0.1505f, 0.5f, -0.1835f, 0.4325f, 0.1515f, 0.5f, -0.112f, 0.4455f, 0.153f, 0.5f, -0.112f, 0.4585f, 0.1535f, 0.5f, -0.069f, 0.4715f, 0.153f, 0.5f, 0.0375f, 0.462f, 0.1465f, 0.5f, 2.375f, 0.452f, 0.1405f, 0.5f, 2.4775f, 0.4415f, 0.1345f, 0.5f, 2.486f, 0.431f, 0.1285f, 0.5f, 2.486f, 0.4215f, 0.122f, 0.5f, 2.4305f, 0.434f, 0.1235f, 0.5f, -0.183f, 0.447f, 0.1255f, 0.5f, -0.183f, 0.4595f, 0.128f, 0.5f, -0.2655f, 0.472f, 0.1305f, 0.5f, -0.2655f, 0.4845f, 0.133f, 0.5f, -0.2655f, 0.497f, 0.136f, 0.5f, -0.2705f, 0.5095f, 0.1385f, 0.5f, -0.2705f, 0.5225f, 0.141f, 0.5f, -0.2705f, 0.535f, 0.1435f, 0.5f, -0.262f, 0.5475f, 0.146f, 0.5f, -0.262f, 0.56f, 0.1485f, 0.5f, -0.262f, 0.5725f, 0.1515f, 0.5f, -0.289f, 0.585f, 0.154f, 0.5f, -0.289f, 0.597f, 0.1575f, 0.5f, -0.3635f, 0.6095f, 0.161f, 0.5f, -0.3635f, 0.621f, 0.166f, 0.5f, -0.4965f, 0.6315f, 0.1715f, 0.5f, -0.639f, 0.6185f, 0.1695f, 0.5f, 2.928f, 0.607f, 0.165f, 0.5f, 2.6785f, 0.595f, 0.161f, 0.5f, 2.6945f, 0.5835f, 0.1565f, 0.5f, 2.6945f, 0.5715f, 0.1525f, 0.5f, 2.72f, 0.5835f, 0.1565f, 0.5f, -0.3875f, 0.5955f, 0.16f, 0.5f, -0.3695f, 0.608f, 0.1635f, 0.5f, -0.3695f, 0.62f, 0.167f, 0.5f, -0.3695f, 0.631f, 0.1725f, 0.5f, -0.593f, 0.6415f, 0.178f, 0.5f, -0.593f, 0.6525f, 0.1835f, 0.5f, -0.593f, 0.6635f, 0.1885f, 0.5f, -0.5635f, 0.6745f, 0.194f, 0.5f, -0.5635f, 0.685f, 0.1995f, 0.5f, -0.5995f, 0.696f, 0.205f, 0.5f, -0.5995f, 0.7065f, 0.2105f, 0.5f, -0.64f, 0.717f, 0.2165f, 0.5f, -0.64f, 0.7265f, 0.2235f, 0.5f, -0.7605f, 0.736f, 0.23f, 0.5f, -0.7605f, 0.7425f, 0.2385f, 0.5f, -1.0395f, 0.739f, 0.248f, 0.5f, -1.8465f, 0.73f, 0.255f, 0.5f, -2.343f, 0.718f, 0.259f, 0.5f, -2.6955f, 0.706f, 0.2635f, 0.5f, -2.6955f, 0.6935f, 0.265f, 0.5f, -2.9495f, 0.6805f, 0.267f, 0.5f, -2.982f, 0.6675f, 0.2685f, 0.5f, -2.982f, 0.655f, 0.27f, 0.5f, -2.98f, 0.642f, 0.272f, 0.5f, -2.9395f, 0.655f, 0.2735f, 0.5f, -0.1885f, 0.6675f, 0.2755f, 0.5f, -0.1885f, 0.6805f, 0.2775f, 0.5f, -0.1905f, 0.693f, 0.279f, 0.5f, -0.1905f, 0.706f, 0.281f, 0.5f, -0.1905f, 0.7185f, 0.2835f, 0.5f, -0.2385f, 0.7315f, 0.2855f, 0.5f, -0.2385f, 0.744f, 0.2885f, 0.5f, -0.2805f, 0.7565f, 0.291f, 0.5f, -0.2805f, 0.7685f, 0.2945f, 0.5f, -0.3455f, 0.781f, 0.2975f, 0.5f, -0.3455f, 0.7925f, 0.302f, 0.5f, -0.4495f, 0.78f, 0.304f, 0.5f, -2.942f, 0.767f, 0.306f, 0.5f, -2.942f, 0.754f, 0.3065f, 0.5f, -3.0655f, 0.741f, 0.3075f, 0.5f, -3.0655f, 0.728f, 0.308f, 0.5f, -3.0675f, 0.715f, 0.309f, 0.5f, -3.0675f, 0.702f, 0.31f, 0.5f, -3.002f, 0.711f, 0.3175f, 0.5f, -0.848f, 0.7215f, 0.323f, 0.5f, -0.622f, 0.732f, 0.329f, 0.5f, -0.622f, 0.743f, 0.334f, 0.5f, -0.5585f, 0.754f, 0.339f, 0.5f, -0.5585f, 0.765f, 0.3445f, 0.5f, -0.5585f, 0.776f, 0.35f, 0.5f, -0.5935f, 0.787f, 0.3555f, 0.5f, -0.5935f, 0.7965f, 0.3615f, 0.5f, -0.716f, 0.8065f, 0.368f, 0.5f, -0.716f, 0.814f, 0.376f, 0.5f, -0.964f, 0.817f, 0.3855f, 0.5f, -1.316f, 0.806f, 0.391f, 0.5f, -2.58f, 0.795f, 0.396f, 0.5f, -2.58f, 0.782f, 0.397f, 0.5f, -3.0265f, 0.769f, 0.398f, 0.5f, -3.059f, 0.756f, 0.399f, 0.5f, -3.059f, 0.743f, 0.3995f, 0.5f, -3.06f, 0.73f, 0.4005f, 0.5f, -3.06f, 0.7175f, 0.402f, 0.5f, -2.992f, 0.7245f, 0.41f, 0.5f, -0.999f, 0.7345f, 0.4165f, 0.5f, -0.692f, 0.7445f, 0.4225f, 0.5f, -0.692f, 0.7545f, 0.4285f, 0.5f, -0.6525f, 0.765f, 0.4345f, 0.5f, -0.6525f, 0.775f, 0.4405f, 0.5f, -0.6815f, 0.7855f, 0.447f, 0.5f, -0.6815f, 0.7945f, 0.454f, 0.5f, -0.816f, 0.7935f, 0.4635f, 0.5f, -1.6405f, 0.7805f, 0.464f, 0.5f, -3.131f, 0.7675f, 0.4625f, 0.5f, 3.0325f, 0.7545f, 0.4615f, 0.5f, 3.0375f, 0.7415f, 0.4605f, 0.5f, 3.0375f, 0.7285f, 0.4595f, 0.5f, 3.0375f, 0.7155f, 0.4595f, 0.5f, 3.109f, 0.7025f, 0.4595f, 0.5f, -3.122f, 0.712f, 0.4665f, 0.5f, -0.766f, 0.721f, 0.473f, 0.5f, -0.766f, 0.7325f, 0.4785f, 0.5f, -0.5575f, 0.7435f, 0.4835f, 0.5f, -0.5575f, 0.7545f, 0.4885f, 0.5f, -0.5575f, 0.7655f, 0.4935f, 0.5f, -0.54f, 0.777f, 0.4985f, 0.5f, -0.54f, 0.7875f, 0.504f, 0.5f, -0.6005f, 0.7985f, 0.5095f, 0.5f, -0.6005f, 0.808f, 0.516f, 0.5f, -0.703f, 0.818f, 0.5225f, 0.5f, -0.703f, 0.8155f, 0.532f, 0.5f, -1.767f, 0.8035f, 0.5355f, 0.5f, -2.785f, 0.7905f, 0.5375f, 0.5f, -2.928f, 0.778f, 0.54f, 0.5f, -2.903f, 0.7845f, 0.548f, 0.5f, -1.028f, 0.7915f, 0.5565f, 0.5f, -1.028f, 0.802f, 0.5625f, 0.5f, -0.6515f, 0.812f, 0.5685f, 0.5f, -0.697f, 0.822f, 0.575f, 0.5f, -0.697f, 0.831f, 0.582f, 0.5f, -0.808f, 0.84f, 0.589f, 0.5f, -0.808f, 0.847f, 0.5975f, 0.5f, -1.009f, 0.839f, 0.6055f, 0.5f, -2.2045f, 0.826f, 0.606f, 0.5f, -3.059f, 0.813f, 0.6065f, 0.5f, -3.1025f, 0.8f, 0.6085f, 0.5f, -2.917f, 0.7935f, 0.617f, 0.5f, -2.0995f, 0.7945f, 0.627f, 0.5f, -1.4955f, 0.8f, 0.6355f, 0.5f, -1.119f, 0.8065f, 0.644f, 0.5f, -1.06f, 0.813f, 0.653f, 0.5f, -1.06f, 0.819f, 0.6615f, 0.5f, -1.1f, 0.825f, 0.67f, 0.5f, -1.1f, 0.8295f, 0.679f, 0.5f, -1.186f, 0.8345f, 0.6885f, 0.5f, -1.186f, 0.837f, 0.698f, 0.5f, -1.38f, 0.8395f, 0.7075f, 0.5f, -1.38f, 0.836f, 0.717f, 0.5f, -1.857f, 0.827f, 0.724f, 0.5f, -2.3185f, 0.818f, 0.731f, 0.5f, -2.3185f, 0.806f, 0.735f, 0.5f, -2.7615f, 0.7935f, 0.738f, 0.5f, -2.822f, 0.781f, 0.741f, 0.5f, -2.822f, 0.769f, 0.744f, 0.5f, -2.805f, 0.7745f, 0.753f, 0.5f, -1.119f, 0.7845f, 0.7595f, 0.5f, -0.723f, 0.794f, 0.7665f, 0.5f, -0.764f, 0.802f, 0.774f, 0.5f, -0.881f, 0.8105f, 0.7815f, 0.5f, -0.881f, 0.815f, 0.7905f, 0.5f, -1.2165f, 0.814f, 0.8005f, 0.5f, -1.632f, 0.8075f, 0.8085f, 0.5f, -2.106f, 0.7975f, 0.815f, 0.5f, -2.434f, 0.7865f, 0.8205f, 0.5f, -2.549f, 0.7765f, 0.8265f, 0.5f, -2.462f, 0.7815f, 0.836f, 0.5f, -1.201f, 0.792f, 0.841f, 0.5f, -0.586f, 0.804f, 0.8455f, 0.5f, -0.449f, 0.8155f, 0.85f, 0.5f, -0.491f, 0.8255f, 0.8565f, 0.5f, -0.7005f, 0.814f, 0.8615f, 0.5f, -2.603f, 0.8015f, 0.8635f, 0.5f, -2.9175f, 0.789f, 0.866f, 0.5f, -2.9175f, 0.776f, 0.8665f, 0.5f, -3.082f, 0.7625f, 0.867f, 0.5f, -3.082f, 0.7495f, 0.8675f, 0.5f, -3.102f, 0.7365f, 0.868f, 0.5f, -3.052f, 0.724f, 0.87f, 0.5f, -2.978f, 0.7345f, 0.8755f, 0.5f, -0.597f, 0.7445f, 0.8815f, 0.5f, -0.6985f, 0.7395f, 0.8905f, 0.5f, -1.9785f, 0.7285f, 0.896f, 0.5f, -2.564f, 0.716f, 0.899f, 0.5f, -2.83f, 0.7035f, 0.902f, 0.5f, -2.83f, 0.691f, 0.903f, 0.5f, -3.025f, 0.678f, 0.904f, 0.5f, -3.025f, 0.665f, 0.9045f, 0.5f, -3.0975f, 0.652f, 0.905f, 0.5f, -3.0975f, 0.639f, 0.9055f, 0.5f, -3.1135f, 0.626f, 0.9065f, 0.5f, -3.0285f, 0.6385f, 0.909f, 0.5f, -0.265f, 0.651f, 0.9115f, 0.5f, -0.2635f, 0.6635f, 0.914f, 0.5f, -0.2635f, 0.676f, 0.917f, 0.5f, -0.324f, 0.687f, 0.922f, 0.5f, -0.5295f, 0.6745f, 0.924f, 0.5f, -2.9255f, 0.6615f, 0.9245f, 0.5f, -3.1315f, 0.6485f, 0.9245f, 0.5f, -3.1315f, 0.6355f, 0.923f, 0.5f, 2.9975f, 0.6225f, 0.9215f, 0.5f, 2.9975f, 0.61f, 0.919f, 0.5f, 2.8605f, 0.5975f, 0.916f, 0.5f, 2.8605f, 0.5855f, 0.9125f, 0.5f, 2.7315f, 0.574f, 0.908f, 0.5f, 2.6955f, 0.5625f, 0.9035f, 0.5f, 2.626f, 0.5515f, 0.898f, 0.5f, 2.6005f, 0.551f, 0.8885f, 0.5f, 1.579f, 0.56f, 0.881f, 0.5f, 0.8345f, 0.57f, 0.8745f, 0.5f, 0.722f, 0.578f, 0.867f, 0.5f, 0.8895f, 0.583f, 0.858f, 0.5f, 1.194f, 0.5775f, 0.849f, 0.5f, 1.995f, 0.5675f, 0.843f, 0.5f, 2.4735f, 0.555f, 0.8395f, 0.5f, 2.776f, 0.5425f, 0.8375f, 0.5f, 2.9635f, 0.5295f, 0.836f, 0.5f, 2.9635f, 0.5165f, 0.8355f, 0.5f, 3.111f, 0.5035f, 0.8365f, 0.5f, -3.0595f, 0.4905f, 0.8375f, 0.5f, -3.0595f, 0.478f, 0.8395f, 0.5f, -2.9075f, 0.465f, 0.842f, 0.5f, -2.9075f, 0.4525f, 0.8445f, 0.5f, -2.856f, 0.44f, 0.8475f, 0.5f, -2.856f, 0.428f, 0.8505f, 0.5f, -2.8045f, 0.4155f, 0.854f, 0.5f, -2.8045f, 0.403f, 0.856f, 0.5f, -2.8925f, 0.3975f, 0.847f, 0.5f, 1.9905f, 0.4025f, 0.838f, 0.5f, 1.1835f, 0.4085f, 0.8295f, 0.5f, 1.1035f, 0.4145f, 0.821f, 0.5f, 1.0855f, 0.4205f, 0.812f, 0.5f, 1.104f, 0.4105f, 0.8185f, 0.5f, -2.4425f, 0.401f, 0.825f, 0.5f, -2.3665f, 0.3915f, 0.832f, 0.5f, -2.3665f, 0.3825f, 0.839f, 0.5f, -2.367f, 0.373f, 0.8455f, 0.5f, -2.367f, 0.3635f, 0.852f, 0.5f, -2.4065f, 0.352f, 0.8575f, 0.5f, -2.5995f, 0.341f, 0.8625f, 0.5f, -2.5995f, 0.328f, 0.8625f, 0.5f, -3.102f, 0.3165f, 0.8585f, 0.5f, 2.678f, 0.309f, 0.8505f, 0.5f, 2.188f, 0.3075f, 0.8405f, 0.5f, 1.678f, 0.311f, 0.8315f, 0.5f, 1.2845f, 0.3145f, 0.822f, 0.5f, 1.2845f, 0.322f, 0.814f, 0.5f, 0.974f, 0.3295f, 0.8055f, 0.5f, 0.974f, 0.3385f, 0.799f, 0.5f, 0.791f, 0.3475f, 0.792f, 0.5f, 0.791f, 0.3585f, 0.786f, 0.5f, 0.6155f, 0.351f, 0.794f, 0.5f, -2.178f, 0.3435f, 0.802f, 0.5f, -2.178f, 0.333f, 0.808f, 0.5f, -2.5115f, 0.3225f, 0.814f, 0.5f, -2.5115f, 0.311f, 0.8185f, 0.5f, -2.6285f, 0.2995f, 0.8235f, 0.5f, -2.6285f, 0.288f, 0.8275f, 0.5f, -2.706f, 0.276f, 0.8315f, 0.5f, -2.706f, 0.2635f, 0.8345f, 0.5f, -2.819f, 0.2515f, 0.838f, 0.5f, -2.819f, 0.2385f, 0.8395f, 0.5f, -2.99f, 0.2255f, 0.8395f, 0.5f, -3.1225f, 0.2125f, 0.8395f, 0.5f, -3.1225f, 0.1995f, 0.837f, 0.5f, 2.889f, 0.189f, 0.8315f, 0.5f, 2.548f, 0.1905f, 0.822f, 0.5f, 1.446f, 0.1975f, 0.8135f, 0.5f, 1.024f, 0.206f, 0.8065f, 0.5f, 0.8565f, 0.2145f, 0.799f, 0.5f, 0.8565f, 0.2245f, 0.793f, 0.5f, 0.669f, 0.214f, 0.787f, 0.5f, 2.519f, 0.202f, 0.783f, 0.5f, 2.711f, 0.1905f, 0.779f, 0.5f, 2.711f, 0.1785f, 0.775f, 0.5f, 2.732f};

        Float[] points = {
                0.097f, 0.38f, 0.5f, 2.732f, 0.1045f, 0.3665f, 0.5f, 2.732f, 0.1125f, 0.353f, 0.5f, 2.732f, 0.122f, 0.3405f, 0.5f, 2.732f, 0.1315f, 0.3275f, 0.5f, 2.732f};

        brushItem.points = Arrays.asList(points); // point: {x, y, p, d} (0, +1f), 'p' means pressure, 'd' means direction (-π，+π)

        brushMask.disabled = false;

        List<Adjustment> localMasks = new ArrayList<>();
        localMasks.add(brushMask);
        localStateMap.put("local_adjustments", localMasks);

        renderView.updateStates(localStateMap);
    }

    private void checkInitFilters() {
        if (mFilters == null) {
            List<FilterPackage> packages = FilterPackageUtil.GetAllFilters(getResources());
            mFilters = new ArrayList<>();
            for (FilterPackage filterPackage : packages) {
                for (FilterItem filterItem : filterPackage.filters) {
                    // easy to show in demo
                    filterItem.name = filterPackage.packageName("zh") + "_" + filterItem.filterName("zh");
                }
                mFilters.addAll(filterPackage.filters);
            }
        }
    }

    private void showFilters() {
        checkInitFilters();
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        final CharSequence items[] = new CharSequence[mFilters.size()];
        for (int i = 0; i < mFilters.size(); i++) {
            items[i] = mFilters.get(i).name.toString();
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

                final String label = "Filter:" + filterItem.name;
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
        sliderCon.setVisibility(View.INVISIBLE);

        hideList();
    }
}
