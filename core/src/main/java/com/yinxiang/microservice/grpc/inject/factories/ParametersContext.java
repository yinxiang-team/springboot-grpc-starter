package com.yinxiang.microservice.grpc.inject.factories;

import com.google.common.base.Strings;
import com.yinxiang.microservice.grpc.inject.annotations.GrpcMethod;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractStub;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * A context which contains some info to help process generate method.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class ParametersContext {
  /** The info of stub's method. */
  private final GrpcMethodInfo grpcMethodInfo;
  /** The class of custom interface's method. */
  private final Class returnType;
  /** The stubClass of custom interface. */
  private final Class<? extends AbstractStub> stubClass;
  /** If need safe. */
  private final boolean safe;
  /** The name of gRPC method. */
  private final String methodName;

  ParametersContext(
          Method method,
          Function<String, GrpcMethodInfo> grpcMethodInfoGetter,
          Class<? extends AbstractStub> stubClass
  ) {
    this.stubClass = stubClass;
    // get return type
    returnType = method.getReturnType();
    // get @GrpcMethod
    GrpcMethod methodAnnotation = method.getAnnotation(GrpcMethod.class);
    // get name of gRPC method
    methodName = methodAnnotation == null || Strings.isNullOrEmpty(methodAnnotation.method()) ?
            method.getName() : methodAnnotation.method();
    this.grpcMethodInfo = grpcMethodInfoGetter.apply(methodName);
    // check safe
    safe = methodAnnotation != null && methodAnnotation.safe();
  }

  /** {@link #grpcMethodInfo#getRequestType()} */
  Class getRequestType() {
    return grpcMethodInfo.getRequestType();
  }

  /** {@link #grpcMethodInfo#getMethodType()} */
  MethodDescriptor.MethodType getMethodType() {
    return grpcMethodInfo.getMethodType();
  }

  /** {@link #returnType} */
  Class getReturnType() {
    return returnType;
  }

  /** {@link #stubClass} */
  Class<? extends AbstractStub> getStubClass() {
    return stubClass;
  }

  /**
   * @return true if returnType is null or is {@link Void}
   */
  boolean isVoid() {
    return returnType == null || returnType.equals(Void.class);
  }

  /** {@link #methodName} */
  String getMethodName() {
    return methodName;
  }

  /** {@link #safe} */
  boolean isSafe() {
    return safe;
  }

  /** {@link #grpcMethodInfo#getParamCount()} */
  int getParamCount() {
    return grpcMethodInfo.getParamCount();
  }
}

/**
 * The info of stub's method.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class GrpcMethodInfo {
  /** Class of method's request type. */
  private final Class requestType;
  /** {@link MethodDescriptor.MethodType} of method. */
  private final MethodDescriptor.MethodType methodType;
  /** Count of method's parameters. */
  private final int paramCount;

  GrpcMethodInfo(Class requestType, MethodDescriptor.MethodType methodType, int paramCount) {
    this.requestType = requestType;
    this.methodType = methodType;
    this.paramCount = paramCount;
  }

  /** {@link #requestType} */
  Class getRequestType() {
    return requestType;
  }

  /** {@link #methodType} */
  MethodDescriptor.MethodType getMethodType() {
    return methodType;
  }

  /** {@link #paramCount} */
  int getParamCount() {
    return paramCount;
  }
}

/**
 * Parameters cache type.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
enum CacheType {
  /** Request's fields. */
  Fill,
  /** Stream request. */
  Stream,
  /** Request.Builder. */
  Builder,
  /** Need transform to json and fill request's fields. */
  Json,
  /** Request. */
  Request,
  /** Need add to metadata. */
  Header,
  /** {@link com.yinxiang.microservice.grpc.inject.StubInterceptor}. */
  Interceptor,
  /** Un know. */
  Dead,

  ;
}
