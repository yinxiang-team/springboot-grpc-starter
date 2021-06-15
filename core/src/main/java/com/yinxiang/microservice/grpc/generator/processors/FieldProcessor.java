package com.yinxiang.microservice.grpc.generator.processors;

import com.google.common.base.Strings;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.Descriptors.*;
import com.yinxiang.grpc.http.Comment;
import com.yinxiang.microservice.grpc.generator.infos.BaseInfo;
import com.yinxiang.microservice.grpc.generator.infos.EnumInfo;
import com.yinxiang.microservice.grpc.generator.infos.FieldInfo;
import com.yinxiang.microservice.grpc.generator.infos.MessageInfo;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE;
import static com.yinxiang.microservice.grpc.util.GrpcUtils.anyMatchOption;

/**
 * Processing Field element of a message.
 * @author Huiyuan Fu
 */
public final class FieldProcessor extends BaseDescriptorProcessor<FieldDescriptor, FieldOptions, FieldInfo> {
  /** Hide name of Field. */
  private static final String PROTO_HIDE = Comment.hideFieldComment.getDescriptor().getFullName();
  /** Linked name of Field. */
  private static final String PROTO_LINKED = Comment.linked.getDescriptor().getFullName();
  /** Reference header name of Field. */
  private static final String PROTO_HEADERS = Comment.referenceHeader.getDescriptor().getFullName();

  /** Marshaller from EnumDescriptor to EnumInfo. */
  private final Function<EnumDescriptor, EnumInfo> enumMarshaller;
  /** Marshaller from Descriptor to MessageInfo. */
  private Function<Descriptor, MessageInfo> messageMarshaller;

  public FieldProcessor(Function<EnumDescriptor, EnumInfo> enumMarshaller, String sourcePath) {
    super(sourcePath);
    this.enumMarshaller = enumMarshaller;
  }

  @Override
  protected void addProperties(FieldDescriptor descriptor, FieldInfo fieldInfo) {
    fieldInfo.setJavaType(descriptor.getJavaType());
    fieldInfo.setArray(!descriptor.isMapField() && descriptor.isRepeated());
    fieldInfo.setLinked(getFieldLinkedComment(descriptor));
  }

  @Override
  protected String getHideName() {
    return PROTO_HIDE;
  }

  /**
   * Find linked struct and transform to a info,
   * which extends BaseInfo correspond to the type which extends GenericDescriptor.
   * @param linked          the full name of struct
   * @param fileDescriptor  the linked file
   * @return  the info which extends BaseInfo correspond to the type which extends GenericDescriptor
   */
  private BaseInfo findLinked(String linked, FileDescriptor fileDescriptor) {
    int lastDotIndex = linked.lastIndexOf(".");
    if (lastDotIndex < 0) {
      // same file
      return find(linked, fileDescriptor);
    } else {
      String name = linked.substring(lastDotIndex + 1);
      String protoPackage = linked.substring(0, lastDotIndex);
      if (fileDescriptor.getPackage().equals(protoPackage)) {
        // same file
        return find(name, fileDescriptor);
      } else {
        for (FileDescriptor dependency : fileDescriptor.getDependencies()) {
          if (dependency.getPackage().equals(protoPackage)) {
            BaseInfo content = find(name, fileDescriptor);
            if (content != null) {
              return content;
            }
          }
        }
        return null;
      }
    }
  }

  /**
   * Find linked struct and transform to a info,
   * which extends BaseInfo correspond to the type which extends GenericDescriptor.
   * @param name            the name of struct
   * @param fileDescriptor  the linked file
   * @return  the info which extends BaseInfo correspond to the type which extends GenericDescriptor
   */
  private BaseInfo find(String name, FileDescriptor fileDescriptor) {
    BaseInfo content = findLinked(name, fileDescriptor.getExtensions(), this);
    if (content == null) {
      content = findLinked(name, fileDescriptor.getMessageTypes(), messageMarshaller);
    }
    return content == null ? findLinked(name, fileDescriptor.getEnumTypes(), enumMarshaller) : content;
  }

  /**
   * Find linked struct and transform to a info,
   * which extends BaseInfo correspond to the type which extends GenericDescriptor.
   * @param name        the name of struct
   * @param descriptors the list of descriptor which load from linked file
   * @param consumer    linked struct consumer
   * @param <T> the type which extends GenericDescriptor
   * @param <R> the type which extends BaseInfo which correspond to the type which extends GenericDescriptor
   * @return  the info which extends BaseInfo correspond to the type which extends GenericDescriptor
   * @see GenericDescriptor
   * @see BaseInfo
   * @see GenericDescriptorProcessor
   */
  private static <T extends GenericDescriptor, R extends BaseInfo> BaseInfo findLinked(
          String name,
          List<T> descriptors,
          Function<T, R> consumer
  ) {
    return descriptors.stream()
            .filter(descriptor -> descriptor.getName().equals(name))
            .findFirst()
            .map(consumer)
            .orElse(null);
  }

  /**
   * Get the comment of linked message from descriptor.
   * @param descriptor  descriptor
   * @return  the comment of linked message
   */
  private BaseInfo getFieldLinkedComment(FieldDescriptor descriptor) {
    if (descriptor.getJavaType() == FieldDescriptor.JavaType.ENUM) {
      return enumMarshaller.apply(descriptor.getEnumType());
    } if (descriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
      return messageMarshaller.apply(descriptor.getMessageType());
    } else {
      String linked = getComment(descriptor.getOptions(), PROTO_LINKED);
      if (!Strings.isNullOrEmpty(linked)) {
        return findLinked(linked, descriptor.getFile());
      }
    }
    return null;
  }

  /**
   * Setter of messageMarshaller
   * @param messageMarshaller messageMarshaller
   * @see #messageMarshaller
   */
  public void setMessageMarshaller(Function<Descriptor, MessageInfo> messageMarshaller) {
    this.messageMarshaller = messageMarshaller;
  }

  @Override
  public boolean test(FieldDescriptor descriptor) {
    return super.test(descriptor) &&
            (descriptor.getJavaType() != MESSAGE || !anyMatchOption(getOptions(descriptor), this::isReferenceHeader));
  }

  /**
   * Check options's entry is reference header.
   * @param entry options's entry
   * @return  is reference header
   */
  private boolean isReferenceHeader(Map.Entry<FieldDescriptor, Object> entry) {
    return entry.getKey().getFullName().equals(PROTO_HEADERS);
  }
}
