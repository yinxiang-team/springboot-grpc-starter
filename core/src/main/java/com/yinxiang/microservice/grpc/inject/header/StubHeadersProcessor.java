package com.yinxiang.microservice.grpc.inject.header;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A processor to process any header of stub.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public final class StubHeadersProcessor {
  private static final Logger log = LoggerFactory.getLogger(StubHeadersProcessor.class);
  /** The register keys, row key is a class of stub, column key is header name. */
  private static final Table<Class, String, Metadata.Key> HEADER_KEYS = HashBasedTable.create();
  @Deprecated
  public static final String PROTO_HEADER = "yinxiang.grpc.http.header";
  /** Headers of proto */
  public static final String PROTO_HEADERS = "yinxiang.grpc.http.headers";

  private StubHeadersProcessor() {}

  /**
   * Register a key.
   * @param clz   a class of stub
   * @param name  header name
   * @param type  type of header value
   */
  public static void registerKey(Class clz, String name, Class type) {
    Metadata.Key key = HEADER_KEYS.get(clz, name);
    if (key == null) {
      key = type.equals(String.class) ?
              Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER) :
              Metadata.Key.of(name, Metadata.BINARY_BYTE_MARSHALLER);
      HEADER_KEYS.put(clz, name, key);
      log.info("StubHeadersProcessor.registerKey clz={}, name={}, type={}", clz.getName(), name, type.getName());
    }
  }

  /**
   * Append a header to a stub.
   * @param stub  {@link AbstractStub}
   * @param name  header name
   * @param value header value
   * @param <S>   stub's type
   * @return  stub
   */
  @SuppressWarnings("unchecked")
  public static <S extends AbstractStub<S>> S withHeader(S stub, String name, Object value) {
    Metadata extraHeaders = new Metadata();
    Metadata.Key key = checkNotNull(HEADER_KEYS.get(stub.getClass(), name));
    extraHeaders.put(key, value);
    return MetadataUtils.attachHeaders(stub, extraHeaders);
  }

  /**
   * Append a header to a stub.
   * @param stub  {@link AbstractStub}
   * @param name  header name
   * @param type  type of header value
   * @param value header value
   * @param <S>   stub's type
   * @return  stub
   */
  @SuppressWarnings("unchecked")
  public static <S extends AbstractStub<S>> S withHeader(S stub, String name, Class type, Object value) {
    Metadata extraHeaders = new Metadata();
    Metadata.Key key = HEADER_KEYS.get(stub.getClass(), name);
    if (key == null) {
      key = type.equals(String.class) ?
              Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER) :
              Metadata.Key.of(name, Metadata.BINARY_BYTE_MARSHALLER);
    }
    extraHeaders.put(key, value);
    return MetadataUtils.attachHeaders(stub, extraHeaders);
  }

  /**
   * Create a {@link Metadata} from headers({@link MultiValueMap}).
   * @param headers headers
   * @return  a {@link Metadata}
   */
  @SuppressWarnings("unchecked")
  public static Metadata createMetadata(MultiValueMap<String, String> headers) {
    Metadata metadata = new Metadata();
    headers.forEach((name, list) -> {
      Metadata.Key key = Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER);
      if (list.size() == 1) {
        metadata.put(key, list.get(0));
      } else {
        metadata.put(key, list);
      }
    });
    return metadata;
  }

  /**
   * Create a {@link Metadata} from headers({@link Map}).
   * @param headers headers
   * @return  a {@link Metadata}
   */
  public static Metadata createMetadata(Map<String, String> headers) {
    Metadata metadata = new Metadata();
    headers.forEach((k, v) -> metadata.put(Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER), v));
    return metadata;
  }
}
