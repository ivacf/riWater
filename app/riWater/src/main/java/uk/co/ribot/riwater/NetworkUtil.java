package uk.co.ribot.riwater;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.HttpResponseCache;
import android.util.Log;

public class NetworkUtil {

    // Check whether network is available
    public static Boolean isNetworkAvailable(Context context) {
        ConnectivityManager mManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo current = mManager.getActiveNetworkInfo();
        if (current == null) {
            return false;
        }
        return (current.getState() == NetworkInfo.State.CONNECTED);
    }

    // Transforms the InputStream into a String
    public static String readResponse(final InputStream response, final String charset) throws IOException {

        BufferedReader reader = null;
        StringBuffer responseBuffer = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(response, charset));
            for (String line; (line = reader.readLine()) != null;) {
                responseBuffer.append(line + "\n");
            }
            return responseBuffer.toString();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException logOrIgnore) {
                }
        }
    }

    public static void setupHttpCache(Context context) {
        try {
            File httpCacheDir = new File(context.getCacheDir(), "http");
            long httpCacheSize = 10 * 1024 * 1024; // 10 MiB cache
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            if (BuildConfig.DEBUG) Log.w("HttpCache", "HTTP response cache installation failed:" + e);
        }
    }

    public static void flushHttpCache() {
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            if (BuildConfig.DEBUG) Log.i("HttpCache", "RequestCount: " + cache.getRequestCount() + " NetworkCount: " + cache.getNetworkCount()
                    + " HitCount " + cache.getHitCount());
            cache.flush();
        }
    }
}
