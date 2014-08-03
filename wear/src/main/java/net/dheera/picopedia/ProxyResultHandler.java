package net.dheera.picopedia;

/**
 * Methods to asynchronously deal with the output of ProxyClient.
 */
public class ProxyResultHandler {
    public void onResult(byte[] data) { /* to be overridden */ }
    public void onFail() { /* to be overridden */ }
}