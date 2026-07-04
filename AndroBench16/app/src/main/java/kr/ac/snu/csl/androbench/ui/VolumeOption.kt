package kr.ac.snu.csl.androbench.ui

data class VolumeOption(
    val label: String, //ui 내에 어느 볼륨인지 표시
    val path: String, //actual paths
    val fsType: String="?", //proc/mount에서 직접 추출해서 파일시스템 저장
    val freeKb: Long=0, //statfs
    val readOnly: Boolean=false,
)