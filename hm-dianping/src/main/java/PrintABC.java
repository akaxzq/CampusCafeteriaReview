/**
 * @create 2023-10-16
 */
public class PrintABC {
    // 当前执行线程标记
    private volatile char currentThread = 'A';

    public static void main(String[] args) {
        PrintABC printer = new PrintABC();

        // 创建三个线程
        Thread threadA = new Thread(() -> printer.print('A', 'B'));
        Thread threadB = new Thread(() -> printer.print('B', 'C'));
        Thread threadC = new Thread(() -> printer.print('C', 'A'));

        // 启动线程
        threadA.start();
        threadB.start();
        threadC.start();
    }

    public synchronized void print(char current, char next) {
        for (int i = 0; i < 100; ) {
            // 检查当前线程是否允许执行
            if (currentThread == current) {
                System.out.print(current);
                currentThread = next;
                i++;
                // 唤醒所有等待中的线程
                notifyAll();
            } else {
                try {
                    // 不是当前线程则等待
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
