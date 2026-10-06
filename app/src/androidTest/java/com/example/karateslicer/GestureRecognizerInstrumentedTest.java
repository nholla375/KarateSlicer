package com.example.karateslicer;

import static org.junit.Assert.assertEquals;

import android.graphics.PointF;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.karateslicer.enemy.ShapeType;
import com.example.karateslicer.gesture.GestureRecognizer;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class GestureRecognizerInstrumentedTest {

    private final GestureRecognizer recognizer = new GestureRecognizer();

    @Test
    public void rightSwipeMapsToCircleEnemy() {
        List<PointF> stroke = Arrays.asList(
                new PointF(0, 100), new PointF(80, 103), new PointF(180, 106));
        assertEquals(ShapeType.CIRCLE, recognizer.recognize(stroke));
    }

    @Test
    public void closedLoopMapsToSquareEnemy() {
        List<PointF> stroke = Arrays.asList(
                new PointF(100, 0), new PointF(200, 100), new PointF(100, 200),
                new PointF(0, 100), new PointF(100, 0));
        assertEquals(ShapeType.SQUARE, recognizer.recognize(stroke));
    }

    @Test
    public void downwardSwipeMapsToTriangleEnemy() {
        List<PointF> stroke = Arrays.asList(
                new PointF(100, 0), new PointF(102, 90), new PointF(105, 190));
        assertEquals(ShapeType.TRIANGLE, recognizer.recognize(stroke));
    }

    @Test
    public void leftSwipeMapsToStarEnemy() {
        List<PointF> stroke = Arrays.asList(
                new PointF(200, 100), new PointF(110, 102), new PointF(0, 104));
        assertEquals(ShapeType.STAR, recognizer.recognize(stroke));
    }

    @Test
    public void shortStrokeIsRejected() {
        List<PointF> stroke = Arrays.asList(new PointF(0, 0), new PointF(20, 0));
        assertEquals(ShapeType.UNKNOWN, recognizer.recognize(stroke));
    }
}
