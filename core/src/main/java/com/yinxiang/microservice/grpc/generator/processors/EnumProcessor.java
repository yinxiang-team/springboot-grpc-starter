package com.yinxiang.microservice.grpc.generator.processors;

import com.google.protobuf.DescriptorProtos.EnumOptions;
import com.google.protobuf.Descriptors.*;
import com.yinxiang.grpc.http.Comment;
import com.yinxiang.microservice.grpc.generator.infos.EnumInfo;
import com.yinxiang.microservice.grpc.generator.infos.EnumValue;

/**
 * Processing Enum element of a message.
 * @author Huiyuan Fu
 */
public final class EnumProcessor extends BaseDescriptorProcessor<EnumDescriptor, EnumOptions, EnumInfo> {
  /** Hide name of Enum. */
  private static final String PROTO_HIDE = Comment.hideEnumComment.getDescriptor().getFullName();

  private final GenericDescriptorProcessor<EnumValueDescriptor, EnumValue> enumValueMarshaller;

  public EnumProcessor(GenericDescriptorProcessor<EnumValueDescriptor, EnumValue> enumValueMarshaller, String sourcePath) {
    super(sourcePath);
    this.enumValueMarshaller = enumValueMarshaller;
  }

  @Override
  protected void addProperties(EnumDescriptor descriptor, EnumInfo enumInfo) {
    getClassDoc(descriptor).ifPresent(classDoc -> {
      enumInfo.setValues(toList(descriptor.getValues(), enumValueMarshaller, classDoc.fields()));
      enumInfo.setCommentByDoc(classDoc);
    });
  }

  @Override
  protected String getHideName() {
    return PROTO_HIDE;
  }
}
