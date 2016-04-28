package com.example.haroon.sample.api;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by Haroon on 4/3/2016.
 */
public class HttpRequest {
    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";

    public String url = "";
    public String method = METHOD_GET;
    public String tag = "";

    public HttpRequestDelegate delegate;
    public ApiRequestDelegate requestDelegate;

    public String responseString;

    private Map<String,String> postMap = new HashMap<String, String>();

    // Handle file sending
    public boolean isMultiPart = false;
    public String m_filePath = "";
    public String m_filename = "";

    private static final String lineEnd = "\r\n";
    private static final String twoHyphens = "--";
    private static final String boundary = "******";

    public void putPostParams(String key, String value){
        postMap.put(key, value);
    }

    public void doRequest(){
        new HttpRequestTask().execute();
    }

    /**
     * initTrust is to allow self-signed certificate
     */
    public static void initTrust(){

        try {
            //step 1
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            }, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());


            //step 2
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }catch (KeyManagementException e1){
            e1.printStackTrace();
        }catch (NoSuchAlgorithmException e2){
            e2.printStackTrace();
        }
    }

    private class HttpRequestTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {

            String result = "";
            try {
                result =  requestUrl();
            } catch (Exception e) {
                Log.d("SHO", e.getMessage());
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            responseString = result;
            if (delegate != null){
                delegate.requestCompleted(HttpRequest.this);
            }
        }

        private String requestUrl() throws IOException {
            InputStream is = null;

            try {
                URL urlObject = new URL(url);

                HttpURLConnection conn = (HttpURLConnection) urlObject.openConnection();

                conn.setReadTimeout(10000); //milliseconds
                conn.setConnectTimeout(15000); //milliseconds
                conn.setRequestMethod(method);
                conn.setDoInput(true);

                if (METHOD_POST.equals(method)) {
                    // application/x-www-form-urlencoded
                    if ( isMultiPart == false ) {
                        conn.setDoOutput(true);
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        String query = makeQueryString();

                        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                        wr.writeBytes(query);
                        wr.flush();
                        wr.close();
                    }
                    // multipart/form-data;boundary
                    else if ( isMultiPart == true ) {
                        int bytesRead, bytesAvailable, bufferSize;
                        byte[] buffer;
                        int maxBufferSize = 1 * 1024 * 1024;

                        conn.setDoOutput(true);
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                        conn.setRequestProperty("Connection", "Keep-Alive"); // TODO Not sure needed
                        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());

                        // Header
                        wr.writeBytes(twoHyphens + boundary + lineEnd); // Consider start with boundary

                        // PARAMS SECTION

                        // Write normal key values
                        writeMultipartMap(wr);

                        // FILE SECTION
                        File file = new File(m_filePath);

                        // Send multipart header
                        wr.writeBytes("Content-Disposition: form-data; name=\""+ m_filename + "\";filename=\""+ file.getName() + "\"" + lineEnd);
                        //wr.writeBytes("Content-Disposition: form-data; name=\""+ m_filename + "\"" + lineEnd);
                        wr.writeBytes("Content-Type: application/octet-stream" + lineEnd); // TODO Not sure needed
                        wr.writeBytes("Content-Transfer-Encoding: binary" + lineEnd); // TODO Not sure needed
                        wr.writeBytes(lineEnd);

                        // Read file and create buffer
                        FileInputStream fis = new FileInputStream(file);
                        bytesAvailable = fis.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        buffer = new byte[bufferSize];

                        // Send file data
                        bytesRead = fis.read(buffer, 0, bufferSize);
                        while (bytesRead > 0) {
                            // Write buffer to socket
                            wr.write(buffer, 0, bufferSize);

                            bytesAvailable = fis.available();
                            bufferSize = Math.min(bytesAvailable, maxBufferSize);
                            bytesRead = fis.read(buffer, 0, bufferSize);
                        }

                        // send multipart form data necesssary after file data
                        wr.writeBytes(lineEnd);
                        // Footer
                        wr.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                        // Clean
                        wr.flush();
                        wr.close();
                        fis.close();
                    }
                }

                conn.connect();

                int responseCode = conn.getResponseCode();

                Log.d("SHO", "responseCode = " + responseCode);

                if (responseCode >= 400){
                    is = conn.getErrorStream();
                }else{
                    is = conn.getInputStream();
                }

                return readStreamToString(is);
            } finally {
                if (is!=null){
                    is.close();
                }
            }

        }

        // Build string out of key / values
        private String makeQueryString() throws UnsupportedEncodingException {
            if (postMap.isEmpty()){
                return "";
            }
            StringBuilder builder = new StringBuilder();

            boolean first=true;
            for (Map.Entry<String, String> entry : postMap.entrySet()){
                if (!first){
                    builder.append("&");
                }

                builder.append(entry.getKey());
                builder.append("=");
                builder.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                if (first){
                    first = false;
                }
            }
            return builder.toString();
        }

        // Directly send key / values in multipart message
        private void writeMultipartMap(DataOutputStream wr)  throws IOException {
            if (postMap.isEmpty()){
                return;
            }

            // Loop over map
            for (Map.Entry<String, String> entry : postMap.entrySet()){
                wr.writeBytes("Content-Disposition: form-data; name=\""+ entry.getKey() + "\""+ lineEnd);
                wr.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd); // TODO Not sure needed
                wr.writeBytes("Content-Transfer-Encoding: 8bit" + lineEnd); // TODO Not sure needed
                wr.writeBytes(lineEnd);

                // Send data
                wr.writeBytes(URLEncoder.encode(entry.getValue(), "UTF-8"));

                // Footer
                wr.writeBytes(lineEnd);
                wr.writeBytes(twoHyphens + boundary + lineEnd);
            }
        }

        public String readStreamToString(InputStream stream) throws IOException {
            BufferedReader reader = null;
            reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));

            StringBuilder builder = new StringBuilder();
            String line = "";
            char[] chars = new char[4*1024];
            int len;

            while ((len = reader.read(chars)) >= 0){
                builder.append(chars, 0, len);
            }

            return builder.toString();
        }
    }




}
