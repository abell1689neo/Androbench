// Bridge between BenchmarkRunner and the UI
package kr.ac.snu.csl.androbench.bench

import android.app.Application
import android.util.Log
import android.system.OsConstants
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.ac.snu.csl.androbench.history.HistoryRepository
import kr.ac.snu.csl.androbench.ui.VolumeOption
import kr.ac.snu.csl.androbench.bench.BenchmarkState

class BenchmarkViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<BenchmarkState>(BenchmarkState.Idle)
    val state: StateFlow<BenchmarkState> = _state.asStateFlow()

    private val runner = BenchmarkRunner(application)
    private val historyRepo = HistoryRepository(application)

    fun startBenchmark(config: BenchmarkConfig, volume: VolumeOption) {
        if (_state.value is BenchmarkState.Running) return

        //campaign 모드면 N회 반복 루프로 분기 (fio 모드 전용)
        if (config.campaignMode && config.mode.name.startsWith("FIO_")) {
            viewModelScope.launch { runCampaign(config, volume) }
            return
        }

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
    
    //for campaign
    private fun pageKb(): Int= (android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE)/1024L).toInt()

    //pick representative data
    private fun primaryOf(mode: BenchmarkMode, r: BenchmarkResult): Pair<String, Double> = when (mode) {
        BenchmarkMode.FIO_SEQ_READ,  BenchmarkMode.FIO_MMAP_SEQ_READ  -> "MB/s" to r.fioReadMBps
        BenchmarkMode.FIO_SEQ_WRITE, BenchmarkMode.FIO_MMAP_SEQ_WRITE -> "MB/s" to r.fioWriteMBps
        BenchmarkMode.FIO_RND_READ,  BenchmarkMode.FIO_MMAP_RND_READ  -> "IOPS" to r.fioReadIops
        BenchmarkMode.FIO_RND_WRITE, BenchmarkMode.FIO_MMAP_RND_WRITE -> "IOPS" to r.fioWriteIops
        else -> "MB/s" to 0.0   //native/ALL은 campaign 대상 아님
    }

    private suspend fun runCampaign(config: BenchmarkConfig, volume: VolumeOption){
        val primaries= mutableListOf<Double>()
        var lastResult: BenchmarkResult?=null //최종 통계
        try{
            for (i in 1..config.campaignRuns){
                //for configured times, 단일 run 동일하게 시행
                val result=runner.run(config){phase, idx,total, iter, totalIter ->
                    _state.value=BenchmarkState.Running(
                        phase="[$i/${config.campaignRuns}] $phase",
                        phaseIndex=idx, totalPhases=total,
                        iteration=iter, totalIterations=totalIter,
                    )
                }

                lastResult=result//이거보다 좋은 방법이 있을텐데...
                primaries+=primaryOf(config.mode, result).second//list of results(double)

                //개별 run 저장
                try{
                    historyRepo.saveRun(config, result, volume.fsType, volume.readOnly, pageKb())
                }catch(e:Exception){
                    Log.e("BenchmarkVM", "History save failed", e)
                }
                //대기
                if(i<config.campaignRuns){
                    for( s in config.campaignTermSec downTo 1){
                        _state.value=BenchmarkState.Running(
                            phase="term - ${s}s left until the next run",
                            phaseIndex=i, totalPhases=config.campaignRuns,
                            iteration=i, totalIterations=config.campaignRuns
                        )
                        kotlinx.coroutines.delay(1000)//wait 1 sec.
                    }
                }
            }
            //summarized stats
            val label =primaryOf(config.mode, lastResult!!).first//?
            val mean=primaries.average()
            val sd= if(primaries.size>1) kotlin.math.sqrt(primaries.sumOf { (it - mean) * (it - mean) } / (primaries.size - 1)) //표준편차
                    else 0.0
            val summary=CampaignSummary(runs=primaries.size, 
                        termSec=config.campaignTermSec, 
                        primaryLabel=label, mean=mean, 
                        cvPercent=if(mean>0) sd/mean*100 else 0.0, 
                        min=primaries.min(), 
                        max=primaries.max(), 
                        perRun=primaries)
            //last run result
            _state.value=BenchmarkState.Done(lastResult.copy(campaign=summary))

        } catch (e: Exception) {
             _state.value = BenchmarkState.Error(e.message ?: "Unknown Error")
        }
    }
}
