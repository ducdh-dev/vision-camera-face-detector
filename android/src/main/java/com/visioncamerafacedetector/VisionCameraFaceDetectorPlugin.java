package com.visioncamerafacedetector;

import static com.visioncamerafacedetector.Constant.TF_OD_API_INPUT_SIZE;
import static java.lang.Math.ceil;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;

import androidx.camera.core.ImageProxy;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.common.internal.ImageConvertUtils;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.mrousavy.camera.frameprocessor.FrameProcessorPlugin;

import java.util.List;

public class VisionCameraFaceDetectorPlugin extends FrameProcessorPlugin {

  FaceDetectorOptions options =
    new FaceDetectorOptions.Builder()
      .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
      .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
      .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
      .setMinFaceSize(0.15f)
      .build();

  FaceDetector faceDetector = FaceDetection.getClient(options);

  private WritableMap processBoundingBox(Rect boundingBox) {
    WritableMap bounds = Arguments.createMap();

    bounds.putDouble("top", boundingBox.top);
    bounds.putDouble("left", boundingBox.left);
    bounds.putDouble("width", boundingBox.width());
    bounds.putDouble("height", boundingBox.height());

    return bounds;
  }

  private WritableMap processFaceContours(Face face) {
    // All faceContours
    int[] faceContoursTypes =
      new int[]{
        FaceContour.FACE,
        FaceContour.LEFT_EYEBROW_TOP,
        FaceContour.LEFT_EYEBROW_BOTTOM,
        FaceContour.RIGHT_EYEBROW_TOP,
        FaceContour.RIGHT_EYEBROW_BOTTOM,
        FaceContour.LEFT_EYE,
        FaceContour.RIGHT_EYE,
        FaceContour.UPPER_LIP_TOP,
        FaceContour.UPPER_LIP_BOTTOM,
        FaceContour.LOWER_LIP_TOP,
        FaceContour.LOWER_LIP_BOTTOM,
        FaceContour.NOSE_BRIDGE,
        FaceContour.NOSE_BOTTOM,
        FaceContour.LEFT_CHEEK,
        FaceContour.RIGHT_CHEEK
      };

    String[] faceContoursTypesStrings = {
      "FACE",
      "LEFT_EYEBROW_TOP",
      "LEFT_EYEBROW_BOTTOM",
      "RIGHT_EYEBROW_TOP",
      "RIGHT_EYEBROW_BOTTOM",
      "LEFT_EYE",
      "RIGHT_EYE",
      "UPPER_LIP_TOP",
      "UPPER_LIP_BOTTOM",
      "LOWER_LIP_TOP",
      "LOWER_LIP_BOTTOM",
      "NOSE_BRIDGE",
      "NOSE_BOTTOM",
      "LEFT_CHEEK",
      "RIGHT_CHEEK"
    };

    WritableMap faceContoursTypesMap = new WritableNativeMap();

    for (int i = 0; i < faceContoursTypesStrings.length; i++) {
      FaceContour contour = face.getContour(faceContoursTypes[i]);
      List<PointF> points = contour.getPoints();
      WritableNativeArray pointsArray = new WritableNativeArray();

      for (int j = 0; j < points.size(); j++) {
        WritableMap currentPointsMap = new WritableNativeMap();
        currentPointsMap.putDouble("x", points.get(j).x);
        currentPointsMap.putDouble("y", points.get(j).y);
        pointsArray.pushMap(currentPointsMap);
      }
      faceContoursTypesMap.putArray(faceContoursTypesStrings[contour.getFaceContourType() - 1], pointsArray);
    }
    return faceContoursTypesMap;
  }

  @SuppressLint("NewApi")
  @Override
  public Object callback(ImageProxy frame, Object[] params) {
    @SuppressLint("UnsafeOptInUsageError")
    Image mediaImage = frame.getImage();

    if (mediaImage != null) {
      try {
        InputImage image = InputImage.fromMediaImage(mediaImage, frame.getImageInfo().getRotationDegrees());
        Task<List<Face>> task = faceDetector.process(image);
        WritableNativeArray array = new WritableNativeArray();
        List<Face> faces = Tasks.await(task);
        for (Face face : faces) {
          WritableMap map = new WritableNativeMap();
          Bitmap bmpFrameResult = ImageConvertUtils.getInstance().getUpRightBitmap(image);
          String imageResult = new Convert().getBase64Image(bmpFrameResult);

          map.putDouble("rollAngle", face.getHeadEulerAngleZ()); // Head is rotated to the left rotZ degrees
          map.putDouble("pitchAngle", face.getHeadEulerAngleX()); // Head is rotated to the right rotX degrees
          map.putDouble("yawAngle", face.getHeadEulerAngleY());  // Head is tilted sideways rotY degrees
//           map.putDouble("leftEyeOpenProbability", face.getLeftEyeOpenProbability());
//           map.putDouble("rightEyeOpenProbability", face.getRightEyeOpenProbability());
//           map.putDouble("smilingProbability", face.getSmilingProbability());

//          WritableMap contours = processFaceContours(face);
          WritableMap bounds = processBoundingBox(face.getBoundingBox());

          map.putMap("bounds", bounds);
//          map.putMap("contours", contours);
          map.putString("imageResult", imageResult);

          array.pushMap(map);
        }
        return array;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  VisionCameraFaceDetectorPlugin() {
    super("scanFaces");
  }
}
