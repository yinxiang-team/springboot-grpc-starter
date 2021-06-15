package com.yinxiang.microservice.grpc.generator.processors;

import com.google.protobuf.DescriptorProtos.MessageOptions;
import com.google.protobuf.Descriptors.Descriptor;
import com.yinxiang.microservice.grpc.generator.infos.MessageInfo;

/**
 * Processing Message struct and do not have any other properties.
 * @author Huiyuan Fu
 */
public class SimpleMessageProcessor extends BaseDescriptorProcessor<Descriptor, MessageOptions, MessageInfo> {
  @Override
  protected void addProperties(Descriptor descriptor, MessageInfo messageInfo) {}
}
