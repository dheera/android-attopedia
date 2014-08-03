package net.dheera.picopedia;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by dheera on 8/2/14.
 */
public class ProxyClient {
    private static final String TAG = "picopedia.ProxyClient";
    private static final boolean D = true;

    private static ProxyClient instance;
    private static Context context;
    private static HashMap requestResults;

    private static int requestCounter = 0;

    private GoogleApiClient mGoogleApiClient = null;
    private Node mPhoneNode = null;

    public static ProxyClient instance(Context context) {
        if(instance == null) {
            instance = new ProxyClient(context);
        }
        return instance;
    }

    public ProxyClient(Context c) {
        context = c;
        requestResults = new HashMap<String, byte[]>();
        connect();
    }

    private void connect() {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        findPhoneNode();
                        Wearable.MessageApi.addListener(mGoogleApiClient, new MessageApi.MessageListener() {
                            @Override
                            public void onMessageReceived(MessageEvent m) {
                                if (D) Log.d(TAG, "onMessageReceived");
                                if (D) Log.d(TAG, "path: " + m.getPath());
                                if (D) Log.d(TAG, "data bytes: " + m.getData().length);

                                Scanner scanner = new Scanner(m.getPath());
                                String requestType = scanner.next();

                                if (D) Log.d(TAG, "requestType: " + requestType);

                                if (requestType.equals("response")) {
                                    if (!scanner.hasNextInt()) {
                                        if (D) Log.d(TAG, "invalid message parameter");
                                        return;
                                    }
                                    int requestId = scanner.nextInt();
                                    onMessageResponse(requestId, m.getData());
                                }
                            }
                        });
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();
    }

    void findPhoneNode() {
        PendingResult<NodeApi.GetConnectedNodesResult> pending = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        pending.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                if(result.getNodes().size()>0) {
                    mPhoneNode = result.getNodes().get(0);
                    if(D) Log.d(TAG, "Found phone: name=" + mPhoneNode.getDisplayName() + ", id=" + mPhoneNode.getId());
                    sendToPhone("start", null, null);
                } else {
                    mPhoneNode = null;
                }
            }
        });
    }

    private void sendToPhone(String path, byte[] data, final ResultCallback<MessageApi.SendMessageResult> callback) {
        if (mPhoneNode != null) {
            PendingResult<MessageApi.SendMessageResult> pending = Wearable.MessageApi.sendMessage(mGoogleApiClient, mPhoneNode.getId(), path, data);
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

    private void onMessageResponse(int requestId, byte[] data) {
        if(D) Log.d(TAG, String.format("onMessageResponse(%d bytes)", data.length));
        try {
            requestResults.put(requestId, GZipper.decompress(data));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void get(final String url, final ProxyResultHandler mResultHandler) {
        if(D) Log.d(TAG, String.format("get(%s)", url));

        final int timeoutMax = 20; // in seconds
        final int requestId = requestCounter++; // unique id per get request; phone will provide back the id upon result
        requestResults.put(requestId, null);
        sendToPhone(String.format("get %d %s", requestId, url), null, null);

        new Thread() {
            public void run() {

                int timeoutCounter = 0;
                while(requestResults.get(requestId) == null && timeoutCounter < timeoutMax*1000/50) {
                    try {
                        Thread.sleep(50);
                        timeoutCounter++;
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(requestResults.get(requestId) == null) {
                    // timed out, give up
                    Log.d(TAG, "get: timed out");
                    mResultHandler.onFail();
                    requestResults.remove(requestId);
                } else {
                    // we have data
                    mResultHandler.onResult((byte[])requestResults.get(requestId));
                    requestResults.remove(requestId);
                }
            }
        }.start();
    }

}