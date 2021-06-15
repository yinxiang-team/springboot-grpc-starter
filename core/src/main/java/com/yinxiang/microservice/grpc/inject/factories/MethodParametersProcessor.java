package com.yinxiang.microservice.grpc.inject.factories;

import com.google.common.collect.Maps;

import java.lang.reflect.Parameter;
import java.util.EnumMap;
import java.util.SortedMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Both {@link MethodParametersCollector} and {@link MethodParametersGenerator}.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
interface MethodParametersProcessor extends MethodParametersCollector, MethodParametersGenerator {}

/**
 * A complete {@link MethodParametersProcessor} and {@link CacheSupplier} implement.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class CacheParametersProcessor extends ParameterGenerators implements
        MethodParametersProcessor, CacheSupplier {
  /**
   * {@link EnumMap} of parameters caches, key is {@link CacheType} value is {@link SortedMap}.
   * The {@link SortedMap} will sorted by parameter's index.
   */
  private final EnumMap<CacheType, SortedMap<Integer, Parameter>> caches = new EnumMap<>(CacheType.class);
  /**
   * The top {@link MethodParametersCollector}, please call {@link #addParametersCollector(Function)} to add
   * new {@link MethodParametersCollector}, the last default is {@link DeadParametersCollector}.
   */
  private MethodParametersCollector collector = new DeadParametersCollector(this);

  /**
   * Getter of the {@link SortedMap} cache.
   * @param cacheType {@link CacheType}
   * @return  a cache
   */
  private SortedMap<Integer, Parameter> getCache(CacheType cacheType) {
    return caches.computeIfAbsent(cacheType, key -> Maps.newTreeMap());
  }

  /**
   * Add a new {@link MethodParametersCollector}, will call at first and next is current {@link #collector}.
   * @param collectorCreator  function to make a new {@link MethodParametersCollector} with current {@link #collector}
   */
  void addParametersCollector(Function<MethodParametersCollector, MethodParametersCollector> collectorCreator) {
    collector = collectorCreator.apply(collector);
  }

  /**
   * Getter of the {@link ParametersGeneratorCache} cache.
   * @param cacheType {@link CacheType}
   * @return  a cache
   */
  ParametersGeneratorCache getGeneratorCache(CacheType cacheType) {
    SortedMap<Integer, Parameter> cache = getCache(cacheType);
    return new ParametersGeneratorCache() {
      @Override
      public Parameter apply(int value) {
        return cache.get(value);
      }

      @Override
      public void forEachParameters(BiConsumer<Integer, Parameter> parameterVisitor) {
        cache.forEach(parameterVisitor);
      }

      @Override
      public boolean visitorFirstKey(Consumer<Integer> consumer) {
        if (cache.size() > 0) {
          consumer.accept(cache.firstKey());
          return true;
        }
        return false;
      }
    };
  }

  @Override
  public boolean collect(int index, Parameter parameter, ParametersContext context) {
    return collector.collect(index, parameter, context);
  }

  @Override
  public BiConsumer<Integer, Parameter> apply(CacheType cacheType) {
    return getCache(cacheType)::put;
  }
}
