package com.yinxiang.microservice.grpc.generator.infos;

import com.google.protobuf.Descriptors;
import com.sun.javadoc.Doc;

/**
 * The FieldDescriptor attributes.
 * @see com.google.protobuf.Descriptors.FieldDescriptor
 * @author Huiyuan Fu
 */
public class FieldInfo extends BaseInfo {
  /** Type in java. */
  private Descriptors.FieldDescriptor.JavaType javaType;
  /** Value reference. */
  private BaseInfo linked;
  /** Repeated and not a map. */
  private boolean isArray;

  /** @see #javaType */
  public Descriptors.FieldDescriptor.JavaType getJavaType() {
    return javaType;
  }

  /** @see #javaType */
  public void setJavaType(Descriptors.FieldDescriptor.JavaType javaType) {
    this.javaType = javaType;
  }

  /** @see #linked */
  @SuppressWarnings("unchecked")
  public <T extends BaseInfo> T getLinked() {
    return (T) linked;
  }

  /** @see #linked */
  public void setLinked(BaseInfo linked) {
    this.linked = linked;
  }

  /** @see #isArray */
  public boolean isArray() {
    return isArray;
  }

  /** @see #isArray */
  public void setArray(boolean array) {
    isArray = array;
  }

  @Override
  public void setCommentByDoc(Doc... docs) {
    for (Doc doc : docs) {
      if (("get" + getName()).equalsIgnoreCase(doc.name())) {
        setComment(doc.commentText());
        break;
      }
    }
  }
}
