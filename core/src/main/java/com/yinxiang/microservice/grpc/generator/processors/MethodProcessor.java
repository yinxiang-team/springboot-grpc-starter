package com.yinxiang.microservice.grpc.generator.processors;

import com.google.api.HttpRule;
import com.google.common.base.Strings;
import com.google.protobuf.DescriptorProtos.MethodOptions;
import com.google.protobuf.Descriptors.*;
import com.yinxiang.grpc.http.HeadersFilter;
import com.yinxiang.grpc.http.Http;
import com.yinxiang.microservice.grpc.generator.infos.MessageInfo;
import com.yinxiang.microservice.grpc.generator.infos.MethodInfo;
import com.yinxiang.microservice.grpc.util.GrpcUtils;

import java.util.function.Function;
import java.util.stream.Collectors;

import static com.yinxiang.microservice.grpc.inject.header.StubHeadersProcessor.PROTO_HEADERS;

/**
 * Processing a RPC of a service.
 * @author Huiyuan Fu
 */
public final class MethodProcessor extends BaseDescriptorProcessor<MethodDescriptor, MethodOptions, MethodInfo> {
  /** Hide name of RPC. */
  private static final String PROTO_HIDE = Http.hideApiComment.getDescriptor().getFullName();
  /** http name of RPC. */
  private static final String PROTO_HTTP = "google.api.http";

  /** Marshaller from Descriptor to MessageInfo. */
  private final Function<Descriptor, MessageInfo> messageMarshaller;

  public MethodProcessor(Function<Descriptor, MessageInfo> messageMarshaller) {
    this.messageMarshaller = messageMarshaller;
  }

  @Override
  public MethodInfo apply(MethodDescriptor descriptor) {
    return GrpcUtils.findFirstOptionValue(getOptions(descriptor), PROTO_HTTP,
            httpRule -> fill((HttpRule) httpRule, descriptor, super.apply(descriptor)));
  }

  /**
   * Append extend properties to MethodInfo.
   * @param httpRule    HttpRule
   * @param descriptor  MethodDescriptor
   * @param methodInfo  MethodInfo
   */
  protected MethodInfo fill(HttpRule httpRule, MethodDescriptor descriptor, MethodInfo methodInfo) {
    methodInfo.setPath(getPath(httpRule));
    methodInfo.setMethod(httpRule.getPatternCase().toString());
    GrpcUtils.<HeadersFilter, MethodOptions>executeFirstOptionValue(
            descriptor.getOptions(), PROTO_HEADERS, headerFilter ->
                    methodInfo.setHeaders(headerFilter.getDescriptorForType()
                            .getFields()
                            .stream()
                            .filter(field -> ((Boolean) headerFilter.getField(field)))
                            .map(FieldDescriptor::getName)
                            .collect(Collectors.toList()))
            );
    methodInfo.setInput(getMessageInfo(descriptor.getInputType(), httpRule.getBody()));
    methodInfo.setOutput(getMessageInfo(descriptor.getOutputType(), httpRule.getResponseBody()));
    return methodInfo;
  }

  /**
   * Get a message to MessageInfo.
   * @param descriptor  Descriptor
   * @param name        message name
   * @return  MessageInfo of a struct
   */
  private MessageInfo getMessageInfo(Descriptor descriptor, String name) {
    return messageMarshaller.apply(getMessage(descriptor, name));
  }

  @Override
  protected void addProperties(MethodDescriptor descriptor, MethodInfo methodInfo) {}

  @Override
  protected String getHideName() {
    return PROTO_HIDE;
  }

  /**
   * Get a message from descriptor.
   * @param descriptor  Descriptor
   * @param name        message name
   * @return  Descriptor of a message
   */
  private static Descriptor getMessage(Descriptor descriptor, String name) {
    if (Strings.isNullOrEmpty(name) || "*".equals(name)) {
      return descriptor;
    }
    FieldDescriptor fieldDescriptor = descriptor.findFieldByName(name);
    return fieldDescriptor == null ? descriptor : fieldDescriptor.getMessageType();
  }

  /**
   * Get path from httpRule.
   * @param httpRule  HttpRule
   * @return  path
   * @see HttpRule
   */
  private static String getPath(HttpRule httpRule) {
    switch (httpRule.getPatternCase()) {
      case GET:
        return httpRule.getGet();
      case POST:
        return httpRule.getPost();
      case PUT:
        return httpRule.getPut();
      case DELETE:
        return httpRule.getDelete();
      case PATCH:
        return httpRule.getPatch();
      case CUSTOM:
        return httpRule.getCustom().getPath();
      default:
        return "";
    }
  }
}
