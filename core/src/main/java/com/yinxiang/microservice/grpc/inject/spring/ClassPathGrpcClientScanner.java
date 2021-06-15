package com.yinxiang.microservice.grpc.inject.spring;

import com.yinxiang.microservice.grpc.inject.factories.GrpcClientCreator;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link ClassPathBeanDefinitionScanner} that registers GrpcClients by
 * {@code basePackage}, {@code annotationClass}. If an {@code annotationClass}
 * is specified, only the specified types will be searched (searching for all
 * interfaces will be disabled).
 * <p>
 * This functionality was previously a private class of
 *
 * @see GrpcClientFactoryBean
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class ClassPathGrpcClientScanner extends ClassPathBeanDefinitionScanner {
  /** The class of {@link GrpcClientCreator}, can use {@link #setGrpcClientCreatorClass(Class)} to set custom class. */
  private static Class<? extends GrpcClientCreator> GRPC_CLIENT_CREATOR_CLASS = GrpcClientFactoryBean.class;
  /** The class of annotation which use to scan with. */
  private Class<? extends Annotation> annotationClass;

  /**
   * The setter of custom {@link GrpcClientCreator}.
   * @param grpcClientCreatorClass  {@link GrpcClientCreator}
   */
  public static void setGrpcClientCreatorClass(Class<? extends GrpcClientCreator> grpcClientCreatorClass) {
    GRPC_CLIENT_CREATOR_CLASS = grpcClientCreatorClass;
  }

  ClassPathGrpcClientScanner(BeanDefinitionRegistry registry) {
    super(registry, false);
  }

  /** {@link Annotation} */
  void setAnnotationClass(Class<? extends Annotation> annotationClass) {
    this.annotationClass = annotationClass;
  }

  /**
   * Configures parent scanner to search for the right interfaces. It can search
   * for all interfaces or just for those those annotated with the annotationClass
   */
  void registerFilters() {
    boolean acceptAllInterfaces = true;

    // if specified, use the given annotation and / or marker interface
    if (this.annotationClass != null) {
      addIncludeFilter(new AnnotationTypeFilter(this.annotationClass));
      acceptAllInterfaces = false;
    }

    if (acceptAllInterfaces) {
      // default include filter that accepts all classes
      addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
    }

    // exclude package-info.java
    addExcludeFilter((metadataReader, metadataReaderFactory) ->
            metadataReader.getClassMetadata().getClassName().endsWith("package-info"));
  }

  /**
   * Calls the parent search that will search and register all the candidates.
   * Then the registered objects are post processed to set them as GrpcClientCreator
   */
  @Override
  @NonNull
  public Set<BeanDefinitionHolder> doScan(String... basePackages) {
    Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

    if (beanDefinitions.isEmpty()) {
      logger.warn("No gRPC client was found in '" + Arrays.toString(basePackages) +
              "' package. Please check your configuration.");
    } else {
      processBeanDefinitions(beanDefinitions);
    }

    return beanDefinitions;
  }

  private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
    GenericBeanDefinition definition;
    for (BeanDefinitionHolder holder : beanDefinitions) {
      definition = (GenericBeanDefinition) holder.getBeanDefinition();

      if (logger.isDebugEnabled()) {
        logger.debug("Creating GrpcClientFactoryBean with name '" + holder.getBeanName()
                + "' and '" + definition.getBeanClassName() + "' grpcClientInterface");
      }

      // the grpcClient interface is the original class of the bean
      // but, the actual class of the bean is GrpcClientCreator
      definition.getConstructorArgumentValues().addGenericArgumentValue(checkNotNull(definition.getBeanClassName()));
      definition.setBeanClass(GRPC_CLIENT_CREATOR_CLASS);

      // support inject GrpcClientCreator's setter
      definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
    return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean checkCandidate(@NonNull final String beanName, @NonNull final BeanDefinition beanDefinition) {
    if (super.checkCandidate(beanName, beanDefinition)) {
      return true;
    } else {
      logger.warn("Skipping GrpcClientCreator with name '" + beanName
              + "' and '" + beanDefinition.getBeanClassName() + "' grpcClientInterface"
              + ". Bean already defined with the same name!");
      return false;
    }
  }
}