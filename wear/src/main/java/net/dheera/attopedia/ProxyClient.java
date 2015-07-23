package net.dheera.attopedia;

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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Connects to DataLayerListenerService on a phone to fetch data from the web.
 */
public class ProxyClient {
    private static final String TAG = "attopedia.ProxyClient";
    private static final boolean D = false;
    private static Context context;

    // raw bytes of whatever URL is being requested
    public static final int GETTER_RAW = 0;
    // get image, resize to watch face size and recompress aggressively to WEBP
    public static final int GETTER_IMAGE = 2;

    // track our instance since the entire app can share one ProxyClient
    private static ProxyClient instance;

    // tracks result data for requestIds
    private static HashMap requestResults;

    // used to generate unique requestIds
    // requestIds are sent to the phone with every get request
    // and returned back with the results so they can be tracked
    // in the case of multiple simultaneous requests
    private static int requestCounter = 0;

    // for communicating with phone
    private GoogleApiClient mGoogleApiClient = null;
    private Node mPhoneNode = null;

    // where to cache files so we don't keep re-downloading the same URLs
    private static File cacheDir;

    public static ProxyClient instance(Context context) {
        if(instance == null) {
            instance = new ProxyClient(context);
        }
        return instance;
    }

    public ProxyClient(Context c) {
        instance = this;
        context = c;
        requestResults = new HashMap<String, byte[]>();
        cacheDir = SearchActivity.instance().getCacheDir();
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

        if(mPhoneNode == null || !mGoogleApiClient.isConnected()) {
            // try to reconnect ... just in case of sporadic disconnects or bugs crashing the GoogleApiClient
            connect();
        }
    }

    private void onMessageResponse(int requestId, byte[] data) {
        if(D) Log.d(TAG, String.format("onMessageResponse(%d bytes)", data.length));
        try {
            requestResults.put(requestId, GZipper.decompress(data));
        } catch(IOException e) {
            e.printStackTrace();
            requestResults.put(requestId, new String("").getBytes());
        }
    }

    public void get(final String url, int getter, final ProxyResultHandler mResultHandler) {
        if(D) Log.d(TAG, String.format("get(%s)", url));

        final String cacheKey = SHA1(String.format("get %d %s", getter, url).getBytes());
        final File cacheFile = new File(cacheDir, cacheKey + ".cache");

        if(cacheFile.exists()) {
            // we have it cached, fetch it from the filesystem
            int size = (int) cacheFile.length();
            final byte[] data = new byte[size];
            try {
                BufferedInputStream buf = new BufferedInputStream(new FileInputStream(cacheFile));
                buf.read(data, 0, data.length);
                buf.close();
                // successful if we got here, trigger the result handler
                new Thread() {
                    public void run() {
                        if(mResultHandler != null) {
                            mResultHandler.onResult(data);
                        }
                    }
                }.start();
                // ... and quit
                return;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                // oops, getting the cache failed for some reason, continue on to try to
                // ask phone to fetch new copy from the web
            } catch (IOException e) {
                e.printStackTrace();
                // oops, getting the cache failed for some reason, continue on to try to
                // ask phone to fetch new copy from the web
            }
        }

        final int timeoutMax = 30; // in seconds
        final int requestId = requestCounter++; // unique id per get request; phone will provide back the id upon result
        requestResults.put(requestId, null);
        sendToPhone(String.format("get %d %d %s", requestId, getter, url), null, null);

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
                if(requestResults.get(requestId) == null || ((byte[])requestResults.get(requestId)).length == 0 ) {
                    // timed out, give up
                    if(D) Log.d(TAG, "get: timed out");
                    if(mResultHandler != null) {
                        mResultHandler.onFail();
                    }
                    requestResults.remove(requestId);
                } else {
                    // we have data
                    // ... cache it
                    byte[] data = (byte[]) requestResults.get(requestId);
                    if(!new String(data).contains("500 Server Error")) {
                        try {
                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(cacheFile));
                            bos.write(data);
                            bos.flush();
                            bos.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // ... and trigger the result handler
                    if(mResultHandler != null && data!= null) {
                        mResultHandler.onResult(data);
                    }
                    requestResults.remove(requestId);
                    // ... clean up old cache files if needed
                    File[] files = cacheDir.listFiles();
                    if(files.length > 80) { // trigger cleanup if we have more than 80 files
                        Arrays.sort(files, new Comparator<File>() {
                            public int compare(File f1, File f2) {
                                return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                            }
                        });
                        // but only delete the oldest 20
                        for(int i=0;i<files.length - 60;i++) {
                            if(files[i].getName().endsWith(".cache")) {
                                if(D) Log.d(TAG, String.format("deleting old cache file %s (mtime = %d)", files[i].getName(), files[i].lastModified()));
                                files[i].delete();
                            }
                        }
                    }
                }
            }
        }.start();
    }

    public static String SHA1(byte[] data) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        }
        catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] digest = md.digest(data);
        StringBuffer hexString = new StringBuffer();
        for (int i=0; i<digest.length; i++) {
            hexString.append(Integer.toHexString(0xFF & digest[i]));
        }
        return hexString.toString();
    }

}