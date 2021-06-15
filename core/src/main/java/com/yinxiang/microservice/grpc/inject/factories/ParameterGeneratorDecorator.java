package com.yinxiang.microservice.grpc.inject.factories;

import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import com.yinxiang.microservice.grpc.inject.Marshaller;
import com.yinxiang.microservice.grpc.inject.annotations.GrpcParam;
import io.grpc.MethodDescriptor;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.function.Function;

import static io.grpc.MethodDescriptor.MethodType.*;

/**
 * An decorator of {@link MethodParametersGenerator}, has a delegate to process generate and generate more code.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
abstract class ParameterGeneratorDecorator implements MethodParametersGenerator {
  /** The real {@link MethodParametersGenerator}. */
  final MethodParametersGenerator delegate;

  ParameterGeneratorDecorator(MethodParametersGenerator delegate) {
    this.delegate = delegate;
  }
}

/**
 * Generate {@code try-catch}.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class CatchableParametersGenerator extends ParameterGeneratorDecorator {
  CatchableParametersGenerator(MethodParametersGenerator delegate) {
    super(delegate);
  }

  @Override
  public boolean fillBody(StringBuilder bodyBuilder, ParametersContext context) {
    bodyBuilder.append("try {");
    delegate.fillBody(bodyBuilder, context);
    bodyBuilder.append("} catch (Exception e) { log.error(getClass().getName() + \".")
            .append(context.getMethodName())
            .append(" Exception: \" + e.getMessage(), e);")
            .append(context.isVoid() ? "" : "return null;")
            .append("}");
    return true;
  }
}

/**
 * Generate {@code return}.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class NormalParametersGenerator extends ParameterGeneratorDecorator {
  NormalParametersGenerator(MethodParametersGenerator delegate) {
    super(delegate);
  }

  @Override
  public boolean fillBody(StringBuilder bodyBuilder, ParametersContext context) {
    bodyBuilder.append(context.isVoid() ? "" : "return ");
    Class returnType = context.getReturnType();
    if (String.class.equals(returnType)) {
      bodyBuilder.append(Marshaller.class.getName()).append(".toJson(");
      fill(bodyBuilder, context).append(");");
    } else if (Message.class.isAssignableFrom(returnType)) {
      fill(bodyBuilder, context).append(";");
    } else {
      bodyBuilder.append(Marshaller.class.getName()).append(".toObject(");
      fill(bodyBuilder, context).append(", ").append(returnType.getName()).append(".class);");
    }
    return true;
  }

  /**
   * A common generate process.
   * @param bodyBuilder {@link StringBuilder} of method
   * @param context     context of this parameter
   * @return  return true when generate success, else false
   */
  private StringBuilder fill(StringBuilder bodyBuilder, ParametersContext context) {
    bodyBuilder.append("stub.").append(context.getMethodName()).append('(');
    delegate.fillBody(bodyBuilder, context);
    bodyBuilder.append(")");
    return bodyBuilder;
  }
}

/**
 * Generate {@code .build()}.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class UseBuilderParametersGenerator extends ParameterGeneratorDecorator {
  UseBuilderParametersGenerator(MethodParametersGenerator delegate) {
    super(delegate);
  }

  @Override
  public boolean fillBody(StringBuilder bodyBuilder, ParametersContext context) {
    delegate.fillBody(bodyBuilder, context);
    bodyBuilder.append(".build()");
    return true;
  }
}

/**
 * Generate the parameter which with {@link GrpcParam} annotation and {@link GrpcParam#isJson()} is true.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class JsonParametersGenerator extends ParameterGeneratorDecorator {
  /** The cache of parameters. */
  private final ParametersGeneratorCache cache;

  JsonParametersGenerator(Function<CacheType, ParametersGeneratorCache> cacheSupplier) {
    super(new FillParametersGenerator(cacheSupplier));
    cache = cacheSupplier.apply(CacheType.Json);
  }

  @Override
  public boolean fillBody(StringBuilder bodyBuilder, ParametersContext context) {
    cache.visitorFirstKey(index -> {
      Parameter parameter = cache.apply(index);
      String marshallerMethod = String.class.equals(parameter.getType()) ? "fromJsonButNot" : "fromObjectButNot";
      bodyBuilder.append("(")
              .append(context.getRequestType().getName())
              .append(")")
              .append(Marshaller.class.getName())
              .append(".")
              .append(marshallerMethod)
              .append("(");
    });
    delegate.fillBody(bodyBuilder, context);
    cache.visitorFirstKey(index -> bodyBuilder.append(", $").append(index + 1).append(")"));
    return true;
  }
}

/**
 * Generate all kinds of parameters of stub' method, ex: Request/Request.Builder/request parameters.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class ParameterGenerators implements MethodParametersGenerator {
  private final List<MethodParametersGenerator> list = Lists.newLinkedList();

  void addParametersGenerator(MethodParametersGenerator parameterGenerator) {
    list.add(parameterGenerator);
  }

  @Override
  public boolean fillBody(StringBuilder bodyBuilder, ParametersContext context) {
    list.forEach(generator -> generator.fillBody(bodyBuilder, context));
    return false;
  }
}

/**
 * Generate Stream method.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class StubParameterGeneratorDecorator extends ParameterGeneratorDecorator {
  StubParameterGeneratorDecorator(MethodParametersGenerator delegate) {
    super(delegate);
  }

  @Override
  public boolean fillBody(StringBuilder bodyBuilder, ParametersContext context) {
    MethodDescriptor.MethodType methodType = context.getMethodType();
    if (methodType == BIDI_STREAMING || methodType == CLIENT_STREAMING) {
      return true;
    }
    String className = context.getStubClass().getSimpleName();
    if (className.endsWith("BlockingStub") || className.endsWith("FutureStub")) {
      boolean ret = delegate.fillBody(bodyBuilder, context);
      bodyBuilder.append(ret && context.getParamCount() > 1 ? ", " : "");
      return ret;
    }
    return true;
  }
}
