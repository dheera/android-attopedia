package net.dheera.picopedia;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.provider.Browser;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * Created by dheera on 8/2/14.
 */
public class BrowserActivity extends Activity {

        private static final String TAG = "picopedia.BrowserActivity";
        private static final boolean D = true;
        private static BrowserActivity self;

        GridViewPager mGridViewPager;
        SearchAdapter mSearchAdapter;

        public static int screenWidth = 0;
        public static int screenHeight = 0;

        public static BrowserActivity instance() {
            if(self != null) {
                return self;
            }
            return null;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            self = this;
            super.onCreate(savedInstanceState);
            setContentView(R.layout.browser);

            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;

            final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
            stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
                @Override
                public void onLayoutInflated(WatchViewStub stub) {
                    mSearchAdapter = new SearchAdapter(self, getFragmentManager(), null);
                    // mGridViewPager = (GridViewPager) findViewById(R.id.pager);
                    // mGridViewPager.setAdapter(mWikiAdapter);
                }
            });

            // poke it to create the instance and get the GoogleApiClient
            ProxyClient.instance(this);
        }
}
