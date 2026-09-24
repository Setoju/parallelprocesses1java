import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;


class ArraySumBenchmarkInt {

    static int SIZE = 1_000_000_000;
    static int[] data;
    static double BASELINE_TIME;

    static void main(String[] args) throws Exception {
        if (args.length > 0) SIZE = Integer.parseInt(args[0]);

        System.out.println("Розмір масиву: " + SIZE
                + "  (int[] ≈ " + (SIZE * 4L / (1024 * 1024)) + " МБ)");
        System.out.println("Доступно процесорних ядер (Runtime.availableProcessors): "
                + Runtime.getRuntime().availableProcessors());

        generateData();

        long expected = sumSingleThread();
        System.out.println();

        int[] threadCounts = {2, 4, 8};

        for (int t : threadCounts) sumWithRawThreads(t, expected);
        System.out.println();
        for (int t : threadCounts) sumWithExecutorService(t, expected);
        System.out.println();
    }

    static void generateData() {
        long start = System.nanoTime();
        data = new int[SIZE];
        for (int i = 0; i < SIZE; i++) {
            data[i] = 1 + (i % 9);
        }
        double sec = (System.nanoTime() - start) / 1e9;
        System.out.printf("Генерація масиву: %.3f c%n", sec);
    }

    static long sumSingleThread() {
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < SIZE; i++) {
            sum += data[i];
        }
        double sec = (System.nanoTime() - start) / 1e9;
        System.out.printf("[Baseline] 1 потік:             сума=%d, час=%.3f c%n", sum, sec);
        BASELINE_TIME = sec;
        return sum;
    }

    static void sumWithRawThreads(int threadCount, long expected) throws InterruptedException {
        long[] partial = new long[threadCount];
        Thread[] threads = new Thread[threadCount];
        int chunk = SIZE / threadCount;

        long start = System.nanoTime();
        for (int t = 0; t < threadCount; t++) {
            final int from = t * chunk;
            final int to = (t == threadCount - 1) ? SIZE : from + chunk;
            final int idx = t;
            threads[t] = new Thread(() -> {
                long localSum = 0;
                for (int i = from; i < to; i++) {
                    localSum += data[i];
                }
                partial[idx] = localSum;
            });
            threads[t].start();
        }
        for (Thread th : threads) th.join();

        long sum = 0;
        for (long p : partial) sum += p;
        double sec = (System.nanoTime() - start) / 1e9;
        report("Thread+join", threadCount, sum, expected, sec);
    }

    static void sumWithExecutorService(int threadCount, long expected) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        int chunk = SIZE / threadCount;
        List<Future<Long>> futures = new ArrayList<>();

        long start = System.nanoTime();
        for (int t = 0; t < threadCount; t++) {
            final int from = t * chunk;
            final int to = (t == threadCount - 1) ? SIZE : from + chunk;
            futures.add(pool.submit(() -> {
                long localSum = 0;
                for (int i = from; i < to; i++) localSum += data[i];
                return localSum;
            }));
        }
        long sum = 0;
        for (Future<Long> f : futures) sum += f.get();
        double sec = (System.nanoTime() - start) / 1e9;
        pool.shutdown();
        report("ExecutorService", threadCount, sum, expected, sec);
    }

    static void report(String name, int threads, long sum, long expected, double sec) {
        boolean ok = sum == expected;
        double speedup = BASELINE_TIME / sec;
        System.out.printf("[%s] потоків=%-2d сума=%d %s, час=%.3f c, прискорення відносно baseline=%.2fx%n",
                name, threads, sum, ok ? "(OK)" : "(!!ПОМИЛКА!!)", sec, speedup);
    }
}