// src/main/java/com/sc/scifunapi/config/ElasticsearchConfig.java
package com.sc.scifunapi.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public ElasticsearchClient elasticsearchClient(
            @Value("${es.node}") String node,
            @Value("${es.apiKey}") String apiKey
    ) {
        String authHeader = "ApiKey " + apiKey;
        RestClient restClient = RestClient.builder(org.apache.http.HttpHost.create(node))
                .setDefaultHeaders(new org.apache.http.Header[]{
                        new BasicHeader(HttpHeaders.AUTHORIZATION, authHeader)
                })
                .build();

        var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
