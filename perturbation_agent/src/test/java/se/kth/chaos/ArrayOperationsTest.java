package se.kth.chaos;

import com.ea.agentloader.AgentLoader;
import se.kth.chaos.testfiles.ArrayOperationsTestObject;

public class ArrayOperationsTest {
    public static void main(String[] args) {
        AgentLoader.loadAgentClass(PerturbationAgent.class.getName(), "mode:array");

        ArrayOperationsTestObject testObject = new ArrayOperationsTestObject();
        testObject.testOperations();
    }
}
