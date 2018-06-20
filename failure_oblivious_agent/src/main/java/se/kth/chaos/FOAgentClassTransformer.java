package se.kth.chaos;

import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.util.CheckClassAdapter;

import java.io.*;
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
        ProtectionDomain protectionDomain, byte[] classFileBuffer) {
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
                FoClassVisitor foClassVisitor = new FoClassVisitor(ASM4, classWriter, arguments);
                ClassVisitor classVisitor = new CheckClassAdapter(foClassVisitor);
                classReader.accept(classVisitor, 0);

                // write into a class file to see whether it is correct
                /*
                try {
                    DataOutputStream dout = new DataOutputStream(new FileOutputStream(new File("AppInstrumented.class")));
                    dout.write(classWriter.toByteArray());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //*/
                break;
	        default:
	            // nothing now
	            break;
        }

        return classWriter != null ? classWriter.toByteArray() : classFileBuffer;
    }

    public class FoClassVisitor extends ClassVisitor {
        private int api;
        private String className;
        private AgentArguments arguments;

        public FoClassVisitor(int api, ClassWriter cv, AgentArguments arguments) {
            super(api, cv);

            this.api = api;
            this.arguments = arguments;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.className = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            FoMethodVisitor foMethodVisitor = new FoMethodVisitor(api, mv, name, className, arguments);
            return foMethodVisitor;
        }
    }

    public class FoMethodVisitor extends MethodVisitor {
        private String className;
        private String methodName;
        private AgentArguments arguments;

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
        public FoMethodVisitor(int api, MethodVisitor mv, String methodName, String className, AgentArguments arguments) {
            super(api, mv);
            this.className = className;
            this.methodName = methodName;
            this.arguments = arguments;
        }

        // We want to add try/catch block for the entire code in the method
        // so adding the try/catch when the method is started visiting the code.
        @Override
        public void visitCode() {
            super.visitCode();

            if (!methodName.startsWith("<") && arguments.filter().matches(className, methodName)) {
                lTryBlockStart = new Label();
                lTryBlockEnd = new Label();
                lCatchBlockStart = new Label();
                lCatchBlockEnd = new Label();

                // set up try-catch block for RuntimeException
                visitTryCatchBlock(lTryBlockStart, lTryBlockEnd, lCatchBlockStart, "java/lang/Exception");

                // started the try block
                visitLabel(lTryBlockStart);
            }
        }

        @Override
        public void visitInsn(int opcode) {
            if (!methodName.startsWith("<") && arguments.filter().matches(className, methodName)) {
                if ((opcode >= IRETURN && opcode <= RETURN) || opcode == ATHROW) {
                    // closing the try block and opening the catch block
                    // closing the try block
                    visitLabel(lTryBlockEnd);

                    // when here, no exception was thrown, so skip exception handler
                    visitJumpInsn(GOTO, lTryBlockEnd);

                    // exception handler starts here, with RuntimeException stored on stack
                    visitLabel(lCatchBlockStart);

                    // store the RuntimeException in local variable
                    visitVarInsn(ASTORE, 1);

                    // here we could for example do e.printStackTrace()
                    visitVarInsn(ALOAD, 1); // load it
                    visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V", false);

                    // exception handler ends here:
                    visitLabel(lCatchBlockEnd);
                }
            }
            super.visitInsn(opcode);
        }

    }
}