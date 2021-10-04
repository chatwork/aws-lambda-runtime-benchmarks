package bootstrap;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.LogFactoryImpl;

@TargetClass(LogFactory.class)
public final class LogFactorySubstitued {
    @Substitute
    protected static LogFactory newFactory(final String factoryClass,
                                           final ClassLoader classLoader,
                                           final ClassLoader contextClassLoader) {
        return new LogFactoryImpl();
    }
}
