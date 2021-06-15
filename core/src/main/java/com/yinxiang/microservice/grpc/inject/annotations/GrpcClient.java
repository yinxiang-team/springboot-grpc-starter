package com.yinxiang.microservice.grpc.inject.annotations;

import io.grpc.stub.AbstractStub;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The annotation of gRPC client, use to add into spring IOC container.
 * <ol>
 *   <li>Only support interface.</li>
 *   <li>If method(not default method) name is inconsistent with stub method which you want to call, please use {@link GrpcMethod}.</li>
 *   <li>The method parameters are support {@code Request} or {@code Request.Builder} or specific parameters of {@code Request}.</li>
 *   <li>If parameter name is inconsistent with target {@code Request}, please use {@link GrpcParam}.</li>
 *   <li>All default methods will not override.</li>
 *   <li>This lib will implement all interfaces which annotation by GrpcClient.</li>
 *   <li>Must assignment the stub which will be use.</li>
 * </ol>
 * @see GrpcMethod
 * @author Huiyuan Fu
 * @since 1.0.0
 */
@Documented
@Inherited
@Retention(RUNTIME)
@Target({ TYPE })
@Component
public @interface GrpcClient {
  /**
   * The dependency stub class.
   * @return  dependency stub class
   */
  Class<? extends AbstractStub> stub();

  /**
   * The log method name, in implement class will generator a log4j's log, can defined a method to get this log.
   * @return  log method name
   * @see com.yinxiang.microservice.grpc.inject.LogSupplier
   */
  String logMethod() default "_log";
}
