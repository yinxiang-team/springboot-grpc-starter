package com.yinxiang.microservice.grpc.doc;

import com.yinxiang.microservice.grpc.generator.BaseContentBuilder;
import com.yinxiang.microservice.grpc.generator.infos.MethodInfo;
import com.yinxiang.microservice.grpc.generator.infos.ServiceInfo;

/**
 * The doc text file builder.
 * @author Huiyuan Fu
 */
final class TextBuilder extends BaseContentBuilder {
  @Override
  protected String buildBegin(ServiceInfo serviceInfo) {
    serviceInfo.setFileName(serviceInfo.getName());
    return serviceInfo.getName() + " gRPC-API\n";
  }

  @Override
  protected String buildMethod(MethodInfo methodInfo) {
    StringBuilder builder = new StringBuilder();
    builder.append("\n").append(methodInfo.getComment());
    builder.append("\nPATH: ").append(methodInfo.getServiceInfo().getUrl()).append(methodInfo.getPath());
    builder.append("\nMETHOD: ").append(methodInfo.getMethod()).append("\n").append("HEADER: ");
    methodInfo.getHeaders().forEach(header -> builder.append(header).append(" | "));
    builder.append("\nBODY: \n");
    appendMessage(builder, methodInfo.getInput(), 1);
    builder.append("\nRESPONSE: \n");
    appendMessage(builder, methodInfo.getOutput(), 1);
    return builder.append("\n").toString();
  }

  @Override
  protected String buildEnd(ServiceInfo serviceInfo) {
    return "";
  }
}
