package com.yinxiang.microservice.grpc.inject.factories;

import com.google.common.base.Strings;
import com.yinxiang.microservice.grpc.inject.StubInterceptor;
import com.yinxiang.microservice.grpc.inject.annotations.GrpcHeader;
import com.yinxiang.microservice.grpc.inject.annotations.GrpcParam;
import io.grpc.stub.StreamObserver;

import java.lang.reflect.Parameter;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Collect and filter a parameter of the method, and record to a cache.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
interface MethodParametersCollector {
  /**
   * Collect and filter a parameter to a cache.
   * @param index     parameter's index
   * @param parameter a parameter of the method
   * @param context   context of this parameter
   * @return  if record return true, else false
   */
  boolean collect(int index, Parameter parameter, ParametersContext context);
}

/**
 * A cache supplier, key is {@link CacheType}, cache is a {@link BiConsumer}.
 * This cache only can record parameter and it's index.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
interface CacheSupplier extends Function<CacheType, BiConsumer<Integer, Parameter>> {}

/**
 * An abstract {@link MethodParametersCollector}, has a cache and the standard collect process.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
abstract class AbstractParametersCollector implements MethodParametersCollector {
  /** The parameter's cache. */
  private final BiConsumer<Integer, Parameter> parameterCache;

  AbstractParametersCollector(Function<CacheType, BiConsumer<Integer, Parameter>> cacheSupplier) {
    this.parameterCache = cacheSupplier.apply(getCacheType());
  }

  /**
   * Getter of the {@link CacheType}.
   * @return  {@link CacheType}
   */
  abstract CacheType getCacheType();

  @Override
  public boolean collect(int index, Parameter parameter, ParametersContext context) {
    if (check(index, parameter, context)) {
      parameterCache.accept(index, parameter);
      return true;
    }
    return false;
  }

  /**
   * Check a parameter if need to record.
   * @param index     parameter's index
   * @param parameter a parameter of the method
   * @param context   context of this parameter
   * @return  if need record return true, else false
   */
  abstract boolean check(int index, Parameter parameter, ParametersContext context);
}

/**
 * An chain of {@link MethodParametersCollector}, has the next chain element and the standard collect process.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
abstract class ParametersCollectorChain extends AbstractParametersCollector {
  /** The next chain element */
  private final MethodParametersCollector next;

  ParametersCollectorChain(CacheSupplier cacheSupplier, MethodParametersCollector next) {
    super(cacheSupplier);
    this.next = next;
  }

  @Override
  public boolean collect(int index, Parameter parameter, ParametersContext context) {
    return super.collect(index, parameter, context) || next.collect(index, parameter, context);
  }
}

/**
 * Collect the request's parameters.
 * @see FillParametersGenerator
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class FillParametersCollector extends ParametersCollectorChain {
  FillParametersCollector(CacheSupplier cacheSupplier, MethodParametersCollector next) {
    super(cacheSupplier, next);
  }

  /**
   * Get the name from {@link Parameter}.
   * @param parameter {@link Parameter}
   * @return  the name from {@link Parameter}
   */
  static String getParamName(Parameter parameter) {
    // get @GrpcParam
    GrpcParam grpcParam = parameter.getAnnotation(GrpcParam.class);
    // get name of gRPC method parameter
    return grpcParam == null || Strings.isNullOrEmpty(grpcParam.param()) ? parameter.getName() : grpcParam.param();
  }

  @Override
  CacheType getCacheType() {
    return CacheType.Fill;
  }

  @Override
  boolean check(int index, Parameter parameter, ParametersContext context) {
    // get @GrpcParam
    GrpcParam grpcParam = parameter.getAnnotation(GrpcParam.class);
    if (grpcParam != null && grpcParam.isJson()) {
      return false;
    }
    try{
      checkNotNull(context.getRequestType().getDeclaredField(getParamName(parameter) + "_"));
      return true;
    } catch (NullPointerException | NoSuchFieldException e) {
      return false;
    }
  }
}

/**
 * Collect the stream request.
 * @see StreamParametersGenerator
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class StreamParametersCollector extends ParametersCollectorChain {
  StreamParametersCollector(CacheSupplier cacheSupplier, MethodParametersCollector next) {
    super(cacheSupplier, next);
  }

  @Override
  CacheType getCacheType() {
    return CacheType.Stream;
  }

  @Override
  boolean check(int index, Parameter parameter, ParametersContext context) {
    return StreamObserver.class.isAssignableFrom(parameter.getType());
  }
}

/**
 * Collect the parameter which with {@link GrpcParam} annotation and {@link GrpcParam#isJson()} is true.
 * @see JsonParametersGenerator
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class JsonParametersCollector extends ParametersCollectorChain {
  JsonParametersCollector(CacheSupplier cacheSupplier, MethodParametersCollector next) {
    super(cacheSupplier, next);
  }

  @Override
  CacheType getCacheType() {
    return CacheType.Json;
  }

  @Override
  boolean check(int index, Parameter parameter, ParametersContext context) {
    GrpcParam annotation = parameter.getAnnotation(GrpcParam.class);
    return annotation != null && annotation.isJson();
  }
}

/**
 * Collect the Request.Builder parameters.
 * @see BuilderParametersGenerator
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class BuilderParametersCollector extends ParametersCollectorChain {
  BuilderParametersCollector(CacheSupplier cacheSupplier, MethodParametersCollector next) {
    super(cacheSupplier, next);
  }

  @Override
  CacheType getCacheType() {
    return CacheType.Builder;
  }

  @Override
  boolean check(int index, Parameter parameter, ParametersContext context) {
    Class requestType = context.getRequestType();
    return requestType != null && parameter.getType().getName().equals(requestType.getName() + "$Builder");
  }
}

/**
 * Collect the Request parameters.
 * @see RequestParametersGenerator
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class RequestParametersCollector extends ParametersCollectorChain {
  RequestParametersCollector(CacheSupplier cacheSupplier, MethodParametersCollector next) {
    super(cacheSupplier, next);
  }

  @Override
  CacheType getCacheType() {
    return CacheType.Request;
  }

  @Override
  boolean check(int index, Parameter parameter, ParametersContext context) {
    return parameter.getType().equals(context.getRequestType());
  }
}

/**
 * Collect the parameters which with {@link GrpcHeader} annotation.
 * @see HeaderParametersGenerator
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class HeaderParametersCollector extends ParametersCollectorChain {
  HeaderParametersCollector(CacheSupplier cacheSupplier, MethodParametersCollector next) {
    super(cacheSupplier, next);
  }

  @Override
  CacheType getCacheType() {
    return CacheType.Header;
  }

  @Override
  boolean check(int index, Parameter parameter, ParametersContext context) {
    return parameter.getAnnotation(GrpcHeader.class) != null;
  }
}

/**
 * Collect the parameters which type is {@link StubInterceptor}.
 * @see InterceptorParametersGenerator
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class InterceptorParametersCollector extends ParametersCollectorChain {
  InterceptorParametersCollector(CacheSupplier cacheSupplier, MethodParametersCollector next) {
    super(cacheSupplier, next);
  }

  @Override
  CacheType getCacheType() {
    return CacheType.Interceptor;
  }

  @Override
  boolean check(int index, Parameter parameter, ParametersContext context) {
    return StubInterceptor.class.equals(parameter.getType());
  }
}

/**
 * Collect the unused parameters, this is the last collector.
 * @see DeadParametersGenerator
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class DeadParametersCollector extends AbstractParametersCollector {
  DeadParametersCollector(Function<CacheType, BiConsumer<Integer, Parameter>> cacheSupplier) {
    super(cacheSupplier);
  }

  @Override
  CacheType getCacheType() {
    return CacheType.Dead;
  }

  @Override
  boolean check(int index, Parameter parameter, ParametersContext context) {
    return true;
  }
}
