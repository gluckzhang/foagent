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
            Thread.currentThread().sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            mte.throwNPE();
        } catch (Exception e) {
            System.out.println("In Java: Got an exception in java code");
        }

        try {
            Thread.currentThread().sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mte.throwNPE();
    }
}

class MayThrowException {
    public void throwNPE() {
        String str = null;
        str.toString();
    }
}