package com.yinxiang.microservice.grpc.generator.processors;

import com.google.protobuf.DescriptorProtos.*;
import com.google.protobuf.Descriptors.*;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.MethodDoc;
import com.yinxiang.microservice.grpc.generator.infos.EnumInfo;
import com.yinxiang.microservice.grpc.generator.infos.FieldInfo;
import com.yinxiang.microservice.grpc.generator.infos.MessageInfo;

import java.util.Optional;

/**
 * Processing Message struct.
 * @author Huiyuan Fu
 */
public final class MessageProcessor extends BaseDescriptorProcessor<Descriptor, MessageOptions, MessageInfo> {
  /** Marshaller from EnumDescriptor to EnumInfo. */
  private final GenericDescriptorProcessor<EnumDescriptor, EnumInfo> enumMarshaller;
  /** Marshaller from FieldDescriptor to FieldInfo. */
  private final GenericDescriptorProcessor<FieldDescriptor, FieldInfo> fieldMarshaller;

  public MessageProcessor(
          GenericDescriptorProcessor<EnumDescriptor, EnumInfo> enumMarshaller,
          GenericDescriptorProcessor<FieldDescriptor, FieldInfo> fieldMarshaller,
          String sourcePath
  ) {
    super(sourcePath);
    this.enumMarshaller = enumMarshaller;
    this.fieldMarshaller = fieldMarshaller;
  }

  @Override
  protected void addProperties(Descriptor descriptor, MessageInfo messageInfo) {
    Optional<ClassDoc> classDoc = getClassDoc(descriptor);
    Doc[] docs = classDoc.map(ClassDoc::methods).orElse(new MethodDoc[]{});
    messageInfo.setEnums(toList(descriptor.getEnumTypes(), enumMarshaller, docs));
    messageInfo.setFields(toList(descriptor.getFields(), fieldMarshaller, docs));
    messageInfo.setExtensions(toList(descriptor.getExtensions(), fieldMarshaller, docs));
    classDoc.ifPresent(messageInfo::setCommentByDoc);
  }
}
