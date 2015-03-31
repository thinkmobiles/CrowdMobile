package com.kes.net;

import android.util.Log;

import com.google.gson.JsonSyntaxException;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

public class DataFetcher {
	private static final String TAG = DataFetcher.class.getSimpleName();

	public static enum RequestType {
		GET, POST, DELETE, PUT
	}	
	private static Map<String, String> acceptHeader = com.kes.net.NetUtils.defaultJsonHeader();

	
	protected static String defaultUserAgent = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.124 Safari/537.36";
	
	private long lastTime = 0;

    private static ArrayList<String> networkLog = new ArrayList<String>();

    public static String[] getNetworkLog()
    {
        synchronized (DataFetcher.class)
        {
            if (networkLog.size() == 0)
                return null;
            String[] result = new String[networkLog.size()];
            networkLog.toArray(result);
            networkLog.clear();
            return result;
        }
    }

    private static void addNetworkLog(String s)
    {
        synchronized (DataFetcher.class) {
            networkLog.add(s);
        }
    }

    public static class KESNetworkException extends Exception {
        public static final int CODE_Invalid_Response = -1;
        public static final int CODE_Unsupported_Encoding = -2;
        public static final int CODE_ClientProtocolException = -3;

        public static final int CODE_Internal_server_error = 0;
        public static final int CODE_Invalid_login_details_for_Facebook_or_Twitter = 1;
        public static final int CODE_Invalid_authentication_code = 2;
        public static final int CODE_Insufficient_credit = 3;
        public static final int CODE_Unable_to_process_image = 4;
        public static final int CODE_Message_too_long = 5;
        public static final int CODE_No_data_provided_for_photo_request = 6;
        public static final int CODE_JSON_malformed = 7;
        public static final int CODE_Photo_ID_not_found = 8;
        public static final int CODE_In_App_purchase_Product_ID_not_found = 9;
        public static final int CODE_Unable_to_process_receipt_data_from_Apple_or_Google = 10;

        public ModelFactory.ServerError error;

        public KESNetworkException()
        {
            createSimple(CODE_Invalid_Response,null);
        }

        public KESNetworkException(int code, String message)
        {
            createSimple(code, message);
        }

        private void createSimple(int code, String message)
        {
            error = new ModelFactory.ServerError();
            error.code = code;
            if (message != null)
                error.message = new String(message);
        }

        public KESNetworkException(String response)
        {
            com.kes.net.ModelFactory.ServerErrorWrapper data = null;
            try {
                data = com.kes.net.ModelFactory.getServerError(response);
            } catch (JsonSyntaxException e) {}
            if (data == null || data.errors == null)
                createSimple(CODE_Invalid_Response,null);
            else
                error = data.errors;
        }

    }

	public static DefaultHttpClient getHttpClient()
	{
        //Log.d(TAG,"Initializing HTTP client");
		DefaultHttpClient result = null;
		HttpParams params = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		// The default value is zero, that means the timeout is not used. 
		int timeoutConnection = 10000;
		HttpConnectionParams.setConnectionTimeout(params, timeoutConnection);
		// Set the default socket timeout (SO_TIMEOUT) 
		// in milliseconds which is the timeout for waiting for data.
		int timeoutSocket = 10000;
		HttpConnectionParams.setSoTimeout(params, timeoutSocket);
		HttpProtocolParams.setUserAgent(params, defaultUserAgent);
		
//		params.setParameter("User-Agent", defaultUserAgent);

		result = new DefaultHttpClient(params);
		result.addResponseInterceptor(new Interceptor());
		return result;
	}

	public static HttpGet getRequest(String url)
	{
		HttpGet retval = new HttpGet(url);
		retval.setHeader("Accept","text/html,application/json,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		retval.setHeader("Accept-Encoding", "gzip,deflate");
		retval.setHeader("Accept-Language","en-US,en;q=0.8");
		return retval;
	}
	
	protected void ensureDelay(long ms) throws InterruptedException
	{
		long now = System.currentTimeMillis();
		long elapsed = now - lastTime;

		if (elapsed < ms)
			Thread.sleep(ms - elapsed);
		lastTime = System.currentTimeMillis();
	}
	
	
	
	static class Interceptor implements HttpResponseInterceptor {
	       public void process(final HttpResponse response,
	               final HttpContext context) throws HttpException,
	               IOException {
	           HttpEntity entity = response.getEntity();
	           Header encheader = entity.getContentEncoding();
	           if (encheader != null) {
	               HeaderElement[] codecs = encheader.getElements();
	               for (int i = 0; i < codecs.length; i++) {
	                   if (codecs[i].getName().equalsIgnoreCase("gzip")) {
	                       response.setEntity(new GzipDecompressingEntity(
	                               entity));
	                       return;
	                   }
	               }
	           }
	       }
	   };
	   
	   static class GzipDecompressingEntity extends HttpEntityWrapper {
	          public GzipDecompressingEntity(final HttpEntity entity) {
	             super(entity);
	          }

	          @Override
	          public InputStream getContent() throws IOException, IllegalStateException {
	             // the wrapped entity's getContent() decides about repeatability
	             InputStream wrappedin = wrappedEntity.getContent();
	             return new GZIPInputStream(wrappedin);
	          }

	          @Override
	          public long getContentLength() {
	             // length of ungzipped content is not known
	             return -1;
	          }
	}
	   
		protected static JSONObject getJSONFromUrl(String url) throws ClientProtocolException, IOException, JSONException {
			JSONObject jObj = null;
		    HttpGet request = getRequest(url);
		    HttpResponse response = null;
            DefaultHttpClient client = getHttpClient();
            try {
				response = client.execute(request);
				StatusLine statusLine = response.getStatusLine();
				int statuscode = statusLine.getStatusCode();
				if (statuscode == 200)
				  jObj = getJSONFromResponse(response);
				else
				  throw new IOException();
		    }
		    finally {
				if (response != null)
				{
					HttpEntity entity = response.getEntity();
					if (entity != null)
						entity.consumeContent();
				}
		    }
		    return jObj;
	}

		protected static JSONObject getJSONFromResponse(HttpResponse response) throws IOException, JSONException {
			JSONObject jObj = null;
		    StringBuilder builder = new StringBuilder();
	        HttpEntity entity = response.getEntity();
	        if (entity == null)
	        	throw new IOException();
	        try
	        {
	        	InputStream content = entity.getContent();
		        BufferedReader reader = new BufferedReader(new InputStreamReader(content));
		        String line;
		        while ((line = reader.readLine()) != null) {
		          builder.append(line);
		        }
//		        String s = builder.toString();
//		        Log.d(TAG, s);
		        jObj = new JSONObject(builder.toString());
		    } finally {
		    	response.getEntity().consumeContent();
		    }
			return jObj;
		}

	   
		public static String requestAction(String url, RequestType type, Map<String, String> getParams, Map<String, String> postParams, String postData) throws KESNetworkException, InterruptedException {
			return requestAction(url, type, getParams, postParams, acceptHeader, postData);
		}
		/*
		public static String requestAction(String url, RequestType type, Map<String, String> getParams, Map<String, String> postParams,
				Map<String, String> headers, String postData) throws KESNetworkException, InterruptedException {
			return requestAction(url, type, null, getParams, postParams, headers, postData);
		}

		public static String requestAction(String url, RequestType type, Map<String, String> getParams, Map<String, String> postParams, String postData, boolean ignoreError) throws KESNetworkException, InterruptedException {
			return requestAction(url, type, null, getParams, postParams, acceptHeader, postData);
		}

		public static String requestAction(String url, RequestType type, StatusLineWrapper statusLineOut, Map<String, String> getParams,
				Map<String, String> postParams, String postData, boolean ignoreError) throws KESNetworkException, InterruptedException {
			return requestAction(url, type, statusLineOut, getParams, postParams, acceptHeader, postData);
		}
		*/

		public static String requestAction(String url, RequestType type, Map<String, String> getParams,
				Map<String, String> postParams, Map<String, String> headers, String postData)
				throws KESNetworkException,InterruptedException {

				url = buildGetUrl(url, getParams);
                addNetworkLog(url.toString());
				HttpResponse response = null;
				HttpUriRequest request = null;

                try {
                    switch (type) {
                        case GET:
                            HttpGet get = new HttpGet(url);
                            request = get;
                            break;
                        case POST:
                            HttpPost post = new HttpPost(url);
                            StringEntity entity = getPostEntity(postParams);
                            if (entity == null && postData != null) {
                                entity = new StringEntity(postData);
                                Log.v(TAG, "Entity data: " + postData);
                            }
                            if (entity != null) {
                                post.setEntity(entity);
                            }
                            request = post;
                            break;
                        case DELETE:
                            HttpDelete del = new HttpDelete(url);
                            request = del;
                            break;
                        case PUT:
                            HttpPut put = new HttpPut(url);
                            StringEntity putEntity = getPostEntity(postParams);
                            if (putEntity == null && postData != null) {
                                putEntity = new StringEntity(postData);
                                Log.v(TAG, "Entity data: " + postData);
                            }
                            if (putEntity != null) {
                                put.setEntity(putEntity);
                            }
                            request = put;
                            break;
                    }
                } catch (UnsupportedEncodingException e)
                {
                    throw new KESNetworkException(KESNetworkException.CODE_Unsupported_Encoding,null);
                }
				setHeaders(request, headers);
                DefaultHttpClient client = getHttpClient();
                try {
                    response = client.execute(request);
                } catch (IOException e) {
//                    Log.d(TAG,"WHAT the");
                    e.printStackTrace();
                    throw new KESNetworkException(KESNetworkException.CODE_ClientProtocolException,null);
                }

                if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK)
                    throw new KESNetworkException(KESNetworkException.CODE_ClientProtocolException,null);

                HttpEntity entity = response.getEntity();
		        if (entity == null)
		        	throw new KESNetworkException();
		        StringBuilder builder = new StringBuilder();
                try {
                    try {
                        InputStream content = entity.getContent();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                        String line;
                        while ((line = reader.readLine()) != null)
                            builder.append(line);
                        return checkForError(builder.toString());
                    } finally {
                        entity.consumeContent();
                    }
                } catch (IOException e)
                {
                    throw new KESNetworkException();
                }
		}

        public static String checkForError(String data) throws KESNetworkException
        {
            if (data == null)
                throw new KESNetworkException();
            if (data.startsWith("{\"errors"))
                throw new KESNetworkException(data);
            return data;
        }

		public static String buildGetUrl(String url, Map<String, String> getParams) {
			if (getParams != null && !getParams.isEmpty()) {
				if (!url.endsWith("?")) {
					url += "?";
				}
				List<NameValuePair> paramList = new ArrayList<NameValuePair>();
				for (Entry<String, String> entry : getParams.entrySet()) {
					paramList.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
				}
				String paramString = URLEncodedUtils.format(paramList, HTTP.UTF_8);
				url += paramString;
			}

			return url;
		}

		private static UrlEncodedFormEntity getPostEntity(Map<String, String> postParams) throws UnsupportedEncodingException {
			if (postParams != null && !postParams.isEmpty()) {
				List<NameValuePair> paramList = new ArrayList<NameValuePair>();
				for (Entry<String, String> entry : postParams.entrySet()) {
					paramList.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
				}
				return new UrlEncodedFormEntity(paramList, HTTP.UTF_8);
			}
			return null;
		}

		private static void setHeaders(HttpUriRequest request, Map<String, String> headers) {
			if (headers != null && !headers.isEmpty()) {
				for (Entry<String, String> entry : headers.entrySet()) {
					// Log.d("DataFetcher", "Adding header: " + entry.getKey() + ":"
					// + entry.getValue());
					request.setHeader(entry.getKey(), entry.getValue());
				}
			}
		}
		
		public static class StatusLineWrapper {
			public StatusLine status;
		}

}
