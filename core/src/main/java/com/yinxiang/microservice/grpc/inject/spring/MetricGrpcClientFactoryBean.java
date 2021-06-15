package com.yinxiang.microservice.grpc.inject.spring;

import com.yinxiang.microservice.grpc.metric.MetricCollectingClientInterceptor;
import io.grpc.ClientInterceptor;

public class MetricGrpcClientFactoryBean<T> extends GrpcClientFactoryBean<T> {
  private MetricCollectingClientInterceptor metricCollectingClientInterceptor;

  public MetricGrpcClientFactoryBean() {}

  public MetricGrpcClientFactoryBean(Class<T> mapperInterface) {
    super(mapperInterface);
  }

  public void setMetricCollectingClientInterceptor(MetricCollectingClientInterceptor metricCollectingClientInterceptor) {
    this.metricCollectingClientInterceptor = metricCollectingClientInterceptor;
  }

  @Override
  protected ClientInterceptor[] createClientInterceptors() {
    return new ClientInterceptor[]{metricCollectingClientInterceptor};
  }
}
