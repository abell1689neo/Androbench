#define _GNU_SOURCE
#include <jni.h>
#include <stdio.h>
#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>
#include <time.h>
#include <string.h>
#include <errno.h>

#include <pthread.h> //for multi-thread version
#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <linux/fs.h>

#include <sys/vfs.h>





#include <dirent.h>
#include <android/log.h>

#define LOG_TAG "Shuffle Data"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);

#define MAX_FILENAME_SIZE	(1024)

#define KILOBYTE			(1024)		// 1KB

#define MEGABYTE			(1024*1024)

#define NS_PER_S			(1000*1000*1000)

#define MAX_THREADS			(32)


// Benchmarking Target

#define TEST_TARGET_DATA	(0)
#define TEST_TARGET_EXTSD	(1)

#define FILE_FLAG_SYNC		(0)
#define FILE_FLAG_DIRECT	(1)

// [To-Do] static to dynamic
#define PATH_DATA			"/data/data/com.andromeda.androbench2/files"

// Error code for File(System Call)
#define ERROR_OPEN			(-1)
#define ERROR_READ			(-2)
#define ERROR_WRITE			(-3)
#define ERROR_MEM_ALLOC		(-4)
#define ERROR_CLOSE			(-5)
#define ERROR_TYPE			(-6)

#define TEST_SEQ_READ		(1)
#define TEST_SEQ_WRITE		(2)
#define TEST_RND_READ		(3)
#define TEST_RND_WRITE		(4)

#define BAR_INIT	(0)
#define BAR_SR		(TEST_SEQ_READ)
#define BAR_SW		(TEST_SEQ_WRITE)
#define BAR_RR		(TEST_RND_READ)
#define BAR_RW		(TEST_RND_WRITE)


#define STATFS_BLOCKS		(0)
#define STATFS_BFREE		(1)
#define STATFS_BAVAIL		(2)
#define STATFS_FILES		(3)
#define STATFS_FFREE		(4)
#define STATFS_BSIZE		(5)
#define STATFS_FRSIZE		(6)


#define DEBUG				(0)


// Structure for Macro Benchmarks
typedef struct __INS_MACRO {
	int IOtype;
	int IOsize;
	char IOfn[MAX_FILENAME_SIZE];
	int IOnum;
} ST_INS_MACRO;

// Environment of calling android in JAVA Class's method
jclass targetClass;
jmethodID publish;


char *getTempPath (JNIEnv * env, jstring path, jint target) {
	if (target == TEST_TARGET_DATA) {
		return PATH_DATA;
	}  else if (target == TEST_TARGET_EXTSD) {
		// [To-Do] fix
		char *extPath = (char *) (*env)->GetStringUTFChars(env, path, 0);
		return extPath;
	}
	return NULL;
}

char *allocBuffer (int bytes) {
	char *addr;

	addr = (char *) mmap((char *) 0, bytes, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANON, -1, 0);
	if (addr == MAP_FAILED)
		return NULL;
	return addr;
}

void freeBuffer (char *addr, int bytes) {
	munmap (addr, bytes);
}

int __WRITE_HELPER(int fd, char *buf, int len) {
	int total = 0;
	int ret = 0;

	while (total < len) {
		ret = write(fd, buf + total, (size_t) len - total);
		if (ret < 0)
			return ERROR_WRITE;
		total += ret;
	}
	return total;
}

int __READ_HELPER(int fd, char *buf, int len) {
	int total = 0;
	int ret = 0;

	while (total < len) {
		ret = read(fd, buf + total, (size_t) len - total);
		if (ret < 0)
			return ERROR_READ;
		total += ret;
	}
	return total;
}

JNIEXPORT jint Java_com_andromeda_androbench2_GetInfo_GET_1STATFS_1FROM_1PATH(JNIEnv * env,
		jobject thiz, jstring path, jlongArray jni_stat_arr) {
	int i;
	struct statfs stfs;
	int len =(*env)->GetArrayLength(env, jni_stat_arr);
	jlong *stat_arr = (*env)->GetLongArrayElements(env,jni_stat_arr, NULL);

	memset(stat_arr, -1, sizeof(jlong) * len);
	if( statfs( (*env)->GetStringUTFChars(env, path, 0), &stfs) != -1){
		stat_arr[STATFS_BLOCKS] = stfs.f_blocks;
		stat_arr[STATFS_BFREE] = stfs.f_bfree;
		stat_arr[STATFS_BAVAIL] = stfs.f_bavail;
		stat_arr[STATFS_FILES] = stfs.f_files;
		stat_arr[STATFS_FFREE] = stfs.f_ffree;
		stat_arr[STATFS_BSIZE] = stfs.f_bsize;
		stat_arr[STATFS_FRSIZE] = stfs.f_frsize;

		(*env)->ReleaseLongArrayElements(env, jni_stat_arr, stat_arr, 0); // [mode == 0] : copy back the content and free the stat_arr buffer
		return 0; // on success
	}
	(*env)->ReleaseLongArrayElements(env, jni_stat_arr, stat_arr, 0);
	return ERROR_OPEN;
}




/*******************************************************************************************
 * Micro benchmarks :: Initializing for sequential & random read benchmarks
 *******************************************************************************************/
JNIEXPORT jint Java_com_andromeda_androbench2_Result_INIT(JNIEnv * env,
		jobject thiz, jint target, jstring path, jint file_flag, jint reclen_kb,
		 jint filesize_kb, jint num_thread) {

	int i, k;
	int fd;
	char *buf;
	char *dir;
	int total_kb = 0;
	struct stat statbuf;
	char filename[MAX_FILENAME_SIZE];

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env, thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");

	if ((buf = (char *) malloc((size_t) reclen_kb * KILOBYTE)) == NULL) {
		return ERROR_MEM_ALLOC;
	}

	// init files.==========================================================================
	(*env)->CallVoidMethod(env, thiz, publish, BAR_INIT, 0);

	// make directory if necessary
	// set benchmark path (/data, /mnt/sdcard, custom path for external sdcard)

	dir = getTempPath(env, path, target);
	if (dir == NULL) {
		return ERROR_OPEN;
	}
	if (stat(dir, &statbuf))
		mkdir(dir, 0777);

	for (k = 0; k < num_thread; k++) {
		snprintf(filename, MAX_FILENAME_SIZE, "%s/%d", dir, k);
		if ((fd = open(filename, O_RDWR | O_CREAT | O_TRUNC, 0666)) < 0) {
			return ERROR_OPEN;
		}
		for (i = 0; i < filesize_kb / reclen_kb; i++) {
			if (__WRITE_HELPER(fd, buf, reclen_kb * KILOBYTE) <  0)
				return ERROR_WRITE;

			total_kb += reclen_kb;
			(*env)->CallVoidMethod(env, thiz, publish, BAR_INIT, total_kb);
		}
		fsync(fd);
		close(fd);
	}
	(*env)->CallVoidMethod(env, thiz, publish, BAR_INIT, total_kb);

	free(buf);
	return 0;
}

/*******************************************************************************************
 * Micro benchmarks :: Initializing for purge cache
 *******************************************************************************************/

// [To-Do] : file size to KB
JNIEXPORT jint Java_com_andromeda_androbench2_Result_INIT_1PURGE_1CACHE(
		JNIEnv * env, jobject thiz, jint file_size) {
	int fd;
	char *tmp, *stmp;
	char filename[MAX_FILENAME_SIZE];
	int i;
	int recs;
	int reclen;
	int processing = 1;
	struct stat s;

//	mkdir(PATH_SDCARD , 0777);
//	strcpy(filename, PATH_SDCARD "purge_buffer.tmp");
//
//	if (lstat(filename, &s) < 0) {
//		processing = 1;
//	} else {
//		if (s.st_size >= file_size) {
//			processing = 0;
//		} else {
//			processing = 1;
//		}
//	}
//
//	if (processing == 1) {
//		// set environment of calling android in JAVA Class's method
//		targetClass = (*env)->GetObjectClass(env, thiz);
//		publish = (*env)->GetMethodID(env, targetClass, "changeDialog",
//				"(II)V");
//
//		reclen = 1048576; // MEGABYTE
//
//		if ((fd = open(filename, O_RDWR | O_CREAT | O_SYNC | O_TRUNC, 0666))
//				< 0) {
//			return ERROR_OPEN;
//		}
//
//		if ((tmp = (char *) malloc((size_t) reclen)) == NULL) {
//			return ERROR_MEM_ALLOC;
//		}
//		memset(tmp, 0x11, reclen);
//
//		(*env)->CallVoidMethod(env, thiz, publish, 0, 0);
//		recs = file_size / reclen;
//		for (i = 0; i < recs; i++) {
//			if (__WRITE_HELPER(fd, tmp, reclen) < 0)
//				return ERROR_WRITE;
//
//			fsync(fd);
//			// Call JAVA method for percentage
//			(*env)->CallVoidMethod(env, thiz, publish, 6, reclen*i);
//
//		}
//		(*env)->CallVoidMethod(env, thiz, publish, 6, file_size);
//
//		free(stmp);
//		close(fd);
//	}
	return ERROR_MEM_ALLOC;
}

/*******************************************************************************************
 * Micro benchmarks :: Purge cache
 *******************************************************************************************/

// [To-Do] : file size to KB
JNIEXPORT void Java_com_andromeda_androbench2_Result_PURGE_1CACHE(
		JNIEnv * env, jobject thiz, jint file_size) {
	int fd;
	int recs;
	int i;
	char *tmp, *stmp;
	char filename[MAX_FILENAME_SIZE];
	int reclen;

//	// set environment of calling android in JAVA Class's method
//	targetClass = (*env)->GetObjectClass(env, thiz);
//	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
//	(*env)->CallVoidMethod(env, thiz, publish, 1, 0);
//
//	char *buf = (char*) malloc(128);
//
//	strcpy(filename, PATH_SDCARD "/purge_buffer.tmp");
//	reclen = 1048576; //MEGABYTE
//
//	fd = open(filename, O_RDONLY);
//
//	page_size = getpagesize();
//	tmp = (char *) malloc((size_t) reclen + (size_t) page_size);
//	stmp = (char *) (((long) tmp + (long) page_size) & (long) ~(page_size - 1));
//
//	recs = file_size / reclen;
//
//	for (i = 0; i < recs; i++) {
//		read(fd, stmp, reclen);
//
//		// Call JAVA method for percentage
//		// (*env)->CallVoidMethod(env, thiz, publish, 7, reclen*i);
//
//	}
//	(*env)->CallVoidMethod(env, thiz, publish, 7, file_size);
//
//	free(tmp);
//	close(fd);

}

/*******************************************************************************************
 * Micro benchmarks :: Delete temporary files
 *******************************************************************************************/
JNIEXPORT jint JNICALL Java_com_andromeda_androbench2_Result_FINAL(
		JNIEnv * env, jobject thiz, jint target, jstring path, jint num_thread) {
	int k;
	char *dir;
	char filename[MAX_FILENAME_SIZE];

	dir = getTempPath(env, path, target);
	if (dir) {
		for (k = 0; k < num_thread; k++) {
			snprintf(filename, MAX_FILENAME_SIZE, "%s/%d", dir, k);
			unlink(filename);
		}
	}
}
#ifdef DEBUG
long long g_rec_cnt[ MAX_THREADS];

JNIEXPORT jlong Java_com_andromeda_androbench2_Result_GET_1RND_1REC_1CNT_1EACH(JNIEnv * env,
		jobject thiz, jint k) {
	return g_rec_cnt[k];
}
#endif


/*******************************************************************************************
 * Micro benchmarks :: Sequential read/write benchmarks
 *******************************************************************************************/

long sequential_access(JNIEnv * env, jobject thiz, jint target, jstring path, jint file_flag, jint reclen_kb,
		jint amount_kb, jint filesize_kb, jint num_thread, int test_type,
		int (*fn)(int fd, char *buf, int len)) {

	int i, k;
	int *pfd;
	char *buf;
	char *dir;
	int mode;
	int recs_per_file;
	int reclen;
	int total_kb;
	int ret;
	long elapsed_time;
	char filename[MAX_FILENAME_SIZE];
	struct timespec tv_start, tv_stop;

	int tmp_ret;
	char *tmp;

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env, thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, test_type, 0);

	// initialize
	dir = getTempPath(env, path, target);
	mode = 0;
	if (test_type == TEST_SEQ_READ) {
			mode = O_RDONLY | O_DIRECT;
	} else if (test_type == TEST_SEQ_WRITE) {
			mode = O_WRONLY | O_DIRECT | O_SYNC;
	} else
		return ERROR_TYPE;

	pfd = (int *) malloc(sizeof(int) * num_thread);
	reclen = reclen_kb * KILOBYTE;
	recs_per_file = filesize_kb / reclen_kb;
	buf = allocBuffer (reclen);
	if (buf == NULL) {
		return ERROR_MEM_ALLOC;
	}
	memset(buf, 0x5a, reclen);

	for (k = 0; k < num_thread; k++) {
		snprintf(filename, MAX_FILENAME_SIZE, "%s/%d", dir, k);
		if ((pfd[k] = open(filename, mode)) < 0) {
			return ERROR_OPEN;
		}
#ifdef DEBUG
		g_rec_cnt[k] = pfd[k];
#endif
	}
	sync();

	// measure
	total_kb = 0;
	elapsed_time = 0;
	for (k = 0; k < num_thread; k++) {
		for (i = 0; i < recs_per_file; i++) {
			clock_gettime(CLOCK_MONOTONIC, &tv_start);
			if ((ret = fn(pfd[k], buf, reclen)) < 0) {
				return ret;
			}
			clock_gettime(CLOCK_MONOTONIC, &tv_stop);
			elapsed_time += (tv_stop.tv_sec - tv_start.tv_sec) * CLOCKS_PER_SEC
					+ (tv_stop.tv_nsec - tv_start.tv_nsec)
							/ (NS_PER_S / CLOCKS_PER_SEC);
			total_kb += reclen_kb;
			(*env)->CallVoidMethod(env, thiz, publish, test_type, total_kb);
		}
	}
	(*env)->CallVoidMethod(env, thiz, publish, test_type, total_kb);

	// finalize
	for(k= 0; k < num_thread; k++)
		close(pfd[k]);
	free(pfd);
	freeBuffer (buf, reclen);

	return elapsed_time;
}

JNIEXPORT jlong Java_com_andromeda_androbench2_Result_SEQ_1READ(JNIEnv * env,
		jobject thiz, jint target, jstring path, jint file_flag, jint reclen_kb,
		jint amount_to_kb, jint filesize_kb, jint num_thread) {

		return sequential_access (env, thiz, target, path, file_flag, reclen_kb, amount_to_kb,
				filesize_kb, num_thread, TEST_SEQ_READ, __READ_HELPER);

}

JNIEXPORT jlong Java_com_andromeda_androbench2_Result_SEQ_1WRITE(
		JNIEnv * env, jobject thiz, jint target, jstring path, jint file_flag,
		jint reclen_kb, jint amount_to_kb, jint filesize_kb, jint num_thread) {

	return sequential_access (env, thiz, target, path, file_flag, reclen_kb, amount_to_kb,
					filesize_kb, num_thread, TEST_SEQ_WRITE, __WRITE_HELPER);

}


/*******************************************************************************************
 * For Random benchmarks
 *******************************************************************************************/
#ifdef XXX
long long g_rec_cnt[MAX_THREADS];

JNIEXPORT jlong Java_com_andromeda_androbench2_Result_GET_1RND_1REC_1CNT_1EACH(JNIEnv * env,
		jobject thiz, jint k) {
	return g_rec_cnt[k];
}
#endif


jlong t_total_recs;

#define THR_INIT	(0)
#define THR_START	(MAX_THREADS + 68)
#define THR_DONE	(MAX_THREADS + 70)

#define THR_DATA_ALIGN	(64)

volatile long 		t_status;
pthread_mutex_t		t_lock;

void thread_init ()
{
	t_status = THR_INIT;
	pthread_mutex_init (&t_lock, NULL);
}

void thread_status_update (int value) {
	pthread_mutex_lock (&t_lock);
	t_status += value;
	pthread_mutex_unlock (&t_lock);
}

typedef struct {
	int fd;
	int filesize_kb;
	int reclen;
	int maxrecs;
	int recs;
	int seed;
	int *offset;
	char *buf;
	int pad[8];
} Thread_data;

#ifdef SHUFFLE
int __SHUFFLE(int* src, int recs, int seed) {
	int i = 0;
	int r1 = 0;
	int r2 = 0;
	int tmp = 0;

	srand48(seed);
	for (i = 0; i < recs; i++) {
		r1 = i;
		r2 = lrand48() % recs;

		tmp = src[r1];
		src[r1] = src[r2];
		src[r2] = tmp;
	}
}
#endif

int *gen_random_offset(int recs_per_file, int maxrecs, int seed) {

	int i;
	int *filerecs;
	int *offset;

	offset = (int *) malloc(sizeof(int) * maxrecs);
	if (offset == NULL)
		return NULL;

	srand48(seed);
	for (i = 0; i < maxrecs; i++) {
		offset[i] = lrand48() % recs_per_file;
	}


#ifdef SHUFFLE
	filerecs = (int *) malloc(sizeof(int) * recs_per_file);
	if (filerecs == NULL)
		return NULL;

	//memset(filerecs, 0, sizeof(int) * recs_per_file);

	for (i = 0; i < recs_per_file; i++)
		filerecs[i] = i;
	__SHUFFLE(filerecs, recs_per_file, seed);

	for (i = 0; i < maxrecs; i++)
		offset[i] = filerecs[i];

	free(filerecs);
#endif

	return offset;
}


JNIEXPORT jlong Java_com_andromeda_androbench2_Result_GET_1RND_1REC_1CNT() {
	return t_total_recs;
}



/*******************************************************************************************
 * Micro benchmarks :: Random read/write benchmarks
 *******************************************************************************************/

void *tmain_rnd_read(void *data) {

	int i;
	Thread_data *td = (Thread_data *) data;

	// wait other threads
	thread_status_update (1);
	while (t_status < THR_START)
		sleep (0);

	// do
	for (i = 0; i < td->maxrecs && t_status != THR_DONE; i++) {
		lseek(td->fd, td->offset[i] * td->reclen, SEEK_SET);
		if (__READ_HELPER(td->fd, td->buf, td->reclen) < 0) {
			t_status = THR_DONE;
			pthread_exit ((void *) ERROR_READ);
		}
		td->recs++;
	}
	t_status = THR_DONE;
	pthread_exit ((void *) 0);
}

void *tmain_rnd_write(void *data) {

	int i;
	Thread_data *td = (Thread_data *) data;

	// wait other threads
	thread_status_update (1);
	while (t_status < THR_START)
		sleep (0);

	// do
	for (i = 0; i < td->maxrecs && t_status != THR_DONE; i++) {
		lseek(td->fd, td->offset[i] * td->reclen, SEEK_SET);
		if (__WRITE_HELPER(td->fd, td->buf, td->reclen) < 0) {
			t_status = THR_DONE;
			pthread_exit ((void *) ERROR_WRITE);
		}
		td->recs++;
	}
	t_status = THR_DONE;
	pthread_exit ((void *) 0);
}


long random_access(JNIEnv *env, jobject thiz, jint target, jstring path, jint file_flag, jint reclen_kb,
		jint filesize_kb, jint maxrecs, jint seed, jint num_thread, int test_type, void *(*fn)(void *data)) {


	int i, k;
	int mode;
	int reclen;
	char *dir;
	char *buf;
	pthread_t *tid;
	char *tdata_org;
	Thread_data *tdata;
	Thread_data *td;
	void **t_ret;
	int current_recs;
	long elapsed_time;
	char filename[MAX_FILENAME_SIZE];
	struct timespec tv_start, tv_stop;

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env, thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, test_type, 0);

	// initialize
	tid = (pthread_t *) calloc(num_thread, sizeof(pthread_t));
	if (tid == NULL)
		return ERROR_MEM_ALLOC;

	tdata_org = (char *) malloc (num_thread * sizeof(Thread_data) + THR_DATA_ALIGN);
	if (tdata_org == NULL)
		return ERROR_MEM_ALLOC;
	tdata = (Thread_data *) ((long) tdata_org + (long) THR_DATA_ALIGN & (long) ~(THR_DATA_ALIGN - 1));

	t_ret = (void **) calloc(num_thread, sizeof(void *));
	if (t_ret == NULL)
		return ERROR_MEM_ALLOC;

	dir = getTempPath(env, path, target);
	mode = 0;
	if (test_type == TEST_RND_READ) {
			mode = O_RDONLY | O_DIRECT;
	} else if (test_type == TEST_RND_WRITE) {
			mode = O_WRONLY | O_DIRECT | O_SYNC;
	} else
		return ERROR_TYPE;

	reclen = reclen_kb * KILOBYTE;
	buf = allocBuffer (reclen * num_thread);
	if (buf == NULL) {
		return ERROR_MEM_ALLOC;
	}
	memset (buf, 0x5a, reclen);
	sync();

	// per-thread initialization
	thread_init();
	for (k = 0; k < num_thread; k++) {
		td = &tdata[k];

		snprintf(filename, MAX_FILENAME_SIZE, "%s/%d", dir, k);
		if ((td->fd = open(filename, mode)) < 0) {
			return ERROR_OPEN;
		}
		td->filesize_kb = filesize_kb;
		td->reclen = reclen;
		td->maxrecs = maxrecs;
		td->recs = 0;
		td->seed = seed + k;
		td->offset = gen_random_offset(filesize_kb / reclen_kb, td->maxrecs, td->seed);
		if (td->offset == NULL)
			return ERROR_MEM_ALLOC;
		td->buf = buf + (k * reclen);
		//(void *) td
		if (pthread_create(&tid[k], NULL, fn, (void *) &tdata[k])) {
			return ERROR_MEM_ALLOC;
		}
	}

	// wait until all threads are ready.
	while (t_status < num_thread)
		sleep(0);

	// measure
	(*env)->CallVoidMethod(env, thiz, publish, test_type, 0);
	current_recs = 0;
	k = 0;
	clock_gettime(CLOCK_MONOTONIC, &tv_start);
	t_status = THR_START;

	while (t_status != THR_DONE) {
		if (current_recs < tdata[k].recs) {
			current_recs = tdata[k].recs;
			(*env)->CallVoidMethod(env, thiz, publish, test_type, num_thread * current_recs * reclen_kb);
		}
		usleep(10000);
		k = (k + 1) % num_thread;
	}
	for (k = 0; k < num_thread; k++)
		pthread_join(tid[k], &(t_ret[k]));
	clock_gettime(CLOCK_MONOTONIC, &tv_stop);

	(*env)->CallVoidMethod(env, thiz, publish, test_type, num_thread * maxrecs * reclen_kb);

	t_total_recs = 0ll;
	for (k = 0; k < num_thread; k++) {
		t_total_recs += tdata[k].recs;
		// [To-Do]
#ifdef	DEBUG
		g_rec_cnt[k] = tdata[k].recs;
#endif
	}

	//free
	freeBuffer(buf, reclen * num_thread);
	for (k = 0; k < num_thread; k++) {
		free(tdata[k].offset);
		close(tdata[k].fd);
	}
	free(tdata_org);
	free(tid);

	// error from pthread
	for(k = 0; k < num_thread; k++){
		if(t_ret[k]) return (int) t_ret[k];
	}

	elapsed_time = (tv_stop.tv_sec - tv_start.tv_sec) * CLOCKS_PER_SEC
			+ (tv_stop.tv_nsec - tv_start.tv_nsec) / (NS_PER_S / CLOCKS_PER_SEC);
	return elapsed_time;
}


JNIEXPORT jlong Java_com_andromeda_androbench2_Result_RND_1READ(JNIEnv * env,
		jobject thiz, jint target, jstring path, jint file_flag, jint reclen_kb,
		jint filesize_kb, jint maxrecs, jint seed, jint num_thread) {

	return random_access (env, thiz, target, path, file_flag, reclen_kb, filesize_kb, maxrecs, seed, num_thread,
			TEST_RND_READ, tmain_rnd_read);
}

JNIEXPORT jlong Java_com_andromeda_androbench2_Result_RND_1WRITE(
		JNIEnv * env, jobject thiz, jint target, jstring path, jint file_flag,
		jint reclen_kb, jint filesize_kb, jint maxrecs, jint seed, jint num_thread) {

	return random_access (env, thiz, target, path, file_flag, reclen_kb, filesize_kb, maxrecs, seed, num_thread,
				TEST_RND_WRITE, tmain_rnd_write);
}


/*******************************************************************************************
 * Macro benchmarks :: Replay script files
 *******************************************************************************************/
JNIEXPORT jlong JNICALL Java_com_andromeda_androbench2_Result_MACRO(
		JNIEnv * env, jobject thiz, jint target, jstring path,
		jstring scriptName, jint numScript, jint scriptLine) {
	int fd;
	char *tmp;
	int i = 0;
	int j = 0;

	struct timespec tv_start, tv_stop;

	int cnt = 0;
	int cntOpen = 0;
	int maxBufferSize = 0;

	ST_INS_MACRO *instruction;
	char targetFileName[MAX_FILENAME_SIZE];
	char targetPathName[MAX_FILENAME_SIZE];
	char funcDirName[MAX_FILENAME_SIZE];

	char filename[MAX_FILENAME_SIZE];
	FILE *scriptFile;

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env, thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, 12, ((numScript - 1) * 2) + 0);

	// script_files are saved at /data partition
	strcpy(filename, PATH_DATA "/");
	const char *scriptFileName = (*env)->GetStringUTFChars(env, scriptName, 0);
	strcat(filename, scriptFileName);

	if (target == TEST_TARGET_DATA) {
		strcpy(targetPathName, PATH_DATA "/script");
	} else if (target == TEST_TARGET_EXTSD) {
		const char *extPath = (*env)->GetStringUTFChars(env, path, 0);
		strcpy(targetPathName, extPath);
		// make base path
		strcpy(funcDirName, "mkdir ");
		strcat(funcDirName, targetPathName);
		system(funcDirName);

		strcat(targetPathName, "/script");
	}

	strcpy(funcDirName, "mkdir ");
	strcat(funcDirName, targetPathName);
	system(funcDirName);

	scriptFile = fopen(filename, "r");

	instruction = (ST_INS_MACRO *) malloc(sizeof(ST_INS_MACRO) * scriptLine);

	cnt = 0;
	while (!feof(scriptFile)) {
		printf("Size of ST_INS_MACRO: %d\n", sizeof(ST_INS_MACRO));
		printf("Step2\n");
		fscanf(scriptFile, "%d %d %s %d", &(instruction[cnt].IOtype),
				&(instruction[cnt].IOsize), targetFileName,
				&(instruction[cnt].IOnum));
		sprintf(instruction[cnt].IOfn, "%s%s%s", targetPathName, "/",
				targetFileName);

		if (maxBufferSize < instruction[cnt].IOsize)
			maxBufferSize = instruction[cnt].IOsize;

		cnt++;
	}

	fclose(scriptFile);

	if ((tmp = (char *) malloc((size_t) maxBufferSize)) == NULL)
		return ERROR_MEM_ALLOC;
	memset(tmp, 0x11, maxBufferSize);


	(*env)->CallVoidMethod(env, thiz, publish, 12, ((numScript - 1) * 2) + 1);

	sync();

	clock_gettime(CLOCK_MONOTONIC, &tv_start);

	for (i = 0; i < cnt; i++) {
		for (j = 0; j < instruction[i].IOnum; j++) {	// Multiple instruction
			switch (instruction[i].IOtype) {
			case 1:
				if ((fd = open(instruction[i].IOfn,
				O_WRONLY | O_CREAT | O_TRUNC | O_LARGEFILE, 0666)) < 0)
					return ERROR_OPEN;
				break;
			case 2:
				//fsync(fd);
				if (close(fd) < 0)
					return ERROR_CLOSE;
				break;
			case 3:
				break;
			case 4:
				if (__WRITE_HELPER(fd, tmp, instruction[i].IOsize) < 0)
					return ERROR_WRITE;
				break;
			default:
				break;
			}
		}
	}

	sync();

	clock_gettime(CLOCK_MONOTONIC, &tv_stop);

	free(tmp);
	free(instruction);

	strcpy(funcDirName, "rmdir -r ");
	strcat(funcDirName, targetPathName);
	system(funcDirName);

	(*env)->CallVoidMethod(env, thiz, publish, 12, ((numScript - 1) * 2) + 2);

	return (tv_stop.tv_sec - tv_start.tv_sec) * CLOCKS_PER_SEC
			+ (tv_stop.tv_nsec - tv_start.tv_nsec) / (NS_PER_S / CLOCKS_PER_SEC);

}

