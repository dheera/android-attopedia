package net.dheera.picopedia;

import android.content.Context;
import android.graphics.Point;
import android.support.wearable.view.GridViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by dheera on 8/3/14.
 */
public class CustomGridViewPager extends GridViewPager {
    private GestureDetector mGestureDetector;
    View.OnTouchListener mGestureListener;

    public CustomGridViewPager (Context context) {
        super(context);
        setFadingEdgeLength(0);
    }

    private float xDistance, yDistance, lastX, lastY;

    // disable vertical scrolling for the GridViewPager for everything except for the first column
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xDistance = yDistance = 0f;
                lastX = ev.getX();
                lastY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                final float curX = ev.getX();
                final float curY = ev.getY();
                xDistance += Math.abs(curX - lastX);
                yDistance += Math.abs(curY - lastY);
                lastX = curX;
                lastY = curY;
                Point p = getCurrentItem();
                if(xDistance < yDistance && p.x !=0 ) // p.x==0 is colNum==0
                    return false;
        }

        return super.onInterceptTouchEvent(ev);
    }
}
