package com.yinxiang.microservice.grpc.generator.infos;

import java.util.List;

/**
 * The EnumDescriptor attributes.
 * @see com.google.protobuf.Descriptors.EnumDescriptor
 * @author Huiyuan Fu
 */
public class EnumInfo extends BaseInfo {
  /** List of all enum. */
  private List<EnumValue> values;

  /** @see #values */
  public List<EnumValue> getValues() {
    return values;
  }

  /** @see #values */
  public void setValues(List<EnumValue> values) {
    this.values = values;
  }
}
