package net.dheera.attopedia;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a (Google Now)-like voice search interface and serves
 * as the main activity for this package.
 *
 * At the time of this writing Google Now does not support intent
 * filters, so we unfortunately cannot launch the WikipediaActivity
 * from Google Now directly.
 */
public class SearchActivity extends Activity {

    private static final String TAG = "attopedia.SearchActivity";
    private static final boolean D = false;
    private static SearchActivity self;

    GridViewPager mGridViewPager;
    SearchAdapter mSearchAdapter;
    FrameLayout mFrameLayout_progress;

    public static int screenWidth = 0;
    public static int screenHeight = 0;

    public static SearchActivity instance() {
        if(self != null) {
            return self;
        }
        return new SearchActivity();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        self = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                final ImageView mImageView = (ImageView) findViewById(R.id.imageView);
                final ImageView mImageView_again = (ImageView) findViewById(R.id.imageView_again);

                // set the progress bar to 3/4 of the screen
                mFrameLayout_progress = (FrameLayout) findViewById(R.id.frameLayout_progress);
                ViewGroup.LayoutParams l = mFrameLayout_progress.getLayoutParams();
                l.height = screenHeight*3/4;
                mFrameLayout_progress.setLayoutParams(l);

                // set the "search again" hot area to 1/4 of the screen
                // (the first search result card will be 3/4 of the screen)
                l = mImageView_again.getLayoutParams();
                l.height = screenHeight/4;
                mImageView_again.setLayoutParams(l);

                mSearchAdapter = new SearchAdapter(self, getFragmentManager(), null);
                mGridViewPager = (GridViewPager) findViewById(R.id.pager);
                mGridViewPager.setAdapter(mSearchAdapter);
                mGridViewPager.setOnPageChangeListener(new GridViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int i, int i2, float v, float v2, int i3, int i4) {

                    }

                    @Override
                    public void onPageSelected(int i, int i2) {
                        if(i==0 && i2==0) {
                            mImageView_again.setVisibility(View.VISIBLE);
                        } else {
                            mImageView_again.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onPageScrollStateChanged(int i) {

                    }
                });

                mImageView_again.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        beginSpeech();
                    }
                });

                mImageView.setImageBitmap(decodeSampledBitmapFromResource(getResources(),
                        R.drawable.searchback, SearchActivity.instance().screenWidth, SearchActivity.instance().screenHeight));
            }
        });

        // poke it to create the instance and get the GoogleApiClient
        ProxyClient.instance(this);
    }

    public void beginSpeech() {
        displaySpeechRecognizer();
    }

    private static final int SPEECH_REQUEST_CODE = 0;

    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            mFrameLayout_progress.setVisibility(View.VISIBLE);
            try {
                final String spokenTextEncoded = URLEncoder.encode(spokenText, "utf-8");
                final String url = "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=-%22disambiguation+page%22+site:en.wikipedia.org+" + spokenTextEncoded;
                ProxyClient.instance(this).get(url, ProxyClient.GETTER_RAW, new ProxyResultHandler() {
                    @Override
                    public void onResult(byte data[]) {
                        Log.d(TAG, String.format("onResult(%d bytes)", data.length));
                        try {
                            JSONObject j = new JSONObject(new String(data));
                            JSONArray results = j.getJSONObject("responseData").getJSONArray("results");
                            final ArrayList<SearchAdapter.SearchResult> outresults = new ArrayList<SearchAdapter.SearchResult>();
                            for(int i=0;i<results.length();i++) {
                                JSONObject result = results.getJSONObject(i);
                                SearchAdapter.SearchResult outresult = mSearchAdapter.newSearchResult();
                                // "Software <b>testing</b> - Wikipedia, the free encyclopedia" -> "Software testing"
                                outresult.title = Html.fromHtml( result.getString("title").split(" - ")[0] ).toString();
                                outresult.summary = Html.fromHtml( result.getString("content") ).toString();
                                outresult.url = result.getString("url");

                                // new server-side parser
                                outresult.url = outresult.url.replace("https://", "http://attopedia.dheera.net/0/");
                                outresult.url = outresult.url.replace("http://", "http://attopedia.dheera.net/0/");

                                if(  !outresult.title.contains("User:")
                                  && !outresult.title.contains("Wikipedia:")
                                  && !outresult.title.contains("File:")
                                  && !outresult.title.contains("Category:")
                                  && !outresult.title.contains("Portal:")
                                  && !outresult.title.contains("Image:")
                                  && !outresult.title.contains("Template:")
                                  && !outresult.url.contains("wiki/wikt")
                                  && !outresult.url.contains("wiki/voy:")
                                  && !outresult.url.contains("wiki/Wiktionary")
                                  && !outresult.title.contains("Special:")  ) {
                                    outresults.add(outresult);
                                }
                            }

                            if(outresults.size()>0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // workaround because notifyDataSetChanged() doesn't work
                                        // and crashes the GridViewPager
                                        // if the number of rows changes

                                        mSearchAdapter = new SearchAdapter(self, getFragmentManager(), outresults);
                                        mGridViewPager.setAdapter(mSearchAdapter);
                                        mFrameLayout_progress.setVisibility(View.GONE);
                                    }
                                });
                            }
                        } catch(JSONException e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mFrameLayout_progress.setVisibility(View.GONE);
                                }
                            });
                        }
                    }
                    @Override
                    public void onFail() {
                        Log.d(TAG, "onFail");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mFrameLayout_progress.setVisibility(View.GONE);
                            }
                        });
                    }
                });
            } catch(UnsupportedEncodingException e) {
                // seriously ... this is utf-8 we're talking about ...
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
