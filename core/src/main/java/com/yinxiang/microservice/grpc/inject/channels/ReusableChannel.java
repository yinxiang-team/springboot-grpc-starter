package com.yinxiang.microservice.grpc.inject.channels;

import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * A channel can reusable when server was restarted.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class ReusableChannel extends ManagedChannel {
  private static final Logger log = LoggerFactory.getLogger(ReusableChannel.class);

  /** Server's address. */
  private final String address;
  /** Array of ClientInterceptor. */
  private final ClientInterceptor[] clientInterceptors;
  /** The real {@link ManagedChannel}. */
  private ManagedChannel delegate;

  public ReusableChannel(String address, ClientInterceptor... clientInterceptors) {
    this.address = address;
    this.clientInterceptors = clientInterceptors;
    delegate = createChannel();
  }

  public ReusableChannel(String host, int port, ClientInterceptor... clientInterceptors) {
    this(String.format("%s:%d", host, port), clientInterceptors);
  }

  /**
   * Get the real {@link ManagedChannel}, and create if not exists.
   * @return  {@link ManagedChannel}
   */
  private synchronized ManagedChannel getDelegate() {
    if (delegate.isShutdown() || delegate.isTerminated()) {
      log.error(String.format("channel(%s) was shutdown(%s) or terminated(%s)",
              address, delegate.isShutdown(), delegate.isTerminated()));
      delegate = createChannel();
    }
    return delegate;
  }

  /**
   * Create the {@link ManagedChannel}.
   * @return  {@link ManagedChannel}
   */
  private ManagedChannel createChannel() {
    return ManagedChannelBuilder.forTarget(address).intercept(clientInterceptors).usePlaintext().build();
  }

  @Override
  public ManagedChannel shutdown() {
    return delegate.shutdown();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public ManagedChannel shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return getDelegate().awaitTermination(timeout, unit);
  }

  @Override
  public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(
          MethodDescriptor<RequestT, ResponseT> methodDescriptor,
          CallOptions callOptions
  ) {
    return getDelegate().newCall(methodDescriptor, callOptions);
  }

  @Override
  public String authority() {
    return getDelegate().authority();
  }
}
