# NDK maps jni.c to jni.so

cmake_minimum_required(VERSION 3.22.1)

project("androbench_jni")

add_library(
    androbench_jni #build result name: libandrobench_jni.so
    SHARED #dynamic
    androbench_jni.c #source to compile
)

find_library(log-lib log) #find system library "liglog.so" and save in log-lib

target_link_libraries(
    androbench_jni
    ${log-lib} #for printing
)