package com.reman.medical.dao;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 将http的工厂转化为https的工厂
 */
public class SimpleHttpsRequestFactory extends SimpleClientHttpRequestFactory {


    @Override
    protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
        try {
            if (!(connection instanceof HttpsURLConnection)) {
                throw new RuntimeException("An instance of HttpsURLConnection is expected");
            }

            HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;

            //lambda表达式写法，忽略域名的认证
            httpsConnection.setHostnameVerifier((s, sslSession) -> true);

            //证书信任管理器信任所有证书
            TrustManager[] trustAllCerts = new TrustManager[]{new TrustEveryManger()};

            SSLContext sslContext = SSLContext.getInstance("TLSv1");

            sslContext.init(null, trustAllCerts, new SecureRandom());

            httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());

            super.prepareConnection(httpsConnection, httpMethod);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

    }
}
