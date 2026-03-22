package de.kittelberger.bosch.adapter.data;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ElasticsearchConfig {

  @Value("${elasticsearch.host:localhost}")
  private String host;

  @Value("${elasticsearch.port:9200}")
  private int port;

  @Bean
  public Rest5Client elasticsearchLowLevelClient() {
    return Rest5Client.builder(new HttpHost(host, port)).build();
  }

  @Bean
  public Rest5ClientTransport elasticsearchTransport(Rest5Client elasticsearchLowLevelClient) {
    return new Rest5ClientTransport(elasticsearchLowLevelClient, new JacksonJsonpMapper());
  }

  @Bean
  public ElasticsearchClient elasticsearchClient(Rest5ClientTransport elasticsearchTransport) {
    return new ElasticsearchClient(elasticsearchTransport);
  }
}
