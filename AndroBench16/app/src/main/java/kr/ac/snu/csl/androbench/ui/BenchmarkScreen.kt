package kr.ac.snu.csl.androbench.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Card
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.FlowRow
import kotlinx.coroutines.selects.select
import kr.ac.snu.csl.androbench.bench.BenchDbHelper
import kr.ac.snu.csl.androbench.bench.BenchmarkConfig
import kr.ac.snu.csl.androbench.bench.BenchmarkResult
import kr.ac.snu.csl.androbench.bench.BenchmarkState
import kr.ac.snu.csl.androbench.bench.BenchmarkViewModel

@Composable //this function draws ui
fun BenchmarkScreen(){
    val context=LocalContext.current
    val vm: BenchmarkViewModel=viewModel()
    val state by vm.state.collectAsState()

    //volume select
    val volumes = remember{VolumeDetector.detect(context)}
    var selectedIdx by remember{ mutableIntStateOf(0) }

    //config
    var config by remember{
        mutableStateOf(BenchmarkConfig(targetPath = volumes[0].path)) //assign path to benchmark
    }

    //volume 변경 시 config target path 자동 update
    LaunchedEffect(selectedIdx) {
        config = config.copy(targetPath = volumes[selectedIdx].path)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ){
        Text(
            text="Androbench 16",
            style=MaterialTheme.typography.headlineMedium,
        )

        VolumeSelector(
            volumes=volumes,
            selected=selectedIdx,
            onSelect = {selectedIdx=it},
        )

        SettingsCard(
            config=config,
            onConfigChange = {config=it},
        )

        when (val s=state){
            is BenchmarkState.Idle -> IdleView(
                config=config,
                onStart = {
                    vm.startBenchmark(config, volumes[selectedIdx])

                }
            )
            is BenchmarkState.Running->RunningView(s)
            is BenchmarkState.Done->DoneView(s.result, onReset={vm.reset()})
            is BenchmarkState.Error->ErrorView(s.message, onReset ={vm.reset()})
        }
    }

}

@Composable
private fun IdleView(
    config: BenchmarkConfig,
    onStart:()->Unit
){
    val warning=validateConfig(config)
    //vertically arrange containers
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ready to benchmark.")
        Text(
            "${config.numThreads}f · ${config.fileSizeKb / 1024}MB/file · " +
                    "Seq ${config.seqReclenKb}KB / Rnd ${config.rndReclenKb}KB · " +
                    "${config.sqliteOperations} SQLite · ${config.numIterations} iters",
            style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = onStart,
            enabled=warning==null//disable when config doesn't match
        ){
            Text("Start Benchmark")
        }

        if (warning != null) {
            Text(
                "Cannot start: $warning",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RunningView(s: BenchmarkState.Running){
    Column(
        modifier= Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ){
        CircularProgressIndicator()
        Text("Phase: ${s.phase}")
        Text(
            "Usually takes 1-3 minutes",
            style= MaterialTheme.typography.bodySmall,
        )
    }
}
@Composable
private fun DoneView(result: BenchmarkResult, onReset:()->Unit){
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)){
        Text("Results", style = MaterialTheme.typography.titleLarge)

        ResultRow("Sequential Read", "${"%.1f".format(result.seqReadMBps)} MB/s")
        ResultRow("Sequential Write", "${"%.1f".format(result.seqWriteMBps)} MB/s")
        ResultRow(
            "Random Read",
            "${"%.1f".format(result.rndReadMBps)} MB/s · ${"%.0f".format(result.rndReadIops)} IOPS",
        )
        ResultRow(
            "Random Write",
            "${"%.1f".format(result.rndWriteMBps)} MB/s · ${"%.0f".format(result.rndWriteIops)} IOPS",
        )
        ResultRow("SQLite Insert", "${"%.1f".format(result.sqliteInsertTps)} TPS")
        ResultRow("SQLite Update", "${"%.1f".format(result.sqliteUpdateTps)} TPS")
        ResultRow("SQLite Delete", "${"%.1f".format(result.sqliteDeleteTps)} TPS")

        Button(onClick=onReset){
            Text("Run Again")
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String){
    Row(
        modifier= Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ){
        Text(label, fontWeight= FontWeight.SemiBold)
        Text(value, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ErrorView(message:String, onReset: ()->Unit){
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)){
        Text(
            "Error",
            style=MaterialTheme.typography.titleLarge,
            color= MaterialTheme.colorScheme.error,
        )
        Text(message)
        Button(onClick=onReset){
            Text("Reset")
        }
    }
}