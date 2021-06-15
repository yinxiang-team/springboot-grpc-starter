package com.yinxiang.microservice.grpc.generator.processors;

import com.google.protobuf.Descriptors.*;
import com.google.protobuf.GeneratedMessageV3.ExtendableMessage;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.yinxiang.microservice.grpc.generator.infos.BaseInfo;
import com.yinxiang.microservice.grpc.util.GrpcUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A basic DescriptorProcessor can fill name/comment/extends and check if need hide.
 * @param <T> the type which extends GenericDescriptor
 * @param <M> the options type which extends ExtendableMessage
 * @param <R> the type which extends BaseInfo which correspond to the type which extends GenericDescriptor
 * @see GenericDescriptor
 * @see ExtendableMessage
 * @see BaseInfo
 * @see GenericDescriptorProcessor
 * @author Huiyuan Fu
 */
public abstract class BaseDescriptorProcessor
        <T extends GenericDescriptor, M extends ExtendableMessage<M>, R extends BaseInfo>
        implements GenericDescriptorProcessor<T, R> {
  protected static final BiFunction<String, String, Optional<ClassDoc>> JAVA_DOC_COLLECTOR = new JavaDocCollector();
  private final Class<R> infoClass;
  private final String sourcePath;

  protected BaseDescriptorProcessor() {
    this("");
  }

  @SuppressWarnings("unchecked")
  protected BaseDescriptorProcessor(String sourcePath) {
    this.sourcePath = sourcePath;
    Class<?> superClass = getClass();
    while (!superClass.getSuperclass().equals(BaseDescriptorProcessor.class)) {
      superClass = superClass.getSuperclass();
    }
    Type[] actualTypes = ((ParameterizedType) superClass.getGenericSuperclass()).getActualTypeArguments();
    infoClass = (Class<R>) actualTypes[2];
  }

  @Override
  public R apply(T descriptor) {
    R r = createR();
    r.setName(descriptor.getName());
//    r.setComment(getComment(getOptions(descriptor), getCommentName()));
    addProperties(descriptor, r);
    return r;
  }

  /**
   * Create the specific class which extends BaseInfo.
   * @return  the specific class which extends BaseInfo
   * @see BaseInfo
   */
  protected R createR() {
    try {
      return infoClass.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the options of the descriptor.
   * @param descriptor  descriptor
   * @return  options
   */
  @SuppressWarnings("unchecked")
  protected M getOptions(T descriptor) {
    try {
      return (M) descriptor.getClass().getMethod("getOptions").invoke(descriptor);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Append some properties to the the specific class
   * @param descriptor  descriptor
   * @param r           the specific class
   */
  protected abstract void addProperties(T descriptor, R r);

  /**
   * Get the comment.
   * @param message options of descriptor
   * @param name    comment name in options
   * @return  the comment.
   */
  protected String getComment(M message, String name) {
    Optional<String> optional = GrpcUtils.findFirstOptionValue(message, name);
    return optional.orElse("");
  }

  /**
   * Let a type T's collection to a type R's list.
   * @param collection  a type T's collection
   * @param predicate   check the entry is hide name or not.
   * @param mapper      map type T to type R
   * @param <T> the type which extends GenericDescriptor
   * @param <R> the type which extends BaseInfo which correspond to the type which extends GenericDescriptor
   * @return  a type R's list
   * @see GenericDescriptor
   * @see BaseInfo
   * @see GenericDescriptorProcessor
   */
  protected static <T extends GenericDescriptor, R extends BaseInfo> List<R> toList(
          Collection<T> collection,
          Predicate<T> predicate,
          Function<T, R> mapper,
          Doc... docs
  ) {
    List<R> list = collection.stream().filter(predicate).map(mapper).collect(Collectors.toList());
    list.forEach(r -> r.setCommentByDoc(docs));
    return list;
  }

  /**
   * Let a type T's collection to a type R's list.
   * @param collection  a type T's collection
   * @param processor   a GenericDescriptorProcessor
   * @param <T> the type which extends GenericDescriptor
   * @param <R> the type which extends BaseInfo which correspond to the type which extends GenericDescriptor
   * @return  a type R's list
   * @see GenericDescriptor
   * @see BaseInfo
   * @see GenericDescriptorProcessor
   */
  protected static <T extends GenericDescriptor, R extends BaseInfo> List<R> toList(
          Collection<T> collection,
          GenericDescriptorProcessor<T, R> processor,
          Doc... docs
  ) {
    return toList(collection, processor, processor, docs);
  }

  /**
   * Check the entry is hide name or not.
   * @param entry one element of options map
   * @return  the entry is hide name or not
   */
  private boolean isHide(Map.Entry<FieldDescriptor, Object> entry) {
    return getHideName().equals(entry.getKey().getFullName()) && ((Boolean) entry.getValue());
  }

  @Override
  public boolean test(T descriptor) {
    return !GrpcUtils.anyMatchOption(getOptions(descriptor), this::isHide);
  }

  /**
   * Get the name in options if any attributes need hide.
   * @return  the name in options if any attributes need hide
   */
  protected String getHideName() {
    return "";
  }

  protected Optional<ClassDoc> getClassDoc(GenericDescriptor descriptor) {
    return getClassDoc(replaceDot(descriptor.getFile().getOptions().getJavaPackage()), descriptor.getName());
  }

  protected Optional<ClassDoc> getClassDoc(String path, String name) {
    return JAVA_DOC_COLLECTOR.apply(sourcePath + path + "/" + name + ".java", name);
  }

  protected Optional<ClassDoc> getClassDoc(String path, String name, String subName) {
    return JAVA_DOC_COLLECTOR.apply(sourcePath + path + "/" + name + ".java", subName);
  }

  protected static String replaceDot(String path) {
    return path.replaceAll("\\.", "/");
  }
}
