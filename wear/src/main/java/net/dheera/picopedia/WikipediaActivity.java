package net.dheera.picopedia;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Created by dheera on 8/2/14.
 */
public class WikipediaActivity extends Activity {

        private static final String TAG = "picopedia.WikipediaActivity";
        private static final boolean D = true;
        private static WikipediaActivity self;

        private static WikipediaAdapter mWikipediaAdapter;
        private static GridViewPager mGridViewPager;

        public static int screenWidth = 0;
        public static int screenHeight = 0;

        private static String url = null;

        public static WikipediaActivity instance() {
            if(self != null) {
                return self;
            }
            return null;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            self = this;
            super.onCreate(savedInstanceState);
            setContentView(R.layout.wikipedia);

            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;

            Bundle b = getIntent().getExtras();
            url = b.getString("url");


            TextView mTextView = (TextView) findViewById(R.id.textView);
            mTextView.setText(url);

            mWikipediaAdapter = new WikipediaAdapter(self, getFragmentManager(), null);
            mGridViewPager = (GridViewPager) findViewById(R.id.pager);
            mGridViewPager.setAdapter(mWikipediaAdapter);

            // poke it to create the instance and get the GoogleApiClient
            ProxyClient.instance(this);
        }
}
