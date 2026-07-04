// Bridge between BenchmarkRunner and the UI
package kr.ac.snu.csl.androbench.bench

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.ac.snu.csl.androbench.history.HistoryRepository
import kr.ac.snu.csl.androbench.ui.VolumeOption

class BenchmarkViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<BenchmarkState>(BenchmarkState.Idle)
    val state: StateFlow<BenchmarkState> = _state.asStateFlow()

    private val runner = BenchmarkRunner(application)
    private val historyRepo = HistoryRepository(application)

    fun startBenchmark(config: BenchmarkConfig, volume: VolumeOption) {
        if (_state.value is BenchmarkState.Running) return

        viewModelScope.launch {
            _state.value = BenchmarkState.Running(
                phase = "Preparing",
                phaseIndex = 0,
                totalPhases = 1,
                iteration = 0,
                totalIterations = 0,
            )
            try {
                val result = runner.run(config) { phase, idx, total, iter, totalIter ->
                    _state.value = BenchmarkState.Running(
                        phase = phase,
                        phaseIndex = idx,
                        totalPhases = total,
                        iteration = iter,
                        totalIterations = totalIter,
                    )
                }

                try {
                    val rowId = historyRepo.saveRun(
                        config = config,
                        result = result,
                        fsType = volume.fsType,
                        readOnly = volume.readOnly,
                        kernelPageKb = (android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE) / 1024L).toInt(), // TODO: 자동 감지
                    )
                    Log.i("History", "saved id=$rowId, total count=${historyRepo.count()}")
                } catch (e: Exception) {
                    Log.e("BenchmarkVM", "History save failed", e)
                }

                _state.value = BenchmarkState.Done(result)
            } catch (e: Exception) {
                _state.value = BenchmarkState.Error(e.message ?: "Unknown Error")
            }
        }
    }

    fun reset() {
        _state.value = BenchmarkState.Idle
    }
}
