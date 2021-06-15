package com.yinxiang.microservice.grpc.inject.factories;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.yinxiang.microservice.grpc.autoconfigure.GrpcServerProperties;
import com.yinxiang.microservice.grpc.inject.channels.ReusableChannel;
import com.yinxiang.microservice.grpc.inject.config.GrpcServiceConfig;
import com.yinxiang.spring.inject.InjectBeanException;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The core of gRPC client inject.
 * <p>
 *   In IOC container(ex: spring), {@link #setGrpcServerProperties(GrpcServerProperties)} will be called,
 *   and record all unique aliases for services package.
 *   In .yml file, need write grpc.services.[any-service].name, this property must unique in grpc.services.
 *   Method {@link #create()} will create a implement of the custom interface:
 *   First, call {@link #getStubClass()} to get a subClass which is a specific class of {@link AbstractStub}.
 *   Second, check {@link #stubs} contains subClass, if not will call {@link #newStub(Class)} to create stub
 *   and record in {@link #stubs}.
 *   Third, create a new {@link GrpcClientProxyFactory} and call
 *   {@link GrpcClientProxyFactory#newInstance(AbstractStub)} to create a implement of the custom interface.
 * </p>
 * @param <T> the custom interface which with annotation which support inject
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public abstract class GrpcClientCreator<T> {
  private static final Logger log = LoggerFactory.getLogger(GrpcClientCreator.class);
  /** Map of {@link Channel}[key=name, value={@link Channel}]. */
  private static Map<String, Channel> channels = Maps.newHashMap();
  /** Map of {@link AbstractStub}[key=sub class of {@link AbstractStub}, value={@link AbstractStub}]. */
  private static Map<Class<? extends AbstractStub>, AbstractStub> stubs = Maps.newHashMap();
  /** Map of aliases[key=alias, value=service]. */
  private static Map<String, String> aliases;
  /** The custom interface which need inject. */
  protected Class<T> mapperInterface;
  /** Config of gRPC. */
  private GrpcServerProperties grpcServerProperties;

  protected GrpcClientCreator() {}

  public GrpcClientCreator(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }

  /**
   * Will create a implement of the custom interface.
   * @return  a implement of the custom interface
   * @throws Exception  Exception
   */
  public T create() throws Exception {
    // get a subClass which is a specific class of AbstractStub
    @SuppressWarnings("unchecked")
    Class<? extends AbstractStub> subClass = getStubClass();
    // check {@link #stubs} contains subClass
    if (!stubs.containsKey(subClass)) {
      // to create stub and record in stubs
      stubs.put(subClass, newStub(subClass));
    }
    // create a new GrpcClientProxyFactory and call newInstance to create a implement of the custom interface
    return new GrpcClientProxyFactory<>(mapperInterface, subClass, getLogMethod())
            .newInstance(subClass.cast(stubs.get(subClass)));
  }

  /**
   * Get a subClass which is a specific class of {@link AbstractStub}.
   * @return  a subClass which is a specific class of {@link AbstractStub}
   */
  protected abstract Class getStubClass();

  /**
   * Get the getter name of log.
   * @return  the getter name of log
   */
  protected abstract String getLogMethod();

  /**
   * Create a {@link ManagedChannel}.
   * @param host  host of server
   * @param port  port of server
   * @return  {@link ManagedChannel}
   */
  protected ManagedChannel createChannel(String host, int port) {
    return new ReusableChannel(host, port, createClientInterceptors());
  }

  /**
   * Create the array of {@link ClientInterceptor}, if have any {@link ClientInterceptor} please
   * add a setter method.
   * @return  array of {@link ClientInterceptor}
   */
  protected ClientInterceptor[] createClientInterceptors() {
    return new ClientInterceptor[]{};
  }

  /**
   * Create a {@link AbstractStub} according to the class name.
   * <p>
   *   Call {@link #getGrpcClassNames(Class)} to split the class name to double parts.
   *   The left part use to get the class of [serviceGrpc], the right part use to call {@link #getStubName(String)}
   *   match a stub.
   *   At last get the method which can create stub and invoke the method to create the {@link AbstractStub}.
   * </p>
   * @param tClass  sub class of {@link AbstractStub}
   * @param name    alias of channel
   * @return  {@link AbstractStub}
   * @throws Exception  Exception
   */
  private AbstractStub newStub(Class<?> tClass, String name) throws Exception {
    // split the class name to double parts
    String[] classNames = getGrpcClassNames(tClass);
    // get the class of [serviceGrpc]
    return (AbstractStub) Class.forName(classNames[0])
            // get the method which can create stub
            .getMethod("new" + getStubName(classNames[1]), Channel.class)
            // create the AbstractStub
            .invoke(null, getOrCreateChannel(name));
  }

  /**
   * To split the class name to double parts.
   * @param stubClass sub class of {@link AbstractStub}
   * @return  double parts
   */
  private static String[] getGrpcClassNames(Class<?> stubClass) {
    return stubClass.getName().split("\\$");
  }

  /**
   * Match a stub name.
   * @param className the stub class name
   * @return  stub name
   */
  private String getStubName(String className) {
    return className.endsWith("BlockingStub") ?
            "BlockingStub" : className.endsWith("FutureStub") ? "FutureStub" : "Stub";
  }

  /**
   * Split a string by dot.
   * @param str a string
   * @return  split result
   */
  protected static String[] splitByDot(String str) {
    return str.split("\\.");
  }

  /**
   * Create a {@link AbstractStub} according to the class parameter.
   * @param tClass  sub class of {@link AbstractStub}
   * @return  {@link AbstractStub}
   * @throws BeansException  BeansException
   */
  private AbstractStub newStub(Class<?> tClass) throws BeansException {
    try {
      // split the package name by dot
      String[] names = splitByDot(tClass.getPackage().getName());
      // set the split visit index, begin with terminal
      int index = names.length - 1;
      // create StringBuilder of name, will keep on insert the package path
      StringBuilder nameBuilder = new StringBuilder();
      String name = null;
      do {
        // insert a dot when has any content
        if (nameBuilder.length() > 0) {
          nameBuilder.insert(0, '.');
        }
        // insert package path
        nameBuilder.insert(0, names[index]);
        // if match a alias assign to name, the name will assign the longest alias
        if (aliases.containsKey(nameBuilder.toString())) {
          name = nameBuilder.toString();
        }
      } while (index-- > 0);
      // check name exists
      checkArgument(!Strings.isNullOrEmpty(name), "Stub name is null or empty: " + nameBuilder);
      // create Stub
      return newStub(tClass, name);
    } catch (Exception e) {
      throw new InjectBeanException(e.getMessage(), e);
    }
  }

  /**
   * Get or create a {@link Channel}
   * @param name  channel's name
   * @return  {@link Channel}
   */
  private Channel getOrCreateChannel(String name) {
    // try to get a channel
    Channel channel = channels.get(name);
    // create a new channel when not exists and record it
    if (channel == null) {
      // get channel config
      GrpcServiceConfig grpcServiceConfig = grpcServerProperties.getServices().get(aliases.get(name));
      // create channel
      channel = createChannel(grpcServiceConfig.getHost(), grpcServiceConfig.getPort());
      // record
      channels.put(name, channel);
      // log
      log.info("createChannel: {}:{}", grpcServiceConfig.getHost(), grpcServiceConfig.getPort());
    }
    return channel;
  }

  /**
   * Support to autowire method.
   * @param grpcServerProperties  {@link GrpcServerProperties}
   */
  public void setGrpcServerProperties(GrpcServerProperties grpcServerProperties) {
    this.grpcServerProperties = grpcServerProperties;
    if (GrpcClientCreator.aliases == null) {
      Map<String, String> aliases = Maps.newHashMap();
      // record all aliases
      if (grpcServerProperties != null && grpcServerProperties.getServices() != null) {
        grpcServerProperties.getServices().forEach((k, v) -> aliases.put(v.getName(), k));
      }
      // record all aliases with a ImmutableMap
      GrpcClientCreator.aliases = ImmutableMap.copyOf(aliases);
    }
  }
}
