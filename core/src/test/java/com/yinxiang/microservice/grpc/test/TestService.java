package com.yinxiang.microservice.grpc.test;

import com.google.common.base.Preconditions;
import com.yinxiang.grpc.http.Headers;
import com.yinxiang.microservice.grpc.GrpcService;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class TestService extends TestServiceGrpc.TestServiceImplBase {
  private static final Logger log = LoggerFactory.getLogger(TestService.class);

  @Override
  public void search(SearchRequest request, StreamObserver<SearchReply> responseObserver) {
    Preconditions.checkArgument(request.hasHeaders());
    Headers header = request.getHeaders();
    Preconditions.checkArgument(header.getAUTHORIZATION().equals(HeadersInterceptor.getAuth()));
    Preconditions.checkArgument(header.getUSERAGENT().equals(HeadersInterceptor.getUserAgent()));
    Preconditions.checkArgument(header.getFORWARDED().equals(HeadersInterceptor.getIp()));
    log.info("Receive gRPC search request: {}.", request);
    responseObserver.onNext(SearchReply.newBuilder().setResult(request.toString()).build());
    responseObserver.onCompleted();
  }

  @Override
  public void detail(DetailRequest request, StreamObserver<DetailReply> responseObserver) {
    Preconditions.checkArgument(request.hasHeaders());
    log.info("Receive gRPC detail request: {}.", request);
    responseObserver.onNext(DetailReply.newBuilder().setResult(request.toString()).build());
    responseObserver.onCompleted();
  }
}
