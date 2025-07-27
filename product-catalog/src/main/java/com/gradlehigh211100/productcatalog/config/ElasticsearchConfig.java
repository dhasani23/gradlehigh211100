package com.gradlehigh211100.productcatalog.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Configuration class for Elasticsearch.
 * Sets up the Elasticsearch client and enables repository support.
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.gradlehigh211100.productcatalog.repository")
public class ElasticsearchConfig extends AbstractElasticsearchConfiguration {

    /**
     * Creates and configures a RestHighLevelClient for Elasticsearch.
     * This is a key component for connecting to the Elasticsearch cluster.
     *
     * @return A configured RestHighLevelClient instance
     */
    @Override
    @Bean
    public RestHighLevelClient elasticsearchClient() {
        final ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo("localhost:9200")
                // Add SSL configuration if needed
                // .useSsl()
                // .withBasicAuth(username, password)
                .build();

        return RestClients.create(clientConfiguration).rest();
    }
    
    // TODO: Add configuration for index creation and mappings
    // TODO: Add configuration for custom analyzers
}