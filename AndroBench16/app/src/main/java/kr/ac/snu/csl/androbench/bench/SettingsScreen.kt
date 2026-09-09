package kr.ac.snu.csl.androbench.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.ac.snu.csl.androbench.bench.BenchmarkConfig

@Composable
fun SettingsCard(
    config: BenchmarkConfig,
    onConfigChange: (BenchmarkConfig) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryCard(config)

        // ── Common — 모든 엔진이 공유하는 워크로드 정의 ──
        GroupLabel("Common — 모든 엔진 공통 (NativeIO · fio · mmap)")

        SettingSection(title = "File & Block", icon = Icons.Default.Memory) {
            ChipSetting(
                label = "File size per thread",
                options = listOf(
                    65536,
                    262144,//256MB
                    1048576,  // 1 GB
                    2097152,  // 2 GB
                    4194304,  // 4 GB
                ),
                display = {
                    when {
                        it >= 1048576 -> "${it / 1048576} GB"
                        else -> "${it / 1024} MB"
                    }
                },
                current = config.fileSizeKb,
                onChange = { onConfigChange(config.copy(fileSizeKb = it)) },
            )
            ChipSetting(
                label = "Sequential block size (최대 BW 측정 관례 = 1 MB)",
                options = listOf(4, 16, 32, 64, 128, 256, 512, 1024),
                display = { if (it >= 1024) "${it / 1024} MB" else "$it KB" },
                current = config.seqReclenKb,
                onChange = { onConfigChange(config.copy(seqReclenKb = it)) },
            )
            ChipSetting(
                label = "Random block size",
                options = listOf(4, 8, 16, 32, 64, 256), //256: 16K페이지×16연발 — 디바이스 선읽기 발동 검증용 (nios≤8192 필요)
                current = config.rndReclenKb,
                onChange = { onConfigChange(config.copy(rndReclenKb = it)) },
                unitLabel = " KB",
            )
            ChipSetting(
                label = "Random ops (direct: per thread · mmap: 총 1-pass 개수)",
                options = listOf(2048, 8192, 16384, 32768, 65536),
                current = config.rndMaxRecs,
                onChange = { onConfigChange(config.copy(rndMaxRecs = it)) },
            )
        }

        // ── Direct 엔진 전용 (페이지 캐시 우회 — 병렬/반복이 의미 있는 축) ──
        GroupLabel("NativeIO · fio 전용 (O_DIRECT)")

        SettingSection(title = "Direct I/O", icon = Icons.Default.Build) {
            ChipSetting(
                label = "Engine (psync = QD1 동기 · libaio = 비동기 deep QD, numjobs 1 고정)",
                options = listOf(0, 1),
                display = { if (it == 1) "libaio" else "psync" },
                current = if (config.fioEngine == "libaio") 1 else 0,
                onChange = { onConfigChange(config.copy(fioEngine = if (it == 1) "libaio" else "psync")) },
            )
            if (config.fioEngine == "libaio") {
                ChipSetting(
                    label = "Sequential QD (iodepth — 저QD로 포화)",
                    options = listOf(1, 2, 4, 8, 16),
                    current = config.seqIoDepth,
                    onChange = { onConfigChange(config.copy(seqIoDepth = it)) },
                )
                ChipSetting(
                    label = "Random QD (iodepth — UFS 큐 포화 지점 부근)",
                    options = listOf(8, 16, 32, 64, 128),
                    current = config.rndIoDepth,
                    onChange = { onConfigChange(config.copy(rndIoDepth = it)) },
                )
            }
            ChipSetting(
                label = "Threads (Random 병렬 job — libaio에선 합산 QD = Threads × QD)",
                options = listOf(1, 2, 4, 8, 16, 32),
                current = config.numThreads,
                onChange = { onConfigChange(config.copy(numThreads = it)) },
            )
            ChipSetting(
                label = "Iterations (평균 낼 반복 횟수 — mmap 미적용)",
                options = listOf(1, 3, 5, 10),
                current = config.numIterations,
                onChange = { onConfigChange(config.copy(numIterations = it)) },
            )
        }

        // ── mmap 엔진 전용 (페이지 캐시 경유 — fault 회계) ──
        GroupLabel("mmap 전용 (page cache · RAF/WAF)")

        SettingSection(title = "mmap", icon = Icons.Default.Memory) {
            ChipSetting(
                label = "Readahead (OFF = fault당 1페이지 → 페이지 크기 기준선 · ON = 커널 기본 선읽기)",
                options = listOf(0, 1),
                display = { if (it == 1) "ON (realistic)" else "OFF (pure page)" },
                current = if (config.mmapReadaheadOn) 1 else 0,
                onChange = { onConfigChange(config.copy(mmapReadaheadOn = it == 1)) },
            )
            Text(
                "numjobs=1 · 1-pass 고정 — 다중 스레드/반복이 끼면 major fault ↔ ops 대응이 깨져 " +
                        "RA/bytes-per-fault 회계가 비결정적이 됨. 통제 가능한 축은 block size · " +
                        "ops 수 · readahead뿐.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ── Campaign (일반 사용자 모드) ──
        GroupLabel("Campaign (trim 없이 N회 연속 — 일반 사용자 관점)")

        SettingSection(title = "Campaign", icon = Icons.Default.Refresh) {
            ChipSetting(
                label = "Campaign 모드 (fio 단일 벤치 전용 — ALL/SQLite 제외)",
                options = listOf(0, 1),
                display = { if (it == 1) "ON" else "OFF" },
                current = if (config.campaignMode) 1 else 0,
                onChange = { onConfigChange(config.copy(campaignMode = it == 1)) },
            )
            if (config.campaignMode) {
                ChipSetting(
                    label = "Runs (반복 횟수)",
                    options = listOf(5, 10, 20),
                    current = config.campaignRuns,
                    onChange = { onConfigChange(config.copy(campaignRuns = it)) },
                )
                ChipSetting(
                    label = "Term (런 간 대기 — 고정값 자체가 통제 조건)",
                    options = listOf(0, 30, 60, 120),
                    display = { "${it}s" },
                    current = config.campaignTermSec,
                    onChange = { onConfigChange(config.copy(campaignTermSec = it)) },
                )
            }
            ChipSetting(
                label = "CPU pinning (direct measure를 mid+big 4-7 고정)",
                options = listOf(0, 1),
                display = { if (it == 1) "ON" else "OFF" },
                current = if (config.cpuPinning) 1 else 0,
                onChange = { onConfigChange(config.copy(cpuPinning = it == 1)) },
            )
        }

        // ── SQLite only ──
        GroupLabel("SQLite only")

        SettingSection(title = "SQLite", icon = Icons.Default.Storage) {
            ChipSetting(
                label = "Operations (Insert/Update/Delete each)",
                options = listOf(256, 512, 1024, 2048),
                current = config.sqliteOperations,
                onChange = { onConfigChange(config.copy(sqliteOperations = it)) },
            )
        }

        // Validation
        val warning = validateConfig(config)
        if (warning != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    "⚠ $warning",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp, start = 4.dp),
    )
}

@Composable
private fun SummaryCard(config: BenchmarkConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.DataUsage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Current config",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "${config.fileSizeKb / 1024} MB × ${config.numThreads} threads",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Seq ${config.seqReclenKb}KB · Rnd ${config.rndReclenKb}KB · " +
                            "${config.numIterations} iters · " +
                            "${config.rndMaxRecs} rnd ops",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipSetting(
    label: String,
    options: List<Int>,
    current: Int,
    onChange: (Int) -> Unit,
    display: ((Int) -> String)? = null,
    unitLabel: String = "",
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { value ->
                val text = display?.invoke(value) ?: "$value$unitLabel"
                val isSelected = current == value
                FilterChip(
                    selected = isSelected,
                    onClick = { onChange(value) },
                    label = {
                        Text(
                            text,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
    }
}

fun validateConfig(c: BenchmarkConfig): String? {
    if (c.seqReclenKb <= 0 || (c.seqReclenKb and (c.seqReclenKb - 1)) != 0)
        return "Seq buffer must be power of 2"
    if (c.rndReclenKb <= 0 || (c.rndReclenKb and (c.rndReclenKb - 1)) != 0)
        return "Rnd buffer must be power of 2"
    if (c.seqReclenKb >= c.fileSizeKb)
        return "Seq buffer must < file size"
    if (c.rndReclenKb.toLong() * c.rndMaxRecs > c.fileSizeKb)
        return "Rnd buffer × ops > file size"
    if (c.numThreads !in 1..32)
        return "Threads must be 1~32"
    if (c.sqliteOperations < 1)
        return "SQLite ops ≥ 1"
    if (c.numIterations < 1)
        return "Iterations ≥ 1"
    // total disk usage warning (8 thread × 4 GB = 32 GB 등)
    val totalUsageMB = c.numThreads.toLong() * c.fileSizeKb / 1024
    if (totalUsageMB > 32 * 1024)
        return "Total file usage ${totalUsageMB / 1024} GB — check device free space"
    return null
}
