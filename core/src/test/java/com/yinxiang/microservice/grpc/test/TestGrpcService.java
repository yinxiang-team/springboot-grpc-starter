package com.yinxiang.microservice.grpc.test;

import com.yinxiang.microservice.grpc.inject.LogSupplier;
import com.yinxiang.microservice.grpc.inject.StubInterceptor;
import com.yinxiang.microservice.grpc.inject.annotations.GrpcClient;
import com.yinxiang.microservice.grpc.inject.annotations.GrpcHeader;
import com.yinxiang.microservice.grpc.inject.annotations.GrpcMethod;
import com.yinxiang.microservice.grpc.inject.annotations.GrpcParam;
import com.yinxiang.microservice.grpc.inject.header.StubHeadersProcessor;

import static com.google.common.base.Preconditions.checkNotNull;

@GrpcClient(stub = TestServiceGrpc.TestServiceBlockingStub.class)
public interface TestGrpcService extends LogSupplier {
  @GrpcMethod(method = "search", safe = true)
  SearchReply s(@GrpcParam(param = "id") int id);
  SearchReply search(@GrpcParam(param = "id") int id);
  DetailReply detail(@GrpcParam(param = "id") int id);
  SearchReply search(@GrpcParam(param = "id") int id, @GrpcHeader(name = "auth") String a);
  SearchReply search(@GrpcParam(param = "id") int id, StubInterceptor<TestServiceGrpc.TestServiceBlockingStub> interceptor);
  DetailReply detail(@GrpcParam(param = "id") int a, @GrpcHeader(name = "auth", type = String.class) String auth);

  default SearchReply searchWithLog(int id) {
    _log().info("will search with id: {}.", id);
    search(id, stub -> StubHeadersProcessor.withHeader(checkNotNull(stub), "auth", "auth"));
    return search(id, stub -> StubHeadersProcessor.withHeader(checkNotNull(stub), "auth", String.class, "auth"));
  }
}
