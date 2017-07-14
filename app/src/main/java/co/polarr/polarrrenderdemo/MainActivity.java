package co.polarr.polarrrenderdemo;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import co.polarr.qrcode.QRUtils;
import co.polarr.renderer.render.OnExportCallback;
import co.polarr.renderer.utils.QRCodeUtil;
import co.polarr.utils.FileUtils;
import co.polarr.utils.ImageLoadUtil;
import co.polarr.utils.Logger;
import co.polarr.utils.ThreadManager;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMPORT_PHOTO = 1;
    private static final int REQUEST_IMPORT_QR_PHOTO = 2;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int ACTIVITY_RESULT_QR_SCANNER = 2;
    private AppCompatSeekBar seekbar;
    private TextView labelTv;

    /**
     * Render View
     */
    private CustomRenderView renderView;
    /**
     * adjustment container
     */
    private View sliderCon;
    /**
     * save adjustment values
     */
    private Map<String, Integer> localStateMap = new HashMap<>();

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    hideAll();
                    showMenu();
                    return false;
                case R.id.navigation_list:
                    showList();
                    return false;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        // init render view
        renderView = (CustomRenderView) findViewById(R.id.render_view);

        // post init render view
        ThreadManager.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                initRenderView();
            }
        });

        sliderCon = findViewById(R.id.slider);
        sliderCon.setVisibility(View.GONE);

        labelTv = (TextView) findViewById(R.id.label_tv);
        seekbar = (AppCompatSeekBar) findViewById(R.id.seekbar);

    }

    private void importImageDemo() {
        renderView.importImage(BitmapFactory.decodeResource(getResources(), R.mipmap.demo));
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
                int orientation = ImageLoadUtil.getOrientation(this, uri);
                int maxTextureSize = ImageLoadUtil.getMaxTextureSize();
                Bitmap imageBm = ImageLoadUtil.decodeThumbBitmapFromUrl(this, uri, maxTextureSize, maxTextureSize, orientation);
                renderView.importImage(imageBm);
            }
        } else if (REQUEST_IMPORT_QR_PHOTO == requestCode && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                final String qrCodeData = QRUtils.decodeImageQRCode(this, FileUtils.getPath(this, uri));
                if (qrCodeData != null) {
                    ThreadManager.executeOnAsyncThread(new Runnable() {
                        @Override
                        public void run() {
                            String statesString = QRCodeUtil.requestQRJson(qrCodeData);
                            updateQrStates(statesString);
                        }
                    });
                }
            }
        } else if (ACTIVITY_RESULT_QR_SCANNER == requestCode && resultCode == RESULT_OK) {
            if (data == null || data.getStringExtra("value") == null) {
                return;
            }
            final String urlString = data.getStringExtra("value");

            ThreadManager.executeOnAsyncThread(new Runnable() {
                @Override
                public void run() {
                    String statesString = QRCodeUtil.requestQRJson(urlString);
                    updateQrStates(statesString);
                }
            });
        }
    }

    private void exportImage() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);

            return;
        }

        renderView.exportImageData(false, new OnExportCallback() {
            @Override
            public void onExport(Bitmap bitmap, byte[] array) {
                try {
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    File storageDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PolarrDemo");

                    //Creating the storage directory if it doesn't exist
                    if (!storageDirectory.exists()) {
                        storageDirectory.mkdirs();
                    }

                    //Creating the temporary storage file
                    File targetImagePath = File.createTempFile(timeStamp + "_", ".jpg", storageDirectory);

                    OutputStream outputStream = new FileOutputStream(targetImagePath);

                    outputStream.write(array);
                    outputStream.close();

                    //Rescanning the icon_library/gallery so it catches up with our own changes
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(Uri.fromFile(targetImagePath));
                    sendBroadcast(mediaScanIntent);

                    Toast.makeText(MainActivity.this, String.format(Locale.ENGLISH, "Saved to: %s", targetImagePath.getAbsolutePath()), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Logger.e("Cannot save exporting file to disk (" + e.toString() + ").");
                }
            }
        });
    }

    private void showQRScan() {
        Intent intent = new Intent(this, QRScannerActivity.class);
        startActivityForResult(intent, ACTIVITY_RESULT_QR_SCANNER);
    }

    private void updateQrStates(String statesString) {
        // reset to default
        renderView.resetAll();
        renderView.updateShaderWithStatesJson(statesString);
    }

    private void reset() {
        renderView.resetAll();
    }

    private void initRenderView() {
        renderView.setRenderBackgroundColor(Color.WHITE);
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
                        renderView.updateShader(label.toString(), adjustmentValue);
                        renderView.requestRender();

                        localStateMap.put(label.toString(), progress);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                if (localStateMap.containsKey(label.toString())) {

                    seekbar.setProgress(localStateMap.get(label.toString()));
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

    private void showMenu() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        final CharSequence items[] = new CharSequence[]{
                "Import image",
                "Import demo",
                "Reset image",
                "Export image",
                "Qr scan",
                "Import qr code image",
        };

        adb.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int n) {
                switch (n) {
                    case 0:
                        importImage();
                        break;
                    case 1:
                        importImageDemo();
                        break;
                    case 2:
                        reset();
                        break;
                    case 3:
                        exportImage();
                        break;
                    case 4:
                        showQRScan();
                        break;
                    case 5:
                        importQrImage();
                        break;
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
        sliderCon.setVisibility(View.GONE);

        hideList();
    }
}
