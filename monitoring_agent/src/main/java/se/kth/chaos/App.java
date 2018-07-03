package se.kth.chaos;

public class App {
    public static void main(String[] args) {
        System.out.println("Hello World!");

        String str = null;
        try {
            str.toString();
        } catch (Exception e) {
            System.out.println("In Java: Got an exception in java code");
        }
//        str.toString();

        MayThrowException mte = new MayThrowException();
        try {
            mte.throwNPE();
        } catch (Exception e) {
            System.out.println("In Java: Got an exception in java code");
        }

        mte.throwNPE();
    }
}

class MayThrowException {
    public void throwNPE() {
        String str = null;
        str.toString();
    }

    public void singleLineMethod() {
        String str = null;
    }

//    public void emptyMethod() {
//
//    }

    public int singleInt() {
        return 0;
    }

//    public void singleThrowMehod() throws Exception {
//        try {
//            throw new Exception();
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw e;
//        }
//    }
}