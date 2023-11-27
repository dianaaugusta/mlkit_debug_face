/*
 * Copyright 2022 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.java.facemeshdetector;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.common.Triangle;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.GraphicOverlay.Graphic;
import com.google.mlkit.vision.demo.R;
import com.google.mlkit.vision.demo.preference.PreferenceUtils;
import com.google.mlkit.vision.facemesh.FaceMesh;
import com.google.mlkit.vision.facemesh.FaceMesh.ContourType;
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions;
import com.google.mlkit.vision.facemesh.FaceMeshPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * Graphic instance for rendering face position and mesh info within the associated graphic overlay
 * view.
 */
public class FaceMeshGraphic extends Graphic {
  private static final int USE_CASE_CONTOUR_ONLY = 999;

  private static final float FACE_POSITION_RADIUS = 8.0f;
  private static final float BOX_STROKE_WIDTH = 5.0f;

  private final Paint positionPaint;
  private final Paint boxPaint;
  boolean samePerson = true;

  private boolean verificationDone = false;
  private boolean pointsDetected = false;

  private final Paint textPaint;
  private volatile FaceMesh faceMesh;
  private final int useCase;
  private float zMin;
  private float zMax;

  private GraphicOverlay graphicOverlay;

  @ContourType
  private static final int[] DISPLAY_CONTOURS = {
    FaceMesh.FACE_OVAL,
    FaceMesh.LEFT_EYEBROW_TOP,
    FaceMesh.LEFT_EYEBROW_BOTTOM,
    FaceMesh.RIGHT_EYEBROW_TOP,
    FaceMesh.RIGHT_EYEBROW_BOTTOM,
    FaceMesh.LEFT_EYE,
    FaceMesh.RIGHT_EYE,
    FaceMesh.UPPER_LIP_TOP,
    FaceMesh.UPPER_LIP_BOTTOM,
    FaceMesh.LOWER_LIP_TOP,
    FaceMesh.LOWER_LIP_BOTTOM,
    FaceMesh.NOSE_BRIDGE
  };

  FaceMeshGraphic( GraphicOverlay overlay, FaceMesh faceMesh) {
    super(overlay);


    this.faceMesh = faceMesh;
    final int selectedColor = Color.WHITE;

    positionPaint = new Paint();
    positionPaint.setColor(selectedColor);

    boxPaint = new Paint();
    boxPaint.setColor(selectedColor);
    boxPaint.setStyle(Style.STROKE);
    boxPaint.setStrokeWidth(BOX_STROKE_WIDTH);

    textPaint = new Paint();
    textPaint.setColor( Color.WHITE);
    textPaint.setTextSize(40F);
    textPaint.setShadowLayer(5.0f, 0f, 0f, Color.BLACK);


    useCase = PreferenceUtils.getFaceMeshUseCase(getApplicationContext());

    graphicOverlay = overlay;
  }

  /** Draws the face annotations for position on the supplied canvas. */
  @Override
  public void draw(Canvas canvas) {
    if (faceMesh == null) {
      return;
    }

    // Draws the bounding box.
    RectF rect = new RectF(faceMesh.getBoundingBox());
    // If the image is flipped, the left will be translated to right, and the right to left.
    float x0 = translateX(rect.left);
    float x1 = translateX(rect.right);
    rect.left = min(x0, x1);
    rect.right = max(x0, x1);
    rect.top = translateY(rect.top);
    rect.bottom = translateY(rect.bottom);
    canvas.drawRect(rect, boxPaint);

    List<FaceMeshPoint> points = new ArrayList<>();

    if (!pointsDetected) {
      // Draw face mesh e obtenha os pontos faciais
            points = useCase == USE_CASE_CONTOUR_ONLY ? getContourPoints(faceMesh) : faceMesh.getAllPoints();

      // Verifica se os pontos foram detectados
      if (!points.isEmpty()) {
        pointsDetected = true; // Marca que os pontos foram detectados

        // Realiza a verificação apenas uma vez após a detecção dos pontos
        samePersonVerification(points, points);
        if(samePerson){
          canvas.drawText("Same Person" , 00F  * 1.5f,
                  500F  * 1.5f, textPaint);
        }
      }
    }

    //
    List<Triangle<FaceMeshPoint>> triangles = faceMesh.getAllTriangles();

    if (!pointsDetected && !points.isEmpty()) {
      pointsDetected = true; // Marca que os pontos foram detectados

      // Marca que a verificação não foi feita para permitir que ocorra uma vez
      verificationDone = false;
    }

    // Verifica se a verificação de mesma pessoa já foi feita e se os pontos foram detectados
    if (!verificationDone && pointsDetected) {
      samePersonVerification(points, points);
      verificationDone = true;
    }


    zMin = Float.MAX_VALUE;
    zMax = Float.MIN_VALUE;
    for (FaceMeshPoint point : points) {
      zMin = min(zMin, point.getPosition().getZ());
      zMax = max(zMax, point.getPosition().getZ());
    }

    // Draw face mesh points
    for (FaceMeshPoint point : points) {
      List<FaceMeshPoint> pointsA = points;
      List<FaceMeshPoint> pointsB = points;
      Log.e("TAG", String.valueOf(points.size()));

      updatePaintColorByZValue(
              positionPaint,
              canvas,
              /* visualizeZ= */ true,
              /* rescaleZForVisualization= */ true,
              point.getPosition().getZ(),
              zMin,
              zMax);
      //um terço do nariz
      updatePaintAndDrawCircle(positionPaint, canvas, zMin, zMax, points.get(6));
      //ponta sobrancelha esquerda
      updatePaintAndDrawCircle(positionPaint, canvas, zMin, zMax, points.get(46));
      //ponta sobrancelha direita
      updatePaintAndDrawCircle(positionPaint, canvas, zMin, zMax, points.get(276));
      //distancia mais proxima da orelha esquerda
      updatePaintAndDrawCircle(positionPaint, canvas, zMin, zMax, points.get(127));
      //distancia mais proxima da orelha direita
      updatePaintAndDrawCircle(positionPaint, canvas, zMin, zMax, points.get(389));


    }

    if (useCase == FaceMeshDetectorOptions.FACE_MESH) {
      // Draw face mesh triangles
      //(ponto 0 - ponto da distancia)
      //(ponto 1 - ponto 0 )
      //(ponto 2 - ponto 0)
      // e normalizar distancia
      for (Triangle<FaceMeshPoint> triangle : triangles) {
        List<FaceMeshPoint> faceMeshPoints = triangle.getAllPoints();

        //tracejando uma linha entre o ponto proximo da orelha ate a ponta da sombrancelha esquerda
       drawLine(canvas, points.get(46).getPosition(), points.get(127).getPosition());

        //tracejando uma linha entre o ponto proximo da orelha ate a ponta da sombrancelha esquerda
        drawLine(canvas, points.get(276).getPosition(), points.get(389).getPosition());

        //tracejando uma linha através do olho esquerdo
        drawLine(canvas, points.get(159).getPosition(), points.get(145).getPosition());

        //tracejando uma linha através do olho esquerdo
        drawLine(canvas, points.get(386).getPosition(), points.get(374).getPosition());

        if (points.size() > 10) {
          PointF3D point5 = points.get(5).getPosition();
          PointF3D point10 = points.get(10).getPosition();
          PointF3D point0 = points.get(0).getPosition();
          //drawLineBetweenPoints(canvas, point5, point10);
          float distance = calculateDistanceBetweenPoints(
                  point5, point10, points.get(0).getPosition());

         // convertPixelsToCm("distance");
//          canvas.drawText(
//                  String.valueOf(translateX(distance)),
//                  00F  * 1.5f,
//                  500F  * 1.5f,
//                  textPaint);

        }
      }
    }
  }

  private List<FaceMeshPoint> getContourPoints(FaceMesh faceMesh) {
    List<FaceMeshPoint> contourPoints = new ArrayList<>();
    for (int type : DISPLAY_CONTOURS) {
      contourPoints.addAll(faceMesh.getPoints(type));
    }
    return contourPoints;
  }

  private void drawLine(Canvas canvas, PointF3D point1, PointF3D point2) {
    updatePaintColorByZValue(
        positionPaint,
        canvas,
        /* visualizeZ= */ true,
        /* rescaleZForVisualization= */ true,
        (point1.getZ() + point2.getZ()) / 2,
        zMin,
        zMax);
    canvas.drawLine(
        translateX(point1.getX()),
        translateY(point1.getY()),
        translateX(point2.getX()),
        translateY(point2.getY()),
        positionPaint);
  }

  private void drawLine(Canvas canvas, PointF3D point1, PointF3D point2, PointF3D point3, PointF3D point4, PointF3D point5) {

    canvas.drawLine(
            translateX(point1.getX()),
            translateY(point1.getY()),
            translateX(point2.getX()),
            translateY(point2.getY()),
            positionPaint);
    canvas.drawLine(
            translateX(point2.getX()),
            translateY(point2.getY()),
            translateX(point3.getX()),
            translateY(point3.getY()),
            positionPaint);
    canvas.drawLine(
            translateX(point3.getX()),
            translateY(point3.getY()),
            translateX(point4.getX()),
            translateY(point4.getY()),
            positionPaint);
    canvas.drawLine(
            translateX(point4.getX()),
            translateY(point4.getY()),
            translateX(point5.getX()),
            translateY(point5.getY()),
            positionPaint);
  }



  private void drawSquare(Canvas canvas,
                          PointF3D point1,
                          PointF3D point2,
                          PointF3D point3,
                          PointF3D point4) {
    drawLine(canvas, point1, point2);
    drawLine(canvas, point2, point3);
    drawLine(canvas, point3, point4);
    drawLine(canvas, point4, point1);
  }

  private void drawLineBetweenPoints(Canvas canvas, PointF3D point1, PointF3D point2) {
    Paint linePaint = new Paint();
    linePaint.setColor(Color.RED);
    linePaint.setStyle(Style.STROKE);
    linePaint.setStrokeWidth(BOX_STROKE_WIDTH);

    canvas.drawLine(
            translateX(point1.getX()),
            translateY(point1.getY()),
            translateX(point2.getX()),
            translateY(point2.getY()),
            linePaint);
  }

  private float calculateDistanceBetweenPoints(PointF3D point1, PointF3D point2, PointF3D point0) {
    float dx = point2.getX() - point1.getX();
    float dy = point2.getY() - point1.getY();
    float dz = point2.getZ() - point1.getZ();

    float offsetX = point0.getX() * 2;
    float offsetY = point0.getY() * 2 ;
    float offsetZ = point0.getZ() * 2 ;

    return (float) Math.sqrt(
            (offsetX - dx) * (offsetX - dx)
            + (offsetY - dy) * (offsetY  - dy)
            + (offsetZ - dz) * (offsetZ  - dz)
    );
  }


  public float convertPixelsToCm(float px) {
    float density = Resources.getSystem().getDisplayMetrics().density;

    float cm = px / (density * 160 / 2.54f);

    return cm;
  }
  public void updatePaintAndDrawCircle(Paint positionPaint, Canvas canvas, float zMin, float zMax, FaceMeshPoint point ) {
    updatePaintColorByZValue(
            positionPaint,
            canvas,
            /* visualizeZ= */ true,
            /* rescaleZForVisualization= */ true,
            point.getPosition().getZ(),
            zMin,
            zMax);

    canvas.drawCircle(
            translateX(point.getPosition().getX()),
            translateY(point.getPosition().getY()),
            FACE_POSITION_RADIUS,
            positionPaint);
  }

  public void samePersonVerification(List<FaceMeshPoint> pointsA, List<FaceMeshPoint> pointsB){
    if (pointsA.size() != pointsB.size()) {
      samePerson = false;
    } else {
      for (int i = 0; i < pointsA.size(); i++) {
        FaceMeshPoint pointA = pointsA.get(i);
        FaceMeshPoint pointB = pointsB.get(i);


        if (pointA.getPosition().getX() != pointB.getPosition().getX() ||
                pointA.getPosition().getY() != pointB.getPosition().getY() ||
                pointA.getPosition().getZ() != pointB.getPosition().getZ()) {
          samePerson = false;
          break;
        }
      }
    }

  }

}
