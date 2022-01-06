/**
 * @projectName ucella-master
 * @fileName HttpTools.java
 * @packageName com.ucella.base.http
 * @author xaoyaoyao
 * @date 2020/4/16 9:11 上午
 * @version V1.0
 * @copyright (c) 2015-2020, xaoyaoyao@aliyun.com All Rights Reserved.
 */
package org.example.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

/**
 * @author xaoyaoyao
 * @className HttpTools
 * @description
 * @date 2020/4/16 9:11 上午
 */
public class HttpTools {

	public final static int  DEFAULT_MAX_TOTAL          = 400;
	public final static int  DEFAULT_MAX_PER_ROUTE      = 200;
	public final static int  DEFAULT_TIMEOUT            = 60000;
	public final static long DEFAULT_KEEP_ALIVE_TIMEOUT = 60000L;

	public final static int CONNECTION_REQUEST_TIMEOUT = DEFAULT_TIMEOUT;
	public final static int CONNECT_TIMEOUT            = DEFAULT_TIMEOUT;
	public final static int SOCKET_TIMEOUT             = DEFAULT_TIMEOUT;

	public final static  String   CONTENT_TYPE_APPLICATION_JSON     = "application/json;charset=utf-8";
	public static final  String   APPLICATION_OCTET_STREAM_VALUE    = "application/octet-stream;charset=utf-8";
	public final static  String   CONTENT_TYPE_APPLICATION_SOAP_XML = "application/soap+xml;charset=utf-8";
	public final static  String   CONTENT_TYPE_TEXT_XML             = "text/xml;charset=utf-8";
	private final static String[] UA_ARR                            = {
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:49.0) Gecko/20100101 Firefox/49.0",
			"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.80 Safari/537.36 Core/1.47.933.400 QQBrowser/9.4.8699.400",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_1) AppleWebKit/602.2.14 (KHTML, like Gecko) Version/10.0.1 Safari/602.2.14",
			"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/602.1.50 (KHTML, like Gecko) Version/10.0 Safari/602.1.50",
			"Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; Win64; x64; Trident/4.0; .NET CLR 2.0.50727; SLCC2; .NET CLR 3.5.30729; .NET CLR 3.0.30729; .NET4.0C; .NET4.0E; Tablet PC 2.0)"
	};
	public final static  String   DEFAULT_UA                        = "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0";

	public final static  int SC_MOVED_PERMANENTLY = 301;
	public final static  int SC_MOVED_TEMPORARILY = 302;
	public final static  int STATUS_CODE_ERROR_0  = 0;
	private final static int SC_MAX_TIMES         = 3;

	/**
	 * 设置SSLConnectionSocketFactory相关信息
	 *
	 * @return org.apache.http.conn.ssl.SSLConnectionSocketFactory
	 * @author xaoyaoyao
	 * @date 2020/4/16 9:50 上午
	 */
	protected static SSLConnectionSocketFactory getSslConnectionSocketFactory() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
		SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
			// 信任所有
			@Override
			public boolean isTrusted(X509Certificate[] chain, String authType) {
				return true;
			}
		}).build();
		return new SSLConnectionSocketFactory(sslContext, new String[]{ "TLSv1.2", "TLSv1.1", "TLSv1", "SSLv3" }, null, new HostnameVerifier() {
			@Override
			public boolean verify(String arg0, SSLSession arg1) {
				return true;
			}
		});
	}

	/**
	 * @param connectionRequestTimeout
	 * @param connectTimeout
	 * @param socketTimeout
	 *
	 * @return
	 */
	protected static RequestConfig setRequestConfig(final int connectionRequestTimeout, final int connectTimeout, final int socketTimeout) {
		return RequestConfig.custom().setConnectionRequestTimeout(connectionRequestTimeout).setConnectTimeout(connectTimeout).setSocketTimeout(socketTimeout).build();
	}

	/**
	 * @return
	 */
	protected static RequestConfig setRequestConfig() {
		return setRequestConfig(CONNECTION_REQUEST_TIMEOUT, CONNECT_TIMEOUT, SOCKET_TIMEOUT);
	}

	/**
	 * @param requestConfig
	 * @param sslSocketFactory
	 *
	 * @return
	 */
	protected static HttpClientBuilder setHttpClientBuilder(RequestConfig requestConfig, SSLConnectionSocketFactory sslSocketFactory) {
		return HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).setSSLSocketFactory(sslSocketFactory);
	}

	/**
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws KeyManagementException
	 */
	protected static HttpClientBuilder setHttpClientBuilder() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		return setHttpClientBuilder(setRequestConfig(), getSslConnectionSocketFactory());
	}

	/**
	 * 设置keepAlive
	 *
	 * @param httpClientBuilder
	 * @param keepAliveTimeout
	 *
	 * @return void
	 * @author xaoyaoyao
	 * @date 2020/4/16 9:50 上午
	 */
	protected static void setKeepAliveStrategy(HttpClientBuilder httpClientBuilder, Long keepAliveTimeout) {
		//定义连接管理器将由多个客户端实例共享。如果连接管理器是共享的，则其生命周期应由调用者管理，如果客户端关闭则不会关闭。
		httpClientBuilder.setConnectionManagerShared(true);
		//设置KeepAlive
		keepAliveTimeout = null == keepAliveTimeout || keepAliveTimeout < 60L ? 60000 : keepAliveTimeout;
		final long finalKeepAliveTimeout = keepAliveTimeout;
		ConnectionKeepAliveStrategy connectionKeepAliveStrategy = (response, context) -> {
			// Honor 'keep-alive' header
			HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
			while (it.hasNext()) {
				HeaderElement he    = it.nextElement();
				String        param = he.getName();
				String        value = he.getValue();
				if (value != null && param.equalsIgnoreCase("timeout")) {
					try {
						return Long.parseLong(value) * 1000;
					} catch (NumberFormatException ignore) {
					}
				}
			}
			return finalKeepAliveTimeout;
		};
		httpClientBuilder.setKeepAliveStrategy(connectionKeepAliveStrategy);
	}

	/**
	 * get请求
	 *
	 * @param httpclient
	 * @param url
	 * @param encoding
	 * @param headerParams
	 *
	 * @return java.lang.String
	 * @author xaoyaoyao
	 * @date 2020/4/16 9:50 上午
	 */
	protected static String getByHeaderParams(CloseableHttpClient httpclient, String url, String encoding, Map<String, String> headerParams) throws IOException {
		return getByHeaderParams(httpclient, url, encoding, headerParams, 0);
	}

	private static String getByHeaderParams(CloseableHttpClient httpclient, String url, String encoding, Map<String, String> headerParams, int times) throws IOException {
		encoding = StringUtils.isBlank(encoding) ? "UTF-8" : encoding;
		HttpGet               httpGet  = new HttpGet(url);
		CloseableHttpResponse response = null;
		String                httpStr  = null;
		try {
			if (MapUtils.isNotEmpty(headerParams)) {
				for (Map.Entry<String, String> entry : headerParams.entrySet()) {
					String key   = entry.getKey();
					String value = entry.getValue();
					httpGet.addHeader(key, value);
				}
			}
			response = httpclient.execute(httpGet);
			Header[] locationHeaders = response.getHeaders("location");
			int      statusCode      = getStatusCode(response);
			boolean  isRedirected    = isRedirected(statusCode);
			String   location        = ArrayUtils.isNotEmpty(locationHeaders) ? locationHeaders[0].getValue() : null;
			if (isRedirected && StringUtils.isNotBlank(location)) {
				// 301 or 302 重定向问题
				times = times + 1;
				if (times < SC_MAX_TIMES) {
					return getByHeaderParams(httpclient, location, encoding, headerParams, times);
				}
			}
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
				httpStr = IOUtils.toString(instream, encoding);
			}
		} finally {
			close(httpclient, response, httpGet);
		}
		return httpStr;
	}

	protected static String delete(CloseableHttpClient httpclient, String url, String encoding, Map<String, String> headerParams) throws IOException {
		return delete(httpclient, url, encoding, headerParams, 0);
	}

	private static String delete(CloseableHttpClient httpclient, String url, String encoding, Map<String, String> headerParams, int times) throws IOException {
		encoding = StringUtils.isBlank(encoding) ? "UTF-8" : encoding;
		HttpDelete            httpGet  = new HttpDelete(url);
		CloseableHttpResponse response = null;
		String                httpStr  = null;
		try {
			if (MapUtils.isNotEmpty(headerParams)) {
				for (Map.Entry<String, String> entry : headerParams.entrySet()) {
					String key   = entry.getKey();
					String value = entry.getValue();
					httpGet.addHeader(key, value);
				}
			}
			response = httpclient.execute(httpGet);
			Header[] locationHeaders = response.getHeaders("location");
			int      statusCode      = getStatusCode(response);
			boolean  isRedirected    = isRedirected(statusCode);
			String   location        = ArrayUtils.isNotEmpty(locationHeaders) ? locationHeaders[0].getValue() : null;
			if (isRedirected && StringUtils.isNotBlank(location)) {
				// 301 or 302 重定向问题
				times = times + 1;
				if (times < SC_MAX_TIMES) {
					return delete(httpclient, location, encoding, headerParams, times);
				}
			}
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
				httpStr = IOUtils.toString(instream, encoding);
			}
		} finally {
			close(httpclient, response, httpGet);
		}
		return httpStr;
	}

	/**
	 * @param httpclient
	 * @param url
	 * @param encoding
	 * @param headerParams
	 *
	 * @return
	 * @throws IOException
	 */
	protected static HttpResponse getToHttpResponse(CloseableHttpClient httpclient, String url, String encoding, Map<String, String> headerParams) throws IOException {
		return getToHttpResponse(httpclient, url, encoding, headerParams, 0);
	}

	private static HttpResponse getToHttpResponse(CloseableHttpClient httpclient, String url, String encoding, Map<String, String> headerParams, int times) throws IOException {
		encoding = StringUtils.isBlank(encoding) ? "UTF-8" : encoding;
		HttpGet               httpGet  = new HttpGet(url);
		CloseableHttpResponse response = null;
		String                httpStr  = null;
		try {
			if (MapUtils.isNotEmpty(headerParams)) {
				for (Map.Entry<String, String> entry : headerParams.entrySet()) {
					String key   = entry.getKey();
					String value = entry.getValue();
					httpGet.addHeader(key, value);
				}
			}
			response = httpclient.execute(httpGet);
			Header[] locationHeaders = response.getHeaders("location");
			int      statusCode      = getStatusCode(response);
			boolean  isRedirected    = isRedirected(statusCode);
			String   location        = ArrayUtils.isNotEmpty(locationHeaders) ? locationHeaders[0].getValue() : null;
			if (isRedirected && StringUtils.isNotBlank(location)) {
				// 301 or 302 重定向问题
				times = times + 1;
				if (times < SC_MAX_TIMES) {
					return getToHttpResponse(httpclient, location, encoding, headerParams, times);
				}
			}
		} finally {
			close(httpclient, response, httpGet);
		}
		return response;
	}

	/**
	 * post请求
	 *
	 * @param httpclient
	 * @param url
	 * @param params
	 * @param headerParams
	 *
	 * @return java.lang.String
	 * @author xaoyaoyao
	 * @date 2020/4/16 9:49 上午
	 */
	protected static String post(CloseableHttpClient httpclient, String url, Map<String, Object> params, Map<String, String> headerParams) throws IOException {
		return post(httpclient, url, params, headerParams, 0);
	}

	private static String post(CloseableHttpClient httpclient, String url, Map<String, Object> params, Map<String, String> headerParams, int times) throws IOException {
		HttpPost              httpPost = addHttpPostHeader(url, headerParams);
		CloseableHttpResponse response = null;
		String                httpStr  = null;
		try {
			List<NameValuePair> pairList = new ArrayList<NameValuePair>(params.size());
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				Object        val  = entry.getValue();
				NameValuePair pair = new BasicNameValuePair(entry.getKey(), null != val ? val.toString() : "");
				pairList.add(pair);
			}
			httpPost.setEntity(new UrlEncodedFormEntity(pairList, Charset.forName("UTF-8")));
			response = httpclient.execute(httpPost);
			Header[] locationHeaders = response.getHeaders("location");
			int      statusCode      = getStatusCode(response);
			boolean  isRedirected    = isRedirected(statusCode);
			String   location        = ArrayUtils.isNotEmpty(locationHeaders) ? locationHeaders[0].getValue() : null;
			if (isRedirected && StringUtils.isNotBlank(location)) {
				// 301 or 302 重定向问题
				times = times + 1;
				if (times < SC_MAX_TIMES) {
					return post(httpclient, location, params, headerParams, times);
				}
			}
			HttpEntity entity = response.getEntity();
			httpStr = EntityUtils.toString(entity, "UTF-8");
		} finally {
			close(httpclient, response, httpPost);
		}
		return httpStr;
	}

	protected static HttpResponse postToHttpResponse(CloseableHttpClient httpclient, String url, Map<String, Object> params, Map<String, String> headerParams) throws IOException {
		return postToHttpResponse(httpclient, url, params, headerParams, 0);
	}

	private static HttpResponse postToHttpResponse(CloseableHttpClient httpclient, String url, Map<String, Object> params, Map<String, String> headerParams, int times) throws IOException {
		HttpPost              httpPost = addHttpPostHeader(url, headerParams);
		CloseableHttpResponse response = null;
		try {
			List<NameValuePair> pairList = new ArrayList<NameValuePair>(params.size());
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				Object        val  = entry.getValue();
				NameValuePair pair = new BasicNameValuePair(entry.getKey(), null != val ? val.toString() : "");
				pairList.add(pair);
			}
			httpPost.setEntity(new UrlEncodedFormEntity(pairList, Charset.forName("UTF-8")));
			response = httpclient.execute(httpPost);
			Header[] locationHeaders = response.getHeaders("location");
			int      statusCode      = getStatusCode(response);
			boolean  isRedirected    = isRedirected(statusCode);
			String   location        = ArrayUtils.isNotEmpty(locationHeaders) ? locationHeaders[0].getValue() : null;
			if (isRedirected && StringUtils.isNotBlank(location)) {
				// 301 or 302 重定向问题
				times = times + 1;
				if (times < SC_MAX_TIMES) {
					return postToHttpResponse(httpclient, location, params, headerParams, times);
				}
			}
		} finally {
			close(httpclient, response, httpPost);
		}
		return response;
	}

	protected static int getStatusCode(CloseableHttpResponse response) {
		return null != response ? response.getStatusLine().getStatusCode() : STATUS_CODE_ERROR_0;
	}

	protected static boolean isRedirected(int statusCode) {
		return statusCode == SC_MOVED_PERMANENTLY || statusCode == SC_MOVED_TEMPORARILY;
	}

	private static HttpPost addHttpPostHeader(String url, Map<String, String> headerParams) {
		HttpPost httpPost = new HttpPost(url);
		if (MapUtils.isNotEmpty(headerParams)) {
			for (Map.Entry<String, String> entry : headerParams.entrySet()) {
				httpPost.addHeader(entry.getKey(), entry.getValue());
			}
		}
		return httpPost;
	}

	/**
	 * post json
	 *
	 * @param httpclient
	 * @param bodyStr
	 * @param url
	 * @param contentType
	 * @param headerParams
	 *
	 * @return java.lang.String
	 * @author xaoyaoyao
	 * @date 2020/4/16 9:48 上午
	 */
	protected static String postRequestBody(CloseableHttpClient httpclient, String bodyStr, String url, String contentType, Map<String, String> headerParams) throws IOException {
		if (StringUtils.isBlank(url) || StringUtils.isBlank(bodyStr)) {
			return null;
		}
		HttpPost              httpPost = addHttpPostHeader(url, headerParams);
		CloseableHttpResponse response = null;
		String                httpStr  = null;
		try {
			ContentType ct = ContentType.parse(contentType);
			httpPost.setEntity(new StringEntity(bodyStr, ct));
			response = httpclient.execute(httpPost);
			httpStr = EntityUtils.toString(response.getEntity(), "UTF-8");
		} finally {
			close(httpclient, response, httpPost);
		}
		return httpStr;
	}

	protected static String patch(CloseableHttpClient httpclient, String bodyStr, String url, String contentType, Map<String, String> headerParams) throws IOException {
		if (StringUtils.isBlank(url) || StringUtils.isBlank(bodyStr)) {
			return null;
		}
		HttpPatch httpPatch = new HttpPatch(url);
		if (MapUtils.isNotEmpty(headerParams)) {
			for (Map.Entry<String, String> entry : headerParams.entrySet()) {
				httpPatch.addHeader(entry.getKey(), entry.getValue());
			}
		}
		CloseableHttpResponse response = null;
		String                httpStr  = null;
		try {
			ContentType ct = ContentType.parse(contentType);
			httpPatch.setEntity(new StringEntity(bodyStr, ct));
			response = httpclient.execute(httpPatch);
			httpStr = EntityUtils.toString(response.getEntity(), "UTF-8");
		} finally {
			close(httpclient, response, httpPatch);
		}
		return httpStr;
	}

	protected static String put(CloseableHttpClient httpclient, String bodyStr, String url, String contentType, Map<String, String> headerParams) throws IOException {
		if (StringUtils.isBlank(url) || StringUtils.isBlank(bodyStr)) {
			return null;
		}
		HttpPut httpPut = new HttpPut(url);
		if (MapUtils.isNotEmpty(headerParams)) {
			for (Map.Entry<String, String> entry : headerParams.entrySet()) {
				httpPut.addHeader(entry.getKey(), entry.getValue());
			}
		}
		CloseableHttpResponse response = null;
		String                httpStr  = null;
		try {
			ContentType ct = ContentType.parse(contentType);
			httpPut.setEntity(new StringEntity(bodyStr, ct));
			response = httpclient.execute(httpPut);
			httpStr = EntityUtils.toString(response.getEntity(), "UTF-8");
		} finally {
			close(httpclient, response, httpPut);
		}
		return httpStr;
	}

	protected static HttpResponse postRequestBodyToHttpResponse(CloseableHttpClient httpclient, String bodyStr, String url, String contentType, Map<String, String> headerParams) throws
			IOException {
		if (StringUtils.isBlank(url) || StringUtils.isBlank(bodyStr)) {
			return null;
		}
		HttpPost              httpPost = addHttpPostHeader(url, headerParams);
		CloseableHttpResponse response = null;
		try {
			ContentType ct = ContentType.parse(contentType);
			httpPost.setEntity(new StringEntity(bodyStr, ct));
			response = httpclient.execute(httpPost);
		} finally {
			close(httpclient, response, httpPost);
		}
		return response;
	}

	/**
	 * post soap格式
	 *
	 * @param httpclient
	 * @param soapRequestXML
	 * @param url
	 * @param contentType
	 * @param soapAction
	 * @param encoding
	 *
	 * @return java.lang.String
	 * @author xaoyaoyao
	 * @date 2020/4/16 9:48 上午
	 */
	protected static String postSOAPRequest(CloseableHttpClient httpclient, String soapRequestXML, String url, String contentType, String soapAction, String encoding) throws IOException {
		encoding = org.apache.commons.lang.StringUtils.isBlank(encoding) ? "UTF-8" : encoding;
		if (StringUtils.isBlank(url) || StringUtils.isBlank(soapRequestXML)) {
			return null;
		}
		HttpPost              httpPost = new HttpPost(url);
		CloseableHttpResponse response = null;
		String                httpStr  = null;
		if (StringUtils.isNotBlank(soapAction)) {
			httpPost.setHeader("SOAPAction", soapAction);
		}
		if (StringUtils.isBlank(contentType)) {
			contentType = "application/soap+xml;charset=utf-8";
		}
		try {
			ContentType ct = ContentType.parse(contentType);
			httpPost.setEntity(new StringEntity(soapRequestXML, ct));
			response = httpclient.execute(httpPost);
			httpStr = EntityUtils.toString(response.getEntity(), encoding);
		} finally {
			close(httpclient, response, httpPost);
		}
		return httpStr;
	}

	/**
	 * post请求xml格式
	 *
	 * @param httpclient
	 * @param xml
	 * @param url
	 * @param contentType
	 * @param authorization
	 * @param cookie
	 *
	 * @return java.lang.String
	 * @author xaoyaoyao
	 * @date 2020/4/16 9:48 上午
	 */
	protected static String postXML(CloseableHttpClient httpclient, String xml, String url, String contentType, String authorization, String cookie) throws IOException {
		if (StringUtils.isBlank(xml) || StringUtils.isBlank(url)) {
			return null;
		}
		HttpPost              httpPost = new HttpPost(url);
		CloseableHttpResponse response = null;
		if (StringUtils.isNotBlank(cookie)) {
			httpPost.addHeader("Cookie", cookie);
		}
		if (StringUtils.isNotBlank(authorization)) {
			httpPost.setHeader("Authorization", authorization);
		}
		String httpStr = null;
		try {
			ContentType ct = ContentType.parse(contentType);
			httpPost.setEntity(new StringEntity(xml, ct));
			response = httpclient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			httpStr = EntityUtils.toString(entity, "UTF-8");
		} finally {
			close(httpclient, response, httpPost);
		}
		return httpStr;
	}

	/**
	 * 关闭连接
	 *
	 * @param httpclient
	 * @param response
	 * @param httpRequest
	 *
	 * @throws IOException
	 */
	public static void close(CloseableHttpClient httpclient, CloseableHttpResponse response, HttpRequestBase httpRequest) throws IOException {
		if (response != null) {
			EntityUtils.consume(response.getEntity());
			response.close();
		}
		if (null != httpRequest) {
			httpRequest.releaseConnection();
		}
		if (null != httpclient) {
			httpclient.close();
		}
	}

	/**
	 * 获取随机UA
	 *
	 * @return java.lang.String
	 * @author xaoyaoyao
	 * @date 2020/4/16 9:47 上午
	 */
	protected static String getRandomUA() {
		Random random = new Random();
		int    index  = random.nextInt(UA_ARR.length);
		return UA_ARR[index];
	}
}
