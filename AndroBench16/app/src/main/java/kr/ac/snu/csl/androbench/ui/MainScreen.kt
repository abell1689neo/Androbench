package kr.ac.snu.csl.androbench.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.snu.csl.androbench.bench.BenchmarkConfig
import kr.ac.snu.csl.androbench.bench.BenchmarkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold() {
    var selectedTab by remember { mutableIntStateOf(0) }

    val vm: BenchmarkViewModel = viewModel()
    val state by vm.state.collectAsState()

    val context = LocalContext.current
    val volumes = remember { VolumeDetector.detect(context) }
    var selectedVolumeIdx by remember { mutableIntStateOf(0) }

    var config by remember {
        mutableStateOf(BenchmarkConfig(targetPath = volumes[0].path))
    }
    LaunchedEffect(selectedVolumeIdx) {
        config = config.copy(targetPath = volumes[selectedVolumeIdx].path)
    }

    val tabs = listOf(
        TabItem("Benchmark", Icons.Default.PlayArrow),
        TabItem("Result",    Icons.Default.Assessment),
        TabItem("History",   Icons.Default.History),
        TabItem("Settings",  Icons.Default.Settings),
    )

    Scaffold(
        topBar = { HeroHeader() },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> BenchmarkTab(
                    volumes = volumes,
                    selectedIdx = selectedVolumeIdx,
                    onVolumeSelect = { selectedVolumeIdx = it },
                    config = config,
                    state = state,
                    onStart = { mode ->
                        vm.startBenchmark(config.copy(mode = mode), volumes[selectedVolumeIdx])
                    },
                    onReset = { vm.reset() },
                )
                1 -> ResultTab(state = state)
                2 -> HistoryTab()
                3 -> SettingsTab(config = config, onChange = { config = it })
            }
        }
    }
}

data class TabItem(val label: String, val icon: ImageVector)

@Composable
private fun HeroHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    "Androbench 16",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    "Storage performance benchmark",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
            }
        }
    }
}
