package kr.ac.snu.csl.androbench.bench

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import java.io.File

//측정 대상 파일 준비 (fio qd1 seq write)
//TLC 정착용

class FioPrep(private val context: Context, private val targetPath: String){
    /**
     * @param names 파일 이름들 (targetPath 기준 상대명)
     * @param sizeKb 파일당 크기
     * @return prep을 실제로 수행한 파일 수
     */
    suspend fun ensure(
        names: List<String>,
        sizeKb: Int,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): Int {
        val wantBytes = sizeKb.toLong() * 1024L
        var deleted = false

        //1) 크기 불일치/부재 판별 (부재는 생성으로, 불일치는 삭제 후 생성으로)
        for (n in names) {
            val f = File(targetPath, n)
            if (f.exists() && f.length() != wantBytes) { f.delete(); deleted = true }
        }
        if (deleted) {
            Log.i(TAG, "size-mismatch files deleted -> settle 60s")
            delay(60_000)   //삭제폭풍 가드
        }

        //2) 전 파일 무조건 QD1 seq write (존재해도 overwrite = 상태 강제 리셋)
        //   반드시 순차로! (동시 prep = layout 인터리브)
        names.forEachIndexed { i, n ->
            onProgress(i, names.size)
            FioRunner(context).run(listOf(
                "--name=prep",
                "--rw=write",
                "--bs=1m",
                "--size=${sizeKb}k",
                "--filename=$targetPath/$n",
                "--direct=1",
                "--ioengine=psync",
                "--thread",
                "--fallocate=none",
            ))
            onProgress(i + 1, names.size)
        }
        return names.size
    }

    /**
     * write 모드용: prep 대신 prefix/이름 일치 파일 전부 삭제 + settle.
     * WB가 깨끗한 fresh 상태에서 측정 시작 → absorb→knee→lockstep 곡선 온전히 관측.
     * (prep을 쓰면 prep 자체가 WB를 채워서 hill config에서 absorb plane이 사라짐)
     */
    suspend fun freshen(
        prefixes: List<String>,
        exact: List<String> = emptyList(),
        settleSec: Int = 60,
        onProgress: (sec: Int, total: Int) -> Unit = { _, _ -> },
    ) {
        val victims = (File(targetPath).listFiles() ?: emptyArray()).filter { f ->
            prefixes.any { f.name.startsWith(it) } || f.name in exact
        }
        victims.forEach { it.delete() }
        if (victims.isEmpty()) return   //지울 게 없으면 discard 폭풍도 없음 → settle 생략
        Log.i(TAG, "freshen: ${victims.size} files deleted -> settle ${settleSec}s")
        for (s in 1..settleSec) {
            onProgress(s, settleSec)
            delay(1_000)
        }
    }
    companion object { private const val TAG = "FioPrep" }
}