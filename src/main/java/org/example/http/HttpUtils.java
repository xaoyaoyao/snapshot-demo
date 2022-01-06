/**
 * @Title: HttpUtils.java
 * @Package com.enchantin.ucella.util
 * @Description: TODO
 * @author weiwei
 * @date 2015-12-2 下午2:37:25
 * @version V1.0
 */
package org.example.http;

import java.io.IOException;
import java.io.Serializable;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;

/**
 * @author weiwei
 * @Date 2015-12-2
 */
public class HttpUtils implements Serializable {

	private static final long serialVersionUID = 4564912612466111417L;

	public final static String CONTENT_TYPE_APPLICATION_JSON     = HttpTools.CONTENT_TYPE_APPLICATION_JSON;
	public final static String APPLICATION_OCTET_STREAM_VALUE    = HttpTools.APPLICATION_OCTET_STREAM_VALUE;
	public final static String CONTENT_TYPE_APPLICATION_SOAP_XML = HttpTools.CONTENT_TYPE_APPLICATION_SOAP_XML;
	public final static String CONTENT_TYPE_TEXT_XML             = HttpTools.CONTENT_TYPE_TEXT_XML;
	public final static String DEFAULT_UA                        = HttpTools.DEFAULT_UA;

	private static   boolean           initizlized       = false;
	private volatile RequestConfig     requestConfig     = null;
	private          HttpClientBuilder httpClientBuilder = null;

	private HttpUtils() {
		synchronized (HttpUtils.class) {
			if (initizlized == false) {
				try {
					init();
				} catch (KeyManagementException e) {
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (KeyStoreException e) {
					e.printStackTrace();
				}
				initizlized = !initizlized;
			} else {
				throw new RuntimeException("获取对象异常::已被破坏");
			}
		}
	}

	public static HttpUtils getInstance() {
		return SingletonHolder.INSTANCE;
	}

	static class SingletonHolder {
		private final static HttpUtils INSTANCE = new HttpUtils();
	}

	/**
	 * @return void
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @Title: init
	 * @Description: 初始化
	 */
	private void init() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		requestConfig = HttpTools.setRequestConfig();
		httpClientBuilder = HttpTools.setHttpClientBuilder(requestConfig, HttpTools.getSslConnectionSocketFactory());
	}

	/**
	 * @param userAgent
	 *
	 * @return CloseableHttpClient
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @Title: createHttpClient
	 * @Description: 创建HttpClient
	 */
	public CloseableHttpClient createHttpClient(String userAgent) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		return this.createHttpClient(userAgent, HttpTools.DEFAULT_KEEP_ALIVE_TIMEOUT);
	}

	public CloseableHttpClient createHttpClient(String userAgent, long keepAliveTimeout) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		if (null == httpClientBuilder) {
			this.init();
		}
		this.setKeepAliveStrategy(keepAliveTimeout);
//		this.setRedirectStrategy();
		return httpClientBuilder.setUserAgent(userAgent).build();
	}

	public CloseableHttpClient createSSLClientDefault() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		return HttpClients.custom().setSSLSocketFactory(HttpTools.getSslConnectionSocketFactory()).build();
	}

	public void setRedirectStrategy() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
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

	/**
	 * 设置keepAlive
	 *
	 * @param keepAliveTimeout 单位毫秒
	 *
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 */
	public void setKeepAliveStrategy(long keepAliveTimeout) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		if (null == httpClientBuilder) {
			this.init();
		}
		keepAliveTimeout = keepAliveTimeout <= 60 ? HttpTools.DEFAULT_KEEP_ALIVE_TIMEOUT : keepAliveTimeout;
		HttpTools.setKeepAliveStrategy(httpClientBuilder, keepAliveTimeout);
	}

	/**
	 * 重试
	 *
	 * @param retryCount
	 *
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 */
	public void setRetry(int retryCount) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		if (null == httpClientBuilder) {
			this.init();
		}
		httpClientBuilder.setRetryHandler(new StandardHttpRequestRetryHandler(retryCount, false));
	}

	/**
	 * @param url
	 *
	 * @return String
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @Title: get
	 * @Description: Httpclient get请求
	 */
	public String get(String url) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.get(url, null);
	}

	/**
	 * @param url
	 * @param ua
	 *
	 * @return String
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @Title: get
	 * @Description: Httpclient get请求
	 */
	public String get(String url, String ua) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.get(url, ua, "");
	}

	/**
	 * @param url
	 * @param ua
	 * @param cookie
	 *
	 * @return String
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @Title: get
	 * @Description: Httpclient get请求
	 */
	public String get(String url, String ua, String cookie) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		Map<String, String> headerParams = null;
		if (StringUtils.isNotBlank(cookie)) {
			headerParams = new ConcurrentHashMap<String, String>(1);
			headerParams.put("Cookie", cookie);
		}
		return this.getByHeaderParams(url, ua, headerParams);
	}

	/**
	 * @param url
	 * @param ua
	 * @param cookie
	 * @param encoding
	 *
	 * @return String
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @Title: get
	 * @Description: Httpclient get请求
	 */
	public String get(String url, String ua, String cookie, String encoding)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		Map<String, String> headerParams = null;
		if (StringUtils.isNotBlank(cookie)) {
			headerParams = new ConcurrentHashMap<String, String>(1);
			headerParams.put("Cookie", cookie);
		}
		return this.getByHeaderParams(url, ua, encoding, headerParams);
	}

	/**
	 * @param url
	 * @param headerParams
	 *
	 * @return String
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @Title: get
	 * @Description: Httpclient get请求
	 */
	public String getByHeaderParams(String url, Map<String, String> headerParams)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.getByHeaderParams(url, null, headerParams);
	}

	/**
	 * @param url
	 * @param ua
	 * @param headerParams
	 *
	 * @return String
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @Title: get
	 * @Description: Httpclient get请求
	 */
	public String getByHeaderParams(String url, String ua, Map<String, String> headerParams)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.getByHeaderParams(url, ua, null, headerParams);
	}

	/**
	 * @param url
	 * @param ua
	 * @param headerParams
	 * @param encoding
	 *
	 * @return String
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @Title: get
	 * @Description: Httpclient get请求
	 */
	public String getByHeaderParams(String url, String ua, String encoding, Map<String, String> headerParams)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return HttpTools.getByHeaderParams(this.createHttpClient(ua), url, encoding, headerParams);
	}

	/**
	 * @param url
	 * @param params
	 *
	 * @return String
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @Title: post
	 * @Description: Httpclient post请求
	 */
	public String post(String url, Map<String, Object> params) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.post(url, params, null);
	}

	/**
	 * @param url
	 * @param params
	 * @param ua
	 *
	 * @return String
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @Title: post
	 * @Description: Httpclient post请求
	 */
	public String post(String url, Map<String, Object> params, String ua)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.post(url, params, ua, "");
	}

	/**
	 * @param url
	 * @param params
	 * @param ua
	 * @param cookie
	 *
	 * @return String
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @Title: post
	 * @Description: Httpclient post请求
	 */
	public String post(String url, Map<String, Object> params, String ua, String cookie)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		Map<String, String> headerParams = null;
		if (StringUtils.isNotBlank(cookie)) {
			headerParams = new ConcurrentHashMap<String, String>(1);
			headerParams.put("Cookie", cookie);
		}
		return this.post(url, params, ua, headerParams);
	}

	/**
	 * @param url
	 * @param params
	 * @param ua
	 *
	 * @return String
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @Title: post
	 * @Description: Httpclient post请求
	 */
	public String post(String url, Map<String, Object> params, String ua, Map<String, String> headerParams)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return HttpTools.post(this.createHttpClient(ua), url, params, headerParams);
	}

	/**
	 * @param bodyStr
	 * @param url
	 * @param contentType
	 *
	 * @return String
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @Title: postRequestBody
	 * @Description: TODO
	 */
	public String postRequestBody(String bodyStr, String url, String contentType)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.postRequestBody(bodyStr, url, contentType, null);
	}

	/**
	 * @param bodyStr
	 * @param url
	 * @param contentType
	 *
	 * @return String
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @Title: postRequestBody
	 * @Description: TODO
	 */
	public String postRequestBody(String bodyStr, String url, String contentType, String authorization)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.postRequestBody(bodyStr, url, contentType, authorization, null, null);
	}

	/**
	 * @param bodyStr
	 * @param url
	 * @param contentType
	 * @param authorization
	 * @param ua
	 * @param cookie
	 *
	 * @return String
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @Title: postRequestBody
	 * @Description: TODO
	 */
	public String postRequestBody(String bodyStr, String url, String contentType, String authorization, String ua, String cookie)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		Map<String, String> headerParams = new ConcurrentHashMap<String, String>(2);
		if (StringUtils.isNotBlank(cookie)) {
			headerParams.put("Cookie", cookie);
		}
		if (StringUtils.isNotBlank(authorization)) {
			headerParams.put("Authorization", authorization);
		}
		return this.postRequestBody(bodyStr, url, contentType, ua, headerParams);
	}

	/**
	 * @param bodyStr
	 * @param url
	 * @param contentType
	 * @param ua
	 * @param headerParams
	 *
	 * @return String
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @Title: postRequestBody
	 * @Description: TODO
	 */
	public String postRequestBody(String bodyStr, String url, String contentType, String ua, Map<String, String> headerParams)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return HttpTools.postRequestBody(this.createHttpClient(ua), bodyStr, url, contentType, headerParams);
	}

	/**
	 * @param soapRequestXML SOAP XML文件
	 * @param url
	 * @param ua
	 * @param contentType
	 * @param soapAction
	 *
	 * @throws IOException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws ClientProtocolException
	 * @throws KeyManagementException
	 * @Title: postSOAPRequest
	 * @Description: 请求SOAP XML格式
	 * @Reutrn String
	 */
	public String postSOAPRequest(String soapRequestXML, String url, String ua, String contentType, String soapAction)
			throws KeyManagementException, ClientProtocolException, NoSuchAlgorithmException, KeyStoreException, IOException {
		return this.postSOAPRequest(soapRequestXML, url, ua, contentType, soapAction, null);
	}

	/**
	 * @param soapRequestXML SOAP XML文件
	 * @param url
	 * @param ua
	 * @param contentType
	 * @param soapAction
	 *
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @Title: postSOAPRequest
	 * @Description: 请求SOAP XML格式
	 * @Reutrn String
	 */
	public String postSOAPRequest(String soapRequestXML, String url, String ua, String contentType, String soapAction, String encoding)
			throws ClientProtocolException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		return HttpTools.postSOAPRequest(this.createHttpClient(ua), soapRequestXML, url, contentType, soapAction, encoding);
	}

	/**
	 * @param xml
	 * @param url
	 *
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @Title: postXML
	 * @Description: TODO
	 * @Reutrn String
	 */
	public String postXML(String xml, String url) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.postXML(xml, url, CONTENT_TYPE_TEXT_XML);
	}

	/**
	 * @param xml
	 * @param url
	 * @param contentType
	 *
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @Title: postXML
	 * @Description: TODO
	 * @Reutrn String
	 */
	public String postXML(String xml, String url, String contentType)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.postXML(xml, url, contentType, null);
	}

	/**
	 * @param xml
	 * @param url
	 * @param contentType
	 * @param authorization
	 *
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @Title: postXML
	 * @Description: TODO
	 * @Reutrn String
	 */
	public String postXML(String xml, String url, String contentType, String authorization)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return this.postXML(xml, url, contentType, authorization, null, null);
	}

	/**
	 * @param xml
	 * @param url
	 * @param contentType
	 * @param authorization
	 * @param ua
	 * @param cookie
	 *
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @Title: postXML
	 * @Description: TODO
	 * @Reutrn String
	 */
	public String postXML(String xml, String url, String contentType, String authorization, String ua, String cookie)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ClientProtocolException, IOException {
		return HttpTools.postXML(this.createHttpClient(ua), xml, url, contentType, authorization, cookie);
	}

	/**
	 * @param httpclient
	 * @param response
	 * @param httpRequest
	 *
	 * @return void
	 * @throws IOException
	 * @Title: close
	 * @Description: TODO
	 */
	public void close(CloseableHttpClient httpclient, CloseableHttpResponse response, HttpRequestBase httpRequest) throws IOException {
		HttpTools.close(httpclient, response, httpRequest);
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