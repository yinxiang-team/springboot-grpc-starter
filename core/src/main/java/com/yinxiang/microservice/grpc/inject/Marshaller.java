package com.yinxiang.microservice.grpc.inject;

import com.google.gson.Gson;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A marshaller of {@link Message.Builder} to json string.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public final class Marshaller {
  private static final Logger log = LoggerFactory.getLogger(Marshaller.class);
  /** The json formatter. */
  private static final JsonFormat JSON_FORMAT = new JsonFormat();
  private static final Gson GSON = new Gson();

  private Marshaller() {}

  /**
   * Transform a json string to {@link Message.Builder}.
   * @param builder {@link Message.Builder}
   * @param json    json string
   * @param <T>     the sub type of {@link Message.Builder}
   * @return  {@link Message.Builder}
   * @throws JsonFormat.ParseException  ParseException
   */
  public static <T extends Message.Builder> T fromJson(T builder, String json) throws JsonFormat.ParseException {
    JSON_FORMAT.merge(json, ExtensionRegistry.getEmptyRegistry(), builder);
    return builder;
  }

  /**
   * Transform a json string to {@link Message.Builder}, if has any {@link JsonFormat.ParseException} will return origin.
   * @param builder {@link Message.Builder}
   * @param json    json string
   * @param <T>     the sub type of {@link Message.Builder}
   * @return  {@link Message.Builder}
   */
  public static <T extends Message.Builder> T fromJsonButNot(T builder, String json) {
    try {
      return fromJson(builder, json);
    } catch (JsonFormat.ParseException e) {
      log.warn("Marshaller.fromJsonButNot ParseException: " + e.getMessage(), e);
      return builder;
    }
  }

  /**
   * Transform a object to {@link Message.Builder}.
   * @param builder {@link Message.Builder}
   * @param src     object which need transform
   * @param <T>     the sub type of {@link Message.Builder}
   * @return  {@link Message.Builder}
   */
  public static <T extends Message.Builder> T fromObjectButNot(T builder, Object src) {
    return fromJsonButNot(builder, commonToJson(src));
  }

  /**
   * Transform a {@link Message} to a json string.
   * @param message {@link Message}
   * @param <T>     the sub type of {@link Message.Builder}
   * @return  json string
   */
  public static <T extends Message> String toJson(T message) {
    return JSON_FORMAT.printToString(message);
  }

  /**
   * Transform a object to a json string.
   * @param src     object which need transform
   * @return  json string
   */
  public static String commonToJson(Object src) {
    return GSON.toJson(src);
  }

  /**
   * Transform a {@link Message} to target type T.
   * @param message {@link Message}
   * @param clz     the class of target type
   * @param <T>     target type
   * @return  {@link Message.Builder}
   */
  public static <T> T toObject(Message message, Class<T> clz) {
    return GSON.fromJson(toJson(message), clz);
  }
}
