package com.yinxiang.microservice.grpc.inject;

import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class DomainClientInterceptor implements ClientInterceptor {
  private static final Logger log = LoggerFactory.getLogger(DomainClientInterceptor.class);
  private static final Metadata.Key<String> HOST =
          Metadata.Key.of("X-Forwarded-Host", Metadata.ASCII_STRING_MARSHALLER);

  private final Supplier<String> domainSupplier;

  public DomainClientInterceptor(Supplier<String> domainSupplier) {
    this.domainSupplier = domainSupplier;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                             CallOptions callOptions, Channel next) {
    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        headers.put(HOST, domainSupplier.get());
        super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
          /**
           * if you don't need receive header from server, you can
           * use {@link io.grpc.stub.MetadataUtils#attachHeaders}
           * directly to send header
           */
          @Override
          public void onHeaders(Metadata headers) {
            log.debug("header received from server:" + headers);
            super.onHeaders(headers);
          }
        }, headers);
      }
    };
  }
}
