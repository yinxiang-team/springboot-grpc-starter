package com.yinxiang.microservice.grpc.inject.factories;

import com.google.common.collect.Maps;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.StreamObserver;
import javassist.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.MethodDescriptor.MethodType.*;

/**
 * The factory of gRPC client proxy.
 * <p>
 *   Generate implement class from custom interface by javassist, only in JVM.
 * </p>
 * @param <T> the type of custom interface which with GrpcClient annotation
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class GrpcClientProxyFactory<T> {
  private static final Logger log = LoggerFactory.getLogger(GrpcClientProxyFactory.class);
  /** The custom interface which with {@link com.yinxiang.microservice.grpc.inject.annotations.GrpcClient} annotation. */
  private final Class<T> grpcInterface;
  /** The sub type of {@link AbstractStub} which in custom interface's annotation. */
  private final Class<? extends AbstractStub> stubClass;
  /** The custom interface's name of log getter. */
  private final String logMethod;

  /**
   * Create the {@link MethodParametersProcessor}.
   * @param safe  is safe method, if true will has {@link CatchableParametersGenerator}
   * @return  {@link MethodParametersProcessor}
   */
  private static MethodParametersProcessor createMethodParameterProcessor(boolean safe) {
    CacheParametersProcessor processor = new CacheParametersProcessor();
    // create collectors
    processor.addParametersCollector(collector -> new FillParametersCollector(processor, collector));
    processor.addParametersCollector(collector -> new StreamParametersCollector(processor, collector));
    processor.addParametersCollector(collector -> new JsonParametersCollector(processor, collector));
    processor.addParametersCollector(collector -> new BuilderParametersCollector(processor, collector));
    processor.addParametersCollector(collector -> new RequestParametersCollector(processor, collector));
    processor.addParametersCollector(collector -> new HeaderParametersCollector(processor, collector));
    processor.addParametersCollector(collector -> new InterceptorParametersCollector(processor, collector));
    // create generator
    processor.addParametersGenerator(new InterceptorParametersGenerator(processor::getGeneratorCache));
    processor.addParametersGenerator(new HeaderParametersGenerator(processor::getGeneratorCache));
    MethodParametersGenerator generator = createNormalParametersGenerator(processor::getGeneratorCache);
    if (safe) {
      generator = new CatchableParametersGenerator(generator);
    }
    processor.addParametersGenerator(generator);
    processor.addParametersGenerator(new DeadParametersGenerator(processor::getGeneratorCache));
    return processor;
  }

  /**
   * Create the NormalParametersGenerator which contains Request/Request.Builder/parameter generator.
   * @param cacheSupplier the cache supplier
   * @return  {@link MethodParametersGenerator}
   */
  private static MethodParametersGenerator createNormalParametersGenerator(
          Function<CacheType, ParametersGeneratorCache> cacheSupplier
  ) {
    ParameterGenerators generator = new ParameterGenerators();
    MethodParametersGenerator generatorChain = createUseBuilderParametersGenerator(cacheSupplier);
    generatorChain = new RequestParametersGenerator(cacheSupplier, generatorChain);
    generator.addParametersGenerator(new StubParameterGeneratorDecorator(generatorChain));
    generator.addParametersGenerator(new StreamParametersGenerator(cacheSupplier));
    return new NormalParametersGenerator(generator);
  }

  /**
   * Create the UseBuilderParametersGenerator which support Request.Builder.
   * @param cacheSupplier the cache supplier
   * @return  {@link MethodParametersGenerator}
   */
  private static MethodParametersGenerator createUseBuilderParametersGenerator(
          Function<CacheType, ParametersGeneratorCache> cacheSupplier
  ) {
    MethodParametersGenerator generator = new JsonParametersGenerator(cacheSupplier);
    return new UseBuilderParametersGenerator(new BuilderParametersGenerator(cacheSupplier, generator));
  }

  GrpcClientProxyFactory(Class<T> grpcInterface, Class<? extends AbstractStub> stubClass, String logMethod) {
    this.grpcInterface = grpcInterface;
    this.stubClass = stubClass;
    this.logMethod = logMethod;
  }

  /**
   * Create a new instance.
   * @param stub  stub
   * @param <S> sub class of AbstractStub
   * @return  the object which implement custom interface
   * @throws Exception  Exception
   */
  @SuppressWarnings("unchecked")
  <S extends AbstractStub> T newInstance(S stub) throws Exception {
    // create ClassPool
    ClassPool pool = ClassPool.getDefault();
    // insert exact classpath
    pool.insertClassPath(new ClassClassPath(this.getClass()));
    // get the name of custom interface
    String interfaceName = grpcInterface.getName();
    // create the proxy which implement the custom interface
    CtClass proxyClass = pool.makeClass(interfaceName + "Impl");
    // add implemented interface
    proxyClass.addInterface(pool.get(interfaceName));
    // get stub class
    CtClass stubClass = pool.get(this.stubClass.getName());
    // add stub field
    proxyClass.addField(new CtField(stubClass, "stub", proxyClass));
    // get log class
    CtClass logClass = pool.get(Logger.class.getName());
    // add log field
    proxyClass.addField(new CtField(logClass, "log", proxyClass));
    // create the constructor
    CtConstructor constructor = new CtConstructor(new CtClass[]{stubClass, logClass}, proxyClass);
    // set constructor body
    constructor.setBody("{this.stub = $1;this.log = $2;}");
    // add constructor to proxy
    proxyClass.addConstructor(constructor);
    // add all implement methods
    addMethods(pool, proxyClass, stub.getClass().getMethods());
    // create impl
    return (T) proxyClass.toClass().getConstructor(stub.getClass(), Logger.class).newInstance(stub, log);
  }

  /**
   * Add all implement methods.
   * @param pool        ClassPool
   * @param proxyClass  the proxyClass
   * @param stubMethods all methods of stub
   * @throws CannotCompileException Exception
   * @throws NotFoundException      Exception
   */
  private void addMethods(ClassPool pool, CtClass proxyClass, Method[] stubMethods)
          throws CannotCompileException, NotFoundException {
    // record all method
    Map<String, Method> grpcMethods = Maps.newHashMap();
    for (Method method : stubMethods) {
      if (method.isBridge()) {
        continue;
      }
      grpcMethods.put(method.getName(), method);
    }
    // implement all methods
    boolean hasLogMethod = false;
    for (Method method : grpcInterface.getMethods()) {
      // do not implement default method
      if (!method.isDefault()) {
        // create the log method
        if (method.getName().equals(logMethod)) {
          if (hasLogMethod) {
            log.warn("GrpcClientProxyFactory addMethods {} has repeated log method: ", grpcInterface, logMethod);
          } else {
            addLogMethod(pool, proxyClass, method);
            hasLogMethod = true;
          }
        }
        // create the stub's method
        else {
          // create context
          ParametersContext context = new ParametersContext(method,
                  name -> createGrpcMethodInfo(checkNotNull(grpcMethods.get(name))), stubClass);
          // implement and add method
          addMethod(pool, proxyClass, context, method);
        }
      }
    }
  }

  /**
   * Create the {@link GrpcMethodInfo} from a stub's {@link Method}.
   * @param stubMethod  a stub's {@link Method}
   * @return  {@link GrpcMethodInfo}
   */
  private GrpcMethodInfo createGrpcMethodInfo(Method stubMethod) {
    MethodDescriptor.MethodType methodType = UNARY;
    int paramCount = stubMethod.getParameterTypes().length;
    Class requestType = stubMethod.getParameterTypes()[0];
    if (StreamObserver.class.isAssignableFrom(requestType)) {
      // if the second parameter is StreamObserver, maybe a SERVER_STREAMING, but do not affect generate
      methodType = Iterator.class.isAssignableFrom(stubMethod.getReturnType()) ? BIDI_STREAMING : CLIENT_STREAMING;
      requestType = null;
    } else if (paramCount == 2) {
      methodType = SERVER_STREAMING;
    }
    return new GrpcMethodInfo(requestType, methodType, paramCount);
  }

  /**
   * Add the log getter.
   * @param pool        {@link ClassPool}
   * @param proxyClass  proxyClass
   * @param method      custom interface's log method
   * @throws CannotCompileException CannotCompileException
   * @throws NotFoundException      NotFoundException
   */
  private void addLogMethod(ClassPool pool, CtClass proxyClass, Method method)
          throws CannotCompileException, NotFoundException {
    // get return type
    CtClass returnCtClass = pool.get(method.getReturnType().getName());
    // create ctMethod
    CtMethod ctMethod = new CtMethod(returnCtClass, method.getName(), new CtClass[0], proxyClass);
    // set public
    ctMethod.setModifiers(Modifier.PUBLIC);
    // set content
    ctMethod.setBody("{return log;}");
    // add to ctClass
    proxyClass.addMethod(ctMethod);
  }

  /**
   * Add the implement method.
   * @param pool        ClassPool
   * @param proxyClass  the proxyClass
   * @param context     ParametersContext
   * @param method      the method which need implement
   * @throws CannotCompileException Exception
   * @throws NotFoundException      Exception
   */
  private void addMethod(ClassPool pool, CtClass proxyClass, ParametersContext context, Method method)
          throws CannotCompileException, NotFoundException {
    // process all parameters and get ctClasses array of parameters
    MethodParametersProcessor parameterProcessor = createMethodParameterProcessor(context.isSafe());
    // get all parameters
    Parameter[] parameters = method.getParameters();
    // create ctClasses array of parameters
    CtClass[] paramClasses = new CtClass[parameters.length];
    // parameters
    for (int i = 0;i < parameters.length;i++) {
      // fill paramClasses
      paramClasses[i] = pool.get(parameters[i].getType().getName());
      // collector parameter
      parameterProcessor.collect(i, parameters[i], context);
    }
    // create method content builder
    StringBuilder methodBody = new StringBuilder("{");
    // append body
    parameterProcessor.fillBody(methodBody, context);
    // end
    methodBody.append("}");
    // create return class
    CtClass returnCtClass = context.isVoid() ? CtClass.voidType : pool.get(context.getReturnType().getName());
    // create ctMethod
    CtMethod ctMethod = new CtMethod(returnCtClass, method.getName(), paramClasses, proxyClass);
    // set public
    ctMethod.setModifiers(Modifier.PUBLIC);
    // set content
    ctMethod.setBody(methodBody.toString());
    // add to ctClass
    proxyClass.addMethod(ctMethod);
  }
}
