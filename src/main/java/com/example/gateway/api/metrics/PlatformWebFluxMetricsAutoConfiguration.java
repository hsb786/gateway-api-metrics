package com.example.gateway.api.metrics;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.reactive.WebFluxMetricsAutoConfiguration;
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsContributor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Collectors;

/**
 * @author hushengbin
 * @date 2023-02-13 10:53
 */
@Configuration
@AutoConfigureAfter({MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class})
@AutoConfigureBefore({WebFluxMetricsAutoConfiguration.class})
public class PlatformWebFluxMetricsAutoConfiguration {

    private final MetricsProperties properties;

    public PlatformWebFluxMetricsAutoConfiguration(MetricsProperties properties) {
        this.properties = properties;
    }

    @Bean
    public CustomWebFluxTagsProvider webFluxTagsProvider(
            ObjectProvider<WebFluxTagsContributor> contributors) {
        return new CustomWebFluxTagsProvider(
                this.properties.getWeb().getServer().getRequest().isIgnoreTrailingSlash(),
                contributors.orderedStream().collect(Collectors.toList()));
    }
}
