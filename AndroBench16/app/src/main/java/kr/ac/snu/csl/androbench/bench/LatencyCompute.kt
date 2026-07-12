package kr.ac.snu.csl.androbench.bench

//raw latency from fio -> latencystat
object LatencyCompute{
    private val BUCKET_BOUNDS = longArrayOf(
        1_000L,           // 0–1 μs
        5_000L,           // 1–5 μs
        10_000L,          // 5–10 μs
        50_000L,          // 10–50 μs
        100_000L,         // 50–100 μs
        500_000L,         // 100–500 μs
        1_000_000L,       // 500μs–1ms
        5_000_000L,       // 1–5 ms
        10_000_000L,      // 5–10 ms
        50_000_000L,      // 10–50 ms
        100_000_000L,     // 50–100 ms
        Long.MAX_VALUE,   // 100ms+
    )
    private val BUCKET_LABELS = listOf(
        "0–1μs", "1–5μs", "5–10μs", "10–50μs", "50–100μs",
        "100–500μs", "500μs–1ms", "1–5ms", "5–10ms",
        "10–50ms", "50–100ms", "100ms+",
    )
    private val EXTENDED_PERCENTILES = doubleArrayOf(
        1.0, 5.0, 10.0, 20.0, 30.0, 40.0, 50.0,
        60.0, 70.0, 80.0, 90.0, 95.0, 99.0,
        99.5, 99.9, 99.95, 99.99,
    )
    fun compute(latencies: LongArray): LatencyStats{
        if(latencies.isEmpty()){
            return LatencyStats(0, 0, 0, 0, 0, 0, 0, 0, emptyList())
        }
        val sorted=latencies.sortedArray() //sort
        val n=sorted.size
        val mean=sorted.sumOf{it}/n//mean latency
        val histogram=computeHistogram(sorted)
        //compute percentiles for CDF
        val extPercentiles = EXTENDED_PERCENTILES.map { pct ->
            PercentilePoint(pct, percentile(sorted, pct))
        }
        return LatencyStats(
            count=n,
            meanNs=mean,
            minNs=sorted.first(),
            maxNs=sorted.last(),
            p50Ns=percentile(sorted, 50.0),
            p90Ns = percentile(sorted, 90.0),
            p99Ns = percentile(sorted, 99.0),
            p99_9Ns = percentile(sorted, 99.9),
            histogram = histogram,
            percentiles=extPercentiles,
        )
    }
    private fun percentile(sorted: LongArray, pct: Double): Long{
        if(sorted.isEmpty()) return 0L
        val n=sorted.size
        val idx=((pct/100.0)*n).toInt().coerceIn(0, n-1)//idx within the array
        return sorted[idx]
    }
    //compute bin count from sorted array
    private fun computeHistogram(sorted: LongArray): List<HistogramBin>{
        val counts= IntArray(BUCKET_BOUNDS.size)
        var idx=0
        for(lat in sorted){
            while(idx<BUCKET_BOUNDS.size-1&&lat>=BUCKET_BOUNDS[idx]){
                idx++
            }
            counts[idx]++
        }
        return BUCKET_BOUNDS.indices.map{ i->
            val low=if(i==0) 0L else BUCKET_BOUNDS[i-1]
            val high=BUCKET_BOUNDS[i]
            HistogramBin(low, high, counts[i], BUCKET_LABELS[i])
        }
    }
}
