import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.*;
import org.apache.hc.core5.net.*;

import com.fasterxml.jackson.databind.node.*;

public class http {
    public static CloseableHttpResponse requestPost(String url, ObjectNode json, Map<String, String> headers, HttpClientBuilder clientBuilder) throws IOException{
        CloseableHttpClient client = clientBuilder.build();
        HttpPost post = new HttpPost(url);
        StringEntity entity = new StringEntity(json.toString(),ContentType.APPLICATION_JSON);
        post.setEntity(entity);
        headers.put("Accept", "application/json");
        headers.put("Content-type", "application/json");
        for (Map.Entry<String, String> entry: headers.entrySet()) {
            post.setHeader(entry.getKey(),entry.getValue());
        }
        CloseableHttpResponse response = client.execute(post);
        return response;
    }
    public static CloseableHttpResponse requestPost(String url, Map<String, String> headers, HttpClientBuilder clientBuilder) throws IOException{
        CloseableHttpClient client = clientBuilder.build();
        HttpPost post = new HttpPost(url);
        for (Map.Entry<String, String> entry: headers.entrySet()) {
            post.setHeader(entry.getKey(),entry.getValue());
        }
        CloseableHttpResponse response = client.execute(post);
        return response;
    }
    public static CloseableHttpResponse requestGet(String url, Map<String, String> params, Map<String, String> headers, HttpClientBuilder clientBuilder) throws IOException, URISyntaxException {
        CloseableHttpClient client = clientBuilder.build();
        HttpGet get = new HttpGet(url);
        URIBuilder uriBuild = new URIBuilder(get.getUri());
        for(Map.Entry<String,String> entry: params.entrySet()) {
            uriBuild.addParameter(entry.getKey(), entry.getValue());
        }
        URI uri = uriBuild.build();
        ((HttpUriRequestBase) get).setUri(uri);
        for(Map.Entry<String,String> entry: headers.entrySet()) {
            get.setHeader(entry.getKey(), entry.getValue());
        }
        CloseableHttpResponse response = client.execute(get);
        return response;
    }
    public static CloseableHttpResponse requestGet(String url, Map<String, String> headers, HttpClientBuilder clientBuilder) throws IOException {
        CloseableHttpClient client = clientBuilder.build();
        HttpGet get = new HttpGet(url);
        for(Map.Entry<String,String> entry: headers.entrySet()) {
            get.setHeader(entry.getKey(), entry.getValue());
        }
        CloseableHttpResponse response = client.execute(get);
        return response;
    }
}