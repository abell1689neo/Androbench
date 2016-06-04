package com.andromeda.androbench2;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;

import com.andromeda.androbench2.R;

import android.content.Context;
import android.util.Log;

public class CustomHttpClient extends DefaultHttpClient {

    private static Context appContext = null;
    private static HttpParams params = null;
    private static SchemeRegistry schmReg = null;
    private static Scheme httpsScheme = null;
    private static Scheme httpScheme = null;
    private static String TAG = "MyHttpClient";

    public CustomHttpClient(Context myContext) {

        appContext = myContext;

        if (httpScheme == null || httpsScheme == null) {
            httpScheme = new Scheme("http", PlainSocketFactory.getSocketFactory(), 80);
            httpsScheme = new Scheme("https", mySSLSocketFactory(), 443);
        }

        getConnectionManager().getSchemeRegistry().register(httpScheme);
        getConnectionManager().getSchemeRegistry().register(httpsScheme);

    }

    private SSLSocketFactory mySSLSocketFactory() {
        SSLSocketFactory ret = null;
        try {
            final KeyStore ks = KeyStore.getInstance("BKS");

            final InputStream inputStream = appContext.getResources().openRawResource(R.raw.androbench);

            ks.load(inputStream, "androbench".toCharArray());
            inputStream.close();

            ret = new SSLSocketFactory(ks);
        } catch (UnrecoverableKeyException ex) {
            Log.d(TAG, ex.getMessage());
        } catch (KeyStoreException ex) {
            Log.d(TAG, ex.getMessage());
        } catch (KeyManagementException ex) {
            Log.d(TAG, ex.getMessage());
        } catch (NoSuchAlgorithmException ex) {
            Log.d(TAG, ex.getMessage());
        } catch (IOException ex) {
            Log.d(TAG, ex.getMessage());
        } catch (Exception ex) {
            Log.d(TAG, ex.getMessage());
        } finally {
            return ret;
        }
    }
}
