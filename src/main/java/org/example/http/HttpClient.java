/**
 * @projectName ucella-master
 * @fileName HttpClient.java
 * @packageName com.ucella.base.http
 * @author xaoyaoyao
 * @date 2020/4/9 13:40
 * @version V1.0
 * @copyright (c) 2020, xaoyaoyao@aliyun.com All Rights Reserved.
 */
package org.example.http;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

/**
 * @author xaoyaoyao
 * @className HttpClient
 * @description
 * @date 2020/4/9 13:40
 */
public class HttpClient {

	public final static String CONTENT_TYPE_APPLICATION_JSON     = HttpTools.CONTENT_TYPE_APPLICATION_JSON;
	public static final String APPLICATION_OCTET_STREAM_VALUE    = HttpTools.APPLICATION_OCTET_STREAM_VALUE;
	public final static String CONTENT_TYPE_APPLICATION_SOAP_XML = HttpTools.CONTENT_TYPE_APPLICATION_SOAP_XML;
	public final static String CONTENT_TYPE_TEXT_XML             = HttpTools.CONTENT_TYPE_TEXT_XML;

	private static HttpClientBuilder                  httpClientBuilder = null;
	private static PoolingHttpClientConnectionManager connManager       = null;

	/**
	 * 最大连接数
	 */
	private Integer maxTotal;

	/**
	 * 默认路由数
	 */
	private Integer defaultMaxPerRoute;

	/**
	 * ua
	 */
	private String userAgent;

	/**
	 * keep alive 时间毫秒
	 */
	private Long keepAliveTimeout;

	/**
	 * 请求连接时间 时间毫秒
	 */
	private Integer connectionRequestTimeout;

	/**
	 * 连接超时 时间毫秒
	 */
	private Integer connectTimeout;

	/**
	 * socket超时 时间毫秒
	 */
	private Integer socketTimeout;

	/**
	 * 重试次数
	 */
	private Integer retryCount;

	HttpClient(Builder builder) {
		super();
		this.userAgent = builder.userAgent;
		this.keepAliveTimeout = builder.keepAliveTimeout;
		this.maxTotal = builder.maxTotal;
		this.defaultMaxPerRoute = builder.defaultMaxPerRoute;
		this.connectionRequestTimeout = builder.connectionRequestTimeout;
		this.connectTimeout = builder.connectTimeout;
		this.socketTimeout = builder.socketTimeout;
		this.retryCount = builder.retryCount;
	}

	public static final class Builder {

		/**
		 * 最大连接数
		 */
		Integer maxTotal;

		/**
		 * 默认路由数
		 */
		Integer defaultMaxPerRoute;

		/**
		 * ua
		 */
		String userAgent;

		/**
		 * keep alive 时间毫秒
		 */
		Long keepAliveTimeout;

		/**
		 * 请求连接时间 时间毫秒
		 */
		Integer connectionRequestTimeout;

		/**
		 * 连接超时 时间毫秒
		 */
		Integer connectTimeout;

		/**
		 * socket超时 时间毫秒
		 */
		Integer socketTimeout;

		/**
		 * 重试次数
		 */
		Integer retryCount;

		public Builder setMaxTotal(Integer maxTotal) {
			this.maxTotal = maxTotal;
			return this;
		}

		public Builder setDefaultMaxPerRoute(Integer defaultMaxPerRoute) {
			this.defaultMaxPerRoute = defaultMaxPerRoute;
			return this;
		}

		public Builder setUserAgent(String userAgent) {
			this.userAgent = userAgent;
			return this;
		}

		public Builder setKeepAliveTimeout(Long keepAliveTimeout) {
			this.keepAliveTimeout = keepAliveTimeout;
			return this;
		}

		public Builder setConnectionRequestTimeout(Integer connectionRequestTimeout) {
			this.connectionRequestTimeout = connectionRequestTimeout;
			return this;
		}

		public Builder setConnectTimeout(Integer connectTimeout) {
			this.connectTimeout = connectTimeout;
			return this;
		}

		public Builder setSocketTimeout(Integer socketTimeout) {
			this.socketTimeout = socketTimeout;
			return this;
		}

		public Builder setRetryCount(Integer retryCount) {
			this.retryCount = retryCount;
			return this;
		}

		public HttpClient build() {
			return new HttpClient(this);
		}
	}

	private void init() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		this.setConnectionManager();
		this.setHttpClientBuilder();
//        this.setRedirectStrategy();
		this.setKeepAliveStrategy();
		this.setRetryHandler();
		httpClientBuilder.setConnectionManager(connManager).setUserAgent(userAgent);
	}

	private RequestConfig setRequestConfig() {
		connectionRequestTimeout = null == connectionRequestTimeout ? HttpTools.CONNECTION_REQUEST_TIMEOUT : connectionRequestTimeout;
		connectTimeout = null == connectTimeout ? HttpTools.CONNECT_TIMEOUT : connectTimeout;
		socketTimeout = null == socketTimeout ? HttpTools.SOCKET_TIMEOUT : socketTimeout;
		return HttpTools.setRequestConfig(connectionRequestTimeout, connectTimeout, socketTimeout);
	}

	private void setHttpClientBuilder() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
		httpClientBuilder = null == httpClientBuilder ? HttpTools.setHttpClientBuilder(this.setRequestConfig(), HttpTools.getSslConnectionSocketFactory()) : httpClientBuilder;
	}

	private void setConnectionManager() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		if (null == connManager) {
			maxTotal = null == maxTotal ? HttpTools.DEFAULT_MAX_TOTAL : maxTotal;
			defaultMaxPerRoute = null == defaultMaxPerRoute ? HttpTools.DEFAULT_MAX_PER_ROUTE : defaultMaxPerRoute;
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory())
					.register("https", HttpTools.getSslConnectionSocketFactory())
					.build();
			connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
			connManager.setMaxTotal(maxTotal);
			connManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
		}
	}

	private void setRedirectStrategy() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		if (null == httpClientBuilder) {
			this.init();
		}
//		httpClientBuilder.setRedirectStrategy(new LaxRedirectStrategy());
		httpClientBuilder.setRedirectStrategy(new DefaultRedirectStrategy() {
			@Override
			public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) {
				boolean isRedirect = false;
				try {
					isRedirect = super.isRedirected(request, response, context);
				} catch (ProtocolException e) {
					e.printStackTrace();
				}
				if (!isRedirect) {
					if (HttpTools.isRedirected(response.getStatusLine().getStatusCode())) {
						return true;
					}
				}
				return isRedirect;
			}
		});
	}

	private void setKeepAliveStrategy() {
		keepAliveTimeout = null == keepAliveTimeout ? HttpTools.DEFAULT_KEEP_ALIVE_TIMEOUT : keepAliveTimeout;
		HttpTools.setKeepAliveStrategy(httpClientBuilder, keepAliveTimeout);
	}

	private void setRetryHandler() {
		if (null != retryCount && retryCount > 0) {
			httpClientBuilder.setRetryHandler(new StandardHttpRequestRetryHandler(retryCount, false));
		}
	}

	/**
	 * 获取 CloseableHttpClient
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 */
	public CloseableHttpClient createHttpClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		if (null == httpClientBuilder) {
			this.init();
		}
		return httpClientBuilder.build();
	}

	/**
	 * CloseableHttpClient
	 *
	 * @param userAgent
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 */
	public CloseableHttpClient createHttpClient(String userAgent) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		if (null == httpClientBuilder) {
			this.init();
		}
		return StringUtils.isBlank(userAgent) ? httpClientBuilder.build() : httpClientBuilder.setUserAgent(userAgent).build();
	}

	public CloseableHttpClient createSSLClientDefault() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		return HttpClients.custom().setSSLSocketFactory(HttpTools.getSslConnectionSocketFactory()).build();
	}


	/**
	 * Httpclient get请求
	 *
	 * @param url
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String get(String url) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.get(url, null);
	}

	/**
	 * Httpclient get请求
	 *
	 * @param url
	 * @param ua
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String get(String url, String ua) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.get(url, ua, "");
	}

	/**
	 * Httpclient get请求
	 *
	 * @param url
	 * @param ua
	 * @param cookie
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String get(String url, String ua, String cookie) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		Map<String, String> headerParams = null;
		if (org.apache.commons.lang.StringUtils.isNotBlank(cookie)) {
			headerParams = new ConcurrentHashMap<String, String>(1);
			headerParams.put("Cookie", cookie);
		}
		return this.getByHeaderParams(url, ua, headerParams);
	}

	/**
	 * Httpclient get请求
	 *
	 * @param url
	 * @param ua
	 * @param cookie
	 * @param encoding
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String get(String url, String ua, String cookie, String encoding) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		Map<String, String> headerParams = null;
		if (org.apache.commons.lang.StringUtils.isNotBlank(cookie)) {
			headerParams = new ConcurrentHashMap<String, String>(1);
			headerParams.put("Cookie", cookie);
		}
		return this.getByHeaderParams(url, ua, encoding, headerParams);
	}

	/**
	 * Httpclient get请求
	 *
	 * @param url
	 * @param headerParams
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String getByHeaderParams(String url, Map<String, String> headerParams) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.getByHeaderParams(url, null, headerParams);
	}

	/**
	 * Httpclient get请求
	 *
	 * @param url
	 * @param ua
	 * @param headerParams
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String getByHeaderParams(String url, String ua, Map<String, String> headerParams) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.getByHeaderParams(url, ua, null, headerParams);
	}

	/**
	 * Httpclient get请求
	 *
	 * @param url
	 * @param ua
	 * @param encoding
	 * @param headerParams
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String getByHeaderParams(String url, String ua, String encoding, Map<String, String> headerParams) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
			ClientProtocolException, IOException {
		return HttpTools.getByHeaderParams(this.createHttpClient(ua), url, encoding, headerParams);
	}

	public String delete(String url) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.delete(url, null, null);
	}

	public String delete(String url, Map<String, String> headerParams) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.delete(url, null, headerParams);
	}

	public String delete(String url, String ua, Map<String, String> headerParams) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.delete(url, ua, null, headerParams);
	}

	public String delete(String url, String ua, String encoding, Map<String, String> headerParams) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
			ClientProtocolException, IOException {
		return HttpTools.delete(this.createHttpClient(ua), url, encoding, headerParams);
	}

	public HttpResponse getToHttpResponse(String url) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.getToHttpResponse(url, null, null);
	}

	public HttpResponse getToHttpResponse(String url, Map<String, String> headerParams) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.getToHttpResponse(url, null, headerParams);
	}

	public HttpResponse getToHttpResponse(String url, String ua, Map<String, String> headerParams) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.getToHttpResponse(url, ua, null, headerParams);
	}

	public HttpResponse getToHttpResponse(String url, String ua, String encoding, Map<String, String> headerParams) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
			ClientProtocolException, IOException {
		return HttpTools.getToHttpResponse(this.createHttpClient(ua), url, encoding, headerParams);
	}

	/**
	 * Httpclient post请求
	 *
	 * @param url
	 * @param params
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String post(String url, Map<String, Object> params) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.post(url, params, null);
	}

	/**
	 * Httpclient post请求
	 *
	 * @param url
	 * @param params
	 * @param ua
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String post(String url, Map<String, Object> params, String ua) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.post(url, params, ua, "");
	}

	/**
	 * Httpclient post请求
	 *
	 * @param url
	 * @param params
	 * @param ua
	 * @param cookie
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String post(String url, Map<String, Object> params, String ua, String cookie) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		Map<String, String> headerParams = null;
		if (org.apache.commons.lang.StringUtils.isNotBlank(cookie)) {
			headerParams = new ConcurrentHashMap<String, String>(1);
			headerParams.put("Cookie", cookie);
		}
		return this.post(url, params, ua, headerParams);
	}

	/**
	 * Httpclient post请求
	 *
	 * @param url
	 * @param params
	 * @param ua
	 * @param headerParams
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String post(String url, Map<String, Object> params, String ua, Map<String, String> headerParams) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
			ClientProtocolException, IOException {
		return HttpTools.post(this.createHttpClient(ua), url, params, headerParams);
	}

	/**
	 * Httpclient post请求
	 *
	 * @param url
	 * @param params
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public HttpResponse postToHttpResponse(String url, Map<String, Object> params) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.postToHttpResponse(url, params, null);
	}

	/**
	 * Httpclient post请求
	 *
	 * @param url
	 * @param params
	 * @param ua
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public HttpResponse postToHttpResponse(String url, Map<String, Object> params, String ua) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.postToHttpResponse(url, params, ua, "");
	}

	/**
	 * Httpclient post请求
	 *
	 * @param url
	 * @param params
	 * @param ua
	 * @param cookie
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public HttpResponse postToHttpResponse(String url, Map<String, Object> params, String ua, String cookie) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		Map<String, String> headerParams = null;
		if (org.apache.commons.lang.StringUtils.isNotBlank(cookie)) {
			headerParams = new ConcurrentHashMap<String, String>(1);
			headerParams.put("Cookie", cookie);
		}
		return this.postToHttpResponse(url, params, ua, headerParams);
	}

	/**
	 * Httpclient post请求
	 *
	 * @param url
	 * @param params
	 * @param ua
	 * @param headerParams
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public HttpResponse postToHttpResponse(String url, Map<String, Object> params, String ua, Map<String, String> headerParams) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
			ClientProtocolException, IOException {
		return HttpTools.postToHttpResponse(this.createHttpClient(ua), url, params, headerParams);
	}

	/**
	 * post josn
	 *
	 * @param bodyStr
	 * @param url
	 * @param contentType
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String postRequestBody(String bodyStr, String url, String contentType) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.postRequestBody(bodyStr, url, contentType, null);
	}

	public HttpResponse postRequestBodyToHttpResponse(String bodyStr, String url, String contentType) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException,
			IOException {
		return this.postRequestBodyToHttpResponse(bodyStr, url, contentType, null);
	}

	/**
	 * post josn
	 *
	 * @param bodyStr
	 * @param url
	 * @param contentType
	 * @param authorization
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String postRequestBody(String bodyStr, String url, String contentType, String authorization) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.postRequestBody(bodyStr, url, contentType, authorization, null, null);
	}

	public HttpResponse postRequestBodyToHttpResponse(String bodyStr, String url, String contentType, String authorization) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.postRequestBodyToHttpResponse(bodyStr, url, contentType, authorization, null, null);
	}

	/**
	 * post josn
	 *
	 * @param bodyStr
	 * @param url
	 * @param contentType
	 * @param authorization
	 * @param ua
	 * @param cookie
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String postRequestBody(String bodyStr, String url, String contentType, String authorization, String ua, String cookie) throws KeyManagementException, NoSuchAlgorithmException,
			KeyStoreException, ClientProtocolException, IOException {
		Map<String, String> headerParams = new ConcurrentHashMap<String, String>(2);
		if (org.apache.commons.lang.StringUtils.isNotBlank(cookie)) {
			headerParams.put("Cookie", cookie);
		}
		if (org.apache.commons.lang.StringUtils.isNotBlank(authorization)) {
			headerParams.put("Authorization", authorization);
		}
		return this.postRequestBody(bodyStr, url, contentType, ua, headerParams);
	}

	public HttpResponse postRequestBodyToHttpResponse(String bodyStr, String url, String contentType, String authorization, String ua, String cookie) throws KeyManagementException, NoSuchAlgorithmException,
			KeyStoreException, ClientProtocolException, IOException {
		Map<String, String> headerParams = new ConcurrentHashMap<String, String>(2);
		if (org.apache.commons.lang.StringUtils.isNotBlank(cookie)) {
			headerParams.put("Cookie", cookie);
		}
		if (org.apache.commons.lang.StringUtils.isNotBlank(authorization)) {
			headerParams.put("Authorization", authorization);
		}
		return this.postRequestBodyToHttpResponse(bodyStr, url, contentType, ua, headerParams);
	}

	/**
	 * post josn
	 *
	 * @param bodyStr
	 * @param url
	 * @param contentType
	 * @param ua
	 * @param headerParams
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String postRequestBody(String bodyStr, String url, String contentType, String ua, Map<String, String> headerParams) throws KeyManagementException, NoSuchAlgorithmException,
			KeyStoreException, ClientProtocolException, IOException {
		return HttpTools.postRequestBody(this.createHttpClient(ua), bodyStr, url, contentType, headerParams);
	}

	public HttpResponse postRequestBodyToHttpResponse(String bodyStr, String url, String contentType, String ua, Map<String, String> headerParams) throws KeyManagementException, NoSuchAlgorithmException,
			KeyStoreException, ClientProtocolException, IOException {
		return HttpTools.postRequestBodyToHttpResponse(this.createHttpClient(ua), bodyStr, url, contentType, headerParams);
	}

	public String patch(String bodyStr, String url, String contentType, String ua, Map<String, String> headerParams) throws KeyManagementException, NoSuchAlgorithmException,
			KeyStoreException, ClientProtocolException, IOException {
		return HttpTools.patch(this.createHttpClient(ua), bodyStr, url, contentType, headerParams);
	}

	public String put(String bodyStr, String url, String contentType, String ua, Map<String, String> headerParams) throws KeyManagementException, NoSuchAlgorithmException,
			KeyStoreException, ClientProtocolException, IOException {
		return HttpTools.put(this.createHttpClient(ua), bodyStr, url, contentType, headerParams);
	}

	/**
	 * SOAP XML文件
	 *
	 * @param soapRequestXML
	 * @param url
	 * @param ua
	 * @param contentType
	 * @param soapAction
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws ClientProtocolException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws IOException
	 */
	public String postSOAPRequest(String soapRequestXML, String url, String ua, String contentType, String soapAction) throws KeyManagementException, ClientProtocolException, NoSuchAlgorithmException,
			KeyStoreException, IOException {
		return this.postSOAPRequest(soapRequestXML, url, ua, contentType, soapAction, null);
	}

	/**
	 * SOAP XML文件
	 *
	 * @param soapRequestXML
	 * @param url
	 * @param ua
	 * @param contentType
	 * @param soapAction
	 * @param encoding
	 *
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 */
	public String postSOAPRequest(String soapRequestXML, String url, String ua, String contentType, String soapAction, String encoding) throws ClientProtocolException, IOException,
			KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		return HttpTools.postSOAPRequest(this.createHttpClient(ua), soapRequestXML, url, contentType, soapAction, encoding);
	}

	/**
	 * xml提交方式
	 *
	 * @param xml
	 * @param url
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String postXML(String xml, String url) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.postXML(xml, url, CONTENT_TYPE_TEXT_XML);
	}

	/**
	 * xml提交方式
	 *
	 * @param xml
	 * @param url
	 * @param contentType
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String postXML(String xml, String url, String contentType) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.postXML(xml, url, contentType, null);
	}

	/**
	 * xml提交方式
	 *
	 * @param xml
	 * @param url
	 * @param contentType
	 * @param authorization
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String postXML(String xml, String url, String contentType, String authorization) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException,
			IOException {
		return this.postXML(xml, url, contentType, authorization, null, null);
	}

	/**
	 * xml提交方式
	 *
	 * @param xml
	 * @param url
	 * @param contentType
	 * @param authorization
	 * @param ua
	 * @param cookie
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String postXML(String xml, String url, String contentType, String authorization, String ua, String cookie) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
			ClientProtocolException, IOException {
		return HttpTools.postXML(this.createHttpClient(ua), xml, url, contentType, authorization, cookie);
	}

	/**
	 * @return String
	 * @Title: getRandomUA
	 * @Description: 随机获取UA
	 */
	public static String getRandomUA() {
		return HttpTools.getRandomUA();
	}
}
