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

    private byte[] downloadUrl(String url) {
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
            if (!scanner.hasNext()) {
                if (D) Log.d(TAG, "invalid message parameter");
                return;
            }
            String url = scanner.next();
            onMessageGet(requestId, url);
        }
    }

    private void onMessageGet(final int requestId, final String url) {
        if(D) Log.d(TAG, String.format("onMessageGet(%s)", url));

        InputStream is;

        new Thread(new Runnable() {
            public void run() {
                try {
                    try {
                        byte[] outdata = downloadUrl(url);
                        if(D) Log.d(TAG, String.format("read %d bytes", outdata.length));
                        sendToWearable(String.format("response %d", requestId), GZipper.compress(outdata), null);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    Log.e(TAG, "onMessageGet: exception:", e);
                }
            }
        }).start();
    }
}

