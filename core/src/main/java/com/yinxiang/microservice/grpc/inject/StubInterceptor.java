package com.yinxiang.microservice.grpc.inject;

import io.grpc.stub.AbstractStub;

import java.util.function.Function;

/**
 * The {@link AbstractStub}'s interceptor in custom interface method which annotation by
 * {@link com.yinxiang.microservice.grpc.inject.annotations.GrpcClient}.
 * @param <S> sub type of {@link AbstractStub}
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public interface StubInterceptor<S extends AbstractStub<S>> extends Function<S, S> {}
