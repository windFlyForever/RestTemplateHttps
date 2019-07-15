package com.reman.medical.config;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

@Configuration
@ConditionalOnClass(value = {RestTemplate.class, HttpClient.class})
public class RestTemplateConfigNew {

    @Value("${remote.maxTotalConnect:0}")
    private int maxTotalConnect; //连接池的最大连接数默认为0
    @Value("${remote.maxConnectPerRoute:200}")
    private int maxConnectPerRoute; //单个主机的最大连接数
    @Value("${remote.connectTimeout:2000}")
    private int connectTimeout; //连接超时默认2s
    @Value("${remote.readTimeout:30000}")
    private int readTimeout; //读取超时默认30s
    @Value("${server.ssl.enabled}")//#{!${server.ssl.enabled}}设置非值得写法
    private boolean verifyHttps;

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        HttpClientBuilder builder= HttpClients.custom().setMaxConnTotal(maxTotalConnect).setMaxConnPerRoute(maxConnectPerRoute);

        if(verifyHttps){

            TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

            //信任所有的证书
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
            //setSSLHostnameVerifier忽略域名验证
            builder.setSSLContext(sslContext).setSSLHostnameVerifier((s,sslSession)->true);

        }

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(builder.build());

        factory.setConnectTimeout(this.connectTimeout);
        factory.setReadTimeout(this.readTimeout);

        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }
}
