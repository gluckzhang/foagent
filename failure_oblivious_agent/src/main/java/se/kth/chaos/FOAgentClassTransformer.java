package se.kth.chaos;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.util.CheckClassAdapter;
import se.kth.chaos.visitors.FoClassVisitor;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import static jdk.internal.org.objectweb.asm.Opcodes.ASM4;

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

        if (inWhiteList(classNode.name) || !arguments.filter().matchClassName(classNode.name)) return classFileBuffer;

        switch (arguments.operationMode()) {
            case FO:
                // the following implementation can't work, don't know why yet ..
                /*
                classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                FoClassVisitor foClassVisitor = new FoClassVisitor(ASM4, classWriter, arguments);
                ClassVisitor classVisitor = new CheckClassAdapter(foClassVisitor);
                classNode.methods.stream()
                    .filter(method -> !method.name.startsWith("<"))
                    .filter(method -> arguments.filter().matchFullName(classNode.name, method.name))
                    .forEach(method -> {
                        method.accept(classVisitor);
                    });
                //*/

                classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                FoClassVisitor foClassVisitor = new FoClassVisitor(ASM4, classWriter, arguments);
                ClassVisitor classVisitor = new CheckClassAdapter(foClassVisitor);
                classReader.accept(classVisitor, 0);

                // write into a class file to see whether it is correct
                writeIntoClassFile(classNode.name, classWriter.toByteArray());
                break;
	        default:
	            // nothing now
	            break;
        }

        return classWriter != null ? classWriter.toByteArray() : classFileBuffer;
    }

    private boolean inWhiteList(String className) {
        String[] whiteList = {"java/", "sun/", "se/kth/chaos/FOAgent", "se/kth/chaos/visitors"};
        boolean result = false;

        for (String prefix : whiteList) {
            if (className.startsWith(prefix)) {
                result = true;
                break;
            }
        }

        return result;
    }

    private void writeIntoClassFile(String className, byte[] data) {
        try {
            String[] parts = className.split("/");
            DataOutputStream dout = new DataOutputStream(new FileOutputStream(new File(parts[parts.length - 1] + ".class")));
            dout.write(data);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}