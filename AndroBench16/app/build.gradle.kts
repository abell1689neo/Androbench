//무슨 앱 사용하는지 결정

//plugin실제 적용
plugins{
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

//안드로이드 빌드 설정
android{
    namespace="kr.ac.snu.csl.androbench"
    compileSdk=36 //android api version

    defaultConfig{
        applicationId="kr.ac.snu.csl.androbench"
        minSdk=29
        targetSdk=36 //android 16
        versionCode=1
        versionName="1.0"

        ndk{
            abiFilters+=listOf("arm64-v8a") //pixel10: arm64
        }

        externalNativeBuild{ //build시 컴파일러 전달 flag
            cmake{
                cFlags+=listOf("-std=c11", "-Wall", "-O2")
                arguments+="-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON"
            }
        }
    }

    externalNativeBuild{//cmake build script
        cmake{
            path=file("src/main/cpp/CMakeLists.txt")
            version="3.22.1"
        }
    }

    buildTypes{
        release{
            isMinifyEnabled=false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug{
            isDebuggable=true
        }
    }

    compileOptions{
        sourceCompatibility=JavaVersion.VERSION_17
        targetCompatibility=JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget="17"
    }
    buildFeatures {
        compose=true
    }

}

dependencies {
        implementation("androidx.compose.ui:ui:1.11.3")
    //BOM: compose library version을 한번에 맞춰줌
        val composeBom= platform("androidx.compose:compose-bom:2024.10.01")
        implementation(composeBom)

        //activity+compose
        implementation("androidx.activity:activity-compose:1.9.3")

        //view model
        implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

        // Compose UI (버전은 BOM이 결정)
        implementation("androidx.compose.ui:ui")
        implementation("androidx.compose.ui:ui-graphics")
        implementation("androidx.compose.material3:material3")
        implementation("androidx.compose.ui:ui-tooling-preview")
        implementation("androidx.compose.material:material-icons-extended")

        // 개발 중 Preview 패널에서만 쓰임 (릴리스 APK 제외)
        debugImplementation("androidx.compose.ui:ui-tooling")

        //room(local history 기록 용)
        val roomVersion="2.6.1"
        implementation("androidx.room:room-runtime:$roomVersion")
        implementation("androidx.room:room-ktx:$roomVersion")
        ksp("androidx.room:room-compiler:$roomVersion") //compile only
}