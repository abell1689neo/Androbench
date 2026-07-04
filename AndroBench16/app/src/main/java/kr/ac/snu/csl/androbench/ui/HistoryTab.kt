package kr.ac.snu.csl.androbench.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.ac.snu.csl.androbench.history.HistoryRecord
import kr.ac.snu.csl.androbench.history.HistoryRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryTab() {
    val context = LocalContext.current
    val repo = remember { HistoryRepository(context) }
    val records by repo.observeAll().collectAsState(initial = emptyList())

    var detailRecord by remember { mutableStateOf<HistoryRecord?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${records.size} runs",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (records.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = records, key = { it.id }) { record ->
                    HistoryCard(record, onClick = { detailRecord = record })
                }
            }
        }
    }

    detailRecord?.let { record ->
        DetailDialog(record = record, onDismiss = { detailRecord = null })
    }
}

@Composable
private fun HistoryCard(record: HistoryRecord, onClick: () -> Unit) {
    val timeStr = remember(record.timestamp) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            .format(Date(record.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    timeStr,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        record.fsType,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (record.readOnly) {
                        Text(
                            "RO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            val isFio = record.mode.startsWith("FIO_")
            if (isFio) {
                val isWrite = "WRITE" in record.mode
                val (mbps, iops) = if (isWrite)
                    record.fioWriteMBps to record.fioWriteIops
                else
                    record.fioReadMBps to record.fioReadIops
                MetricRow(
                    "fio ${record.mode.removePrefix("FIO_").replace("_", " ")}",
                    formatMetric(mbps, iops),
                )
            } else {
                MetricRow("Seq Read",   formatMetric(record.seqReadMBps, record.seqReadIops))
                MetricRow("Seq Write",  formatMetric(record.seqWriteMBps, record.seqWriteIops))
                MetricRow("Rnd Read",   formatMetric(record.rndReadMBps, record.rndReadIops))
                MetricRow("Rnd Write",  formatMetric(record.rndWriteMBps, record.rndWriteIops))
                MetricRow(
                    "SQLite",
                    "%.0f / %.0f / %.0f TPS".format(
                        record.sqliteInsertTps,
                        record.sqliteUpdateTps,
                        record.sqliteDeleteTps,
                    ),
                )
            }
//            MetricRow("Seq R/W", "%.0f / %.0f MB/s".format(record.seqReadMBps, record.seqWriteMBps))
//            MetricRow("Rnd R/W", "%.0f / %.0f IOPS".format(record.rndReadIops, record.rndWriteIops))
//            MetricRow(
//                "SQLite",
//                "%.0f / %.0f / %.0f TPS".format(
//                    record.sqliteInsertTps,
//                    record.sqliteUpdateTps,
//                    record.sqliteDeleteTps,
//                ),
//            )
            Text(
                "Tap for details",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DetailDialog(record: HistoryRecord, onDismiss: () -> Unit) {
    val timeStr = remember(record.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(record.timestamp))
    }
    val isFio = record.mode.startsWith("FIO_")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Run #${record.id}  ·  ${if (isFio) "fio" else "NativeIO"}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DetailSection("Environment") {
                    DetailRow("When", timeStr)
                    DetailRow("Mode", record.mode)
                    DetailRow("Target", record.targetPath)
                    DetailRow("FS", "${record.fsType}${if (record.readOnly) " (RO)" else ""}")
                    DetailRow("Page size", "${record.kernelPageKb} KB")
                    DetailRow("Device", "${record.deviceModel} (Android ${record.androidRelease})")
                }

                HorizontalDivider()

                if (isFio) {
                    FioDetail(record)
                } else {
                    NativeDetail(record)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun FioDetail(record: HistoryRecord) {
    val isRandom = "RND" in record.mode
    val isWrite = "WRITE" in record.mode

    DetailSection("Configuration (fio)") {
        DetailRow("Threads (numjobs)", if (isRandom) "${record.numThreads}" else "1")
        DetailRow("File size", "${record.fileSizeKb / 1024} MB")
        DetailRow(
            "Block size",
            if (isRandom) "${record.rndReclenKb} KB" else "${record.seqReclenKb} KB",
        )
        DetailRow("Pattern", if (isRandom) "Random" else "Sequential")
        DetailRow("Operation", if (isWrite) "Write" else "Read")
    }

    HorizontalDivider()

    DetailSection("fio result") {
        if (isWrite) {
            DetailRow("Write MB/s", "%.2f".format(record.fioWriteMBps))
            DetailRow("Write IOPS", "%.0f".format(record.fioWriteIops))
        } else {
            DetailRow("Read MB/s", "%.2f".format(record.fioReadMBps))
            DetailRow("Read IOPS", "%.0f".format(record.fioReadIops))
        }
    }
}

@Composable
private fun NativeDetail(record: HistoryRecord) {
    val mode = record.mode    // "ALL", "SEQUENTIAL", "RANDOM", "SQLITE"
    val showSeq    = mode == "ALL" || mode == "SEQUENTIAL"
    val showRnd    = mode == "ALL" || mode == "RANDOM"
    val showSqlite = mode == "ALL" || mode == "SQLITE"

    DetailSection("Configuration (NativeIO)") {
        DetailRow("Threads", "${record.numThreads}")
        DetailRow("File size / thread", "${record.fileSizeKb / 1024} MB")
        if (showSeq) DetailRow("Seq buffer", "${record.seqReclenKb} KB")
        if (showRnd) {
            DetailRow("Rnd buffer", "${record.rndReclenKb} KB")
            DetailRow("Rnd ops / thread", "${record.rndMaxRecs}")
        }
        if (showSqlite) DetailRow("SQLite ops", "${record.sqliteOperations}")
        DetailRow("Iterations", "${record.numIterations}")
    }

    HorizontalDivider()

    if (showSeq) {
        DetailSection("Sequential") {
            DetailRow("Read MB/s",  "%.2f".format(record.seqReadMBps))
            DetailRow("Read IOPS",  "%.0f".format(record.seqReadIops))
            DetailRow("Write MB/s", "%.2f".format(record.seqWriteMBps))
            DetailRow("Write IOPS", "%.0f".format(record.seqWriteIops))
        }
    }

    if (showRnd) {
        DetailSection("Random") {
            DetailRow("Read MB/s",  "%.2f".format(record.rndReadMBps))
            DetailRow("Read IOPS",  "%.0f".format(record.rndReadIops))
            DetailRow("Write MB/s", "%.2f".format(record.rndWriteMBps))
            DetailRow("Write IOPS", "%.0f".format(record.rndWriteIops))
        }
    }

    if (showSqlite) {
        DetailSection("SQLite") {
            DetailRow("Insert", "%.2f TPS".format(record.sqliteInsertTps))
            DetailRow("Update", "%.2f TPS".format(record.sqliteUpdateTps))
            DetailRow("Delete", "%.2f TPS".format(record.sqliteDeleteTps))
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text("No history yet", style = MaterialTheme.typography.titleMedium)
        Text(
            "Past benchmark runs will appear here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
