package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.asm.Hook.LocalVariable;
import gloomyfolken.hooklib.asm.Hook.ReturnValue;
import org.objectweb.asm.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map.Entry;

public class HookContainerParser {

    private HookClassTransformer transformer;
    private String currentClassName;
    private String currentMethodName;
    private String currentMethodDesc;
    private boolean currentMethodPublicStatic;

    /*
    Ключ - название значения аннотации, значение - нутыпонел.
     */
    private HashMap<String, Object> annotationValues;

    /*
    Ключ - номер параметра, значение - номер локальной переменной для перехвата
    или -1 для перехвата значения наверху стека.
     */
    private HashMap<Integer, Integer> parameterAnnotations = new HashMap<Integer, Integer>();

    private boolean inHookAnnotation;
    
    private static final String HOOK_DESC = Type.getDescriptor(Hook.class);
    private static final String LOCAL_DESC = Type.getDescriptor(LocalVariable.class);
    private static final String RETURN_DESC = Type.getDescriptor(ReturnValue.class);

    public HookContainerParser(HookClassTransformer transformer) {
        this.transformer = transformer;
    }

    protected void parseHooks(String className) {
        transformer.logger.debug("Parsing hooks contatiner " + className);
        parseHooks(ReadClassHelper.getClassData(className));
    }

    protected void parseHooks(InputStream input) {
        ReadClassHelper.acceptVisitor(input, new HookClassVisitor());
    }

    protected void visitClass(String name) {
        this.currentClassName = name.replace('/', '.');
    }

    protected void visitMethod(String name, String desc, boolean publicAndStatic) {
        this.currentMethodName = name;
        this.currentMethodDesc = desc;
        this.currentMethodPublicStatic = publicAndStatic;
    }

    protected void visitAnnotation(String desc) {
        if (HOOK_DESC.equals(desc)) {
            annotationValues = new HashMap<String, Object>();
            inHookAnnotation = true;
        }
    }

    protected void visitReturnAnnotation(int paramId) {
        parameterAnnotations.put(paramId, -1);
    }

    protected void visitLocalAnnotation(int paramId, int localId) {
        parameterAnnotations.put(paramId, localId);
    }

    private void invalidHook(String message) {
        transformer.logger.warning("Found invalid hook " + currentClassName + "#" + currentMethodName);
        transformer.logger.warning(message);
    }

    protected void visitValue(String name, Object value) {
        if (inHookAnnotation) {
            annotationValues.put(name, value);
        }
    }

    protected void visitAnnotationEnd() {
        inHookAnnotation = false;
    }

    protected void visitMethodEnd() {
        if (annotationValues != null) {
            createHook();
        }
        parameterAnnotations.clear();
        currentMethodName = currentMethodDesc = null;
        currentMethodPublicStatic = false;
        annotationValues = null;
    }

    private void createHook() {
        AsmHook.Builder builder = AsmHook.newBuilder();
        Type methodType = Type.getMethodType(currentMethodDesc);
        Type[] argumentTypes = methodType.getArgumentTypes();

        if (!currentMethodPublicStatic) {
            invalidHook("Hook method must be public and static.");
            return;
        }

        if (argumentTypes.length < 1) {
            invalidHook("Hook method has no parameters. First parameter of a " +
                    "hook method must belong the type of the target class.");
            return;
        }

        if (argumentTypes[0].getSort() != Type.OBJECT) {
            invalidHook("First parameter of the hook method is not an object. First parameter of a " +
                    "hook method must belong the type of the target class.");
            return;
        }

        builder.setTargetClass(argumentTypes[0].getClassName());

        if (annotationValues.containsKey("targetMethod")) {
            builder.setTargetMethod((String) annotationValues.get("targetMethod"));
        } else {
            builder.setTargetMethod(currentMethodName);
        }

        builder.setHookClass(currentClassName);
        builder.setHookMethod(currentMethodName);
        builder.addThisToHookMethodParameters();

        boolean injectOnExit = Boolean.TRUE.equals(annotationValues.get("injectOnExit"));

        int currentParameterId = 1;
        for (int i = 1; i < argumentTypes.length; i++) {
            Type argType = argumentTypes[i];
            if (parameterAnnotations.containsKey(i)) {
                int localId = parameterAnnotations.get(i);
                if (localId == -1) {
                    builder.setTargetMethodReturnType(argType);
                    builder.addReturnValueToHookMethodParameters();
                } else {
                    builder.addHookMethodParameter(argType, localId);
                }
            } else {
                builder.addTargetMethodParameters(argType);
                builder.addHookMethodParameter(argType, currentParameterId);
                currentParameterId += argType == Type.LONG_TYPE || argType == Type.DOUBLE_TYPE ? 2 : 1;
            }
        }

        if (injectOnExit) builder.setInjectorFactory(AsmHook.ON_EXIT_FACTORY);

        if (annotationValues.containsKey("injectOnLine")) {
            int line = (Integer) annotationValues.get("injectOnLine");
            builder.setInjectorFactory(new HookInjectorFactory.LineNumber(line));
        }

        if (annotationValues.containsKey("returnType")) {
            builder.setTargetMethodReturnType((String)annotationValues.get("returnType"));
        }

        ReturnCondition returnCondition = ReturnCondition.NEVER;
        if (annotationValues.containsKey("returnCondition")) {
            returnCondition = ReturnCondition.valueOf((String) annotationValues.get("returnCondition"));
            builder.setReturnCondition(returnCondition);
        }

        if (returnCondition != ReturnCondition.NEVER) {
            Object primitiveConstant = getPrimitiveConstant();
            if (primitiveConstant != null) {
                builder.setReturnValue(gloomyfolken.hooklib.asm.ReturnValue.PRIMITIVE_CONSTANT);
                builder.setPrimitiveConstant(primitiveConstant);
            } else if (Boolean.TRUE.equals(annotationValues.get("returnNull"))) {
                builder.setReturnValue(gloomyfolken.hooklib.asm.ReturnValue.NULL);
            } else if (annotationValues.containsKey("returnAnotherMethod")) {
                builder.setReturnValue(gloomyfolken.hooklib.asm.ReturnValue.ANOTHER_METHOD_RETURN_VALUE);
                builder.setReturnMethod((String) annotationValues.get("returnAnotherMethod"));
            } else if (methodType.getReturnType() != Type.VOID_TYPE) {
                builder.setReturnValue(gloomyfolken.hooklib.asm.ReturnValue.HOOK_RETURN_VALUE);
            }
        }

        if (returnCondition == ReturnCondition.ON_TRUE && methodType.getReturnType() != Type.BOOLEAN_TYPE) {
            invalidHook("Hook method must return boolean if returnCodition is ON_TRUE.");
            return;
        }
        if ((returnCondition == ReturnCondition.ON_NULL || returnCondition == ReturnCondition.ON_NOT_NULL) &&
                methodType.getReturnType().getSort() != Type.OBJECT &&
                methodType.getReturnType().getSort() != Type.ARRAY) {
            invalidHook("Hook method must return object if returnCodition is ON_NULL or ON_NOT_NULL.");
            return;
        }

        if (annotationValues.containsKey("priority")) {
            builder.setPriority(HookPriority.valueOf((String)annotationValues.get("priority")));
        }

        builder.setHookMethodReturnType(methodType.getReturnType());

        transformer.registerHook(builder.build());
    }

    private Object getPrimitiveConstant() {
        for (Entry<String, Object> entry : annotationValues.entrySet()) {
            if (entry.getKey().endsWith("Constant")) {
                return entry.getValue();
            }
        }
        return null;
    }



    private class HookClassVisitor extends ClassVisitor {

        public HookClassVisitor() {
            super(Opcodes.ASM4);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            HookContainerParser.this.visitClass(name);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
        {
            boolean publicAndStatic = (access & Opcodes.ACC_PUBLIC) != 0 && (access & Opcodes.ACC_STATIC) != 0;
            HookContainerParser.this.visitMethod(name, desc, publicAndStatic);
            return new HookMethodVisitor();
        }
    }

    private class HookMethodVisitor extends MethodVisitor {

        public HookMethodVisitor() {
            super(Opcodes.ASM4);
        }

        public AnnotationVisitor visitAnnotationDefault() {
            return new HookAnnotationVisitor();
        }

        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            HookContainerParser.this.visitAnnotation(desc);
            return new HookAnnotationVisitor();
    }

        public AnnotationVisitor visitParameterAnnotation(final int parameter, String desc, boolean visible) {
            if (RETURN_DESC.equals(desc)) {
                visitReturnAnnotation(parameter);
            }
            if (LOCAL_DESC.equals(desc)) {
                return new AnnotationVisitor(Opcodes.ASM4) {
                    @Override
                    public void visit(String name, Object value) {
                        visitLocalAnnotation(parameter, (Integer) value);
                    }
                };
            }
            return null;
        }

        public void visitEnd() {
            HookContainerParser.this.visitMethodEnd();
        }
    }

    private class HookAnnotationVisitor extends AnnotationVisitor {

        public HookAnnotationVisitor() {
            super(Opcodes.ASM4);
        }

        public void visit(String name, Object value) {
            HookContainerParser.this.visitValue(name, value);
        }

        public void visitEnum(String name, String desc, String value) {
            HookContainerParser.this.visitValue(name, value);
        }

        public void visitEnd() {
            HookContainerParser.this.visitAnnotationEnd();
        }
    }
}
