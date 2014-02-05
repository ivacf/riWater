package uk.co.ribot.riwater;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Class to handle API requests, retrieve data and return to requested fragment
 * To do: - Reduce code
 * 
 * @author Joe Birch
 */
public class NetworkAsyncTask extends AsyncTask<Void, String, String> {

    private static final String TAG = "NetworkThreadActivity";
    /**
     * List to hold parameters for POST operations
     */
    private List<NameValuePair> requestParams = new ArrayList<NameValuePair>();
    /**
     * URL of request
     */
    private String url = null;
    /**
     * Request method
     */
    private HttpMethod method = null;

    private final AsyncTaskCompleteListener<String> callback;

    private static final String CHARSET = "UTF-8";
    
    //Connection time out  in milliseconds
    private static final int CONNECTION_TIMEOUT = 30000;
    //7 days in seconds
    public static final int MAX_AGE_HTTP_CACHE = 7 * 24 * 60 * 60;
    private int mTimeout = CONNECTION_TIMEOUT;

    public enum HttpMethod {
        GET, POST;
    }

    /*
     * If method GET: All parameters must be sent as arguments in the List of
     * NameValuePair
     */
    /*
     * If method POST: POST parameters must be sent as arguments in the List of
     * NameValuePair, other parameters can be sent as part of the URL.
     */
    public NetworkAsyncTask(String url, HttpMethod method, List<NameValuePair> params, AsyncTaskCompleteListener<String> cb) {
        requestParams = params;
        this.method = method;
        this.callback = cb;
        this.url = url;
    }
    
    public void setTimeout(int timeout) {
        this.mTimeout = timeout;
    }

    /**
     * Depending on the method, parameters are added to the URL using the post
     * parameters. From here a request is constructed and the response is
     * retrieved to this request.
     */
    @Override
    protected String doInBackground(Void... params) {
        URLConnection connection = null;
        InputStream responseStream = null;
        OutputStream output = null;
        String query = URLEncodedUtils.format(requestParams, CHARSET);
        try {
            if (method == HttpMethod.GET) {
                // DO GET REQUEST
                if (url.contains("?")) {
                    throw new IllegalArgumentException(
                            "URL must not contain parameters, send parameters as arguments on NameValuePair format");
                }
                connection = new URL(url + "?" + query).openConnection();
                connection.setConnectTimeout(mTimeout);
                connection.setRequestProperty("Accept-Charset", CHARSET);
                connection.addRequestProperty("Cache-Control", "max-age=" +MAX_AGE_HTTP_CACHE);
                responseStream = connection.getInputStream();
            } else if (method == HttpMethod.POST) {
                // DO POST REQUEST
                connection = new URL(url).openConnection();
                connection.setDoOutput(true); // Triggers POST.
                connection.setRequestProperty("Accept-Charset", CHARSET);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + CHARSET);
                connection.setConnectTimeout(mTimeout);
                output = connection.getOutputStream();
                output.write(query.getBytes(CHARSET));
            } else {
                throw new IllegalArgumentException("Invalid http method");
            }
            responseStream = connection.getInputStream();
            return NetworkUtil.readResponse(responseStream, CHARSET);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Invalid URL '" + url + "'", e);
        } catch (IOException e) {
            Log.e(TAG, "IOException requesting '" + url + "'", e);
        } finally {
            if (output != null)
                try {
                    output.close();
                } catch (IOException logOrIgnore) {
                }
            if (responseStream != null)
                try {
                    responseStream.close();
                } catch (IOException logOrIgnore) {
                }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (result != null) {
            callback.onTaskComplete(result);
        } else {
            callback.onTaskFailed();
        }
    }

}
