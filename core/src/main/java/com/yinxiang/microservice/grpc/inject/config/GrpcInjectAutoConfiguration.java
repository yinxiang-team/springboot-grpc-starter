package com.yinxiang.microservice.grpc.inject.config;

import com.google.common.base.Strings;
import com.yinxiang.microservice.grpc.autoconfigure.GrpcServerProperties;
import com.yinxiang.microservice.grpc.inject.spring.AutoConfiguredGrpcClientScannerRegistrar;
import com.yinxiang.spring.inject.config.Domain;
import com.yinxiang.spring.inject.config.DomainConfig;
import com.yinxiang.spring.inject.config.KubernetesConfig;
import com.yinxiang.spring.inject.cross.DomainCache;
import com.yinxiang.spring.inject.cross.ThreadDomainCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Entrance of gRPC inject，import {@link AutoConfiguredGrpcClientScannerRegistrar}
 * @author Huiyuan Fu
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties({GrpcServerProperties.class, DomainConfig.class, KubernetesConfig.class})
@Import(AutoConfiguredGrpcClientScannerRegistrar.class)
public class GrpcInjectAutoConfiguration {
  @Bean
  @ConditionalOnMissingBean
  public DomainCache createConfigDomainCache(DomainConfig domainConfig) {
    List<Domain> domains = domainConfig.getList();
    return domainConfig.isEnable() && !CollectionUtils.isEmpty(domains) && domains.size() > 0 ?
            new ConfigDomainCache(domainConfig) : new DomainCache() {
      @Override
      public void setDomain(String s) {}

      @Override
      public String getDomain() {
        return "stage";
      }
    };
  }

  private class ConfigDomainCache extends ThreadDomainCache {
    private final DomainConfig domainConfig;

    private ConfigDomainCache(DomainConfig domainConfig) {
      this.domainConfig = domainConfig;
    }

    @Override
    public String getDomain() {
      String domain = super.getDomain();
      return Strings.isNullOrEmpty(domain) ?
              domainConfig.getList().size() == 1 ? domainConfig.getList().get(0).getHost() : domain
              : domain;
    }
  }
}
