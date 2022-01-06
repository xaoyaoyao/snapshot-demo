/**
 * @projectName snapshot-demo
 * @fileName Demo.java
 * @packageName org.example.demo
 * @author xaoyaoyao
 * @date 2022/1/6 13:47
 * @version V1.0
 * @copyright (c) 2022, xaoyaoyao@aliyun.com All Rights Reserved.
 */
package org.example.demo;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.example.http.HttpClient;
import org.example.http.HttpUtils;

/**
 * @author xaoyaoyao
 * @className Demo
 * @description
 * @date 2022/1/6 13:47
 */
public class Demo {

	public static void main(String[] args) throws NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
		String url    = "https://translate.google.cn/?sl=auto&tl=en&op=translate";
		String result = new HttpClient.Builder().setRetryCount(3).build().get(url, HttpUtils.getRandomUA());
		System.out.println(result);

		String              webhook = "http://112.74.59.57:9090/PacPortSupport/eventWebhook";
		Map<String, Object> params  = new HashMap<>();
		params.put("code", "200");
		params.put("pkgId", "100005-2021090617-5215722-414880");
		params.put("time", "2021-09-06 17:53:31");
		params.put("info", "The package is taken away.");
		params.put("photoUrl", null);
		params.put("status", "2");
		HttpResponse response = new HttpClient.Builder().setRetryCount(3).build().postToHttpResponse(webhook, params);
		System.out.println(response);
	}
}
