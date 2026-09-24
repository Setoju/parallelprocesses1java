# ArraySumBenchmarkInt

A small Java benchmark for comparing array-summing performance across different execution models.

It generates a large `int[]` array, calculates the expected sum in a single-threaded baseline, then measures:
- raw `Thread` + `join`
- `ExecutorService` with a fixed thread pool

The program prints elapsed time and relative speedup for each configuration.

## Run
```bash
javac src/Main.java
java -cp src ArraySumBenchmarkInt
