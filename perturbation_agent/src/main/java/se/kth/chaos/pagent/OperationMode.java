package se.kth.chaos.pagent;

import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.*;

public enum OperationMode {
    ARRAY_ANALYSIS {
        @Override
        public InsnList generateByteCode(TryCatchBlockNode tryCatchBlock, MethodNode methodNode, ClassNode classNode, int tcIndex, AgentArguments arguments) {
            InsnList list = new InsnList();

            return list;
        }

        @Override
        public InsnList generateByteCode(MethodNode method, AgentArguments arguments) {
            InsnList list = new InsnList();

            return list;
        }
    },
    ARRAY_PONE {
        @Override
        public InsnList generateByteCode(TryCatchBlockNode tryCatchBlock, MethodNode methodNode, ClassNode classNode, int tcIndex, AgentArguments arguments) {
            InsnList list = new InsnList();

            return list;
        }

        @Override
        public InsnList generateByteCode(MethodNode method, AgentArguments arguments) {
            InsnList list = new InsnList();

            list.add(new InsnNode(Opcodes.ICONST_1));
            list.add(new InsnNode(Opcodes.IADD));

            list.add(new LdcInsnNode("INFO PAgent add an array_pone perturbator into " + method.name));
            list.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "se/kth/chaos/pagent/PAgent",
                    "printLog",
                    "(Ljava/lang/String;)V",
                    false // this is not a method on an interface
            ));

            return list;
        }
    },

    DATAGRAM_SOCKET_TIMEOUT {
        @Override
        public InsnList generateByteCode(TryCatchBlockNode tryCatchBlock, MethodNode methodNode, ClassNode classNode, int tcIndex, AgentArguments arguments) {
            InsnList list = new InsnList();

            return list;
        }

        @Override
        public InsnList generateByteCode(MethodNode method, AgentArguments arguments) {
            InsnList list = new InsnList();

            return list;
        }
    };

    public static OperationMode fromLowerCase(String mode) {
        return OperationMode.valueOf(mode.toUpperCase());
    }

    public abstract InsnList generateByteCode(MethodNode method, AgentArguments arguments);
    public abstract InsnList generateByteCode(TryCatchBlockNode tryCatchBlock, MethodNode methodNode, ClassNode classNode, int tcIndex, AgentArguments arguments);
}
