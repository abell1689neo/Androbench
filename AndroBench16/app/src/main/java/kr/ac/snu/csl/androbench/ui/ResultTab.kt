package kr.ac.snu.csl.androbench.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.ac.snu.csl.androbench.bench.BenchmarkMode
import kr.ac.snu.csl.androbench.bench.BenchmarkResult
import kr.ac.snu.csl.androbench.bench.BenchmarkState
import kr.ac.snu.csl.androbench.bench.HistogramBin
import kr.ac.snu.csl.androbench.bench.LatencyStats
import kr.ac.snu.csl.androbench.bench.PercentilePoint


@Composable
fun ResultTab(state: BenchmarkState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader("Results")

        when (state) {
            is BenchmarkState.Done -> ResultBody(state.result)
            is BenchmarkState.Running -> EmptyState(
                icon = Icons.Default.Assessment,
                title = "Running…",
                subtitle = "Results will appear here when measurement completes.",
            )
            else -> EmptyState(
                icon = Icons.Default.Assessment,
                title = "No results yet",
                subtitle = "Run a benchmark from the Benchmark tab.",
            )
        }
    }
}

@Composable
private fun ResultBody(result: BenchmarkResult) {
    val showSeq    = result.mode == BenchmarkMode.ALL || result.mode == BenchmarkMode.SEQUENTIAL
    val showRnd    = result.mode == BenchmarkMode.ALL || result.mode == BenchmarkMode.RANDOM
    val showSqlite = result.mode == BenchmarkMode.ALL || result.mode == BenchmarkMode.SQLITE
    val showFio =result.mode in listOf(BenchmarkMode.FIO_RND_READ, BenchmarkMode.FIO_RND_WRITE,BenchmarkMode.FIO_SEQ_READ, BenchmarkMode.FIO_SEQ_WRITE)

    val isFioWrite=result.mode in listOf(BenchmarkMode.FIO_SEQ_WRITE, BenchmarkMode.FIO_RND_WRITE)

    if (showSeq) {
        CategoryResultCard(
            title = "Sequential",
            icon = Icons.Default.Timeline,
            rows = listOf(
                "Read"  to formatMetric(result.seqReadMBps, result.seqReadIops),
                "Write" to formatMetric(result.seqWriteMBps, result.seqWriteIops),
            ),
        )
    }
    if (showRnd) {
        CategoryResultCard(
            title = "Random",
            icon = Icons.Default.Shuffle,
            rows = listOf(
                "Read"  to formatMetric(result.rndReadMBps, result.rndReadIops),
                "Write" to formatMetric(result.rndWriteMBps, result.rndWriteIops),
            ),
        )
        result.rndReadLatency?.let { stats ->
            LatencyCard("Latency · Random Read", stats)
        }
        result.rndWriteLatency?.let { stats ->
            LatencyCard("Latency · Random Write", stats)
        }
    }
    if (showSqlite) {
        CategoryResultCard(
            title = "SQLite",
            icon = Icons.Default.Storage,
            rows = listOf(
                "Insert" to "%.1f TPS".format(result.sqliteInsertTps),
                "Update" to "%.1f TPS".format(result.sqliteUpdateTps),
                "Delete" to "%.1f TPS".format(result.sqliteDeleteTps),
            ),
        )
    }

    if (showFio) {
        val modeName = result.mode.name.removePrefix("FIO_").replace("_", " ")
        CategoryResultCard(
            title = "fio · $modeName",
            icon = Icons.Default.Speed,
            rows = if (isFioWrite) {
                listOf("Write" to formatMetric(result.fioWriteMBps, result.fioWriteIops))
            } else {
                listOf("Read" to formatMetric(result.fioReadMBps, result.fioReadIops))
            },
        )
        //latency results
        result.fioLatency?.let{stats->
            LatencyCard("Latency · $modeName", stats)
        }
    }

    Text(
        "Mode: ${result.mode.name.lowercase().replaceFirstChar { it.uppercase() }}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
fun formatLatency(ns: Long): String=when{
    ns<1_000 -> "$ns ns"
    ns<1_000_000 -> "%.1f μs".format(ns/1_000.0)
    ns<1_000_000_000 -> "%.1f ms".format(ns/1_000_000.0)
    else -> "%.2f s".format(ns/1_000_000_000.0)
}
fun formatCount(n:Int):String=when{ //format big counts
    n>=10_000_000 -> "${n/1_000_000}M"      // 10M+ : 정수
    n>=1_000_000 -> "%.1fM".format(n/1_000_000.0)
    n>=100_000 -> "${n/1_000}k"             // 100k+ : 정수 (예: 166k)
    n>=1_000 -> "%.1fk".format(n/1_000.0)   // 1k~99k : 소수 1자리 (예: 9.1k)
    else -> n.toString()
}
internal fun trimEmptyBins(histogram: List<HistogramBin>): List<HistogramBin>{
    if(histogram.isEmpty()) return emptyList()
    val firstNonEmpty=histogram.indexOfFirst {it.count>0}
    if(firstNonEmpty<0) return histogram //all empty
    val lastNonEmpty=histogram.indexOfLast { it.count>0 }
    val from=(firstNonEmpty-1).coerceAtLeast(0)
    val to =(lastNonEmpty+2).coerceAtMost(histogram.size)
    return histogram.subList(from, to)
}
@Composable
private fun LatencyRow(label: String, ns: Long, accent: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            formatLatency(ns),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (accent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
        )
    }
}
// Unified metric formatter: "415 MB/s · 26.1k IOPS"
internal fun formatMetric(mbps: Double, iops: Double): String =
    "${formatMBps(mbps)} · ${formatIops(iops)} IOPS"

internal fun formatMBps(mbps: Double): String = when {
    mbps >= 1024 -> "%.2f GB/s".format(mbps / 1024.0)
    mbps >= 100  -> "%.0f MB/s".format(mbps)
    else         -> "%.1f MB/s".format(mbps)
}

internal fun formatIops(iops: Double): String = when {
    iops >= 1_000_000 -> "%.1fM".format(iops / 1_000_000.0)
    iops >= 1000      -> "%.1fk".format(iops / 1000.0)
    else              -> "%.0f".format(iops)
}
private enum class LatencyTab { STATISTICS, DISTRIBUTION, CDF }

@Composable
private fun LatencyCard(title: String, stats: LatencyStats) {
    // remember(stats): 새 measurement 들어오면 default tab으로 reset
    var tab by remember(stats) { mutableStateOf(LatencyTab.DISTRIBUTION) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header: 제목 + ops 수
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${formatCount(stats.count)} ops",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Toggle: Statistics / Distribution / CDF
            LatencyTabSelector(
                selected = tab,
                hasHistogram = stats.histogram.isNotEmpty(),
                hasCdf = stats.percentiles.isNotEmpty(),
                onSelect = { tab = it },
            )

            // 선택된 view (한 번에 1개씩, 크게)
            when (tab) {
                LatencyTab.STATISTICS -> StatisticsBox(stats)
                LatencyTab.DISTRIBUTION -> {
                    if (stats.histogram.isNotEmpty()) {
                        DistributionBox(bins = trimEmptyBins(stats.histogram))
                    } else {
                        EmptyChartHint("No distribution data")
                    }
                }
                LatencyTab.CDF -> {
                    if (stats.percentiles.isNotEmpty()) {
                        CdfBox(stats)
                    } else {
                        EmptyChartHint("No CDF data")
                    }
                }
            }
        }
    }
}

@Composable
private fun LatencyTabSelector(
    selected: LatencyTab,
    hasHistogram: Boolean,
    hasCdf: Boolean,
    onSelect: (LatencyTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TabPill(
            label = "Statistics",
            selected = selected == LatencyTab.STATISTICS,
            enabled = true,
            onClick = { onSelect(LatencyTab.STATISTICS) },
            modifier = Modifier.weight(1f),
        )
        TabPill(
            label = "Distribution",
            selected = selected == LatencyTab.DISTRIBUTION,
            enabled = hasHistogram,
            onClick = { onSelect(LatencyTab.DISTRIBUTION) },
            modifier = Modifier.weight(1f),
        )
        TabPill(
            label = "CDF",
            selected = selected == LatencyTab.CDF,
            enabled = hasCdf,
            onClick = { onSelect(LatencyTab.CDF) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TabPill(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        !enabled -> MaterialTheme.colorScheme.surface
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    val fg = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        selected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = fg,
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyChartHint(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatisticsBox(stats: LatencyStats) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
            )
            .border(
                width = 1.dp,
                color = outline,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            "Statistics",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        LatencyRow("mean", stats.meanNs)
        LatencyRow("min", stats.minNs)
        // 구분선 1
        Spacer(modifier = Modifier.height(1.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(outline.copy(alpha = 0.5f)),
        )
        Spacer(modifier = Modifier.height(1.dp))
        LatencyRow("p50", stats.p50Ns)
        LatencyRow("p90", stats.p90Ns)
        LatencyRow("p99", stats.p99Ns, accent = true)
        LatencyRow("p99.9", stats.p99_9Ns, accent = true)
        // 구분선 2
        Spacer(modifier = Modifier.height(1.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(outline.copy(alpha = 0.5f)),
        )
        Spacer(modifier = Modifier.height(1.dp))
        LatencyRow("max", stats.maxNs)
    }
}

@Composable
private fun CdfBox(stats: LatencyStats) {
    if (stats.percentiles.isEmpty() || stats.maxNs <= 0) return
    val outline = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
            )
            .border(
                width = 1.dp,
                color = outline,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "CDF",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "log scale · ${stats.percentiles.size} points",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        CdfChart(
            percentiles = stats.percentiles,
            minNs = stats.minNs,
            maxNs = stats.maxNs,
        )
    }
}

@Composable
private fun CdfChart(
    percentiles: List<PercentilePoint>,
    minNs: Long,
    maxNs: Long,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // log scale 범위
    val logMin = kotlin.math.log10(minNs.toDouble().coerceAtLeast(1.0))
    val logMax = kotlin.math.log10(maxNs.toDouble().coerceAtLeast(logMin + 1.0))
    val logRange = logMax - logMin

    Column(modifier = modifier.fillMaxWidth()) {
        // 메인 영역: [Y라벨 38dp] [Plot]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            // Y라벨 영역 — linear percentile scale에서 잘 분리되는 4점만
            Box(modifier = Modifier.width(38.dp).fillMaxHeight()) {
                val yPcts = listOf(1f to "100%", 0.9f to "90%", 0.5f to "50%", 0f to "0%")
                for ((pct, label) in yPcts) {
                    // 220dp chart 안에서 (1-pct) 비율 위치, text 높이 보정
                    val yDp = 220f * (1f - pct) - 5f
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = onVariant,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = yDp.dp)
                            .padding(end = 4.dp),
                        maxLines = 1,
                    )
                }
            }

            // Plot 영역
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // 1. Gridline (Y라벨과 매칭: 0/50/90/100%)
                    val gridPcts = listOf(0f, 0.5f, 0.9f, 1f)
                    for (p in gridPcts) {
                        val y = size.height * (1f - p)
                        drawLine(
                            color = outline.copy(alpha = 0.35f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 0.6f,
                        )
                    }

                    // 2. CDF 곡선
                    if (percentiles.size >= 2 && logRange > 0) {
                        val path = Path()

                        // 시작점: (min latency 위치, 0%) → 좌하단
                        path.moveTo(0f, size.height)

                        for (point in percentiles) {
                            val logLat = kotlin.math.log10(
                                point.latencyNs.toDouble().coerceAtLeast(1.0)
                            )
                            val x = ((logLat - logMin) / logRange).toFloat()
                                .coerceIn(0f, 1f) * size.width
                            val y = size.height * (1f - (point.percentile / 100.0).toFloat())
                            path.lineTo(x, y)
                        }

                        // 끝점: (max, 100%) → 우상단
                        path.lineTo(size.width, 0f)

                        drawPath(
                            path = path,
                            color = primary,
                            style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                        )

                        // 3. 데이터 점 강조
                        for (point in percentiles) {
                            val logLat = kotlin.math.log10(
                                point.latencyNs.toDouble().coerceAtLeast(1.0)
                            )
                            val x = ((logLat - logMin) / logRange).toFloat()
                                .coerceIn(0f, 1f) * size.width
                            val y = size.height * (1f - (point.percentile / 100.0).toFloat())
                            drawCircle(
                                color = primary,
                                radius = 2.5f,
                                center = Offset(x, y),
                            )
                        }
                    }
                }
            }
        }

        // Baseline (X축) — 좌측 38dp 비우기
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(38.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(outline.copy(alpha = 0.6f)),
            )
        }

        // X-axis 라벨 (min / mid / max)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Spacer(modifier = Modifier.width(38.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    formatLatency(minNs),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = onVariant,
                )
                Text(
                    formatLatency(kotlin.math.sqrt(minNs.toDouble() * maxNs.toDouble()).toLong()),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = onVariant,
                )
                Text(
                    formatLatency(maxNs),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = onVariant,
                )
            }
        }
    }
}
@Composable
private fun DistributionBox(bins: List<HistogramBin>) {
    val maxCount = bins.maxOfOrNull { it.count } ?: return
    if (maxCount <= 0) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        // 헤더: 제목 + scale 표시
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Distribution",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "log scale · max ${formatCount(maxCount)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Plot 본체
        HistogramPlot(bins = bins, maxCount = maxCount)
    }
}

@Composable
private fun HistogramPlot(
    bins: List<HistogramBin>,
    maxCount: Int,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // 차트 크기: 더 큼 (220dp 총, plot 200dp + 막대 라벨 20dp)
    val totalHeightDp = 220
    val topLabelDp = 20
    val plotHeightDp = totalHeightDp - topLabelDp   // 200dp

    // Y축 tick 값 (10의 거듭제곱 기준)
    val yTicks = computeYTicks(maxCount)
    val logMax = kotlin.math.log10(maxCount.toDouble() + 1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        // 메인 영역: [Y라벨 38dp] [Plot]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeightDp.dp),
        ) {
            // Y라벨 영역
            Box(modifier = Modifier.width(38.dp).fillMaxHeight()) {
                for (tick in yTicks) {
                    val logTick = kotlin.math.log10(tick.toDouble() + 1.0)
                    val fraction = (logTick / logMax).toFloat()
                    // top offset (라벨 공간) + plot 안에서 fraction 위치
                    val yDp = topLabelDp + plotHeightDp * (1f - fraction)
                    Text(
                        text = formatCount(tick),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = onVariant,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = yDp.dp - 6.dp)
                            .padding(end = 4.dp),
                        maxLines = 1,
                    )
                }
            }

            // Plot 영역
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Gridline — Y tick과 같은 위치
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val topOffsetPx = topLabelDp.dp.toPx()
                    val plotPx = size.height - topOffsetPx
                    for (tick in yTicks) {
                        val logTick = kotlin.math.log10(tick.toDouble() + 1.0)
                        val fraction = (logTick / logMax).toFloat()
                        val y = topOffsetPx + plotPx * (1f - fraction)
                        drawLine(
                            color = outline.copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 0.6f,
                        )
                    }
                }
                // 2. 막대 + count 라벨
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(plotHeightDp.dp)
                        .align(Alignment.BottomCenter),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    bins.forEach { bin ->
                        val fraction = computeBarFraction(bin.count, maxCount)
                        val isPeak = bin.count == maxCount
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(fraction.coerceAtLeast(0.003f))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            if (isPeak) primary
                                            else primary.copy(alpha = 0.85f),
                                            primary.copy(alpha = 0.4f),
                                        ),
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 3.dp,
                                        topEnd = 3.dp,
                                    ),
                                ),
                        ) {
                            // 막대 위에 count 표시 (0 제외)
                            if (bin.count > 0) {
                                Text(
                                    text = formatCount(bin.count),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .wrapContentWidth(unbounded = true)  // ★ 부모 너비 무시
                                        .offset(y = (-14).dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                    color = if (isPeak) MaterialTheme.colorScheme.primary
                                            else onVariant,
                                    fontWeight = if (isPeak) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Baseline (X축) — Y라벨 38dp 비우기
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(38.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(outline.copy(alpha = 0.6f)),
            )
        }

        // 4. X-axis 라벨
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Spacer(modifier = Modifier.width(38.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                bins.forEach { bin ->
                    Text(
                        text = shortLabel(bin.highNs),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = onVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// Y축 tick 값 계산 — 10, 100, 1k, 10k ... + max
private fun computeYTicks(maxCount: Int): List<Int> {
    if (maxCount <= 0) return emptyList()
    val result = mutableListOf<Int>()
    var tick = 10
    while (tick <= maxCount) {
        result.add(tick)
        tick *= 10
    }
    return result
}

// bin 상한 → 짧은 라벨 (e.g. "5μ", "50m", "+")
private fun shortLabel(hi: Long): String = when {
    hi == Long.MAX_VALUE -> "+"
    hi < 1_000L -> "${hi}n"
    hi < 1_000_000L -> "${hi / 1_000}μ"
    hi < 1_000_000_000L -> "${hi / 1_000_000}m"
    else -> "${hi / 1_000_000_000}s"
}

// count → log scale 비율 (0.0 ~ 1.0)
private fun computeBarFraction(count: Int, maxCount: Int): Float {
    if (count <= 0 || maxCount <= 0) return 0f
    val logC = kotlin.math.log10(count.toDouble() + 1.0)
    val logMax = kotlin.math.log10(maxCount.toDouble() + 1.0)
    return (logC / logMax).toFloat()
}
@Composable
private fun CategoryResultCard(
    title: String,
    icon: ImageVector,
    rows: List<Pair<String, String>>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
}
