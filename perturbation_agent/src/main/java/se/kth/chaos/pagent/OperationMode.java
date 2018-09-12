package se.kth.chaos.pagent;

import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.*;

public enum OperationMode {
    ARRAY {
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

            return list;
        }
    };

    public static OperationMode fromLowerCase(String mode) {
        return OperationMode.valueOf(mode.toUpperCase());
    }

    public abstract InsnList generateByteCode(MethodNode method, AgentArguments arguments);
    public abstract InsnList generateByteCode(TryCatchBlockNode tryCatchBlock, MethodNode methodNode, ClassNode classNode, int tcIndex, AgentArguments arguments);
}
