package com.yinxiang.microservice.grpc.doc;

import com.yinxiang.microservice.grpc.generator.AbstractGrpcGenerator;
import com.yinxiang.microservice.grpc.generator.processors.ServiceProcessor;

/**
 * The REST API text file generator by gRPC protobuf classes.
 *
 * Can use in maven build plugin like this:
 *
 * <ol>
 *   <li>groupId: org.codehaus.mojo</li>
 *   <li>artifactId: exec-maven-plugin</li>
 *   <li>version: 3.0.0</li>
 *   <li>executions.execution.phase: compile</li>
 *   <li>executions.execution.goals.goal: java</li>
 *   <li>executions.execution.configuration.mainClass: com.yinxiang.microservice.grpc.doc.RestApiGenerator</li>
 *   <li>executions.execution.configuration.arguments.argument: ${basedir}/../test-stub/target/generated-sources/protobuf/</li>
 *   <li>executions.execution.configuration.arguments.argument: ${basedir}/../test-stub/target/classes</li>
 *   <li>executions.execution.configuration.arguments.argument: ${basedir}</li>
 *   <li>executions.execution.configuration.arguments.argument: </li>
 * </ol>
 *
 * The first argument is gRPC protobuf classpath.
 * The second argument is output path.
 * The third argument unused.
 * @author Huiyuan Fu
 */
public class RestApiGenerator extends AbstractGrpcGenerator {
  private RestApiGenerator(String sourcePath) {
    super(ServiceProcessor.createFullServiceProcessor(sourcePath), new TextBuilder());
  }

  public static void main(String[] args) throws Exception {
    new RestApiGenerator(args[0]).generator(args[1], args[2], args[3]);
  }

  @Override
  protected String getFileFormat() {
    return ".txt";
  }
}
