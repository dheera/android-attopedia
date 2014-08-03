package net.dheera.picopedia;

import android.app.Service;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.InputStream;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Created by Dheera Venkatraman
 * http://dheera.net
 */
public class DataLayerListenerService extends WearableListenerService {

    private static final String TAG = "picopedia/" + String.valueOf((new Random()).nextInt(10000));
    private static final boolean D = true;

    // raw bytes of whatever URL is being requested
    public static final int GETTER_RAW = 0;
    // attempt to parse Wikipedia page and give back a nice JSON object
    public static final int GETTER_WIKIPEDIA = 1;
    // get image, resize to watch face size and recompress aggressively to WEBP
    public static final int GETTER_IMAGE = 2;

    private static GoogleApiClient mGoogleApiClient = null;
    private static Node mWearableNode = null;

    private void sendToWearable(String path, byte[] data, final ResultCallback<MessageApi.SendMessageResult> callback) {
        if (mWearableNode != null) {
            PendingResult<MessageApi.SendMessageResult> pending = Wearable.MessageApi.sendMessage(mGoogleApiClient, mWearableNode.getId(), path, data);
            pending.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult result) {
                    if (callback != null) {
                        callback.onResult(result);
                    }
                    if (!result.getStatus().isSuccess()) {
                        if(D) Log.d(TAG, "ERROR: failed to send Message: " + result.getStatus());
                    }
                }
            });
        } else {
            if(D) Log.d(TAG, "ERROR: tried to send message before device was found");
        }
    }

    void findWearableNodeAndBlock() {
        PendingResult<NodeApi.GetConnectedNodesResult> nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        nodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                if(result.getNodes().size()>0) {
                    mWearableNode = result.getNodes().get(0);
                    if(D) Log.d(TAG, "Found wearable: name=" + mWearableNode.getDisplayName() + ", id=" + mWearableNode.getId());
                } else {
                    mWearableNode = null;
                }
            }
        });
        int i = 0;
        while(mWearableNode == null && i++<50) {
            try {
                Thread.sleep(100);
            } catch(InterruptedException e ) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate() {
        if(D) Log.d(TAG, "onCreate");
        super.onCreate();

        try {
            Class.forName("android.os.AsyncTask");
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        if(D) Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onPeerConnected(Node peer) {
        if(D) Log.d(TAG, "onPeerConnected");
        if(D) Log.d(TAG, "Connected: name=" + peer.getDisplayName() + ", id=" + peer.getId());
        super.onPeerConnected(peer);
    }

    @Override
    public void onMessageReceived(MessageEvent m) {
        if(D) Log.d(TAG, "onMessageReceived");
        if(D) Log.d(TAG, "path: " + m.getPath());
        if(D) Log.d(TAG, "data bytes: " + m.getData().length);

        Scanner scanner = new Scanner(m.getPath());
        String requestType = scanner.next();

        if (D) Log.d(TAG, "requestType: " + requestType);

        // in case the GoogleApiClient got disconnected

        if(mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            if(D) Log.d(TAG, "setting up GoogleApiClient");
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addApi(LocationServices.API)
                    .build();
            if(D) Log.d(TAG, "connecting to GoogleApiClient");
            ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess()) {
                Log.e(TAG, String.format("GoogleApiClient connect failed with error code %d", connectionResult.getErrorCode()));
                return;
            } else {
                if(D) Log.d(TAG, "GoogleApiClient connect success, finding wearable node");
                findWearableNodeAndBlock();
                if(D) Log.d(TAG, "wearable node found");
            }
        }

        // in case the WearableNode wasn't found before

        if(mWearableNode == null) {
            if(D) Log.d(TAG, "GoogleApiClient was connceted but wearable not found, finding wearable node");
            findWearableNodeAndBlock();
            if(mWearableNode == null) {
                if(D) Log.d(TAG, "wearable node not found");
                return;
            }
        }

        // process messages

        if (requestType.equals("get")) {
            if (!scanner.hasNextInt()) {
                if (D) Log.d(TAG, "invalid message parameter");
                return;
            }
            int requestId = scanner.nextInt();
            if (!scanner.hasNextInt()) {
                if (D) Log.d(TAG, "invalid message parameter");
                return;
            }
            int getter = scanner.nextInt();
            if (!scanner.hasNext()) {
                if (D) Log.d(TAG, "invalid message parameter");
                return;
            }
            String url = scanner.next();
            onMessageGet(requestId, url, getter);
        }
    }

    private void onMessageGet(final int requestId, final String url, final int getter) {
        if(D) Log.d(TAG, String.format("onMessageGet(%s)", url));

        InputStream is;

        new Thread(new Runnable() {
            public void run() {
                try {
                        byte[] outdata = null;
                        // fetch
                        if(getter == GETTER_RAW || getter == GETTER_IMAGE) {
                            outdata = getRaw(url);
                        } else if(getter == GETTER_WIKIPEDIA) {
                            outdata = getWikipedia(url);
                        }
                        // and send
                        if(outdata != null) {
                            if (D) Log.d(TAG, String.format("read %d bytes", outdata.length));
                            sendToWearable(String.format("response %d", requestId), GZipper.compress(outdata), null);
                        }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private byte[] getRaw(String url) {
        if(D) Log.d(TAG, String.format("getRaw(%s)", url));
        HttpGet httpGet = new HttpGet(url);
        HttpClient httpclient = new DefaultHttpClient();
        try {
            HttpResponse response = httpclient.execute(httpGet);
            return EntityUtils.toByteArray(response.getEntity());
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] getWikipedia(String url) {
        if(D) Log.d(TAG, String.format("getWikipedia(%s)", url));

        url = "http://en.wikipedia.org/w/index.php?title=Boston&action=render";

        final JSONObject json = new JSONObject();

        try {
            if (D) Log.d(TAG, "begin");
            Document doc = Jsoup.connect(url).get();
            Elements elements = doc.select("body").get(0).children();
            String jsubsection_text = "";
            JSONArray jsections = new JSONArray();
            JSONObject jsection = new JSONObject();
            JSONArray jsubsections = new JSONArray();
            JSONObject jsubsection = new JSONObject();

            // first section
            jsection.put("title", "Overview");
            try {
                // todo: rewrite this to check length of select result
                String image_src = doc.select("img").get(0).attr("abs:src");
                jsection.put("image_url", image_src);
            } catch(Exception e) {
                // todo: (see above) we just don't have an image for this article
            }

            // first section's subsection
            jsubsection.put("title", "");

            for (Element element : elements) {
                if (element.tagName().equals("p") && !element.text().equals("")) {
                    // we have text, add it to the current subsection
                    jsubsection_text += element.text() + "\n\n";
                }

                if (element.tagName().equals("h2")) {
                    // we have a new section ...
                    // wrap up the subsection first
                    if (!jsubsection_text.trim().equals("")) {
                        jsubsection_text = jsubsection_text.replaceAll("\\[[0-9]*\\]", "");
                        jsubsection.put("text", jsubsection_text);
                        jsubsections.put(jsubsection);
                    }
                    // ... begin a new blank subsection
                    jsubsection = new JSONObject();
                    jsubsection_text = "";
                    jsubsection.put("title", "");
                    // then wrap up the section
                    if (jsubsections.length() > 0) {
                        jsection.put("subsections", jsubsections);
                        jsections.put(jsection);
                    }
                    // ... and begin a new section
                    jsubsections = new JSONArray();
                    jsection = new JSONObject();
                    jsection.put("title", element.text());
                }
                if (element.tagName().equals("h3")) {
                    // we have a new subsection ...
                    // wrap up the old subsection first
                    if (!jsubsection_text.trim().equals("")) {
                        jsubsection_text = jsubsection_text.replaceAll("\\[[0-9]*\\]", "");
                        jsubsection.put("text", jsubsection_text);
                        jsubsections.put(jsubsection);
                    }
                    // ... and begin an new subsection
                    jsubsection = new JSONObject();
                    jsubsection_text = "";
                    jsubsection.put("title", element.text());
                }
            }
            return jsections.toString().getBytes();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

