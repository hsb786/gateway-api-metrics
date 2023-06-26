# gateway-api-metrics

## 问题
gateway http_server_requests_seconds_count指标 uri都统计成UNKNOW
```
http_server_requests_seconds_count{exception="None",method="POST",outcome="SUCCESS",status="200",uri="UNKNOWN",} 2.0
```

github 作者的解释
[Align WebFluxTags#uri() with WebMvcTags#uri()](https://github.com/spring-projects/spring-boot/pull/15609#issuecomment-452446348)

无法避免指标过多，导致prometheus oom。

例如：有个url /user/{userId}， 如果直接统计url的话，每个userId都会统计成一条指标项，最终导致prometheus oom

## gateway的另一种解决方案

[gateway-metrics-filter](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-gateway-metrics-filter)

GatewayMetricsFilter会上报一个新的指标
```
spring_cloud_gateway_requests_seconds_count{httpMethod="POST",httpStatusCode="200",outcome="SUCCESSFUL",path="/api/user/**",path_enabled="true",routeId="user",routeUri="lb://user",status="OK",} 1.0
```

该指标的path 取自 GATEWAY_PREDICATE_MATCHED_PATH_ATTR，即gateway router配置中的PathPredicate
```
public class GatewayPathTagsProvider implements GatewayTagsProvider {

	@Override
	public Tags apply(ServerWebExchange exchange) {
		Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);

		if (route != null) {
			String matchedPathRouteId = exchange.getAttribute(GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR);
			String matchedPath = exchange.getAttribute(GATEWAY_PREDICATE_MATCHED_PATH_ATTR);

			// check that the matched path belongs to the route that was actually
			// selected.
			if (route.getId().equals(matchedPathRouteId) && matchedPath != null) {
				return Tags.of("path", matchedPath);
			}
		}

		return Tags.empty();
	}
}
```

这样一来能统计具体的path，也能有效的控制tag数量，避免oom

## 服务现状
pathPredicate只配置了/user/** ,但user服务有/user/info, /user/point等接口；这样tag path只会有/user/** ，要想统计具体地址，只能在pathPredicate配置具体的path

## 如何解决
### 背景
+ pathPredicate只配置了/user/**, 想统计具体的path
+ user服务不存在restful风格的接口，即/user/{userId}，直接统计path不会导致prometheus oom

### 实现
DefaultWebFluxTagsProvider提供了http_server_requests_seconds_count tag信息

通过创建CustomWebFluxTagsProvider，替换DefaultWebFluxTagsProvider，完成具体uri的统计

```
private Tag uriTag(ServerWebExchange exchange, Throwable e) {
    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    if (route != null) {
        return Tag.of("uri", exchange.getRequest().getURI().getPath());
    }
    return WebFluxTags.uri(exchange, this.ignoreTrailingSlash);
}
```
