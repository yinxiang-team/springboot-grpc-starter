package com.yinxiang.microservice.grpc.inject.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation use to qualify a parameter, let it fill the stub's {@link io.grpc.Metadata}.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
@Documented
@Inherited
@Retention(RUNTIME)
@Target({ PARAMETER })
public @interface GrpcHeader {
  /**
   * The name of header, default value means use the parameter's name.
   * @return  name of header
   */
  String name() default "";

  /**
   * The type of header, propose use the default type.
   * @return  type of header
   */
  Class type() default String.class;
}
