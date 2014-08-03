package net.dheera.picopedia;

import android.content.Context;
import android.graphics.Point;
import android.support.wearable.view.GridViewPager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Custom GridViewPager which disables vertical scrolling for everything
 * except the first column. Other columns will be using ScrollViews and
 * we don't want the GridViewPager fighting with those ScrollViews.
 * Users will have to go back to the first column to switch between sections.
 */
public class CustomGridViewPager extends GridViewPager {

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
