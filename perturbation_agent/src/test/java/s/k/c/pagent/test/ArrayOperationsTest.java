package s.k.c.pagent.test;

import com.ea.agentloader.AgentLoader;
import s.k.c.pagent.test.testfiles.ArrayOperationsTestObject;
import se.kth.chaos.pagent.PerturbationAgent;

public class ArrayOperationsTest {
    public static void main(String[] args) {
//        AgentLoader.loadAgentClass(PerturbationAgent.class.getName(), "mode:array_pone,filter:s/k/c/pagent/test/testfiles");

        ArrayOperationsTestObject testObject = new ArrayOperationsTestObject();
        testObject.testOperations();
    }
}
