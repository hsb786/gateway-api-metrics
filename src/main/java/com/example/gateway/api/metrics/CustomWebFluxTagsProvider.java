package com.example.gateway.api.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.springframework.boot.actuate.metrics.web.reactive.server.DefaultWebFluxTagsProvider;
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTags;
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsContributor;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

/**
 * @author hushengbin
 * @date 2023-02-13 10:46
 */
public class CustomWebFluxTagsProvider extends DefaultWebFluxTagsProvider {

    private final boolean ignoreTrailingSlash;

    private final List<WebFluxTagsContributor> contributors;

    public CustomWebFluxTagsProvider(
            boolean ignoreTrailingSlash, List<WebFluxTagsContributor> contributors) {
        this.ignoreTrailingSlash = ignoreTrailingSlash;
        this.contributors = contributors;
    }

    @Override
    public Iterable<Tag> httpRequestTags(ServerWebExchange exchange, Throwable exception) {
        Tags tags =
                Tags.of(
                        WebFluxTags.method(exchange),
                        uriTag(exchange, exception),
                        WebFluxTags.exception(exception),
                        WebFluxTags.status(exchange),
                        WebFluxTags.outcome(exchange));
        for (WebFluxTagsContributor contributor : this.contributors) {
            tags = tags.and(contributor.httpRequestTags(exchange, exception));
        }
        return tags;
    }

    private Tag uriTag(ServerWebExchange exchange, Throwable e) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route != null) {
            return Tag.of("uri", exchange.getRequest().getURI().getPath());
        }
        return WebFluxTags.uri(exchange, this.ignoreTrailingSlash);
    }
}
