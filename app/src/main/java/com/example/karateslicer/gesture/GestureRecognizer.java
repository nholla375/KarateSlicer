package com.example.karateslicer.gesture;

import android.graphics.PointF;

import com.example.karateslicer.enemy.ShapeType;

import java.util.List;

/**
 * Stateless rule-based gesture recognizer (spec §5.5, §5.6).
 *
 * Each gesture maps to the ShapeType of the enemy it kills:
 *   Swipe Right  → CIRCLE   (Tier 1)
 *   Circle/Loop  → SQUARE   (Tier 2)
 *   Swipe Down   → TRIANGLE (Tier 3)
 *   Swipe Left   → STAR     (Tier 4)
 *   Swipe Up + Swipe Down → LIGHTNING(Tier 5)
 *
 * Recognition order (spec §5.6):
 *   1. Swipe Right — net X displacement + axis ratio + left-to-right
 *   2. Circle   — closedness ratio < CLOSEDNESS_THRESHOLD
 *   3. Swipe Down — net Y displacement + axis ratio + top-to-bottom
 *   4. Swipe Left — net X displacement + axis ratio + right-to-left
 *   5. UNKNOWN
 */
public class GestureRecognizer {

    public ShapeType recognize(List<PointF> rawStroke) {
        if (rawStroke == null || rawStroke.size() < 2) return ShapeType.UNKNOWN;

        float length = strokeLength(rawStroke);
        if (length < GestureConfig.MIN_STROKE_LENGTH_PX) return ShapeType.UNKNOWN;

        if (isSwipeRight(rawStroke)) return ShapeType.CIRCLE;
        if (isLoop(rawStroke, length)) return ShapeType.SQUARE;
        if (isSwipeDown(rawStroke)) return ShapeType.TRIANGLE;
        if (isSwipeLeft(rawStroke)) return ShapeType.STAR;

        return ShapeType.UNKNOWN;
    }

    private boolean isSwipeRight(List<PointF> pts) {
        float[] d = netDirection(pts, 0, pts.size() - 1);
        return d[0] > 0 && Math.abs(d[0]) > Math.abs(d[1]) * 1.5f;
    }

    private boolean isSwipeLeft(List<PointF> pts) {
        float[] d = netDirection(pts, 0, pts.size() - 1);
        return d[0] < 0 && Math.abs(d[0]) > Math.abs(d[1]) * 1.5f;
    }

    public boolean isSwipeDown(List<PointF> pts) {
        float[] d = netDirection(pts, 0, pts.size() - 1);
        return d[1] > 0 && Math.abs(d[1]) > Math.abs(d[0]) * 1.5f;
    }

    public boolean isSwipeUp(List<PointF> pts) {
        float[] d = netDirection(pts, 0, pts.size() - 1);
        return d[1] < 0 && Math.abs(d[1]) > Math.abs(d[0]) * 1.5f;
    }

    private boolean isLoop(List<PointF> pts, float length) {
        PointF first = pts.get(0);
        PointF last  = pts.get(pts.size() - 1);
        float endDist = dist(first, last);
        float closednessRatio = endDist / length;
        return closednessRatio < GestureConfig.CLOSEDNESS_THRESHOLD;
    }

    // ------------------------------------------------------------------
    // Geometry helpers
    // ------------------------------------------------------------------

    /** Net direction vector from pts[start] to pts[end]. */
    private float[] netDirection(List<PointF> pts, int start, int end) {
        if (end <= start || end >= pts.size()) return new float[]{1f, 0f};
        PointF s = pts.get(start), e = pts.get(end);
        return new float[]{e.x - s.x, e.y - s.y};
    }

    private float strokeLength(List<PointF> pts) {
        float len = 0;
        for (int i = 1; i < pts.size(); i++) len += dist(pts.get(i - 1), pts.get(i));
        return len;
    }

    private float dist(PointF a, PointF b) {
        float dx = a.x - b.x, dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
