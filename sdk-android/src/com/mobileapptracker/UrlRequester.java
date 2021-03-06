package com.mobileapptracker;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

class UrlRequester {
    // HTTP client for firing requests
    private HttpClient client;
    
    public UrlRequester() {
        // Set up HttpClient
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        HttpConnectionParams.setConnectionTimeout(params, MATConstants.TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, MATConstants.TIMEOUT);
        
        ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
        
        client = new DefaultHttpClient(ccm, params);
    }
    
    /**
     * Does an HTTP request to the given url, GET or POST based on whether json was passed or not
     * @param url the url to hit
     * @param json JSONObject with event item and iap verification json, if not null or empty then will POST to url
     * @return JSONObject of the server response, null if request failed
     */
    public JSONObject requestUrl(String url, JSONObject json) {
        HttpResponse response = null;
        
        // If no JSON passed, do HttpGet
        if (json == null || json.length() == 0) {
            try {
                response = client.execute(new HttpGet(url));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Put JSON as entity for HttpPost
            try {
                StringEntity se = new StringEntity(json.toString());
                se.setContentType("application/json");
                
                HttpPost request = new HttpPost(url);
                request.setEntity(se);
                response = client.execute(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (response != null) {
            try {
                StatusLine statusLine = response.getStatusLine();
                Log.d(MATConstants.TAG, "Request completed with status " + statusLine.getStatusCode());
                if (statusLine.getStatusCode() >= 200 && statusLine.getStatusCode() <= 299) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                    StringBuilder builder = new StringBuilder();
                    for (String line = null; (line = reader.readLine()) != null;) {
                        builder.append(line).append("\n");
                    }
                    reader.close();
                    if (builder.length() > 0) {
                        JSONTokener tokener = new JSONTokener(builder.toString());
                        return new JSONObject(tokener);
                    } else {
                        return new JSONObject();
                    }
                }
                // for HTTP 3XX and 4XX, assume we're doing it wrong and it will never succeed
                // for HTTP 5XX, assume the server is broken and will be fixed later
                else if (statusLine.getStatusCode() >= 500) {
                    return new JSONObject();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
