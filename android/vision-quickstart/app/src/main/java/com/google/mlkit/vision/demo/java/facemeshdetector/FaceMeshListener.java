package com.google.mlkit.vision.demo.java.facemeshdetector;

import com.google.mlkit.vision.facemesh.FaceMeshPoint;

import java.util.List;

public interface FaceMeshListener {
    void onFaceMeshDetected(List<FaceMeshPoint> pointsDetected);

}
