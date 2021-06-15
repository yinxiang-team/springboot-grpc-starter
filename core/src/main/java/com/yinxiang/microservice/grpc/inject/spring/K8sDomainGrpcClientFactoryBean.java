package com.yinxiang.microservice.grpc.inject.spring;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.yinxiang.microservice.grpc.inject.DomainClientInterceptor;
import com.yinxiang.microservice.grpc.inject.channels.DomainChannel;
import com.yinxiang.spring.inject.KubernetesClient;
import com.yinxiang.spring.inject.config.Domain;
import com.yinxiang.spring.inject.config.DomainConfig;
import com.yinxiang.spring.inject.config.KubernetesConfig;
import com.yinxiang.spring.inject.cross.DomainCache;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class K8sDomainGrpcClientFactoryBean<T> extends GrpcClientFactoryBean<T> {
  private static final Logger log = LoggerFactory.getLogger(K8sDomainGrpcClientFactoryBean.class);
  private static final String K8S_HOST = ".svc.cluster.local";
  private DomainConfig domainConfig;
  private KubernetesConfig kubernetesConfig;
  /** 域缓存 */
  private DomainCache domainCache;

  public K8sDomainGrpcClientFactoryBean() {}

  public K8sDomainGrpcClientFactoryBean(Class<T> mapperInterface) {
    super(mapperInterface);
  }

  public void setKubernetesConfig(KubernetesConfig kubernetesConfig) {
    this.kubernetesConfig = kubernetesConfig;
  }

  public void setDomainConfig(DomainConfig domainConfig) {
    this.domainConfig = domainConfig;
  }

  public void setDomainCache(DomainCache domainCache) {
    this.domainCache = domainCache;
  }

  @Override
  protected ManagedChannel createChannel(String host, int port) {
    if (domainConfig.isEnable() && host.endsWith(K8S_HOST)) {
      try {
        // 创建通道
        Map<String, ManagedChannel> channels = createChannels(host);
        return channels.size() == 0 ?
                super.createChannel(host, port) : new DomainChannel(channels, domainCache::getDomain);
      } catch (Exception e) {
        log.warn("createChannel Exception: " + e.getMessage(), e);
      }
    }
    return super.createChannel(host, port);
  }

  @Override
  protected ClientInterceptor[] createClientInterceptors() {
    return domainCache == null ?
            new ClientInterceptor[]{} : new ClientInterceptor[]{new DomainClientInterceptor(domainCache::getDomain)};
  }

  private static String getIpSegment(String ip) {
    return splitByDot(ip)[0];
  }

  private Map<String, ManagedChannel> createChannels(String grpcServiceHost) throws IOException {
    // ip 段
    String localIpSegment = getIpSegment(InetAddress.getLocalHost().getHostAddress());
    // K8S 客户端
    KubernetesClient kubernetesClient = new KubernetesClient(kubernetesConfig.getHost())
            .login(kubernetesConfig.getToken(), kubernetesConfig.isSsl());
    // 端口集合
    SortedMap<String, Integer> portMap = Maps.newTreeMap();
    // 解析主机信息
    String[] hostInfos = splitByDot(grpcServiceHost);
    // 创建服务通道集合
    Map<String, ManagedChannel> channels = Maps.newHashMap();
    // 创建独立通道
    ManagedChannel singleChannel = null;
    if (domainConfig.getSingles().contains(hostInfos[0])) {
      JSONObject service = JSONObject.parseObject(kubernetesClient.getService(hostInfos[1], hostInfos[0]));
      singleChannel = createChannel(service, portMap, localIpSegment);
    }
    // 获取域配置列表
    List<Domain> domains = domainConfig.getList();
    // 重新调整域配置列表
    domains = CollectionUtils.isEmpty(domains) ? createDefaultDomains(hostInfos) : domains;
    // 处理所有域
    for (Domain domain : domains) {
      if (singleChannel == null) {
        try {
          // 从 K8S 获取服务信息
          JSONObject service = JSONObject.parseObject(kubernetesClient.getService(domain.getNamespace(), hostInfos[0]));
          // 存储对应的服务通道
          channels.put(domain.getHost(), createChannel(service, portMap, localIpSegment));
        } catch (Exception e) {
          log.error("Can not create channel: " + hostInfos[0] + " in namespace: " + domain.getHost(), e);
        }
      } else {
        // 存储对应的服务通道
        channels.put(domain.getHost(), singleChannel);
      }
    }
    return channels;
  }

  private ManagedChannel createChannel(JSONObject service, SortedMap<String, Integer> portMap, String localIpSegment) {
    // 获取端点
    JSONArray endpoints = service.getJSONObject("endpointList").getJSONArray("endpoints");
    // 记录端口以及本地和服务是否在相同 ip 段
    boolean sameIpSegment = recordPorts(endpoints, portMap, localIpSegment);
    // 获取 grpc 端口
    int port = portMap.size() == 1 ? portMap.get(portMap.firstKey()) :
            portMap.containsKey("grpc") ? portMap.get("grpc") : portMap.get("server");
    // 解析主机地址：ip 段相同时使用内部地址，否则使用外部地址
    String host = sameIpSegment ? getInternalHost(service) : getExternalHost(service);
    try {
      return super.createChannel(host, port);
    } finally {
      log.info("K8sDomainGrpcClientFactoryBean.createChannel {}:{}.", host, port);
    }
  }

  private List<Domain> createDefaultDomains(String[] hostInfos) {
    Domain domain = new Domain();
    domain.setHost(domainConfig.getMaster());
    domain.setNamespace(hostInfos[1]);
    return ImmutableList.of(domain);
  }

  private static boolean recordPorts(JSONArray endpoints, Map<String, Integer> portMap, String localIpSegment) {
    // 记录本地和服务是否在相同 ip 段
    boolean sameIpSegment = false;
    // 清空端口集合
    portMap.clear();
    // 处理所有端点
    for (int i = 0;i < endpoints.size();i++) {
      // 获取端点
      JSONObject endpoint = endpoints.getJSONObject(i);
      // 计算本地和服务是否在相同 ip 段
      sameIpSegment = getIpSegment(endpoint.getString("host")).equals(localIpSegment);
      // 处理所有端口
      recordPorts(endpoint.getJSONArray("ports"), portMap);
    }
    return sameIpSegment;
  }

  private static void recordPorts(JSONArray ports, Map<String, Integer> portMap) {
    // 处理所有端口
    for (int j = 0;j < ports.size();j++) {
      // 获取端口
      JSONObject port = ports.getJSONObject(j);
      // 只记录 TCP 端口
      if ("TCP".equals(port.getString("protocol"))) {
        String name = port.getString("name");
        if (Strings.isNullOrEmpty(name)) {
          name = ("TCP_" + j);
        } else if (name.contains("debug") || name.contains("prometheus")) {
          continue;
        }
        portMap.put(name, port.getIntValue("port"));
      }
    }
  }

  private static String getInternalHost(JSONObject service) {
    return service.getJSONObject("internalEndpoint").getString("host");
  }

  private static String getExternalHost(JSONObject service) {
    return service.getJSONArray("externalEndpoints").getJSONObject(0).getString("host");
  }
}
