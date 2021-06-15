package com.yinxiang.microservice.grpc.inject.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation use to qualify a method, let it can use a alias name and contains a try-catch in the method body.
 * <p>The parameters type priority is:</p>
 * <ol>
 *   <li>Request</li>
 *   <li>Request.builder</li>
 *   <li>json</li>
 *   <li>other</li>
 * </ol>
 * <p>
 *   A special parameter type is {@link com.yinxiang.microservice.grpc.inject.StubInterceptor},
 *   it can get the stub and do anything. If not match any type, the parameter will be dropped.
 * </p>
 * @author Huiyuan Fu
 * @since 1.0.0
 */
@Documented
@Inherited
@Retention(RUNTIME)
@Target({ METHOD })
public @interface GrpcMethod {
  /**
   * If method name is inconsistent with stub method which you want to call, please assignment this attribute.
   * @return  the correct method name of stub
   */
  String method() default "";

  /**
   * If true will contains a try-catch in the method body and return null when catch any exception.
   * @return  can contains a try-catch
   */
  boolean safe() default false;
}
