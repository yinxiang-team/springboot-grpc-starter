package com.yinxiang.microservice.grpc.controller;

import com.google.common.base.Joiner;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import com.yinxiang.grpc.http.Headers;
import com.yinxiang.grpc.http.HeadersFilter;
import com.yinxiang.microservice.grpc.inject.Marshaller;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.yinxiang.microservice.grpc.GrpcServerRunner.serverInterceptorOrderComparator;
import static com.yinxiang.microservice.grpc.inject.header.StubHeadersProcessor.createMetadata;

/**
 * A base controller by gRPC from http request, unified processing gRPC interceptors.
 * This controller can use to start a http server and transform to gRPC server.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public abstract class BaseGrpcController implements ApplicationContextAware {
  /** protobuf - json formatter */
  private static final JsonFormat JSON_FORMAT = new JsonFormat();
  /** A none marshaller. */
  private static final MethodDescriptor.Marshaller NONE_MARSHALLER = new MethodDescriptor.Marshaller() {
    @Override
    public InputStream stream(Object value) {
      return null;
    }

    @Override
    public Object parse(InputStream stream) {
      return null;
    }
  };

  /** List of gRPC interceptors. Will init by springboot, load all interceptors from applicationContext. */
  private List<ServerInterceptor> interceptors;

  /**
   * Process gRPC request from http.
   * @param params    a json string of request parameters
   * @param headers   http headers source from spring controller
   * @param builder   proto builder
   * @param consumer  proto consumer
   * @param method    the descriptor of gRPC method
   * @param <T>       request type
   * @param <B>       response type
   * @return  a json of response
   * @throws JsonFormat.ParseException  Json Exception
   */
  protected <T extends Message, B extends Message> String _process(
          String params,
          MultiValueMap<String, String> headers,
          B.Builder builder,
          BiConsumer<B, StreamObserver<T>> consumer,
          MethodDescriptor method
  ) throws JsonFormat.ParseException {
    return _process(params, createMetadata(headers), builder, consumer, method);
  }

  /**
   * Process gRPC request from http.
   * @param params    a json string of request parameters
   * @param headers   http headers source from any way
   * @param builder   proto builder
   * @param consumer  proto consumer
   * @param method    the descriptor of gRPC method
   * @param <T>       request type
   * @param <B>       response type
   * @return  a json of response
   * @throws JsonFormat.ParseException  Json Exception
   */
  protected <T extends Message, B extends Message> String _process(
          String params,
          Map<String, String> headers,
          B.Builder builder,
          BiConsumer<B, StreamObserver<T>> consumer,
          MethodDescriptor method
  ) throws JsonFormat.ParseException {
    return _process(params, createMetadata(headers), builder, consumer, method);
  }

  /**
   * Process gRPC request from http.
   * @param params    a json string of request parameters
   * @param metadata  gRPC metadata
   * @param builder   proto builder
   * @param consumer  proto consumer
   * @param method    the descriptor of gRPC method
   * @param <T>       request type
   * @param <B>       response type
   * @return  a json of response
   * @throws JsonFormat.ParseException  Json Exception
   */
  @SuppressWarnings("unchecked")
  private <T extends Message, B extends Message> String _process(
          String params,
          Metadata metadata,
          B.Builder builder,
          BiConsumer<B, StreamObserver<T>> consumer,
          MethodDescriptor method
  ) throws JsonFormat.ParseException {
    // merge parameters to message builder
    JSON_FORMAT.merge(params, ExtensionRegistry.getEmptyRegistry(), builder);
    // create response StreamObserver
    RestControllerStreamObserver<T> streamObserver = new RestControllerStreamObserver<>(JSON_FORMAT);
    // process
    _processInterceptors(metadata, message -> consumer.accept((B) message, streamObserver), method)
            .onMessage(builder.build());
    // return result
    return streamObserver.toString();
  }

  /**
   * Process gRPC interceptors.
   * @param metadata  gRPC metadata
   * @param consumer  process consumer
   * @param method    the descriptor of gRPC method
   * @param <T>       process type
   * @return  a json of response
   */
  @SuppressWarnings("unchecked")
  protected <T> ServerCall.Listener<T> _processInterceptors(Metadata metadata, Consumer<T> consumer, MethodDescriptor method) {
    // create the terminal handler, call consumer
    ServerCallHandler handler = (call, hs) -> new ServerCall.Listener() {
      @Override
      public void onMessage(Object message) {
        consumer.accept((T) message);
      }
    };
    // link all interceptors and refresh handler
    for (ServerInterceptor interceptor : interceptors) {
      final ServerCallHandler next = handler;
      handler = (call, hs) -> interceptor.interceptCall(call, hs, next);
    }
    // execute handler and return result
    return handler.startCall(new SimpleServerCall(method), metadata);
  }

  /**
   * Process http headers by gRPC interceptors, thus look like a gRPC request.
   * @param headers   http headers source from spring controller
   * @param consumer  headers consumer
   * @param method    the descriptor of gRPC method
   */
  protected void _processHeadersAndDo(MultiValueMap<String, String> headers, Consumer<Headers> consumer, MethodDescriptor method) {
    _processInterceptors(createMetadata(headers), consumer, method).onMessage(Headers.newBuilder().build());
  }

  /**
   * Process http headers by gRPC interceptors, thus look like a gRPC request.
   * @param headers   http headers source from any way
   * @param consumer  headers consumer
   * @param method    the descriptor of gRPC method
   */
  protected void _processHeadersAndDo(Map<String, String> headers, Consumer<Headers> consumer, MethodDescriptor method) {
    _processInterceptors(createMetadata(headers), consumer, method).onMessage(Headers.newBuilder().build());
  }

  /**
   * Process http request by gRPC interceptors, thus look like a gRPC request.
   * @param headers   http headers source from spring controller
   * @param consumer  headers consumer
   */
  protected void _processHeadersAndDo(MultiValueMap<String, String> headers, Consumer<Headers> consumer) {
    _processHeadersAndDo(headers, consumer, createMethodDescriptor());
  }

  /**
   * Process http request by gRPC interceptors, thus look like a gRPC request.
   * @param headers   http headers source from any way
   * @param consumer  headers consumer
   */
  protected void _processHeadersAndDo(Map<String, String> headers, Consumer<Headers> consumer) {
    _processHeadersAndDo(headers, consumer, createMethodDescriptor());
  }

  /**
   * Process http request by gRPC interceptors, thus look like a gRPC request.
   * @param headers   http headers source from any way
   * @param function  a supplier of result by headers consumer
   */
  protected <T> T _processHeadersAndDo(MultiValueMap<String, String> headers, Function<Headers, T> function) {
    Result<T> result = new Result<>();
    _processHeadersAndDo(headers, header -> {result.t = function.apply(header);});
    return result.t;
  }

  /**
   * Process http request by gRPC interceptors, thus look like a gRPC request.
   * @param headers   http headers source from spring controller
   * @param function  a supplier of result by headers consumer
   */
  protected <T> T _processHeadersAndDo(Map<String, String> headers, Function<Headers, T> function) {
    Result<T> result = new Result<>();
    _processHeadersAndDo(headers, header -> {result.t = function.apply(header);});
    return result.t;
  }

  /** The supplier of result by headers consumer's result container. */
  private static class Result<T> {
    /** Result */T t;
  }

  @Override
  public void setApplicationContext(@NonNull final ApplicationContext applicationContext) throws BeansException {
    Map<String, ServerInterceptor> map = applicationContext.getBeansOfType(ServerInterceptor.class);
    interceptors = map.values().stream()
            .distinct()
            .sorted(serverInterceptorOrderComparator())
            .collect(Collectors.toList());
  }

  /**
   * A common method of error response with HttpStatus#INTERNAL_SERVER_ERROR
   * @param response  HttpServletResponse
   * @param message   a gRPC message
   * @param <T>       gRPC message type
   * @return  string result of error response
   * @see HttpStatus#INTERNAL_SERVER_ERROR
   */
  protected static <T extends Message> String responseError(HttpServletResponse response, T message) {
    return responseError(response, message, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * A common method of error response
   * @param response  HttpServletResponse
   * @param message   a gRPC message
   * @param code      HttpStatus
   * @param <T>       gRPC message type
   * @return  string result of error response
   */
  protected static <T extends Message> String responseError(HttpServletResponse response, T message, HttpStatus code) {
    return responseError(response, message, code.value());
  }

  /**
   * A common method of error response
   * @param response  HttpServletResponse
   * @param message   a gRPC message
   * @param code      status
   * @param <T>       gRPC message type
   * @return  string result of error response
   */
  protected static <T extends Message> String responseError(HttpServletResponse response, T message, int code) {
    response.setStatus(code);
    return Marshaller.toJson(message);
  }

  /**
   * A simple implement of ServerCall, only with the methodDescriptor.
   * @param <T>
   * @param <R>
   */
  private static class SimpleServerCall<T, R> extends ServerCall<T, R> {
    private final MethodDescriptor<T, R> methodDescriptor;

    private SimpleServerCall(MethodDescriptor<T, R> methodDescriptor) {
      this.methodDescriptor = methodDescriptor;
    }

    @Override
    public void request(int numMessages) {}

    @Override
    public void sendHeaders(Metadata headers) {}

    @Override
    public void sendMessage(R message) {}

    @Override
    public void close(Status status, Metadata trailers) {}

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public MethodDescriptor<T, R> getMethodDescriptor() {
      return methodDescriptor;
    }
  }

  /**
   * Create a simulation of MethodDescriptor, use to unified processing gRPC interceptors.
   * @return  a simulation of MethodDescriptor
   * @see MethodDescriptor
   */
  @SuppressWarnings("unchecked")
  private static MethodDescriptor createMethodDescriptor() {
    StackTraceElement element = getStack();
    String[] classNames = element.getClassName().split("\\.");
    return MethodDescriptor.newBuilder(NONE_MARSHALLER, NONE_MARSHALLER)
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(Joiner.on('/').join(classNames[classNames.length - 1], element.getMethodName()))
            .setSchemaDescriptor(HeadersFilter.newBuilder().build())
            .build();
  }

  /**
   * Get stack of call chain.
   * @return  stack of call chain
   */
  private static StackTraceElement getStack() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    int index = 0;
    while (index++ >= stackTrace.length) {
      StackTraceElement element = stackTrace[index];
      if (!element.getClassName().startsWith(BaseGrpcController.class.getName())) {
        return stackTrace[index];
      }
    }
    return stackTrace[1];
  }
}
