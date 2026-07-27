// Native I/O benchmark core
// Ported from release/4.1/jni/Interface_JNI.c
//
// Major changes:
//  - Time unified to ns (clock_gettime CLOCK_MONOTONIC)
//  - O_DIRECT failure -> posix_fadvise(DONTNEED) fallback
//  - JNI symbols use new package: kr.ac.snu.csl.androbench.NativeIO
//  - Removed dead code (PURGE_CACHE shells, MACRO replay, SHUFFLE ifdef)
//  - Errors as negative jlong with stable codes

#define _GNU_SOURCE
#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <pthread.h>
#include <sched.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <time.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <android/log.h>

#define LOG_TAG "AndroBenchJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define KB           (1024L)
#define MB           (1024L * 1024L)
#define NS_PER_SEC   (1000000000LL)
#define MAX_THREADS  (32)
#define MAX_PATH_LEN (1024)

// Error codes returned as negative jlong (kept in sync with Kotlin)
#define ERR_OPEN     (-1L)
#define ERR_READ     (-2L)
#define ERR_WRITE    (-3L)
#define ERR_ALLOC    (-4L)

/*helpers*/
static long long now_ns(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (long long)ts.tv_sec *NS_PER_SEC +ts.tv_nsec; //return current nanosecond 
}

static char* alloc_aligned(size_t bytes) {//페이지 단위 정렬된 메모리 얻어옴
    void* p=mmap(NULL, bytes,  
            PROT_READ|PROT_WRITE, 
            MAP_PRIVATE|MAP_ANON, -1, 0);
    return (p==MAP_FAILED)? NULL: (char*)p;
}
static void free_aligned(char *p, size_t bytes){
    if(p) munmap(p, bytes);
}

static int pwrite_all(int fd, const char* buf, size_t len, off_t offset){
    size_t done=0;
    while(done<len){
        ssize_t r=pwrite(fd,buf+done, len-done, offset+(off_t)done);
        if(r<0) return-1;
        done+=(size_t)r;
    }
    return 0;
}

static int pread_all(int fd, char* buf, size_t len, off_t offset){
    size_t done=0;
    while(done<len){
        ssize_t r=pread(fd, buf+done, len-done, offset+(off_t)done);
        if(r<=0){
            LOGE("pread_all FAILED: r=%zd errno=%d (%s) fd=%d len=%zu done=%zu offset=%lld",
                 r, errno, strerror(errno), fd, len, done, (long long)(offset+done));
            return-1;
        }
        done+=(size_t)r;
    }
    return 0;
}

static int write_all(int fd, const char* buf, size_t len){ //partial write
    size_t done=0;
    while(done<len){
        ssize_t r=write(fd, buf+done, len-done);
        if(r<0) return -1;
        done+=(size_t)r;
    }
    return 0;
}

static int read_all(int fd, char* buf, size_t len){
    size_t done=0;
    while(done<len){
        ssize_t r=read(fd, buf+done, len-done);
        if(r<=0){
            LOGE("read_all FAILED: r=%zd errno=%d (%s) fd=%d len=%zu done=%zu bufptr=%p",
                 r, errno, strerror(errno), fd, len, done, buf+done);
            return -1;
        }
        done+=(size_t)r;

    }
    return 0;
}

//try direct access
static int open_with_direct (const char* path, int rw_flags, int* used_direct){
    int fd=open(path, rw_flags|O_DIRECT);
    if(fd>=0){
        *used_direct=1;
        LOGI("open_with_direct: O_DIRECT OK   path=%s", path);
        return fd;
    } //success
    if(errno!=EINVAL) {
        LOGE("open_with_direct: open failed errno=%d (%s)", errno, strerror(errno));
        return -1; //real error}
    }

    LOGI("open_with_direct: O_DIRECT EINVAL → fallback   path=%s", path);
    fd=open(path, rw_flags);
    if(fd<0) {
        LOGE("open_with_direct: fallback open failed errno=%d", errno);
        return -1;
    }
    *used_direct=0;
    //삭제
    posix_fadvise(fd, 0, 0, POSIX_FADV_DONTNEED); //don't save in page cache, mimics O_DIRECT
    return fd;
}

/* JNI: init */
JNIEXPORT jint JNICALL //가시성 제어, 호출 규약
Java_kr_ac_snu_csl_androbench_bench_NativeIO_initFiles( //경로, 파일, 스레드 수 
    JNIEnv* env, jobject thiz, 
    jstring path, jint file_size_kb, jint num_thread
){
    //1. jva string->C string
    const char* dir=(*env)->GetStringUTFChars(env, path, NULL);
    if(!dir) return ERR_OPEN;

    //2. 1MB buffer
    const int reclen=(int)(1*MB); //빠르게 채우기
    char* buf=(char*)malloc((size_t)reclen);//allocate
    if(!buf){
        (*env)->ReleaseStringUTFChars(env,path, dir);//clean dir
        return ERR_ALLOC;
    }
    memset(buf, 0x5A, (size_t)reclen); //avoid compression ->이거 확인 필요

    //3. cleanup 
    int rc=0;
    char filename[MAX_PATH_LEN];
    long total_bytes=(long)file_size_kb*KB;

    for(int k=0; k<num_thread; k++){ //number of files to create
        snprintf(filename, sizeof filename, "%s/%d", dir, k);
        int fd=open(filename, O_WRONLY|O_CREAT|O_TRUNC, 0666);

        //fill in the files
        long written=0;
        while(written<total_bytes){
            size_t chunk=(size_t)((total_bytes-written)<reclen
                        ? (total_bytes-written):reclen);
            if(write_all(fd, buf, chunk)<0){
                close(fd); rc=ERR_WRITE; goto out;
            }
            written +=(long)chunk;
        }
        fsync(fd);//flush 
        close(fd); 
    }
    out:
        free(buf);
        (*env)->ReleaseStringUTFChars(env, path, dir);
        return rc;

}

JNIEXPORT jint JNICALL
Java_kr_ac_snu_csl_androbench_bench_NativeIO_finalizeFiles( //unlink created files
        JNIEnv* env, jobject thiz,
        jstring path, jint num_thread
){
    const char *dir = (*env)->GetStringUTFChars(env, path, NULL);
    if(!dir) return ERR_OPEN;

    char filename[MAX_PATH_LEN];
    for(int k=0; k<num_thread; k++){
        snprintf(filename, sizeof filename, "%s/%d", dir, k);
        unlink(filename); //posix syscall, removes file from directory (managing refcnt)
    }
    (*env)->ReleaseStringUTFChars(env, path, dir);
    return 0;
}

//===========================
// 1. sequential read/write
//===========================
//N개의 파일, 큰 chunk (32KB)로 끝까지 read/write 하는 데 걸린 시간
//num_thread: number of files, reclen_kb: chunk size(32KB)
//mode
static jlong seq_access(JNIEnv* env, jstring path, 
                        int reclen_kb, int filesize_kb, 
                        int num_thread, int is_write
){
    //1. setup
    const char *dir=(*env)->GetStringUTFChars(env, path, NULL);
    if(!dir) return ERR_OPEN;

    int reclen=reclen_kb*(int)KB;   //def 32*1024 bytes
    int recs_per_file=filesize_kb/reclen_kb; //65536/32=2048 chunks
    int* pfd=(int*)calloc((size_t)num_thread, sizeof(int)); //fd 저장 배열
    char* buf=alloc_aligned((size_t)reclen); //페이지 정렬된 32KB buffer (O_DIRECT)

    if(!pfd||!buf){
        free(pfd);
        free_aligned(buf, (size_t)reclen);
        (*env)->ReleaseStringUTFChars(env, path, dir);
        return ERR_ALLOC;
    }
    memset(buf, 0x5A, (size_t)reclen);

    //2. pick mode
    int base_flags=is_write? O_WRONLY:O_RDONLY;
    //need dsync, since real latency after writing to the disk should be measured
    char filename[MAX_PATH_LEN];
    int used_direct=0;

    for(int k=0; k<num_thread; k++){
        snprintf(filename, sizeof filename, "%s/%d", dir, k);
        int fd=open_with_direct(filename, base_flags, &used_direct);
        if(fd<0){
            //error: close all the fds currently opened
            for(int j=0; j<k; j++) close(pfd[j]);
            free(pfd); free_aligned(buf, (size_t)reclen);
            (*env)->ReleaseStringUTFChars(env, path, dir);
            return ERR_OPEN;
        }
        pfd[k]=fd;
    }
    //sync(); //디스크 동기화

    //3. measurement
    long long start_ns=now_ns();
    //ver1: thread 개수만큼 file 순회 반복
    /*
    for(int k=0; k<num_thread; k++){//file 개수
        for(int i=0; i<recs_per_file; i++){//모든 chunk동안 반복 (def 32KB씩)
            int rc=is_write? write_all(pfd[k], buf, (size_t)reclen)
                            :read_all(pfd[k], buf, (size_t)reclen);
            if(rc<0){
                //cleanup
                for(int j=0; j<num_thread; j++) close(pfd[j]);
                free(pfd); free_aligned(buf, (size_t) reclen);
                (*env)->ReleaseStringUTFChars(env, path, dir);
                return is_write ? ERR_WRITE: ERR_READ;
            }
        }
    }
    */
    //ver2: file 1개만 사용 (0번 파일)
    for(int i=0 ; i<recs_per_file; i++){
        int rc=is_write? write_all(pfd[0], buf, (size_t)reclen)
                       :read_all(pfd[0], buf, (size_t)reclen);
        if(rc<0){
            //cleanup
            for(int j=0; j<num_thread; j++) close(pfd[j]);
            free(pfd); free_aligned(buf, (size_t) reclen);
            (*env)->ReleaseStringUTFChars(env, path, dir);
            return is_write ? ERR_WRITE: ERR_READ;
        }
    }
    if(is_write){
        fdatasync(pfd[0]);
    }
    long long elapsed_ns=now_ns()-start_ns;

    //4. cleanup
    for(int k=0; k<num_thread; k++) close(pfd[k]);
    free(pfd);
    free_aligned(buf, (size_t)reclen);
    (*env)->ReleaseStringUTFChars(env, path, dir);

    return (jlong)elapsed_ns;
}

//wrapper functions
JNIEXPORT jlong JNICALL
Java_kr_ac_snu_csl_androbench_bench_NativeIO_seqRead(
    JNIEnv* env, jobject thiz,
    jstring path, jint reclen_kb, jint filesize_kb, jint num_thread
){
    return seq_access(env, path, reclen_kb, filesize_kb, num_thread, 0);
}

JNIEXPORT jlong JNICALL
Java_kr_ac_snu_csl_androbench_bench_NativeIO_seqWrite(
    JNIEnv* env, jobject thiz,
    jstring path, jint reclen_kb, jint filesize_kb, jint num_thread
){
    return seq_access(env, path, reclen_kb, filesize_kb, num_thread, 1);
}

//===========================
// 2. random read/write
//===========================

//per thread data structure
typedef struct{
    int fd;         //file fd
    int reclen;     //record size (4KB)
    int maxrecs;    //최대 몇개 처리? (2048)
    int* offsets;   //random offset
    char* buf;      //전용 read/write vuffer
    volatile int recs_done; //처리한 record 수
    int is_write;
    int64_t* latencies; // thread별 매 op latency 저장
} ThreadData;

//manual start-barrier
//pthread_barrier_t 는 NDK에서 일관성 부족? 
static volatile int g_barrier_status;   //counter
static volatile int g_done_flag;         //finished
static pthread_mutex_t g_barrier_mutex=PTHREAD_MUTEX_INITIALIZER;  
static const int BARRIER_FIRE = 0x7FFFFFFF; //매우 큰 값
static jlong g_last_rec_count;
static int64_t *g_last_latencies=NULL; //array for latency
static jsize g_last_latencies_count=0;


//worker function (executed by each thread)
static void* rnd_worker(void* arg){
    ThreadData* td=(ThreadData*)arg;

    //1. start barrier: current thread ready
    pthread_mutex_lock(&g_barrier_mutex);
    g_barrier_status++;
    pthread_mutex_unlock(&g_barrier_mutex);

    //2. wait for init signal
    while(g_barrier_status<BARRIER_FIRE && !g_done_flag){
        sched_yield();
    }

    //3. IO occurs: start concurrently on main signal
    struct timespec t1, t2;
    for(int i=0; i<td->maxrecs&&!g_done_flag; i++){ //until maxrecs amount is processed
        off_t offset = (off_t)td->offsets[i] * (off_t)td->reclen;
        clock_gettime(CLOCK_MONOTONIC, &t1);//start time
        int rc=td->is_write
            ? pwrite_all(td->fd, td->buf, (size_t)td->reclen, offset) //random read
            : pread_all (td->fd, td-> buf, (size_t)td->reclen, offset);

        clock_gettime(CLOCK_MONOTONIC, &t2);
        
        if(rc<0){
            g_done_flag=1;
            return (void*)(intptr_t)(td->is_write? ERR_WRITE:ERR_READ);
        }
        int64_t ns=(int64_t)(t2.tv_sec-t1.tv_sec)*1000000000LL
                +(int64_t)(t2.tv_nsec-t1.tv_nsec);
        td->latencies[td->recs_done]=ns;
        td->recs_done++;
    }
    return NULL;

}

static int* gen_random_offsets(int recs_per_file, int maxrecs, int seed){
    int* all =(int*)malloc((size_t)recs_per_file*sizeof(int));
    if(!all) return NULL;

    //init: [0, 1, 2, ..., recs_per_file-1]
    for(int i=0; i<recs_per_file; i++) all[i]=i;

    //shuffle
    srand48((long)seed);
    for(int i=recs_per_file-1; i>0;i--){
        int j=(int)(lrand48()%(i+1));
        //swap
        int tmp=all[i];
        all[i]=all[j];
        all[j]=tmp;
    }
    //maxrecs개만 떼어서 반환
    int* off=(int*)malloc((size_t)maxrecs*sizeof(int));
    if(!off){free(all); return NULL;}
    int n=(maxrecs<recs_per_file)? maxrecs:recs_per_file;
    memcpy(off, all, (size_t)n*sizeof(int));
    free(all);
    /*
    int* off=(int*)malloc((size_t)maxrecs*sizeof(int)); //int ptr array to store offset for each rec
    if(!off) return NULL;

    srand48((long)seed);

    for(int i=0; i<maxrecs; i++){
        off[i]=(int)(lrand48()%recs_per_file);
    }*/
    return off;
}

static jlong rnd_access(JNIEnv* env, jstring path,
                        int reclen_kb, int filesize_kb, int maxrecs,
                        int seed, int num_thread, int is_write
){
    //1. setup
    if(num_thread>MAX_THREADS) return ERR_ALLOC;

    const char* dir=(*env)->GetStringUTFChars(env, path, NULL);
    if(!dir) return ERR_OPEN;

    int reclen=reclen_kb*(int)KB;//total bytes
    int recs_per_file=filesize_kb/reclen_kb; //total number of records

    pthread_t tids[MAX_THREADS];
    ThreadData tdata[MAX_THREADS];
    memset(tdata, 0, sizeof tdata);

    char* shared_buf =alloc_aligned((size_t)reclen*(size_t)num_thread);//buffer to r/w for each thread
    if(!shared_buf){
        (*env)->ReleaseStringUTFChars(env, path, dir);
        return ERR_ALLOC;
    }
    memset(shared_buf, 0x5A, (size_t)reclen*(size_t)num_thread);

    int base_flags=is_write? O_WRONLY:O_RDONLY;

    char filename[MAX_PATH_LEN];
    int used_direct=0;

    //initialize mutex
    g_barrier_status=0;
    g_done_flag=0;

    for(int k=0; k<num_thread; k++){
        snprintf(filename, sizeof filename, "%s/%d", dir, k);
        int fd=open_with_direct(filename,base_flags, &used_direct);

        if(fd<0){
            for(int j=0; j<k; j++){
                close(tdata[j].fd);
                free(tdata[j].offsets);
            }
            free_aligned(shared_buf, (size_t)reclen*(size_t)num_thread); //clean buffer
            (*env)->ReleaseStringUTFChars(env, path, dir);
            return ERR_OPEN;
        }

        tdata[k].fd=fd;//per thread file
        tdata[k].reclen=reclen;
        tdata[k].maxrecs=maxrecs;
        tdata[k].offsets=gen_random_offsets(recs_per_file, maxrecs, seed+k);
        tdata[k].buf=shared_buf+(k*reclen);
        tdata[k].recs_done=0;
        tdata[k].is_write=is_write;
        tdata[k].latencies=(int64_t*)malloc((size_t)maxrecs*sizeof(int64_t));

        if(!tdata[k].offsets||
            pthread_create(&tids[k], NULL, rnd_worker, &tdata[k])!=0){
                //failed to spawn thread -> cleanup
                for(int j=0; j<=k; j++){
                    if(tdata[j].fd>=0) close(tdata[j].fd);
                    free(tdata[j].offsets);
                    free(tdata[j].latencies);
                }

                free_aligned(shared_buf, (size_t)reclen*(size_t)num_thread);
                (*env)->ReleaseStringUTFChars(env, path, dir);
                return ERR_ALLOC;
        }
    }
    //sync();

    //wait for all workers to reach the barrier
    while(g_barrier_status<num_thread) sched_yield();

    //fire
    long long start_ns=now_ns();
    g_barrier_status=BARRIER_FIRE; //worker starts running in rnd_worker

    for(int k=0; k<num_thread; k++){
        pthread_join(tids[k], NULL); //wait for all threads to terminate
    }
    if(is_write){
        for(int k=0; k<num_thread; k++){
            fdatasync(tdata[k].fd);
        }
    }

    long long elapsed_ns=now_ns()-start_ns;

    long long total=0;//total operation(recs processed over all threads)
    for(int k=0; k<num_thread; k++) total+=tdata[k].recs_done;
    g_last_rec_count=total;

    //thread별 latency 합치기
    if(g_last_latencies){
        free(g_last_latencies);
        g_last_latencies=NULL;
    }
    if(total>0){
        g_last_latencies=(int64_t*)malloc((size_t)total*sizeof(int64_t));
        if(g_last_latencies){
            jsize idx=0;
            for(int k=0; k<num_thread; k++){
                memcpy(&g_last_latencies[idx], tdata[k].latencies, (size_t)tdata[k].recs_done*sizeof(int64_t)); //각 배열 크기만큼 복사
                idx+=tdata[k].recs_done;
            }
            g_last_latencies_count=(jsize)total;
        }else{
            g_last_latencies_count=0;
        }
    }else{
        g_last_latencies_count=0;
    }

    for(int k = 0; k < num_thread; k++){
        close(tdata[k].fd);
        free(tdata[k].offsets);
        free(tdata[k].latencies);
    }
    free_aligned(shared_buf, (size_t)reclen*(size_t)num_thread);
    (*env)->ReleaseStringUTFChars(env, path, dir);

    return (jlong) elapsed_ns;
    
}

JNIEXPORT jlong JNICALL
Java_kr_ac_snu_csl_androbench_bench_NativeIO_rndRead(
    JNIEnv* env, jobject thiz, 
    jstring path, jint reclen_kb, jint filesize_kb,
    jint maxrecs, jint seed, jint num_thread
){
    return rnd_access(env, path, reclen_kb, filesize_kb, maxrecs, seed, num_thread, 0);
}

JNIEXPORT jlong JNICALL
Java_kr_ac_snu_csl_androbench_bench_NativeIO_rndWrite(
    JNIEnv* env, jobject thiz, 
    jstring path, jint reclen_kb, jint filesize_kb,
    jint maxrecs, jint seed, jint num_thread
){
    return rnd_access(env, path, reclen_kb, filesize_kb, maxrecs, seed, num_thread, 1);
}

JNIEXPORT jlong JNICALL
Java_kr_ac_snu_csl_androbench_bench_NativeIO_getLastRndRecCount (
    JNIEnv* env, jobject thiz
){
    return g_last_rec_count; //전역 반환
}

JNIEXPORT jlongArray JNICALL
Java_kr_ac_snu_csl_androbench_bench_NativeIO_getLastLatencies(
        JNIEnv* env, jobject thiz){
    if(!g_last_latencies ||g_last_latencies_count<=0){
        return (*env)->NewLongArray(env, 0);
    }
    jlongArray  arr=(*env)->NewLongArray(env, g_last_latencies_count); //make array
    if(!arr) return NULL;
    (*env)->SetLongArrayRegion(env, arr, 0, g_last_latencies_count, (const jlong*)g_last_latencies);
    return arr;
}