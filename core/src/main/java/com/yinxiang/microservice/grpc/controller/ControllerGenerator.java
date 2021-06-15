package com.yinxiang.microservice.grpc.controller;

import com.yinxiang.microservice.grpc.generator.AbstractGrpcGenerator;
import com.yinxiang.microservice.grpc.generator.processors.MethodProcessor;
import com.yinxiang.microservice.grpc.generator.processors.ServiceProcessor;
import com.yinxiang.microservice.grpc.generator.processors.SimpleMessageProcessor;
import com.yinxiang.microservice.grpc.generator.processors.SimpleServiceProcessor;

/**
 * The controller java file generator by gRPC protobuf classes.
 *
 * Can use in maven build plugin like this:
 *
 * <ol>
 *   <li>groupId: org.codehaus.mojo</li>
 *   <li>artifactId: exec-maven-plugin</li>
 *   <li>version: 3.0.0</li>
 *   <li>executions.execution.phase: compile</li>
 *   <li>executions.execution.goals.goal: java</li>
 *   <li>executions.execution.configuration.mainClass: com.yinxiang.microservice.grpc.controller.ControllerGenerator</li>
 *   <li>executions.execution.configuration.arguments.argument: ${basedir}/../test-stub/target/generated-sources/protobuf/</li>
 *   <li>executions.execution.configuration.arguments.argument: ${basedir}/../test-stub/target/classes</li>
 *   <li>executions.execution.configuration.arguments.argument: ${basedir}/com/yinxiang/microservice/grpc/controller</li>
 *   <li>executions.execution.configuration.arguments.argument: com.yinxiang.microservice.grpc.controller</li>
 * </ol>
 *
 * The first argument is gRPC protobuf classpath.
 * The second argument is controller java file output path.
 * The third argument is controller's class package.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public final class ControllerGenerator extends AbstractGrpcGenerator {
  private static SimpleServiceProcessor createServiceProcessor() {
    return new SimpleServiceProcessor(new MethodProcessor(new SimpleMessageProcessor()));
  }

  private ControllerGenerator() {
    super(createServiceProcessor(), new ControllerBuilder());
  }

  @Override
  protected String getFileFormat() {
    return ".java";
  }

  public static void main(String[] args) throws Exception {
    new ControllerGenerator().generator(args[0], args[1], args[2]);
  }
}
