package com.yinxiang.microservice.grpc.inject.spring;

import com.yinxiang.microservice.grpc.inject.annotations.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.util.List;

import static org.springframework.boot.autoconfigure.AutoConfigurationPackages.get;

/**
 * Register the scanner {@link ClassPathGrpcClientScanner} and scan base package as Spring Boot does.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class AutoConfiguredGrpcClientScannerRegistrar
        implements BeanFactoryAware, ImportBeanDefinitionRegistrar, ResourceLoaderAware {
  private static final Logger log = LoggerFactory.getLogger(AutoConfiguredGrpcClientScannerRegistrar.class);
  /** Spring's {@link BeanFactory}. */
  private BeanFactory beanFactory;
  /** Spring's {@link ResourceLoader}. */
  private ResourceLoader resourceLoader;

  @Override
  public void setBeanFactory(@NonNull final BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  @Override
  public void setResourceLoader(@NonNull final ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @Override
  public void registerBeanDefinitions(
          @NonNull final AnnotationMetadata importingClassMetadata,
          @NonNull final BeanDefinitionRegistry registry
  ) {
    // create the scanner
    ClassPathGrpcClientScanner scanner = new ClassPathGrpcClientScanner(registry);

    try {
      if (this.resourceLoader != null) {
        scanner.setResourceLoader(this.resourceLoader);
      }
      // get the packages which need scan
      List<String> packages = get(this.beanFactory);
      if (log.isDebugEnabled()) {
        for (String pkg : packages) {
          log.debug("Using auto-configuration base package '{}'", pkg);
        }
      }
      // set the scan annotation
      scanner.setAnnotationClass(GrpcClient.class);
      // register the filters
      scanner.registerFilters();
      // scan
      scanner.doScan(StringUtils.toStringArray(packages));
    } catch (IllegalStateException ex) {
      log.debug("Could not determine auto-configuration package, automatic mapper scanning disabled.", ex);
    }
  }
}
