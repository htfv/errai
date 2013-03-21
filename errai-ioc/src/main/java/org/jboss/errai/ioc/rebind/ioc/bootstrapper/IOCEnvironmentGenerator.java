package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.builder.impl.ClassBuilder;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.config.rebind.EnvUtil;
import org.jboss.errai.ioc.client.container.ClientBeanManager;
import org.jboss.errai.ioc.client.container.IOCEnvironment;
import org.jboss.errai.ioc.client.container.SyncBeanManagerImpl;
import org.jboss.errai.ioc.client.container.async.AsyncBeanManagerImpl;

import java.io.PrintWriter;

/**
 * @author Mike Brock
 */
public class IOCEnvironmentGenerator extends Generator {


  @Override
  public String generate(final TreeLogger logger,
                         final GeneratorContext context,
                         final String typeName) throws UnableToCompleteException {
    try {
      final JClassType classType = context.getTypeOracle().getType(typeName);
      final String packageName = classType.getPackage().getName();
      final String className = classType.getSimpleSourceName() + "Impl";

      logger.log(TreeLogger.INFO, "Generating Extensions Bootstrapper...");

      // Generate class source code
      generateIOCEnvironment(packageName, className, logger, context);

      // return the fully qualified name of the class generated
      return packageName + "." + className;
    }
    catch (Throwable e) {
      // record sendNowWith logger that Map generation threw an exception
      e.printStackTrace();
      logger.log(TreeLogger.ERROR, "Error generating extensions", e);
      throw new RuntimeException("error generating", e);
    }
  }

  private void generateIOCEnvironment(final String packageName,
                                      final String className,
                                      final TreeLogger logger,
                                      final GeneratorContext generatorContext) {

    final PrintWriter printWriter = generatorContext.tryCreate(logger, packageName, className);
    if (printWriter == null) {
      return;
    }

    final boolean asyncBootstrap;

    final String s = EnvUtil.getEnvironmentConfig().getFrameworkOrSystemProperty("errai.ioc.async_bean_manager");
    asyncBootstrap = s != null && Boolean.parseBoolean(s);

    final Statement newBeanManager = asyncBootstrap ? Stmt.newObject(AsyncBeanManagerImpl.class) : Stmt.newObject(SyncBeanManagerImpl.class);

    final ClassStructureBuilder<? extends ClassStructureBuilder<?>> builder
        = ClassBuilder.define(packageName + "." + className).publicScope()
        .implementsInterface(IOCEnvironment.class)
        .body()
        .publicMethod(boolean.class, "isAsync")
        .append(Stmt.load(asyncBootstrap).returnValue())
        .finish()
        .publicMethod(ClientBeanManager.class, "getNewBeanManager")
        .append(Stmt.nestedCall(newBeanManager).returnValue())
        .finish();

    final String csq = builder.toJavaString();
    printWriter.append(csq);
    generatorContext.commit(logger, printWriter);
  }
}