package net.dheera.attopedia;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * This activity is to browse an actual wikipedia page and
 * is launched by a SearchResultFragment.
 * It requires an url argument in order to function.
 *
 * The hope is that perhaps one day in the future, Google Now
 * for Android Wear will support intent filters and can directly
 * launch this activity from the Google Now search results.
 */
public class WikipediaActivity extends Activity {

        private static final String TAG = "attopedia.WikipediaActivity";
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

        FrameLayout mFrameLayout_progress;
        FrameLayout mFrameLayout_retry;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            self = this;
            super.onCreate(savedInstanceState);
            setContentView(R.layout.wikipedia_main);

            FrameLayout mFrameLayout = (FrameLayout) findViewById(R.id.frameLayout);
            mGridViewPager = new CustomGridViewPager(this);
            mGridViewPager.setId(View.generateViewId());
            GridViewPager.LayoutParams l = new GridViewPager.LayoutParams();
            l.setMargins(0, 0, 0, 0);
            mGridViewPager.setLayoutParams(l);
            mGridViewPager.setKeepScreenOn(true);
            mFrameLayout.addView(mGridViewPager, 0);

            mFrameLayout_progress = (FrameLayout) findViewById(R.id.frameLayout_progress);
            mFrameLayout_retry = (FrameLayout) findViewById(R.id.frameLayout_retry);
            mFrameLayout_progress.setVisibility(View.VISIBLE);
            mFrameLayout_retry.setVisibility(View.GONE);

            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;

            Bundle b = getIntent().getExtras();
            url = b.getString("url");

            // use ProxyClient.GETTER_WIKIPEDIA if using the Wikipedia URL directly
            // use ProxyClient.GETTER_RAW if using http://attopedia.dheera.net/ server-side parser
            // (parsing on phones with Jsoup is slow)
            // source code for server-side parser at http://github.com/dheera/attopedia-server

            Log.d(TAG, "Getting " + url);
            ProxyClient.instance(this).get(url, ProxyClient.GETTER_RAW, new ProxyResultHandler() {
                @Override
                public void onResult(byte data[]) {
                    Log.d(TAG, "result: " + new String(data));
                    try {
                        final JSONArray jsections = new JSONArray(new String(data));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mFrameLayout_progress.setVisibility(View.GONE);
                                mWikipediaAdapter = new WikipediaAdapter(self, getFragmentManager(), jsections);
                                mGridViewPager.setAdapter(mWikipediaAdapter);
                            }
                        });
                    } catch(JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mFrameLayout_retry.setVisibility(View.VISIBLE);
                                mFrameLayout_progress.setVisibility(View.GONE);
                            }
                        });
                    }
                }
                @Override
                public void onFail() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mFrameLayout_retry.setVisibility(View.VISIBLE);
                            mFrameLayout_progress.setVisibility(View.GONE);
                        }
                    });
                }
            });

            mWikipediaAdapter = new WikipediaAdapter(self, getFragmentManager(), null);
            // mGridViewPager = (GridViewPager) findViewById(R.id.pager);
            mGridViewPager.setAdapter(mWikipediaAdapter);

            // poke it to create the instance and get the GoogleApiClient
            ProxyClient.instance(this);
        }

    public void onClickRetry(View v) {
        mFrameLayout_retry.setVisibility(View.GONE);
        mFrameLayout_progress.setVisibility(View.VISIBLE);
        ProxyClient.instance(this).get(url, ProxyClient.GETTER_RAW, new ProxyResultHandler() {
            @Override
            public void onResult(byte data[]) {
                try {
                    final JSONArray jsections = new JSONArray(new String(data));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mFrameLayout_progress.setVisibility(View.GONE);
                            mWikipediaAdapter = new WikipediaAdapter(self, getFragmentManager(), jsections);
                            mGridViewPager.setAdapter(mWikipediaAdapter);
                        }
                    });
                } catch(JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mFrameLayout_retry.setVisibility(View.VISIBLE);
                            mFrameLayout_progress.setVisibility(View.GONE);
                        }
                    });
                }
            }
            @Override
            public void onFail() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mFrameLayout_retry.setVisibility(View.VISIBLE);
                        mFrameLayout_progress.setVisibility(View.GONE);
                    }
                });
            }
        });
    }
}
