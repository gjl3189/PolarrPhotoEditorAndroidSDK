package co.polarr.polarrrenderdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.support.annotation.RawRes;

import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.polarr.renderer.entities.FaceItem;

/**
 * Created by Colin on 2017/10/27.
 */

public class FaceUtil {
    private static FaceDet mFaceDet;

    /**
     * Init facedet module.
     * Copy module file to local path at first time
     */
    public static void InitFaceUtil(Context context) {
        if (mFaceDet != null) {
            return;
        }
        final String targetPath = context.getFilesDir().getAbsolutePath() + "face.dat";
        if (!new File(targetPath).exists()) {
            //copy face module to sd card
            CopyFileFromRawToOthers(context, R.raw.face, targetPath);
        }
        // init the statics
        if (mFaceDet == null) {
            try {
                mFaceDet = new FaceDet(targetPath);
            } catch (java.lang.UnsatisfiedLinkError e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * call after face detected
     */
    public static void Release() {
        mFaceDet.release();
        mFaceDet = null;
    }

    /**
     * detect face
     *
     * @param sourceBitmap source image max width / height 500px and ARGB8888 will get better result
     */
    public static Map<String, Object> DetectFace(final Bitmap sourceBitmap) {
        Map<String, Object> faces = new HashMap<>();

        if (sourceBitmap == null || mFaceDet == null) {
            return faces;
        }

        Bitmap processMap;
        if (sourceBitmap.getConfig() == Bitmap.Config.ARGB_8888) {
            processMap = sourceBitmap;
        } else {
            processMap = Bitmap.createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(processMap);
            canvas.drawBitmap(sourceBitmap, 0, 0, null);
        }

        int bitmapWidth = processMap.getWidth();
        int bitmapHeight = processMap.getHeight();
        try {
            List<VisionDetRet> faceList = mFaceDet.detect(processMap);
            faces = CreateFaceStates(faceList, bitmapWidth, bitmapHeight);
        } catch (Exception | Error e) {
            e.printStackTrace();
        }

        if (processMap != sourceBitmap) {
            processMap.recycle();
        }

        return faces;
    }

    /**
     * reset face states to default
     *
     * @param faceStates
     */
    public static void ResetFaceStates(Map<String, Object> faceStates) {
        List<FaceItem> faces = (List<FaceItem>) faceStates.get("faces");
        List<co.polarr.renderer.entities.Context.FaceFeaturesState> faceFeatures = new ArrayList<>();

        if (faces != null) {
            for (FaceItem faceItem : faces) {
                faceItem.adjustments = new co.polarr.renderer.entities.Context.FaceState();
                faceFeatures.add(new co.polarr.renderer.entities.Context.FaceFeaturesState());
            }

            faceStates.put("face_features", faceFeatures);
        }
    }

    /**
     * create a object to request js
     *
     * @param faceList     detect result
     * @param bitmapWidth  source image width
     * @param bitmapHeight source image height
     */
    private static Map<String, Object> CreateFaceStates(List<VisionDetRet> faceList, int bitmapWidth, int bitmapHeight) {
        Map<String, Object> faceStates = new HashMap<>();
        List<FaceItem> faces = new ArrayList<>();
        List<co.polarr.renderer.entities.Context.FaceFeaturesState> faceFeatures = new ArrayList<>();

        faceStates.put("faces", faces);
        faceStates.put("face_features", faceFeatures);

        assert faceList != null;
        for (VisionDetRet visionDetRet : faceList) {
            FaceItem faceItem = new FaceItem();
            ArrayList<Point> markArray = visionDetRet.getFaceLandmarks();
            float keyPoints[][] = new float[markArray.size()][];
            for (int i = 0; i < markArray.size(); i++) {
                Point point = markArray.get(i);
                ;
                keyPoints[i] = new float[]{
                        point.x / (float) bitmapWidth,
                        point.y / (float) bitmapHeight,
                };
            }
            faceItem.markers = keyPoints;
            faceItem.boundaries = new float[]{
                    visionDetRet.getLeft() / (float) bitmapWidth,
                    visionDetRet.getTop() / (float) bitmapHeight,
                    (visionDetRet.getRight() - visionDetRet.getLeft()) / (float) bitmapWidth,
                    (visionDetRet.getBottom() - visionDetRet.getTop()) / (float) bitmapHeight
            };
            faces.add(faceItem);

            faceFeatures.add(new co.polarr.renderer.entities.Context.FaceFeaturesState());
        }

        return faceStates;
    }

    /**
     * Copy raw file to target path
     */
    private static void CopyFileFromRawToOthers(final Context context, @RawRes int id, final String targetPath) {
        InputStream in = context.getResources().openRawResource(id);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(targetPath);
            byte[] buff = new byte[1024];
            int read = 0;
            while ((read = in.read(buff)) > 0) {
                out.write(buff, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
