package com.visioncamerafacedetector;


import android.annotation.SuppressLint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.Image;


import androidx.camera.core.ImageProxy;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
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
      .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
      .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
      .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
      .setMinFaceSize(0.15f)
      .build();

  FaceDetector faceDetector = FaceDetection.getClient(options);



  private WritableMap processBoundingBox(Rect boundingBox) {
    WritableMap bounds = Arguments.createMap();

    bounds.putInt("x", boundingBox.left);
    bounds.putInt("y", boundingBox.top);
    bounds.putInt("width", boundingBox.width());
    bounds.putInt("height", boundingBox.height());

    return bounds;
  }


  private WritableMap  processFaceContours(Face face) {
    // All faceContours
    int[] faceContoursTypes =
      new int[] {
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

    WritableMap faceContoursTypesMap = Arguments.createMap();

    for (int i = 0; i < faceContoursTypes.length; i++) {
      FaceContour contour = face.getContour(faceContoursTypes[i]);
      List<PointF> points = contour.getPoints();
      WritableNativeArray pointsArray = new WritableNativeArray();



      for (int j = 0; j < points.size(); j++) {
        WritableMap currentPointsMap = Arguments.createMap();

        currentPointsMap.putDouble("x", contour.getPoints().get(j).x);
        currentPointsMap.putDouble("y", contour.getPoints().get(j).y);

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
      InputImage image = InputImage.fromMediaImage(mediaImage, frame.getImageInfo().getRotationDegrees());
      Task<List<Face>> task = faceDetector.process(image);
      WritableNativeArray array = new WritableNativeArray();
      try {
        List<Face> faces = Tasks.await(task);

        for (Face face : faces) {
          WritableMap map = Arguments.createMap();


          // Log.d("boundingBox", Integer.toString(boundingBox.bottom));

          map.putDouble("eulerAngleX", face.getHeadEulerAngleX()); // Head is rotated to the left rotY degrees
          map.putDouble("eulerAngleY", face.getHeadEulerAngleY()); // Head is rotated to the right rotY degrees
          map.putDouble("eulerAngleZ", face.getHeadEulerAngleZ());  // Head is tilted sideways rotZ degrees
          map.putDouble("leftEyeOpenProbability", face.getLeftEyeOpenProbability());
          map.putDouble("rightEyeOpenProbability", face.getRightEyeOpenProbability());
          map.putDouble("SmilingProbability", face.getSmilingProbability());

          WritableMap contours = processFaceContours(face);
          WritableMap bounds = processBoundingBox(face.getBoundingBox());

          map.putMap("bounds", bounds);
          map.putMap("contours", contours);

          // classifications all
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