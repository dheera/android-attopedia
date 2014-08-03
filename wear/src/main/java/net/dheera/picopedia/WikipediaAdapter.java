package net.dheera.picopedia;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.ImageReference;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class WikipediaAdapter extends FragmentGridPagerAdapter {
    private static final String TAG = "picopedia.WikipediaAdapter";
    private static final boolean D = true;

    private final Context mContext;
    public final JSONArray jsections;

    public WikipediaAdapter(Context ctx, FragmentManager fm, JSONArray jsections) {
        super(fm);
        mContext = ctx;
        this.jsections = jsections;
    }

    @Override
    public int getColumnCount(int arg0) {
        if(jsections == null) {
            return 1;
        } else {
            try {
                return jsections.getJSONObject(arg0).getJSONArray("subsections").length() + 1;
            } catch (JSONException e) {
                e.printStackTrace();
                return 1;
            }
        }
    }

    @Override
    public int getRowCount() {
        if(jsections == null) {
            return 1;
        } else {
            return jsections.length();
        }
    }

    @Override
    public Fragment getFragment(int rowNum, int colNum) {
        Log.d("blah", String.format("getFragment(%d, %d)", rowNum, colNum));
        if(jsections == null) {
            return CardFragment.create("miao", "haha");
        } else {
            if(colNum == 0) {
                try {
                    JSONObject jsection = jsections.getJSONObject(rowNum);
                    return WikipediaSectionFragment.newInstance(jsection.getString("title"));
                } catch (JSONException e) {
                    return CardFragment.create("miao", "hahaha");
                }
            } else {
                try {
                    JSONObject jsubsection = jsections.getJSONObject(rowNum).getJSONArray("subsections").getJSONObject(colNum - 1);
                    if (D) Log.d(TAG, jsubsection.toString());
                    return WikipediaSubsectionFragment.newInstance(jsubsection.getString("title"), jsubsection.getString("text"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    return CardFragment.create("miao", "hahaha");
                }
            }
        }
    }
}
