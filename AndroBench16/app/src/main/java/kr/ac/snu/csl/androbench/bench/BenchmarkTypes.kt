package kr.ac.snu.csl.androbench.bench

//which bench to run?
enum class BenchmarkMode{
    ALL,
    SEQUENTIAL,
    RANDOM,
    SQLITE,
    FIO_SEQ_READ,
    FIO_SEQ_WRITE,
    FIO_RND_READ,
    FIO_RND_WRITE,
}

//defines the configuration for running the benchmark
data class BenchmarkConfig(
    val targetPath: String, //directory to make test file
    val seqReclenKb: Int=32, //Sequential R/W record size — UFS 표준
    val rndReclenKb: Int=4,  //Random R/W record size — UFS page size
    val fileSizeKb: Int=2097152,  //2 GB — Pixel 9 어디서나 안전 (NativeIO 4 threads × 2 GB = 8 GB)
    val numThreads: Int=4,         //4 — Random parallelism QD=4 + NativeIO 디스크 8 GB
    val rndMaxRecs: Int=16384,     //working set = 4 × 16384 × 4 KB = 256 MB ≥ SRAM 64 MB
    val numIterations: Int=3,
    val sqliteOperations: Int=1024,
    val mode: BenchmarkMode = BenchmarkMode.ALL,
)
//latency state
data class LatencyStats(
    val count: Int, //total operations measured
    val meanNs: Long,
    val minNs: Long,
    val maxNs: Long,
    val p50Ns: Long,
    val p90Ns: Long,
    val p99Ns: Long,
    val p99_9Ns: Long,
    val histogram: List<HistogramBin> = emptyList(),
    val percentiles: List<PercentilePoint> = emptyList(),
)
data class HistogramBin(
    val LowNs: Long, //bin의 하한
    val highNs: Long, //upper bound, exclusive
    val count: Int,
    val label: String,
)
data class PercentilePoint(//point 1개
    val percentile: Double,
    val latencyNs: Long,
)
//saves the result of 1 execution of benchmark
data class BenchmarkResult(
    val seqReadMBps: Double,
    val seqReadIops: Double=0.0,
    val seqWriteMBps: Double,
    val seqWriteIops: Double=0.0,

    val rndReadMBps: Double,    //throughput
    val rndReadIops: Double,    //operation per sec
    val rndWriteMBps: Double,
    val rndWriteIops: Double,

    val sqliteInsertTps: Double,
    val sqliteUpdateTps: Double,
    val sqliteDeleteTps: Double,
    val mode: BenchmarkMode = BenchmarkMode.ALL,
    //fio results
    val fioReadMBps: Double=0.0,
    val fioReadIops: Double=0.0,
    val fioWriteMBps: Double = 0.0,
    val fioWriteIops: Double = 0.0,

    //latency stats
    val rndReadLatency: LatencyStats?=null,
    val rndWriteLatency: LatencyStats?=null,
    val fioLatency: LatencyStats?=null,
    )

//saves the progress of benchmark
sealed class BenchmarkState {
    object Idle: BenchmarkState()

    data class Running(
        val phase: String,           // "Sequential Read" 등
        val phaseIndex: Int,         // 1-indexed: 현재 단계 번호
        val totalPhases: Int,        // 총 단계 수 (mode에 따라 1~5)
        val iteration: Int,          // 1-indexed iter (0이면 표시 안 함)
        val totalIterations: Int,    // 총 iter (0이면 단일 phase, e.g. SQLite)
    ): BenchmarkState() {
        //전체 진행률 0.0 ~ 1.0
        val overallProgress: Float
            get() {
                if (totalPhases <= 0) return 0f
                val perPhase = 1f / totalPhases
                val withinPhase = if (totalIterations > 0)
                    (iteration - 1).coerceAtLeast(0).toFloat() / totalIterations
                else 0f
                return ((phaseIndex - 1).coerceAtLeast(0) * perPhase + withinPhase * perPhase)
                    .coerceIn(0f, 1f)
            }
    }

    data class Done(val result: BenchmarkResult): BenchmarkState()
    data class Error(val message: String): BenchmarkState()
}