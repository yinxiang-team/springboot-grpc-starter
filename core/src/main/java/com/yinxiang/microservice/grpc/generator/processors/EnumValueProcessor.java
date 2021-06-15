package com.yinxiang.microservice.grpc.generator.processors;

import com.google.protobuf.DescriptorProtos.EnumValueOptions;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.yinxiang.grpc.http.Comment;
import com.yinxiang.microservice.grpc.generator.infos.EnumValue;

/**
 * Processing EnumValue element of a enum.
 * @author Huiyuan Fu
 */
public final class EnumValueProcessor extends BaseDescriptorProcessor<EnumValueDescriptor, EnumValueOptions, EnumValue>{
  @Override
  protected void addProperties(EnumValueDescriptor descriptor, EnumValue enumValue) {}
}
