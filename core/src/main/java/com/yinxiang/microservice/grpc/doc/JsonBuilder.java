package com.yinxiang.microservice.grpc.doc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.yinxiang.microservice.grpc.generator.BaseContentBuilder;
import com.yinxiang.microservice.grpc.generator.infos.MessageInfo;
import com.yinxiang.microservice.grpc.generator.infos.ServiceInfo;

import java.util.function.Function;

/**
 * The doc json file builder.
 * @author Huiyuan Fu
 */
final class JsonBuilder implements Function<ServiceInfo, String> {
  @Override
  public String apply(ServiceInfo serviceInfo) {
    serviceInfo.setFileName(serviceInfo.getName());
    JsonArray array = new JsonArray();
    String url = serviceInfo.getUrl();
    serviceInfo.getMethods().stream().map(methodInfo -> {
      JsonObject method = new JsonObject();
      method.addProperty("name", methodInfo.getComment() + "[" + methodInfo.getName() + "]");
      method.addProperty("path", url + methodInfo.getPath());
      method.addProperty("method", methodInfo.getMethod());
      JsonArray headers = new JsonArray();
      methodInfo.getHeaders().forEach(headers::add);
      method.add("header", headers);
      method.addProperty("body", toJsonString(methodInfo.getInput()));
      method.addProperty("response", toJsonString(methodInfo.getOutput()));
      return method;
    }).forEach(array::add);
    return array.toString();
  }

  private String toJsonString(MessageInfo messageInfo) {
    StringBuilder builder = new StringBuilder();
    BaseContentBuilder.appendMessage(builder, messageInfo, 1);
    return builder.toString();
  }
}
