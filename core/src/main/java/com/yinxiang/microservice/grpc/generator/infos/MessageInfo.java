package com.yinxiang.microservice.grpc.generator.infos;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * The Descriptor attributes.
 * @see com.google.protobuf.Descriptors.Descriptor
 * @author Huiyuan Fu
 */
public class MessageInfo extends BaseInfo {
  /** List of enums. */
  private List<EnumInfo> enums = ImmutableList.of();
  /** List of fields. */
  private List<FieldInfo> fields = ImmutableList.of();
  /** List of extensions fields. */
  private List<FieldInfo> extensions = ImmutableList.of();

  /** @see #enums */
  public List<EnumInfo> getEnums() {
    return enums;
  }

  /** @see #enums */
  public void setEnums(List<EnumInfo> enums) {
    this.enums = enums;
  }

  /** @see #fields */
  public List<FieldInfo> getFields() {
    return fields;
  }

  /** @see #fields */
  public void setFields(List<FieldInfo> fields) {
    this.fields = fields;
  }

  /** @see #extensions */
  public List<FieldInfo> getExtensions() {
    return extensions;
  }

  /** @see #extensions */
  public void setExtensions(List<FieldInfo> extensions) {
    this.extensions = extensions;
  }
}
