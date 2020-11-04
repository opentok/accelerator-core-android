package com.tokbox.android.annotations;

import android.graphics.Path;
import android.graphics.PointF;

import java.util.ArrayList;

/**
 * Defines a customizable Path for Android
 */
public class AnnotationsPath extends Path {

    private PointF currentPoint;
    private PointF startPoint;
    private PointF endPoint;
    private ArrayList<PointF> points;
    private PointF lastPoint;

    /**
     * Constructor
     */
    public AnnotationsPath() {
        this.points = new ArrayList<PointF>();
    }

    /**
     * Sets the start point
     * @param startPoint point to set
     */
    public void setStartPoint(PointF startPoint) {
        this.startPoint = startPoint;
    }

    /**
     * Sets the end point
     * @param endPoint point to set
     */
    public void setEndPoint(PointF endPoint) {
        this.endPoint = endPoint;
    }

    /**
     * Returns the start point
     */
    public PointF getStartPoint() {
        return startPoint;
    }

    /**
     * Returns the end point
     */
    public PointF getEndPoint() {
        return endPoint;
    }

    /**
     * Adds a new point to the path
     * @param point to be added
     */
    public void addPoint(PointF point) {
        if (points.size() == 0) {
            startPoint = point;
        }
        points.add(point);
        endPoint = point;
    }

    /**
     * Returns the points list of the path
     */
    public ArrayList<PointF> getPoints() {
        return points;
    }
}

