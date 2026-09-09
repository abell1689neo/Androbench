//app accessing wrapper
package kr.ac.snu.csl.androbench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import kr.ac.snu.csl.androbench.ui.AppScaffold
import kr.ac.snu.csl.androbench.ui.BenchmarkScreen

class MainActivity: ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) { //app 시작 시 호출하는 method
        super.onCreate(savedInstanceState)

        //벤치/campaign 중 화면 꺼짐→doze로 런이 얼어붙는 사고 방지 (wake lock의 앱 버전)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            //화면으로 렌더할 compose tree
            MaterialTheme{//theme value (default)
                Surface(modifier=Modifier.fillMaxSize()){
                    AppScaffold()
                }
            }
        }
    }
}