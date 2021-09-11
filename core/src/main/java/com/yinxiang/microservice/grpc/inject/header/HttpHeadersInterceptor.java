package com.yinxiang.microservice.grpc.inject.header;

import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.protobuf.Descriptors.*;
import com.yinxiang.grpc.http.Headers;
import com.yinxiang.grpc.http.HeadersFilter;
import com.yinxiang.microservice.grpc.autoconfigure.GrpcServerProperties;
import io.grpc.Context;
import io.grpc.Metadata;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.BOOLEAN;

/**
 * Process headers based yinxiang.grpc.http package's proto.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class HttpHeadersInterceptor extends AbstractHeadersInterceptor<Headers, HeadersFilter> {
  /** The key of headers */
  private static final Context.Key<Headers> HEADERS = Context.key("headers");
  /** The key of extend_headers */
  private static final Context.Key EXTEND_HEADERS = Context.key("extend_headers");
  /** Cookie's name. */
  protected static final String COOKIE = Headers.getDefaultInstance().getCOOKIE();
  /** The default empty string set. */
  protected static final Set<String> NONE = ImmutableSet.of();
  /** A always true {@link Predicate}. */
  protected static final Predicate<String> ALWAYS_TRUE = value -> true;

  /** The aliases of headers. */
  protected final Map<String, Set<String>> headerNames;
  /** The valid filter fields in {@link HeadersFilter} except COOKIE. */
  protected final List<FieldDescriptor> filterFields = HeadersFilter.getDescriptor()
          .getFields()
          .stream()
          .filter(descriptor -> descriptor.getJavaType() == BOOLEAN && !descriptor.getName().equals("COOKIE"))
          .collect(Collectors.toList());
  /** The valid filter fields in {@link Headers} with {@link #filterFields}. */
  protected final Map<String, FieldDescriptor> headerFields;
  /** The header filter's checkers. */
  protected final Map<String, Predicate<String>> checkers;
  /** The default name of headers. */
  protected final Map<String, String> baseHeaderNames;

  public HttpHeadersInterceptor(GrpcServerProperties properties) throws Exception {
    this.headerNames = properties.getHeaderNames() == null ? ImmutableMap.of() : properties.getHeaderNames();
    Map<String, FieldDescriptor> headerFields = Maps.newHashMap();
    Map<String, String> baseHeaderNames = Maps.newHashMap();
    Descriptor descriptor = Headers.getDescriptor();
    filterFields.forEach(field -> {
      String name = field.getName();
      headerFields.put(name, descriptor.findFieldByName(name));
      baseHeaderNames.put(name, toHeaderName(name));
    });
    this.headerFields = ImmutableMap.copyOf(headerFields);
    this.baseHeaderNames = baseHeaderNames;
    this.checkers = ImmutableMap.copyOf(putCheckers(Maps.newHashMap()));
  }

  /**
   * Add some checkers.
   * @param checkers checkers
   * @return  checkers
   */
  protected Map<String, Predicate<String>> putCheckers(Map<String, Predicate<String>> checkers) {
    filterFields.stream()
            .filter(field -> ((Boolean) field.getDefaultValue()))
            .forEach(field -> checkers.put(field.getName(), StringUtils::isNotBlank));
    return checkers;
  }

  /** {@link #HEADERS} */
  public static Optional<Headers> getHeader() {
    return Optional.ofNullable(HEADERS.get());
  }

  /**
   * Transform a name to valid header name.
   * @param name  name
   * @return  header name
   */
  protected String toHeaderName(String name) {
    return name.toLowerCase().replaceAll("_", "-");
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Pair<Context, Headers> filterHeaders(Context contexts, Metadata metadata, HeadersFilter filter) {
    Headers.Builder builder = Headers.newBuilder();
    // get cookie
    String cookie = filter.getCOOKIE() ? getHeader(metadata, COOKIE, headerNames.getOrDefault(COOKIE, NONE)) : null;
    // parse cookie to a list
    ListMultimap<String, String> cookies = parseCookie(cookie);
    // filter all headers
    filterFields.forEach(descriptor -> {
      Object value = filter.getField(descriptor);
      if (value instanceof Boolean && (Boolean) value) {
        String name = descriptor.getName();
        String headerName = baseHeaderNames.computeIfAbsent(name, this::toHeaderName);
        String header = getHeader(metadata, headerName, cookies, checkers.getOrDefault(name, ALWAYS_TRUE));
        builder.setField(headerFields.get(name), header);
      }
    });
    Object extendHeaders = processExtendHeaders(metadata, filter);
    Headers header = builder.build();
    return Pair.of(extendHeaders == null ? contexts.withValue(HEADERS, header) :
            contexts.withValues(HEADERS, header, EXTEND_HEADERS, extendHeaders), header);
  }

  /**
   * Process the extend headers.
   * @param metadata  {@link Metadata}
   * @param filter    {@link HeadersFilter}
   * @return  extend headers
   */
  protected Object processExtendHeaders(Metadata metadata, HeadersFilter filter) {
    return null;
  }

  /**
   * Get a header from {@link Metadata} or cookie.
   * @param headers     {@link Metadata}
   * @param headerName  name of header
   * @param cookies     cookie
   * @param checker     a function to check the header
   * @return  value of header
   */
  protected String getHeader(
          Metadata headers,
          String headerName,
          ListMultimap<String, String> cookies,
          Predicate<String> checker
  ) {
    // get the set of all aliases for header name
    Set<String> headerNames = this.headerNames.getOrDefault(headerName, NONE);
    // get header from Metadata
    String header = getHeader(headers, headerName, headerNames);
    if (checker.test(header)) {
      return header;
    }
    // get header from cookie
    String name = checkNotNull(headerNames.stream().filter(cookies::containsKey).findFirst().orElse(null),
            "header name " + headerName + " is null.");
    return checkNotNull(cookies.get(name).stream().findFirst().orElse(null),
            "header " + headerName + " is null.");
  }

  /**
   * Parse a cookies.
   * @param cookies cookies
   * @return  result of parse
   * @see ServerCookieDecoder
   */
  protected ListMultimap<String, String> parseCookie(String cookies) {
    if (Strings.isNullOrEmpty(cookies)) {
      return ImmutableListMultimap.of();
    }
    ListMultimap<String, String> map = LinkedListMultimap.create();
    ServerCookieDecoder.LAX.decode(cookies).stream()
            .filter(cookie -> !"null".equals(cookie.value()) && !Strings.isNullOrEmpty(cookie.value()))
            .forEach(cookie -> map.put(cookie.name(), cookie.value()));
    return ImmutableListMultimap.copyOf(map);
  }
}
