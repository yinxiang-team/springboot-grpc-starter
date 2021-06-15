package com.yinxiang.microservice.grpc.inject.factories;

import com.google.common.base.Strings;
import com.yinxiang.microservice.grpc.inject.StubInterceptor;
import com.yinxiang.microservice.grpc.inject.header.StubHeadersProcessor;
import com.yinxiang.microservice.grpc.inject.annotations.GrpcHeader;
import com.yinxiang.microservice.grpc.util.StringUtils;
import io.grpc.stub.AbstractStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Generate javasisst code to method body.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
interface MethodParametersGenerator {
  /**
   * Generate javasisst code to method body.
   * @param bodyBuilder {@link StringBuilder} of method
   * @param context     context of this parameter
   * @return  return true when generate success, else false
   */
  boolean fillBody(StringBuilder bodyBuilder, ParametersContext context);
}

/**
 * The cache of parameters correspond to a {@link MethodParametersCollector}.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
interface ParametersGeneratorCache extends IntFunction<Parameter> {
  /**
   * Traverse all parameters in the cache.
   * @param parameterConsumer  consumer of parameter
   */
  void forEachParameters(BiConsumer<Integer, Parameter> parameterConsumer);

  /**
   * Visit the first parameter's index.
   * @param consumer  consumer of parameter's index
   * @return  true if has a key, else false
   */
  boolean visitorFirstKey(Consumer<Integer> consumer);
}

/**
 * An abstract {@link MethodParametersGenerator} and is a {@link ParametersGeneratorCache}.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
abstract class AbstractParameterGenerator implements MethodParametersGenerator, ParametersGeneratorCache {
  /** The real cache. */
  private final ParametersGeneratorCache cache;

  AbstractParameterGenerator(Function<CacheType, ParametersGeneratorCache> cacheSupplier) {
    this.cache = cacheSupplier.apply(getCacheType());
  }

  /**
   * Getter of the {@link CacheType}.
   * @return  {@link CacheType}
   */
  abstract CacheType getCacheType();

  @Override
  public void forEachParameters(BiConsumer<Integer, Parameter> parameterVisitor) {
    cache.forEachParameters(parameterVisitor);
  }

  @Override
  public boolean visitorFirstKey(Consumer<Integer> consumer) {
    return cache.visitorFirstKey(consumer);
  }

  @Override
  public Parameter apply(int value) {
    return cache.apply(value);
  }
}

/**
 * An chain of {@link AbstractParameterGenerator}, has the next chain element and the standard generate process.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
abstract class ParametersGeneratorChain extends AbstractParameterGenerator {
  /** The next chain element */
  private final MethodParametersGenerator next;

  ParametersGeneratorChain(Function<CacheType, ParametersGeneratorCache> cacheSupplier, MethodParametersGenerator next) {
    super(cacheSupplier);
    this.next = next;
  }

  @Override
  public boolean fillBody(StringBuilder bodyBuilder, ParametersContext context) {
    return fillBodyChain(bodyBuilder, context) || next.fillBody(bodyBuilder, context);
  }

  /**
   * Generate code in this chain.
   * @param bodyBuilder {@link StringBuilder} of method
   * @param context     context of this parameter
   * @return  return true when generate success, else false
   */
  abstract boolean fillBodyChain(StringBuilder bodyBuilder, ParametersContext context);
}

/**
 * Generate the request's parameters.
 * @see FillParametersCollector
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class FillParametersGenerator extends AbstractParameterGenerator {
  FillParametersGenerator(Function<CacheType, ParametersGeneratorCache> cacheSupplier) {
    super(cacheSupplier);
  }

  @Override
  CacheType getCacheType() {
    return CacheType.Fill;
  }

  @Override
  public boolean fillBody(StringBuilder bodyBuilder, ParametersContext context) {
    // create request builder
    bodyBuilder.append(context.getRequestType().getName()).append(".newBuilder()");
    // append setter
    forEachParameters((index, parameter) -> {
      // get type of parameter
      Class type = parameter.getType();
      // fill request parameter
      bodyBuilder.append(".")
              .append(Iterable.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type) ? "addAll" : "set")
              .append(StringUtils.firstUpper(FillParametersCollector.getParamName(parameter)))
              .append("($")
              .append(index + 1)
              .append(")");
    });
    return true;
  }
}

/**
 * Generate the Stream parameters.
 * @see StreamParametersCollector
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class StreamParametersGenerator extends AbstractParameterGenerator {
  StreamParametersGenerator(Function<CacheType, ParametersGeneratorCache> cacheSupplier) {
    super(cacheSupplier);
  }

  @Override
  CacheType getCacheType() {
    return CacheType.Stream;
  }

  @Override
  public boolean fillBody(StringBuilder bodyBuilder, ParametersContext context) {
    return visitorFirstKey(index -> bodyBuilder.append("$").append(index + 1));
  }
}

/**
 * Generate the Request.Builder parameters.
 * @see BuilderParametersCollector
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class BuilderParametersGenerator extends ParametersGeneratorChain {
  BuilderParametersGenerator(
          Function<CacheType, ParametersGeneratorCache> cacheSupplier,
          MethodParametersGenerator next
  ) {
    super(cacheSupplier, next);
  }

  @Override
  public boolean fillBodyChain(StringBuilder bodyBuilder, ParametersContext context) {
    return visitorFirstKey(index -> bodyBuilder.append("$").append(index + 1));
  }

  @Override
  CacheType getCacheType() {
    return CacheType.Builder;
  }
}

/**
 * Generate the Request parameters.
 * @see RequestParametersCollector
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class RequestParametersGenerator extends ParametersGeneratorChain {
  RequestParametersGenerator(
          Function<CacheType, ParametersGeneratorCache> cacheSupplier,
          MethodParametersGenerator next
  ) {
    super(cacheSupplier, next);
  }

  @Override
  public boolean fillBodyChain(StringBuilder bodyBuilder, ParametersContext context) {
    return visitorFirstKey(index -> bodyBuilder.append("$").append(index + 1));
  }

  @Override
  CacheType getCacheType() {
    return CacheType.Request;
  }
}

/**
 * Generate the parameters which with {@link GrpcHeader} annotation.
 * @see HeaderParametersCollector
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class HeaderParametersGenerator extends AbstractParameterGenerator {
  private static final String _HEADER_PROCESSOR = StubHeadersProcessor.class.getName();

  HeaderParametersGenerator(Function<CacheType, ParametersGeneratorCache> cacheSupplier) {
    super(cacheSupplier);
  }

  @Override
  CacheType getCacheType() {
    return CacheType.Header;
  }

  @Override
  public boolean fillBody(StringBuilder bodyBuilder, ParametersContext context) {
    Class<? extends AbstractStub> stubClass = context.getStubClass();
    forEachParameters((index, parameter) -> {
      // header
      GrpcHeader grpcHeader = parameter.getAnnotation(GrpcHeader.class);
      // header key name
      String name = grpcHeader.name();
      if (Strings.isNullOrEmpty(name)) {
        name = parameter.getName();
      }
      // register header key
      StubHeadersProcessor.registerKey(stubClass, name, grpcHeader.type());
      // add header code
      bodyBuilder.append(stubClass.getName())
              .append(" stub = (")
              .append(stubClass.getName())
              .append(")")
              .append(_HEADER_PROCESSOR)
              .append(".withHeader(this.stub, \"")
              .append(name)
              .append("\", $")
              .append((index + 1))
              .append(");");
    });
    return true;
  }
}

/**
 * Generate the parameters which type is {@link StubInterceptor}.
 * @see InterceptorParametersCollector
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class InterceptorParametersGenerator extends AbstractParameterGenerator {
  InterceptorParametersGenerator(Function<CacheType, ParametersGeneratorCache> cacheSupplier) {
    super(cacheSupplier);
  }

  @Override
  CacheType getCacheType() {
    return CacheType.Interceptor;
  }

  @Override
  public boolean fillBody(StringBuilder bodyBuilder, ParametersContext context) {
    Class<? extends AbstractStub> stubClass = context.getStubClass();
    forEachParameters((index, parameter) -> bodyBuilder.append(stubClass.getName())
            .append(" stub = (")
            .append(stubClass.getName())
            .append(")$")
            .append((index + 1))
            .append(".apply(stub);"));
    return true;
  }
}

/**
 * Generate the unused parameters, this is the last collector.
 * @see DeadParametersCollector
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class DeadParametersGenerator extends AbstractParameterGenerator {
  private static final Logger log = LoggerFactory.getLogger(DeadParametersGenerator.class);

  DeadParametersGenerator(Function<CacheType, ParametersGeneratorCache> cacheSupplier) {
    super(cacheSupplier);
  }

  @Override
  public boolean fillBody(StringBuilder bodyBuilder, ParametersContext context) {
    forEachParameters((index, parameter) -> log.warn("unused parameter[{}: {}]", index, parameter.getName()));
    return true;
  }

  @Override
  CacheType getCacheType() {
    return CacheType.Dead;
  }
}
