package net.dheera.picopedia;

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
import android.view.WindowManager;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends Activity {

    private static final String TAG = "picopedia.SearchActivity";
    private static final boolean D = true;
    private static SearchActivity self;

    GridViewPager mGridViewPager;
    SearchAdapter mSearchAdapter;

    public static int screenWidth = 0;
    public static int screenHeight = 0;

    public static SearchActivity instance() {
        if(self != null) {
            return self;
        }
        return null;
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
                mSearchAdapter = new SearchAdapter(self, getFragmentManager(), null);
                mGridViewPager = (GridViewPager) findViewById(R.id.pager);

                mGridViewPager.setAdapter(mSearchAdapter);
                ImageView mImageView = (ImageView) findViewById(R.id.imageView);
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
            try {
                final String spokenTextEncoded = URLEncoder.encode(spokenText, "utf-8");
                final String url = "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=-%22may%20refer%20to%22+-%22may%20refer%20to%22+site:en.wikipedia.org+" + spokenTextEncoded;
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
                                if(  !outresult.title.contains("User:")
                                  && !outresult.title.contains("Wikipedia:")
                                  && !outresult.title.contains("File:")
                                  && !outresult.title.contains("Category:")
                                  && !outresult.title.contains("Image:")
                                  && !outresult.title.contains("Template:")
                                  && !outresult.title.contains("Special:")  ) {
                                    outresults.add(outresult);
                                }
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // workaround because notifyDataSetChanged() doesn't work
                                    // and crashes the GridViewPager
                                    // if the number of rows changes

                                    mSearchAdapter = new SearchAdapter(self, getFragmentManager(), outresults);
                                    mGridViewPager.setAdapter(mSearchAdapter);
                                }
                            });

                        } catch(JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    @Override
                    public void onFail() {
                        Log.d(TAG, "onFail");
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
