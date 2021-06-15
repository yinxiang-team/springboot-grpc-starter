package com.yinxiang.microservice.grpc.inject.spring;

import com.yinxiang.microservice.grpc.inject.annotations.GrpcClient;
import com.yinxiang.microservice.grpc.inject.factories.GrpcClientCreator;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.factory.FactoryBean;

/**
 * The {@link AbstractStub} creator by {@link GrpcClient}.
 * @param <T> a interface annotation by {@link GrpcClient}
 * @see GrpcClient
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class GrpcClientFactoryBean<T> extends GrpcClientCreator<T> implements FactoryBean<T> {
  public GrpcClientFactoryBean() {}

  public GrpcClientFactoryBean(Class<T> mapperInterface) {
    super(mapperInterface);
  }

  @Override
  protected Class<? extends AbstractStub> getStubClass() {
    return mapperInterface.getAnnotation(GrpcClient.class).stub();
  }

  @Override
  protected String getLogMethod() {
    return mapperInterface.getAnnotation(GrpcClient.class).logMethod();
  }

  @Override
  public T getObject() throws Exception {
    return create();
  }

  @Override
  public Class<?> getObjectType() {
    return mapperInterface;
  }
}
