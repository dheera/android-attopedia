package net.dheera.picopedia;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;

public class WikipediaSubsectionFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.wikipedia_fragment_subsection, container, false);
        Bundle bundle = getArguments();

        ProxyClient mProxyClient = ProxyClient.instance(SearchActivity.instance());

        TextView mTextView_title = (TextView) contentView.findViewById(R.id.textView_title);
        if(bundle.getString("title").equals("")) {
            mTextView_title.setVisibility(View.GONE);
        } else {
            mTextView_title.setText(bundle.getString("title"));
        }

        LinearLayout mLinearLayout = (LinearLayout) contentView.findViewById(R.id.linearLayout);

        try {
            JSONArray contentBoxes = new JSONArray(bundle.getString("contentBoxes"));
            for(int i=0;i<contentBoxes.length();i++) {
                JSONObject contentBox = contentBoxes.getJSONObject(i);
                if(contentBox.getString("type").equals("text") && contentBox.has("text")) {
                    TextView tv = (TextView) inflater.inflate(R.layout.wikipedia_fragment_subsection_text, null);
                    tv.setText(contentBox.getString("text"));
                    mLinearLayout.addView(tv);
                } else if(contentBox.getString("type").equals("image") && contentBox.has("url")) {
                    final ImageView iv = (ImageView) inflater.inflate(R.layout.wikipedia_fragment_subsection_image_image, null);
                    final String image_url = contentBox.getString("url");
                    if(!image_url.equals("")) {
                        mProxyClient.get(image_url, ProxyClient.GETTER_IMAGE, new ProxyResultHandler() {
                            @Override
                            public void onResult(final byte data[]) {
                                // this was throwing NPE's if the user flipped pages too fast
                                // for some odd reason before so let's just be aggressive about checking
                                if(iv != null && data!=null && data.length>0 && getActivity()!=null) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);
                                            iv.setImageBitmap(b);
                                            iv.setBackgroundColor(getResources().getColor(R.color.white));
                                        }
                                    });
                                }
                            }
                        });
                    }
                    mLinearLayout.addView(iv);
                    if(contentBox.has("caption")) {
                        TextView tv = (TextView) inflater.inflate(R.layout.wikipedia_fragment_subsection_image_caption, null);
                        tv.setText(contentBox.getString("caption"));
                        mLinearLayout.addView(tv);
                    }
                }
            }
        } catch(JSONException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            e.printStackTrace();
            TextView t = (TextView)getActivity().getLayoutInflater().inflate(R.layout.wikipedia_fragment_subsection_text, null);
            t.setText("Error rendering content:" + sw.toString());
            mLinearLayout.addView(t);
        }

        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        return contentView;
    }

    public static WikipediaSubsectionFragment newInstance(String title, JSONArray contentBoxes) {
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putString("contentBoxes", contentBoxes.toString());
        WikipediaSubsectionFragment f = new WikipediaSubsectionFragment();
        f.setArguments(bundle);
        return f;
    }

}
