package com.yinxiang.microservice.grpc.generator;

import com.google.protobuf.Descriptors;
import com.yinxiang.microservice.grpc.generator.infos.*;

import java.util.List;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A basic content builder.
 * @author Huiyuan Fu
 */
public abstract class BaseContentBuilder implements Function<ServiceInfo, String> {
  @Override
  public String apply(ServiceInfo serviceInfo) {
    StringBuilder builder = new StringBuilder(buildBegin(serviceInfo));
    serviceInfo.getMethods().stream().map(this::buildMethod).forEach(builder::append);
    return builder.append(buildEnd(serviceInfo)).toString();
  }

  /**
   * Build the begin of content.
   * @param serviceInfo ServiceInfo
   * @return  the begin of content
   */
  protected abstract String buildBegin(ServiceInfo serviceInfo);

  /**
   * Build method content.
   * @param methodInfo MethodInfo
   * @return  the end of content
   */
  protected abstract String buildMethod(MethodInfo methodInfo);

  /**
   * Build the end of content.
   * @param serviceInfo ServiceInfo
   * @return  the end of content
   */
  protected abstract String buildEnd(ServiceInfo serviceInfo);

  /**
   * Append a MessageInfo to string builder.
   * @param builder     a StringBuilder
   * @param messageInfo MessageInfo
   * @param tab         tab count
   */
  public static void appendMessage(StringBuilder builder, MessageInfo messageInfo, int tab) {
    appendTab(builder, tab - 1).append("{").append("\n");
    int length = builder.length();
    messageInfo.getEnums().forEach(element -> appendEnum(builder, element, tab).append("\n"));
    messageInfo.getFields().forEach(element -> appendField(builder, element, tab).append("\n"));
    messageInfo.getExtensions().forEach(element -> appendField(builder, element, tab).append("\n"));
    if (length < builder.length()) {
      builder.setLength(builder.length() - 2);
    }
    builder.append("\n");
    appendTab(builder, tab - 1).append("}");
  }

  /**
   * Append a EnumInfo to string builder.
   * @param builder   a StringBuilder
   * @param enumInfo  EnumInfo
   * @param tab       tab count
   * @return  the StringBuilder parameter
   */
  private static StringBuilder appendEnum(StringBuilder builder, EnumInfo enumInfo, int tab) {
    appendTab(builder.append("\n"), tab).append("// ").append(enumInfo.getName());
    enumInfo.getValues().forEach(value -> appendEnumValue(builder, value, tab));
    return builder;
  }

  /**
   * Append a EnumValue to string builder.
   * @param builder   a StringBuilder
   * @param value     EnumValue
   * @param tab       tab count
   */
  private static void appendEnumValue(StringBuilder builder, EnumValue value, int tab) {
    appendTab(builder.append("\n"), tab).append("// ").append(value.getName()).append(": ").append(value.getComment());
  }

  /**
   * Append a FieldInfo to string builder.
   * @param builder   a StringBuilder
   * @param fieldInfo FieldInfo
   * @param tab       tab count
   * @return  the StringBuilder parameter
   */
  private static StringBuilder appendField(StringBuilder builder, FieldInfo fieldInfo, int tab) {
    appendTab(builder, tab).append("// (")
            .append(fieldInfo.getJavaType())
            .append(fieldInfo.isArray() ? " [数组]" : "")
            .append(") ")
            .append(getFieldComment(fieldInfo, tab))
            .append("\n");
    return appendTab(builder, tab)
            .append("\"")
            .append(fieldInfo.getName())
            .append("\": ")
            .append(getExample(fieldInfo, tab))
            .append(",\n");
  }

  /**
   * Get comment of a field.
   * @param fieldInfo FieldInfo
   * @param tab       tab count
   * @return  comment of a field
   */
  private static String getFieldComment(FieldInfo fieldInfo, int tab) {
    StringBuilder builder = new StringBuilder();
    if (fieldInfo.getJavaType() == Descriptors.FieldDescriptor.JavaType.ENUM) {
      appendEnum(builder, fieldInfo.getLinked(), tab);
//      builder.setLength(builder.length() - 2);
    } else {
      builder.append(fieldInfo.getComment());
      BaseInfo linked = fieldInfo.getLinked();
      if (linked != null) {
        if (linked instanceof EnumInfo) {
          appendEnum(builder, fieldInfo.getLinked(), tab);
        } else if (linked instanceof FieldInfo) {
          appendField(builder, fieldInfo.getLinked(), tab);
        }
      }
    }
    return builder.toString();
  }

  /**
   * Append some tabs to string builder.
   * @param builder a StringBuilder
   * @param tab     tab count
   * @return  the StringBuilder parameter
   */
  private static StringBuilder appendTab(StringBuilder builder, int tab) {
    for (int i = 0;i < tab;i++) {
      builder.append("\t");
    }
    return builder;
  }

  /**
   * Get a example of the fieldInfo.
   * @param fieldInfo FieldInfo
   * @param tab       tab count
   * @return  a example of the fieldInfo
   */
  private static String getExample(FieldInfo fieldInfo, int tab) {
    StringBuilder builder = new StringBuilder();
    boolean isArray = fieldInfo.isArray();
    if (isArray) {
      builder.append("[");
    }
    switch (fieldInfo.getJavaType()) {
      case INT:
        builder.append(1);
        break;
      case DOUBLE: case FLOAT:
        builder.append(1.0);
        break;
      case BOOLEAN:
        builder.append("true");
        break;
      case MESSAGE:
        appendMessage(builder.append("\n"), checkNotNull(fieldInfo.getLinked(), fieldInfo.getName()), tab + 1);
        break;
      case ENUM:
        List<EnumValue> enumValueList = ((EnumInfo) fieldInfo.getLinked()).getValues();
        int count = enumValueList.size();
        builder.append("\"").append(count == 0 ? "" : enumValueList.get(count > 1 ? 1 : 0).getName()).append("\"");
        break;
      default:
        builder.append("\"\"");
    }
    if (isArray) {
      builder.append(", ]");
    }
    return builder.toString();
  }
}
