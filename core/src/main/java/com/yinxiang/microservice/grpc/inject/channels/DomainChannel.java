package com.yinxiang.microservice.grpc.inject.channels;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class DomainChannel extends ManagedChannel {

  private final Map<String, ManagedChannel> channels;
  private final Supplier<String> domainSupplier;

  public DomainChannel(Map<String, ManagedChannel> channels, Supplier<String> domainSupplier) {
    this.channels = channels;
    this.domainSupplier = domainSupplier;
  }

  @Override
  public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(
          MethodDescriptor<RequestT, ResponseT> methodDescriptor,
          CallOptions callOptions
  ) {
    return getChannel().newCall(methodDescriptor, callOptions);
  }

  @Override
  public String authority() {
    return getChannel().authority();
  }

  private ManagedChannel getChannel() {
    return channels.get(domainSupplier.get());
  }

  @Override
  public ManagedChannel shutdown() {
    return getChannel().shutdown();
  }

  @Override
  public boolean isShutdown() {
    return getChannel().isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return getChannel().isTerminated();
  }

  @Override
  public ManagedChannel shutdownNow() {
    return getChannel().shutdownNow();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return getChannel().awaitTermination(timeout, unit);
  }
}
