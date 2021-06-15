package com.yinxiang.microservice.grpc.inject.header;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessageV3.ExtendableMessage;
import com.google.protobuf.Message;
import com.yinxiang.grpc.http.Comment;
import com.yinxiang.microservice.grpc.util.GrpcUtils;
import io.grpc.*;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.protobuf.ProtoMethodDescriptorSupplier;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import static com.yinxiang.microservice.grpc.inject.header.StubHeadersProcessor.PROTO_HEADERS;
import static com.yinxiang.microservice.grpc.util.GrpcUtils.findFirstOptionValue;
import static io.grpc.Status.Code.INVALID_ARGUMENT;

/**
 * An abstract headers interceptor of gRPC server.
 * <p>
 *   Support a new rule for process gRPC {@link Metadata}, add a filter option to rpc of service in proto, and in
 *   sub class declare the F(filter) and T(headers). This interceptor will filter headers which need use. In message
 *   processing the {@link HeaderServerCallListener} will fill reference header's fields.
 * </p>
 * @param <T> headers's type
 * @param <F> headers filter's type
 */
public abstract class AbstractHeadersInterceptor<T extends ExtendableMessage<T>, F extends ExtendableMessage<F>>
        implements ServerInterceptor {
  protected static final Logger logger = LoggerFactory.getLogger(AbstractHeadersInterceptor.class);
  /** Reference header's option name. */
  private static final String FIELD_REFERENCE_HEADER = Comment.referenceHeader.getDescriptor().getFullName();

  /** Class of headers filter. */
  private final Class<F> headersFilterClass;
  /** Class of headers. */
  private final Class<T> headersClass;
  /** {@link Descriptor} of headers. */
  private final Descriptor headersDescriptor;
  /** Cache a {@link MethodDescriptor}'s filter. */
  private ConcurrentMap<io.grpc.MethodDescriptor, Optional<F>> filters = Maps.newConcurrentMap();
  /** Cache a {@link Message}'s reference fields. */
  private ConcurrentMap<Class<? extends Message>, Map<FieldDescriptor, String>> references = Maps.newConcurrentMap();

  @SuppressWarnings("unchecked")
  protected AbstractHeadersInterceptor() throws Exception {
    Class<?> superClass = getClass();
    while (!superClass.getSuperclass().equals(AbstractHeadersInterceptor.class)) {
      superClass = superClass.getSuperclass();
    }
    Type[] actualTypes = ((ParameterizedType) superClass.getGenericSuperclass()).getActualTypeArguments();
    headersClass = (Class<T>) actualTypes[0];
    headersFilterClass = (Class<F>) actualTypes[1];
    headersDescriptor = ((Descriptor) headersClass.getMethod("getDescriptor").invoke(null));
  }

  /**
   * Getter of headers filter's name.
   * @return  headers filter's name
   */
  protected String getHeadersFilterName() {
    return PROTO_HEADERS;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
          ServerCall<ReqT, RespT> call, Metadata headers,
          ServerCallHandler<ReqT, RespT> next) {
    // get the current Context
    Context context = Context.current();
    T header = null;
    try {
      // get the Optional of F(filter) and first time will get from SchemaDescriptor
      Optional<F> filter = filters.computeIfAbsent(call.getMethodDescriptor(), this::getFilter);
      // process filter headers
      if (filter.isPresent()) {
        Pair<Context, T> pair = filterHeaders(context, headers, filter.get());
        context = pair.getKey();
        header = pair.getRight();
      }
    } catch (IllegalArgumentException e) {
      logger.info("process headers IllegalArgumentException: " + e.getMessage(), e);
      throw new StatusRuntimeException(Status.fromCode(INVALID_ARGUMENT).withDescription(e.getMessage()), headers);
    } catch (Exception e) {
      logger.info("process headers Exception: ", e);
    }
    // create a special ServerCall.Listener for headers
    return createListener(Contexts.interceptCall(context, call, headers, next), header);
  }

  /**
   * Get an optional of F(filter) from SchemaDescriptor.
   * @param method  {@link MethodDescriptor} of a call
   * @return  an optional of F(filter)
   */
  @SuppressWarnings("unchecked")
  private Optional<F> getFilter(io.grpc.MethodDescriptor method) {
    // get the SchemaDescriptor
    Object schemaDescriptor = method.getSchemaDescriptor();
    // SchemaDescriptor is a ProtoMethodDescriptorSupplier
    if (schemaDescriptor instanceof ProtoMethodDescriptorSupplier) {
      // get the MethodDescriptor of schemaDescriptor
      MethodDescriptor methodDescriptor = ((ProtoMethodDescriptorSupplier) schemaDescriptor)
              .getMethodDescriptor();
      // get the first value of options which name same with PROTO_HEADERS
      return findFirstOptionValue(methodDescriptor.getOptions(), getHeadersFilterName());
    }
    // schemaDescriptor is a F(filter)
    else if (schemaDescriptor != null && headersFilterClass.equals(schemaDescriptor.getClass())) {
      return Optional.of((F) schemaDescriptor);
    }
    // Un know type
    return Optional.empty();
  }

  /**
   * Create a special {@link ServerCall.Listener} for headers.
   * @param listener  the current listener
   * @param headers    {@link T}
   * @param <ReqT>    request type
   * @return  a special {@link ServerCall.Listener}
   */
  protected <ReqT> ServerCall.Listener<ReqT> createListener(ServerCall.Listener<ReqT> listener, T headers) {
    return new HeaderServerCallListener<>(listener, headers);
  }

  /**
   * Filter all headers from {@link Metadata}.
   * @param contexts  {@link Context}
   * @param metadata  {@link Metadata}
   * @param headersFilter {@link F}
   * @return  result of {@link Context} and {@link T}
   */
  protected abstract Pair<Context, T> filterHeaders(Context contexts, Metadata metadata, F headersFilter);

  /**
   * Get a header from {@link Metadata}.
   * @param metadata  {@link Metadata}.
   * @param key       key of header
   * @return  value of header
   */
  protected String getHeader(Metadata metadata, String key) {
    return metadata.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
  }

  /**
   * Get a header from {@link Metadata}.
   * @param metadata    {@link Metadata}.
   * @param defaultKey  default key of header
   * @param keys        key's aliases of header
   * @return  value of header
   */
  protected String getHeader(Metadata metadata, String defaultKey, Collection<String> keys) {
    String value = getHeader(metadata, defaultKey);
    if (!Strings.isNullOrEmpty(value)) {
      return value;
    }
    for (String key : keys) {
      value = getHeader(metadata, key);
      if (!Strings.isNullOrEmpty(value)) {
        return value;
      }
    }
    return "";
  }

  /**
   * Get a map of {@link FieldDescriptor} to reference({@link String}), if not exists get from a {@link Message}.
   * @param message {@link Message}
   * @return  a map of {@link FieldDescriptor} to reference({@link String})
   */
  private Map<FieldDescriptor, String> getReferenceFields(Message message) {
    return references.computeIfAbsent(message.getClass(), c -> {
      try {
        // try to get Descriptor of message
        Descriptor descriptor = (Descriptor) message.toBuilder()
                .getClass()
                .getMethod("getDescriptor")
                .invoke(null);
        // find all references
        Map<FieldDescriptor, String> ret = Maps.newHashMap();
        descriptor.getFields().forEach(field ->
                GrpcUtils.<String, FieldOptions>findFirstOptionValue(field.getOptions(), FIELD_REFERENCE_HEADER)
                        .ifPresent(reference -> ret.put(field, reference)));
        return ret;
      } catch (NoSuchMethodException e) {
        return ImmutableMap.of();
      } catch (IllegalAccessException | InvocationTargetException e) {
        return null;
      }
    });
  }

  /**
   * A special {@link ServerCall.Listener} for headers.
   * @param <Q> message type
   */
  private class HeaderServerCallListener<Q> extends SimpleForwardingServerCallListener<Q> {
    /** {@link T} */
    private final T headers;

    private HeaderServerCallListener(ServerCall.Listener<Q> delegate, T headers) {
      super(delegate);
      this.headers = headers;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onMessage(Q requestMessage) {
      try {
        if (requestMessage instanceof Message) {
          // merge if same type
          if (headersClass.equals(requestMessage.getClass())) {
            requestMessage = (Q) ((T) requestMessage).toBuilder().mergeFrom(headers).build();
          } else {
            // get all reference fields
            Map<FieldDescriptor, String> fields = getReferenceFields(((Message) requestMessage));
            // has reference fields
            if (fields.size() > 0) {
              for (Map.Entry<FieldDescriptor, String> entry : fields.entrySet()) {
                FieldDescriptor field = entry.getKey();
                String reference = entry.getValue();
                // reference a T's field
                if (reference.length() > 0) {
                  // get reference field value and set into the message
                  FieldDescriptor referenceField = headersDescriptor.findFieldByName(reference);
                  if (referenceField != null && referenceField.getJavaType() == field.getJavaType()) {
                    Object value = headers.getAllFields().get(referenceField);
                    requestMessage = (Q) ((Message) requestMessage).toBuilder().setField(field, value).build();
                  }
                }
                // reference T
                else {
                  requestMessage = (Q) ((Message) requestMessage).toBuilder().setField(field, headers).build();
                }
              }
            }
          }
        }
      } catch (Exception e) {
        logger.debug("HeaderServerCallListener.onMessage {}: {}", e.getClass(), e.getMessage());
      }
      super.onMessage(requestMessage);
    }
  }
}