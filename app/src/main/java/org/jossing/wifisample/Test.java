package org.jossing.wifisample;

/**
 * @author jossing
 * @date 2018/12/29
 */
public class Test {

    public static class SyncObject {

        private final Object countLock = new Object();
        private int count = 0;

        public int getCount() {
            synchronized (countLock) {
                System.out.println("开始处理 count 对象");
                try {
                    Thread.sleep(3000);
                } catch (Exception e) { /**/ }
                System.out.println("完成处理 count 对象");
                return count;
            }
        }

        public void setCount(int count) {
            synchronized (countLock) {
                this.count = count;
            }
        }

        public void testA() {
            try {
                Thread.sleep(5000);
                System.out.println("count 对象处理结果：" + getCount());
            } catch (Exception e) { /**/ }
        }

        public void testB() {
            for (int i = 0; i < 10; i++) {
                final int curCount = count;
                System.out.println("count = " + curCount);
                try {
                    Thread.sleep(1000);
                } catch (Exception e) { /**/ }
                setCount(curCount + 1);
            }
        }

        private static boolean getXXX() {
            System.out.println("???");
            return true;
        }

        public static void main(String[] args) {
            final boolean success = true && getXXX();
            System.out.println(success);
        }
    }


    public static void main(String[] args) {

        final SyncObject syncObject = new SyncObject();

        new Thread(() -> {
            syncObject.testA();
        }).start();

        new Thread(() -> {
            syncObject.testB();
        }).start();

    }
}
