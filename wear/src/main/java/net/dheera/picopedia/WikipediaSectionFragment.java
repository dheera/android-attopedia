package net.dheera.picopedia;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class WikipediaSectionFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.wikipedia_fragment_section, container, false);
        Bundle bundle = getArguments();

        final TextView mTextView = (TextView) contentView.findViewById(R.id.textView);
        final ImageView mImageView = (ImageView) contentView.findViewById(R.id.imageView);

        // set the title text

        mTextView.setText(bundle.getString("title"));

        // fetch the background image asynchronously

        ProxyClient mProxyClient = ProxyClient.instance(SearchActivity.instance());

        String image_url = bundle.getString("image_url");
        if(!image_url.equals("")) {
            mProxyClient.get(image_url, ProxyClient.GETTER_IMAGE, new ProxyResultHandler() {
                @Override
                public void onResult(final byte data[]) {
                    if(data!=null && data.length>0 && getActivity()!=null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);
                                    if (mImageView != null) {
                                        mImageView.setImageBitmap(b);
                                    }
                                } catch(Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            });
        }

        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // do something fun
            }
        });

        return contentView;
    }

    /**
     * Creates a new section header for use in the leftmost column of a GridViewPager
     * @param title section title to display
     * @param image_url optional background image URL
     * @return Fragment to be passed to adapter
     */

    public static WikipediaSectionFragment newInstance(String title, String image_url) {
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        if(image_url == null) image_url = "";
        bundle.putString("image_url", image_url);
        WikipediaSectionFragment f = new WikipediaSectionFragment();
        f.setArguments(bundle);
        return f;
    }

}
