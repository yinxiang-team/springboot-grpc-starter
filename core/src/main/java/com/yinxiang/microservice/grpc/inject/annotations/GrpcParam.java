package com.yinxiang.microservice.grpc.inject.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation use to qualify a parameter, let it can use a alias name or need transform to json and fill Request.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
@Documented
@Inherited
@Retention(RUNTIME)
@Target({ PARAMETER })
public @interface GrpcParam {
  /**
   * If parameter name is inconsistent with target {@code Request}, please assignment this attribute.
   * @return  the correct parameter name of Request
   */
  String param() default "";

  /**
   * If true this parameter will transform to json and fill Request.
   * @return  if need transform to json
   */
  boolean isJson() default false;
}
