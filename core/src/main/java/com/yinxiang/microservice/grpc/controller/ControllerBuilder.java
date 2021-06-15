package com.yinxiang.microservice.grpc.controller;

import com.yinxiang.microservice.grpc.generator.BaseContentBuilder;
import com.yinxiang.microservice.grpc.generator.infos.MethodInfo;
import com.yinxiang.microservice.grpc.generator.infos.ServiceInfo;
import com.yinxiang.spring.inject.utils.StringUtils;

import java.text.DateFormat;
import java.util.Date;

/**
 * The controller java file builder.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
final class ControllerBuilder extends BaseContentBuilder {
  @Override
  protected String buildBegin(ServiceInfo serviceInfo) {
    StringBuilder builder = new StringBuilder("package ");
    String serviceName = serviceInfo.getName();
    String className = serviceName + "Controller";
    serviceInfo.setFileName(className);
    // append the gRPC class name
    String grpcName = serviceName + "Grpc." + serviceName + "ImplBase";
    String javaPackage = serviceInfo.getJavaPackage();
    // fill codes
    return builder.append(serviceInfo.getPkg()).append(";\n\n")
            .append("import com.googlecode.protobuf.format.JsonFormat;\nimport ")
            .append(javaPackage)
            .append(".")
            .append(serviceName)
            .append("Grpc;\nimport org.springframework.util.MultiValueMap;\n")
            .append("import org.springframework.web.bind.annotation.*;\n\n")
            .append("/**\n * This is a generate file.\n * ")
            .append(DateFormat.getDateInstance(DateFormat.FULL).format(new Date()))
            .append("\n * A rest http local proxy for ")
            .append(serviceName)
            .append(".\n * Powered by com.yinxiang.microservice.grpc.controller.\n */\n")
            .append("@RestController\npublic class ")
            .append(className)
            .append(" extends com.yinxiang.microservice.grpc.controller.BaseGrpcController {\n")
            .append("\tprivate final ")
            .append(grpcName)
            .append(" grpc;\n\n\tpublic ")
            .append(className)
            .append("(")
            .append(grpcName)
            .append(" grpc) {\n\t\tthis.grpc = grpc;\n\t}\n").toString();
  }

  @Override
  protected String buildMethod(MethodInfo methodInfo) {
    ServiceInfo serviceInfo = methodInfo.getServiceInfo();
    StringBuilder builder = new StringBuilder();
    // append annotation
    builder.append("\n\t@RequestMapping(value = \"")
            .append(methodInfo.getPath())
            .append("\", method = RequestMethod.")
            .append(methodInfo.getMethod())
            .append(")\n");
    String methodName = methodInfo.getName();
    // append method body
    builder.append("\tpublic String ").append(methodName)
            .append("(@RequestBody String params, @RequestHeader MultiValueMap<String, String> headers)")
            .append(" throws JsonFormat.ParseException {\n")
            .append("\t\treturn _process(params, headers, ")
            .append(serviceInfo.getJavaPackage())
            .append(".")
            .append(methodInfo.getInput().getName())
            .append(".newBuilder(), grpc::")
            .append(StringUtils.firstLower(methodName))
            .append(", ")
            .append(serviceInfo.getName())
            .append("Grpc.get")
            .append(StringUtils.firstUpper(methodName))
            .append("Method());\n\t}\n");
    return builder.toString();
  }

  @Override
  protected String buildEnd(ServiceInfo serviceInfo) {
    return "}";
  }
}
