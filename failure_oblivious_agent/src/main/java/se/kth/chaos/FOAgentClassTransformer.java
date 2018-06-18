package se.kth.chaos;

import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.util.CheckClassAdapter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class FOAgentClassTransformer implements ClassFileTransformer {

    private final AgentArguments arguments;

    public FOAgentClassTransformer(String args) {
        this.arguments = new AgentArguments(args == null ? "" : args);
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain, byte[] classFileBuffer
    ) throws IllegalClassFormatException {
        return meddle(classFileBuffer);
    }

    private byte[] meddle(byte[] classFileBuffer) {
        ClassReader classReader = new ClassReader(classFileBuffer);
        ClassWriter classWriter = null;
        ClassNode classNode = new ClassNode();

        classReader.accept(classNode, 0);

        if (classNode.name.startsWith("java/") || classNode.name.startsWith("sun/") || classNode.name.startsWith("se/kth/chaos/ChaosMonkey")) return classFileBuffer;

        switch (arguments.operationMode()) {
            case FO:
                classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                FoClassVisitor foClassVisitor = new FoClassVisitor(ASM4, classWriter);
                ClassVisitor classVisitor = new CheckClassAdapter(foClassVisitor);

//                classNode.methods.stream()
//                    .filter(method -> !method.name.startsWith("<"))
//                    .filter(method -> arguments.filter().matches(classNode.name, method.name))
//                    .forEach(method -> {
//                        method.accept(foClassVisitor);
//                    });
                classReader.accept(classVisitor, 0);
                break;
	        default:
	            // nothing now
	            break;
        }

        return classWriter != null ? classWriter.toByteArray() : classFileBuffer;
    }

    public static class FoClassVisitor extends ClassVisitor {
        private int api;

        public FoClassVisitor(int api, ClassWriter cv) {
            super(api, cv);
            this.api = api;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

            FoMethodVisitor foMethodVisitor = new FoMethodVisitor(api, mv, name);
            return foMethodVisitor;
        }
    }

    public static class FoMethodVisitor extends MethodVisitor {
        private String methodName;

        // below label variables are for adding try/catch blocks in instrumented code.
        private Label lTryBlockStart;
        private Label lTryBlockEnd;
        private Label lCatchBlockStart;
        private Label lCatchBlockEnd;

        /**
         * constructor for accepting methodVisitor object and methodName
         *
         * @param api: the ASM API version implemented by this visitor
         * @param mv: MethodVisitor obj
         * @param methodName : methodName to make sure adding try catch block for the specific method.
         */
        public FoMethodVisitor(int api, MethodVisitor mv, String methodName) {
            super(api, mv);
            this.methodName = methodName;
        }

        // We want to add try/catch block for the entire code in the method
        // so adding the try/catch when the method is started visiting the code.
        @Override
        public void visitCode() {
            super.visitCode();

            if (methodName.equals("throwNPE")) {
                lTryBlockStart = new Label();
                lTryBlockEnd = new Label();
                lCatchBlockStart = new Label();
                lCatchBlockEnd = new Label();

                // set up try-catch block for RuntimeException
                visitTryCatchBlock(lTryBlockStart, lTryBlockEnd,
                        lCatchBlockStart, "java/lang/Exception");

                // started the try block
                visitLabel(lTryBlockStart);
            }

        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {

            // closing the try block and opening the catch block
            if (methodName.equals("throwNPE")) {
                // closing the try block
                visitLabel(lTryBlockEnd);

                // when here, no exception was thrown, so skip exception handler
                visitJumpInsn(GOTO, lCatchBlockEnd);

                // exception handler starts here, with RuntimeException stored on stack
                visitLabel(lCatchBlockStart);

                // store the RuntimeException in local variable
                visitVarInsn(ASTORE, 2);

                // here we could for example do e.printStackTrace()
                visitVarInsn(ALOAD, 2); // load it
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V", false);

                // exception handler ends here:
                visitLabel(lCatchBlockEnd);
            }

            super.visitMaxs(maxStack, maxLocals);
            super.visitEnd();
        }

    }
}