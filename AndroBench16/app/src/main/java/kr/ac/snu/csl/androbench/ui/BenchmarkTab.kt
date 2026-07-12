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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.ac.snu.csl.androbench.bench.BenchmarkConfig
import kr.ac.snu.csl.androbench.bench.BenchmarkMode
import kr.ac.snu.csl.androbench.bench.BenchmarkState

@Composable
fun BenchmarkTab(
    volumes: List<VolumeOption>,
    selectedIdx: Int,
    onVolumeSelect: (Int) -> Unit,
    config: BenchmarkConfig,
    state: BenchmarkState,
    onStart: (BenchmarkMode) -> Unit,
    onReset: () -> Unit,
) {
    val readOnly = volumes[selectedIdx].readOnly

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        VolumeSelector(volumes, selectedIdx, onVolumeSelect)

        when (state) {
            is BenchmarkState.Idle -> IdleContent(config, readOnly, onStart)
            is BenchmarkState.Running -> RunningProgressCard(state)
            is BenchmarkState.Done -> DoneSummary(onReset)
            is BenchmarkState.Error -> ErrorContent(state.message, onReset)
        }
    }
}

@Composable
private fun IdleContent(
    config: BenchmarkConfig,
    readOnly: Boolean,
    onStart: (BenchmarkMode) -> Unit,
) {
    val configWarning = validateConfig(config)

    fun canRun(mode: BenchmarkMode): Boolean {
        if (configWarning != null) return false
        val isWrite=mode in listOf(
            BenchmarkMode.SEQUENTIAL, BenchmarkMode.RANDOM, BenchmarkMode.ALL,
            BenchmarkMode.FIO_SEQ_WRITE, BenchmarkMode.FIO_RND_WRITE,
            //init files 때문에 제외
            BenchmarkMode.FIO_SEQ_READ, BenchmarkMode.FIO_RND_READ,
        )
        if(readOnly&&isWrite) return false
        return true
    }

    SectionHeader("Measure")
    Text(
        "${config.numThreads} threads · ${config.fileSizeKb / 1024} MB/file · " +
                "Seq ${config.seqReclenKb}KB / Rnd ${config.rndReclenKb}KB · " +
                "${config.numIterations} iters",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryCard(
                label = "All",
                subtitle = "Seq + Rnd + SQLite",
                icon = Icons.Default.SelectAll,
                enabled = canRun(BenchmarkMode.ALL),
                onClick = { onStart(BenchmarkMode.ALL) },
                modifier = Modifier.weight(1f),
                emphasis = true,
            )
            CategoryCard(
                label = "Sequential",
                subtitle = "Seq R/W only",
                icon = Icons.Default.Timeline,
                enabled = canRun(BenchmarkMode.SEQUENTIAL),
                onClick = { onStart(BenchmarkMode.SEQUENTIAL) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryCard(
                label = "Random",
                subtitle = "Rnd R/W only",
                icon = Icons.Default.Shuffle,
                enabled = canRun(BenchmarkMode.RANDOM),
                onClick = { onStart(BenchmarkMode.RANDOM) },
                modifier = Modifier.weight(1f),
            )
            CategoryCard(
                label = "SQLite",
                subtitle = "Ins / Upd / Del",
                icon = Icons.Default.Storage,
                enabled = canRun(BenchmarkMode.SQLITE),
                onClick = { onStart(BenchmarkMode.SQLITE) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    SectionHeader("fio (standard tool)")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryCard(
                label = "fio Seq R",
                subtitle = "Sequential read",
                icon = Icons.Default.Timeline,
                enabled = canRun(BenchmarkMode.FIO_SEQ_READ),
                onClick = { onStart(BenchmarkMode.FIO_SEQ_READ) },
                modifier = Modifier.weight(1f),
            )
            CategoryCard(
                label = "fio Seq W",
                subtitle = "Sequential write",
                icon = Icons.Default.Timeline,
                enabled = canRun(BenchmarkMode.FIO_SEQ_WRITE),
                onClick = { onStart(BenchmarkMode.FIO_SEQ_WRITE) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryCard(
                label = "fio Rnd R",
                subtitle = "Random read",
                icon = Icons.Default.Shuffle,
                enabled = canRun(BenchmarkMode.FIO_RND_READ),
                onClick = { onStart(BenchmarkMode.FIO_RND_READ) },
                modifier = Modifier.weight(1f),
            )
            CategoryCard(
                label = "fio Rnd W",
                subtitle = "Random write",
                icon = Icons.Default.Shuffle,
                enabled = canRun(BenchmarkMode.FIO_RND_WRITE),
                onClick = { onStart(BenchmarkMode.FIO_RND_WRITE) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    when {
        configWarning != null -> {
            Text(
                "⚠ $configWarning",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        readOnly -> {
            Text(
                "ℹ Read-only volume — write operations disabled. SQLite still works (uses app data).",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CategoryCard(
    label: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
) {
    val containerColor =
        if (emphasis) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primaryContainer
    val contentColor =
        if (emphasis) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = modifier
            .height(110.dp)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) containerColor
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (enabled) contentColor
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (emphasis) 4.dp else 1.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun RunningProgressCard(state: BenchmarkState.Running) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val isFio = state.phase.startsWith("fio:")
            // Top: phase name + iter counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Running",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        state.phase,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                if (isFio && state.iteration > 0) {
                    // count-based: elapsed seconds only
                    Text(
                        "${state.iteration}s elapsed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else if (state.totalIterations > 0) {
                    Text(
                        "iter ${state.iteration}/${state.totalIterations}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // Linear progress — fio is count-based, no ETA → indeterminate
            if (isFio) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                )
            } else {
                LinearProgressIndicator(
                    progress = { state.overallProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                )
            }

            // Bottom: phase index + percentage (or status for fio)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Phase ${state.phaseIndex} / ${state.totalPhases}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    if (isFio) "running…" else "${(state.overallProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Text(
                "Typically takes 1-3 minutes. Do not lock the screen.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun DoneSummary(onReset: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "✓ Done",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Check the Result tab for measurements, History tab for past runs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onReset) { Text("Run Again") }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onReset: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "✕ Error",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Button(onClick = onReset) { Text("Reset") }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}
