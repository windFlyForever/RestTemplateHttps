package com.reman.medical.config;


import com.reman.medical.dao.SimpleHttpsRequestFactory;
import com.reman.medical.dao.TrustEveryManger;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import sun.net.www.http.HttpClient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;

//@Configuration
@ConditionalOnClass(value = {RestTemplate.class, HttpClient.class})
public class RestTemplateConfig {

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

    //创建HTTP客户端工厂
    private ClientHttpRequestFactory createFactory() throws KeyManagementException, NoSuchAlgorithmException {
        return this.maxTotalConnect <= 0?getSimpleClientHttpRequestFactory():getHttpComponentsClientHttpRequestFactory();

    }

    private SimpleClientHttpRequestFactory getSimpleClientHttpRequestFactory(){

        SimpleClientHttpRequestFactory factory;
        if(verifyHttps){
            factory=new SimpleHttpsRequestFactory();
        }else{
            factory= new SimpleClientHttpRequestFactory();
        }
        factory.setConnectTimeout(this.connectTimeout);
        factory.setReadTimeout(this.readTimeout);
        return factory;
    }

    private HttpComponentsClientHttpRequestFactory getHttpComponentsClientHttpRequestFactory() throws NoSuchAlgorithmException, KeyManagementException {


        HttpClientBuilder builder = HttpClientBuilder.create().setMaxConnTotal(this.maxTotalConnect).setMaxConnPerRoute(this.maxConnectPerRoute);
        if(verifyHttps){
            //证书信任管理器信任所有证书
            TrustManager[] trustAllCerts = new TrustManager[]{new TrustEveryManger()};

            SSLContext sslContext = SSLContext.getInstance("TLSv1");

            sslContext.init(null, trustAllCerts, new SecureRandom());

            builder.setSSLContext(sslContext).setSSLHostnameVerifier((s,sslSession)->true);

        }

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(builder.build());
        factory.setConnectTimeout(this.connectTimeout);
        factory.setReadTimeout(this.readTimeout);
        return factory;
    }

    //初始化RestTemplate,并加入spring的Bean工厂，由spring统一管理
    @Bean
    @LoadBalanced
    public RestTemplate getRestTemplate() throws NoSuchAlgorithmException, KeyManagementException {
        RestTemplate restTemplate = new RestTemplate(this.createFactory());
        List<HttpMessageConverter<?>> converterList = restTemplate.getMessageConverters();

        //重新设置StringHttpMessageConverter字符集为UTF-8，解决中文乱码问题
        converterList.stream().filter(conver->conver instanceof StringHttpMessageConverter).forEach(conver->((StringHttpMessageConverter) conver).setDefaultCharset(StandardCharsets.UTF_8));

        //加入FastJson转换器 根据使用情况进行操作，此段注释，默认使用jackson
        //converterList.add(new FastJsonHttpMessageConverter4());
        return restTemplate;
    }


}
