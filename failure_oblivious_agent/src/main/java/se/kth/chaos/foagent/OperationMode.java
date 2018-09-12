package se.kth.chaos.foagent;

import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.*;

public enum OperationMode {
    FO {
        @Override
        public InsnList generateByteCode(TryCatchBlockNode tryCatchBlock, MethodNode methodNode, ClassNode classNode, int tcIndex, AgentArguments arguments) {
            InsnList list = new InsnList();

            return list;
        }

        @Override
        public InsnList generateByteCode(MethodNode method, AgentArguments arguments) {
            // won't use this method
            return null;
        }
    },

    FO_ARRAY {
        @Override
        public InsnList generateByteCode(TryCatchBlockNode tryCatchBlock, MethodNode methodNode, ClassNode classNode, int tcIndex, AgentArguments arguments) {
            InsnList list = new InsnList();

            return list;
        }

        @Override
        public InsnList generateByteCode(MethodNode method, AgentArguments arguments) {
            InsnList list = new InsnList();

            list.add(new InsnNode(Opcodes.DUP2));
            list.add(new InsnNode(Opcodes.POP));
            list.add(new InsnNode(Opcodes.ARRAYLENGTH));

            list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "se/kth/chaos/FOAgent",
                "foArrayReading",
                "(II)I",
                false // this is not a method on an interface
            ));

            return list;
        }
    };

    public static OperationMode fromLowerCase(String mode) {
        return OperationMode.valueOf(mode.toUpperCase());
    }

    public abstract InsnList generateByteCode(MethodNode method, AgentArguments arguments);
    public abstract InsnList generateByteCode(TryCatchBlockNode tryCatchBlock, MethodNode methodNode, ClassNode classNode, int tcIndex, AgentArguments arguments);
}
