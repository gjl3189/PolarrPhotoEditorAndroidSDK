package co.polarr.polarrrenderdemo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import co.polarr.qrcode.QRUtils;
import co.polarr.renderer.FilterPackageUtil;
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
            case R.id.btn_filters:
                showFilters();
                break;
        }
    }

    @Override
    protected void onDestroy() {
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
        };


        adb.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int n) {
                sliderCon.setVisibility(View.VISIBLE);
                final CharSequence label = items[n];
                labelTv.setText(label);
                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        float adjustmentValue = (float) progress / 100f * 2f - 1f;
                        localStateMap.put(label.toString(), adjustmentValue);

                        labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", label, adjustmentValue));

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
