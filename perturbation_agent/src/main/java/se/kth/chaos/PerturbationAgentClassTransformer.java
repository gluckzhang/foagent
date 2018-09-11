package se.kth.chaos;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.*;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class PerturbationAgentClassTransformer implements ClassFileTransformer {

    private final AgentArguments arguments;

    public PerturbationAgentClassTransformer(String args) {
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

        if (inWhiteList(classNode.name)) return classFileBuffer;

        switch (arguments.operationMode()) {
            case ARRAY:
                classNode.methods.stream()
                    .filter(method -> !method.name.startsWith("<"))
                    .filter(method -> arguments.filter().matches(classNode.name, method.name))
                    .forEach(method -> {
                        InsnList insnList = method.instructions;
                        for (AbstractInsnNode node : insnList.toArray()) {
                            if (node instanceof VarInsnNode && node.getOpcode() == Opcodes.ALOAD) {
                                System.out.println("INFO PerturbationAgent load an array variable");
                            } else if (node instanceof InsnNode && node.getOpcode() == Opcodes.IALOAD) {
                                System.out.println("INFO PerturbationAgent read an array");
                                AbstractInsnNode previousNode = node.getPrevious();
                                String readingIndex = "UNKNOWN";
                                if (previousNode.getOpcode() >= Opcodes.ICONST_M1 && previousNode.getOpcode() <= Opcodes.ICONST_5) {
                                    readingIndex = previousNode.getOpcode() - 3 + "";
                                } else if (previousNode.getOpcode() == Opcodes.BIPUSH) {
                                    readingIndex = ((IntInsnNode) previousNode).operand + "";
                                } else if (previousNode.getOpcode() == Opcodes.ILOAD) {
                                    readingIndex = "a local variable, index: " + ((VarInsnNode) previousNode).var;
                                }

                                System.out.println("INFO PerturbationAgent the array index is:" + readingIndex);

                                System.out.println("INFO PerturbationAgent now we try to perturb with PONE operation!");
                                insnList.insertBefore(node, OperationMode.ARRAY.generateByteCode(method, arguments));
                            }
                        }
                    });
                break;
	        default:
	            // nothing now
	            break;
        }

        classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(classWriter);

        return classWriter != null ? classWriter.toByteArray() : classFileBuffer;
    }

    private boolean inWhiteList(String className) {
        String[] whiteList = {"java/", "sun/", "se/kth/chaos/PAgent"};
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