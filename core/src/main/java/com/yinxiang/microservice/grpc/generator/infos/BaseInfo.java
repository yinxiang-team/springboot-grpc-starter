package com.yinxiang.microservice.grpc.generator.infos;

import com.sun.javadoc.Doc;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * gRPC descriptor base attribute.
 * @see com.google.protobuf.Descriptors.Descriptor
 * @see com.google.protobuf.Descriptors.EnumValueDescriptor
 * @see com.google.protobuf.Descriptors.EnumDescriptor
 * @see com.google.protobuf.Descriptors.FieldDescriptor
 * @see com.google.protobuf.Descriptors.MethodDescriptor
 * @see com.google.protobuf.Descriptors.ServiceDescriptor
 * @author Huiyuan Fu
 */
public abstract class BaseInfo {
  /** Name of descriptor. */
  private String name;
  /** Descriptor's comment option. */
  private String comment;

  public void setCommentByDoc(Doc... docs) {
    for (Doc doc : docs) {
      if (getName().equalsIgnoreCase(doc.name())) {
        setComment(doc.commentText());
        break;
      }
    }
  }

  /** @see #name */
  public String getName() {
    return name;
  }

  /** @see #name */
  public void setName(String name) {
    this.name = name;
  }

  /** @see #comment */
  public String getComment() {
    return comment;
  }

  /** @see #comment */
  public void setComment(String comment) {
    comment = "<html><body>" + comment + "</body></html>";
    Document doc = Jsoup.parseBodyFragment(comment);
    Element element = doc.body().getElementsByTag("pre").first();
    this.comment = element == null ? "" : element.text();
  }
}
