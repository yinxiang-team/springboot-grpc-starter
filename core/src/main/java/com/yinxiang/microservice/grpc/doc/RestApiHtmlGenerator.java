package com.yinxiang.microservice.grpc.doc;

import com.yinxiang.microservice.grpc.generator.*;
import com.yinxiang.microservice.grpc.generator.processors.*;

/**
 * The REST API html file generator by gRPC protobuf classes.
 *
 * Can use in maven build plugin like this:
 *
 * <ol>
 *   <li>groupId: org.codehaus.mojo</li>
 *   <li>artifactId: exec-maven-plugin</li>
 *   <li>version: 3.0.0</li>
 *   <li>executions.execution.phase: compile</li>
 *   <li>executions.execution.goals.goal: java</li>
 *   <li>executions.execution.configuration.mainClass: com.yinxiang.microservice.grpc.doc.RestApiHtmlGenerator</li>
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
public class RestApiHtmlGenerator extends AbstractGrpcGenerator {
  private RestApiHtmlGenerator(String sourcePath) {
    super(ServiceProcessor.createFullServiceProcessor(sourcePath), new HtmlBuilder());
  }

  @Override
  protected String getFileFormat() {
    return ".html";
  }

  public static void main(String[] args) throws Exception {
    new RestApiHtmlGenerator(args[0]).generator(args[1], args[2], args[3]);
  }
}
