#define _GNU_SOURCE
#include "jni.h"
#include <stdio.h>
#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>
#include <time.h>
#include <string.h>
#include <errno.h>

#include <sys/time.h>

#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <linux/fs.h>

#define MAX_FILENAME_SIZE	1024
#define KILOBYTE			1024		// 1KB
#define MEGABYTE			1024*1024	// 10MB
// Benchmarking Target
// DATA : /data/data/{project-name}/files
// SDCARD : /sdcard
// USER : Used custom target
#define TEST_TARGET_DATA	0
#define TEST_TARGET_SDCARD	1
#define TEST_TARGET_EXTSD	2

#define FILE_FLAG_SYNC		0
#define FILE_FLAG_DIRECT	1

// Error code for File(System Call)
#define ERROR_NOT_OPEN			-1
#define ERROR_NOT_READ			-2
#define ERROR_NOT_WRITE			-3
#define ERROR_NOT_ALLOCATION	-4
#define ERROR_NOT_CLOSE			-5
#define ERROR_NOT_CREATE_RNDNUM	-6

#define RND_GRP_NUMBER		32

// Structure for Macro Benchmarks
typedef struct __INS_MACRO {
	int IOtype;
	int IOsize;
	char IOfn[MAX_FILENAME_SIZE];
	int IOnum;
} ST_INS_MACRO;

// Default Page Size for O_DIRECT (Memory Align)
long long page_size = 4096;

// Environment of calling android in JAVA Class's method
jclass targetClass;
jmethodID publish;

// Write system call helper
int __WRITE_HELPER(int fd, const char *strTarget, int iSize) {
	int total = 0;
	int ret = 0;

	while (total < iSize) {
		ret = write(fd, strTarget, (size_t) iSize);
		if (ret < 0)
			return -1;
		total += ret;
	}

	return total;
}

int __SHUFFLE(unsigned short* Src, int recs, int rndSeed, int iNum) {
	int i = 0;
	int big_rand_fst = 0;
	int big_rand_snd = 0;
	unsigned short tmp = 0;

	srand48(rndSeed);
	for (i = 0; i < iNum; i++) {
		big_rand_fst = lrand48() % recs;
		big_rand_snd = lrand48() % recs;

		tmp = Src[big_rand_fst];
		Src[big_rand_fst] = Src[big_rand_snd];
		Src[big_rand_snd] = tmp;
	}
}

int __PM_GET_SIZE(int file_name, int num_min, int num_max, int buffer) {
	if (num_min < 4)
		num_min = 4;

	if (num_min > num_max)
		num_max = num_min;

	if (buffer > num_min)
		buffer = num_min;

	if (buffer > num_max)
		buffer = num_min;

	return (buffer * (num_min / buffer))
			* (file_name % (num_max / (buffer * (num_min / buffer))) + 1);
}

/*******************************************************************************************
 * Micro benchmarks :: Initializing for sequential & random read benchmarks
 *******************************************************************************************/
JNIEXPORT jint Java_com_andromeda_androbenchplus_Result_INIT_1READ(JNIEnv * env,
		jobject thiz, jstring path, jint reclen, jint filesize) {

	int fd;
	char *tmp, *stmp;
	char filename[MAX_FILENAME_SIZE];
	int i;
	int recs;

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env, thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");

	const char *mPath = (*env)->GetStringUTFChars(env, path, 0);
	mkdir(mPath, 0777);
	strcpy(filename, mPath);
	strcat(filename, "/disk_perf_test_read.tmp");

	if ((fd = open(filename, O_RDWR | O_CREAT | O_SYNC | O_TRUNC, 0666)) < 0) {
		return ERROR_NOT_OPEN;
	}

	if ((tmp = (char *) malloc((size_t) reclen)) == NULL) {
		return ERROR_NOT_ALLOCATION;
	}
	memset(tmp, 0x11, reclen);

	(*env)->CallVoidMethod(env, thiz, publish, 0, 0);
	recs = filesize / reclen;
	for (i = 0; i < recs; i++) {
		if (__WRITE_HELPER(fd, tmp, reclen) < 0)
			return ERROR_NOT_WRITE;

		fsync(fd);

		// Call JAVA method for percentage
		(*env)->CallVoidMethod(env, thiz, publish, 0, reclen * i);
	}
	(*env)->CallVoidMethod(env, thiz, publish, 0, filesize);

	free(tmp);
	close(fd);

	return 0;
}

/*******************************************************************************************
 * Micro benchmarks :: Purge cache
 *******************************************************************************************/
JNIEXPORT void Java_com_andromeda_androbenchplus_Result_PURGE_1CACHE(
		JNIEnv * env, jobject thiz) {
	/*
	 int fd;
	 int recs;
	 int i;
	 char *tmp, *stmp;
	 char filename[MAX_FILENAME_SIZE];
	 int reclen;

	 // set environment of calling android in JAVA Class's method
	 targetClass = (*env)->GetObjectClass(env,thiz);
	 publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	 (*env)->CallVoidMethod(env, thiz, publish, 1, 0);

	 char *buf = (char*)malloc(128);

	 strcpy(filename, "/sdcard/.androbenchplus/purge_buffer.tmp");
	 reclen = MEGABYTE;

	 fd = open(filename, O_RDONLY);

	 page_size = getpagesize();
	 tmp = (char *)malloc((size_t)reclen + page_size);
	 stmp = (char *)(((long)tmp+(long)page_size) & (long)~(page_size-1));

	 recs = file_size/reclen;

	 for(i=0; i<recs; i++){
	 read(fd,stmp,reclen);

	 // Call JAVA method for percentage
	 (*env)->CallVoidMethod(env, thiz, publish, 7, reclen*i);

	 }
	 (*env)->CallVoidMethod(env, thiz, publish, 7, file_size);

	 free(tmp);
	 close(fd);
	 */
	system("sync");
	system("su -c echo 0 > /proc/sys/vm/drop_caches");
	system("sync");
	system("su -c echo 3 > /proc/sys/vm/drop_caches");
	system("sync");
	system("su -c echo 0 > /proc/sys/vm/drop_caches");
	system("sync");
	/*
	 FILE *f;
	 sync();
	 f = fopen("/proc/sys/vm/drop_caches", "w");
	 if (f){
	 fprintf(f, "3");
	 fclose(f);
	 fflush(f);
	 }

	 f = fopen("/proc/sys/vm/drop_caches", "w");
	 if (f){
	 fprintf(f, "0");
	 fclose(f);
	 fflush(f);
	 }
	 */
}

/*******************************************************************************************
 * Micro benchmarks :: Delete temporary files
 *******************************************************************************************/
JNIEXPORT jint JNICALL Java_com_andromeda_androbenchplus_Result_FINAL(
		JNIEnv * env, jobject thiz, jstring path) {
	char filename_r[MAX_FILENAME_SIZE];
	char filename_w[MAX_FILENAME_SIZE];
	const char *mPath = (*env)->GetStringUTFChars(env, path, 0);
	strcpy(filename_r, mPath);
	strcat(filename_r, "/disk_perf_test_read.tmp");
	strcpy(filename_w, mPath);
	strcat(filename_w, "/disk_perf_test.tmp");
	unlink(filename_r);
	unlink(filename_w);
}

/*******************************************************************************************
 * Micro benchmarks :: Sequential read benchmarks
 *******************************************************************************************/
JNIEXPORT jlong Java_com_andromeda_androbenchplus_Result_SEQ_1READ(JNIEnv * env,
		jobject thiz, jstring path, jint reclen, jint filesize, struct timeval* result_t) {
	int fd;
	int recs;
	int i;
	int percent;
	char *tmp, *stmp;
	char filename[MAX_FILENAME_SIZE];
	clock_t begin_time, end_time, take_time;
	struct timeval start_t, end_t, temp_t;

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env, thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, 1, 0);

	char *buf = (char*) malloc(128);

	take_time = 0;

	const char *mPath = (*env)->GetStringUTFChars(env, path, 0);
	strcpy(filename, mPath);
	strcat(filename, "/disk_perf_test_read.tmp");

	if ((fd = open(filename, O_RDONLY)) < 0) {
		return ERROR_NOT_OPEN;
	}

	page_size = getpagesize();
	if ((tmp = (char *) malloc((size_t) reclen + page_size)) == NULL) {
		return ERROR_NOT_ALLOCATION;
	}
	stmp = (char *) (((long) tmp + (long) page_size) & (long) ~(page_size - 1));

	recs = filesize / reclen;
	
	result_t->tv_sec=0; result_t->tv_usec=0;

	for (i = 0; i < recs; i++) {
		begin_time = clock();
		gettimeofday(&start_t, NULL);
		if (read(fd, stmp, reclen) != reclen) {
			return ERROR_NOT_READ;
		}
		gettimeofday(&end_t, NULL);
		end_time = clock();
		timersub(&end_t, &start_t, &temp_t); timeradd(&temp_t, result_t, result_t);
		take_time += (end_time - begin_time);
		
		// Call JAVA method for notice percentage
		(*env)->CallVoidMethod(env, thiz, publish, 1, reclen * i);

	}
	(*env)->CallVoidMethod(env, thiz, publish, 1, filesize);

	free(tmp);
	close(fd);
	return take_time;

}

/*******************************************************************************************
 * Micro benchmarks :: Sequential write benchmarks
 *******************************************************************************************/
JNIEXPORT jlong Java_com_andromeda_androbenchplus_Result_SEQ_1WRITE(
		JNIEnv * env, jobject thiz, jstring path, jint reclen, jint filesize, struct timeval* result_t) {
	int fd;
	int recs;
	int i;
	char *tmp, *stmp;
	char filename[MAX_FILENAME_SIZE];
	clock_t begin_time, end_time, take_time;
	struct timeval start_t, end_t, temp_t;

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env, thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, 2, 0);

	take_time = 0;

	const char *mPath = (*env)->GetStringUTFChars(env, path, 0);
	strcpy(filename, mPath);
	strcat(filename, "/disk_perf_test_write.tmp");

	if ((fd = open(filename, O_RDWR | O_CREAT | O_SYNC | O_TRUNC, 0666)) < 0) {
		return ERROR_NOT_OPEN;
	}

	if ((tmp = (char *) malloc((size_t) reclen)) == NULL) {
		return ERROR_NOT_ALLOCATION;
	}
	memset(tmp, 0x11, reclen);
	recs = filesize / reclen;

	system("sync");

	result_t->tv_sec=0; result_t->tv_usec=0;
		gettimeofday(&start_t, NULL);
	for (i = 0; i < recs; i++) {
		begin_time = clock();
		if (__WRITE_HELPER(fd, tmp, reclen) < 0)
			return ERROR_NOT_WRITE;
		end_time = clock();
		take_time += (end_time - begin_time);

		// Call JAVA method for notice percentage
		(*env)->CallVoidMethod(env, thiz, publish, 2, reclen * i);
	}
	(*env)->CallVoidMethod(env, thiz, publish, 2, filesize);
		fsync(fd);
		gettimeofday(&end_t, NULL);
		timersub(&end_t, &start_t, result_t);

	free(tmp);
	close(fd);

	return take_time;
}

/*******************************************************************************************
 * Micro benchmarks :: Random read benchmarks
 *******************************************************************************************/
JNIEXPORT jlong Java_com_andromeda_androbenchplus_Result_RND_1READ(JNIEnv * env,
		jobject thiz, jstring path, jint reclen, jint filesize, jint rndSeed, struct timeval* result_t) {
	int fd;
	int recs;
	int i, j;
	int lpNum;
	char *tmp, *stmp;
	char filename[MAX_FILENAME_SIZE];
	clock_t begin_time, end_time, take_time;
	unsigned long long big_rand;
	unsigned short *offset;
	struct timeval start_t, end_t, temp_t;

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env, thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, 3, 0);

	take_time = 0;
	result_t->tv_sec=0; result_t->tv_usec=0;

	const char *mPath = (*env)->GetStringUTFChars(env, path, 0);
	strcpy(filename, mPath);
	strcat(filename, "/disk_perf_test_read.tmp");

	if ((fd = open(filename, O_RDONLY)) < 0) {
		return ERROR_NOT_OPEN;
	}

	page_size = getpagesize();
	if ((tmp = (char *) malloc((size_t) reclen + page_size)) == NULL) {
		return ERROR_NOT_ALLOCATION;
	}
	stmp = (char *) (((long) tmp + (long) page_size) & (long) ~(page_size - 1));

	recs = filesize / reclen;

	if (recs > 65535) {
		return ERROR_NOT_CREATE_RNDNUM;
	}

	offset = (unsigned short*) malloc(sizeof(unsigned short) * recs);
	memset(offset, 0, sizeof(unsigned short) * recs);

	for (i = 0; i < recs; i++)
		offset[i] = (unsigned short) i;

	__SHUFFLE(offset, recs, rndSeed, 100);

	lpNum = 0;
	for (i = 0; i < ((recs - 1) / RND_GRP_NUMBER) + 1; i++) {
		if (i == ((recs - 1) / RND_GRP_NUMBER))
			lpNum = recs % RND_GRP_NUMBER;
		else
			lpNum = RND_GRP_NUMBER;

		begin_time = clock();
		gettimeofday(&start_t, NULL);
		for (j = 0; j < lpNum; j++) {
			lseek(fd, (offset[(i * RND_GRP_NUMBER) + j] * reclen), SEEK_SET);
			if (read(fd, stmp, reclen) != reclen) {
				return ERROR_NOT_READ;
			}
		}
		gettimeofday(&end_t, NULL);
		end_time = clock();
		take_time += (end_time - begin_time);
		timersub(&end_t, &start_t, &temp_t); timeradd(&temp_t, result_t, result_t);

		// Call JAVA method for notice percentage
		(*env)->CallVoidMethod(env, thiz, publish, 3,
				reclen * (i * RND_GRP_NUMBER));
	}
	(*env)->CallVoidMethod(env, thiz, publish, 3, filesize);

	//free(read_flag);
	free(offset);
	free(tmp);
	close(fd);

	return take_time;
}

/*******************************************************************************************
 * Micro benchmarks :: Random write benchmarks
 *******************************************************************************************/
JNIEXPORT jlong Java_com_andromeda_androbenchplus_Result_RND_1WRITE(
		JNIEnv * env, jobject thiz, jstring path, jint reclen, jint filesize,
		jint rndSeed, struct timeval* result_t) {
	int fd;
	int recs;
	int i, j;
	int lpNum;
	char *tmp, *stmp;
	char filename[MAX_FILENAME_SIZE];
	clock_t begin_time, end_time, take_time;
	unsigned long long big_rand;
	unsigned short *offset;
	struct timeval start_t, end_t, temp_t;

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env, thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, 4, 0);

	take_time = 0;
	result_t->tv_sec=0; result_t->tv_usec=0;

	srand48(rndSeed);

	const char *mPath = (*env)->GetStringUTFChars(env, path, 0);
	strcpy(filename, mPath);
	strcat(filename, "/disk_perf_test_write.tmp");

	if ((fd = open(filename, O_RDWR | O_CREAT | O_SYNC | O_TRUNC, 0666)) < 0) {
		return ERROR_NOT_OPEN;
	}

	if ((tmp = (char *) malloc((size_t) reclen)) == NULL) {
		return ERROR_NOT_ALLOCATION;
	}
	memset(tmp, 0x11, reclen);

	recs = filesize / reclen;

	if (recs > 65535) {
		return ERROR_NOT_CREATE_RNDNUM;
	}

	offset = (unsigned short*) malloc(sizeof(unsigned short) * recs);
	memset(offset, 0, sizeof(unsigned short) * recs);

	for (i = 0; i < recs; i++)
		offset[i] = (unsigned short) i;

	__SHUFFLE(offset, recs, rndSeed, 100);

	lpNum = 0;
	for (i = 0; i < ((recs - 1) / RND_GRP_NUMBER) + 1; i++) {
		if (i == ((recs - 1) / RND_GRP_NUMBER))
			lpNum = recs % RND_GRP_NUMBER;
		else
			lpNum = RND_GRP_NUMBER;

		begin_time = clock();
		gettimeofday(&start_t, NULL);
		for (j = 0; j < lpNum; j++) {
			lseek(fd, (offset[(i * RND_GRP_NUMBER) + j] * reclen), SEEK_SET);
			if (__WRITE_HELPER(fd, tmp, reclen) < 0)
				return ERROR_NOT_WRITE;
		}
			fsync(fd);
		gettimeofday(&end_t, NULL);
		end_time = clock();
		take_time += (end_time - begin_time);
		timersub(&end_t, &start_t, &temp_t); timeradd(&temp_t, result_t, result_t);

		// Call JAVA method for notice percentage
		(*env)->CallVoidMethod(env, thiz, publish, 4,
				reclen * (i * RND_GRP_NUMBER));
	}
	(*env)->CallVoidMethod(env, thiz, publish, 4, filesize);

	free(tmp);
	close(fd);

	return take_time;
}

JNIEXPORT jlong Java_com_andromeda_androbenchplus_Result_PM_1INITIALIZE(
		JNIEnv * env, jobject thiz, jstring path, jint num_dir, jint num_file,
		jint filesize_min, jint filesize_max, jint buffersize, struct timeval* result_t) {
	int fd;
	char *tmp;
	char filename[MAX_FILENAME_SIZE];
	int rnd;
	int i;
	int j;
	int k;
	clock_t begin_time, end_time, take_time;
	struct timeval start_t, end_t, temp_t;
	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env, thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, 8, 0);

	if ((tmp = (char *) malloc((size_t) buffersize)) == NULL) {
		return ERROR_NOT_ALLOCATION;
	}

	srand(0);
	memset(tmp, 0x11, buffersize);

	result_t->tv_sec=0; result_t->tv_usec=0;
	const char *mPath = (*env)->GetStringUTFChars(env, path, 0);
	mkdir(mPath, 0777);

	for (k = 0; k < num_dir; k++) {
		char mmPath[MAX_FILENAME_SIZE];
		sprintf(mmPath, "%s/%d", mPath, k);
		mkdir(mmPath, 0777);

		for (i = 0; i < num_file; i++) {
			sprintf(filename, "%s/pm%d", mmPath, i);
			rnd = rand() % (num_file * num_dir);
			begin_time = clock();
			gettimeofday(&start_t, NULL);
			if ((fd = open(filename, O_RDWR | O_CREAT | O_TRUNC, 0666)) < 0) {
				return ERROR_NOT_OPEN;
			}
			for (j = 0;	j < (__PM_GET_SIZE(rnd, filesize_min, filesize_max, buffersize) / buffersize); j++) {
				__WRITE_HELPER(fd, tmp, buffersize);
			}
			fsync(fd);
			close(fd);
			gettimeofday(&end_t, NULL);
			end_time = clock();
			take_time += (end_time - begin_time);
			timersub(&end_t, &start_t, &temp_t); timeradd(&temp_t, result_t, result_t);

			// Call JAVA method for notice percentage
			(*env)->CallVoidMethod(env, thiz, publish, 8, i + k * num_file);
		}
	}

	return take_time;
}

JNIEXPORT jlong Java_com_andromeda_androbenchplus_Result_PM_1DELETE(
		JNIEnv * env, jobject thiz, jstring path, jint num_dir, jint num_file,	jint num_trans, struct timeval* result_t) {
	char filename[MAX_FILENAME_SIZE];
	int rnd;
	int i;
	int j;
	clock_t begin_time, end_time, take_time;
	struct timeval start_t, end_t, temp_t;
	
	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env, thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, 9, 0);

	result_t->tv_sec=0; result_t->tv_usec=0;
	
	srand(0);
	take_time = 0;
	const char *mPath = (*env)->GetStringUTFChars(env, path, 0);

	for (j = 0; j < num_dir; j++) {
		for (i = 0; i < num_trans; i++) {
			rnd = (337*i) % num_file;
			sprintf(filename, "%s/%d/pm%d", mPath, j, rnd);

			begin_time = clock();
			gettimeofday(&start_t, NULL);
			
			remove(filename);
			
			gettimeofday(&end_t, NULL);
			end_time = clock();
			take_time += (end_time - begin_time);
			timersub(&end_t, &start_t, &temp_t); timeradd(&temp_t, result_t, result_t);
			
			// Call JAVA method for notice percentage
			(*env)->CallVoidMethod(env, thiz, publish, 9, j * num_trans + i);
		}
	}

	return take_time;
}

JNIEXPORT jlong Java_com_andromeda_androbenchplus_Result_PM_1CREATE(
		JNIEnv * env, jobject thiz, jstring path, jint num_dir, jint num_file,
		jint num_trans, jint filesize_min, jint filesize_max, jint buffersize, struct timeval* result_t) {
	int fd;
	char *tmp;
	char filename[MAX_FILENAME_SIZE];
	int rnd;
	int rnd_size;
	int i;
	int j;
	int k;
	clock_t begin_time, end_time, take_time;
	struct timeval start_t, end_t, temp_t;
	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env, thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, 10, 0);

	if ((tmp = (char *) malloc((size_t) buffersize)) == NULL) {
		return ERROR_NOT_ALLOCATION;
	}

	srand(0);
	memset(tmp, 0x11, buffersize);

	const char *mPath = (*env)->GetStringUTFChars(env, path, 0);
	result_t->tv_sec=0; result_t->tv_usec=0;

	for (k = 0; k < num_dir; k++) {
		for (i = 0; i < num_trans; i++) {
			rnd = (337*i) % num_file;
			rnd_size = rand() % (num_file*num_dir);
			sprintf(filename, "%s/%d/pm%d", mPath, k, rnd);
			begin_time = clock();
			gettimeofday(&start_t, NULL);
			if ((fd = open(filename, O_RDWR | O_CREAT | O_TRUNC, 0666)) < 0) {
				return ERROR_NOT_OPEN;
			}
			for (j = 0;	j < (__PM_GET_SIZE(rnd_size, filesize_min, filesize_max, buffersize) / buffersize); j++) {
				__WRITE_HELPER(fd, tmp, buffersize);
			}
			fsync(fd);
			close(fd);
			gettimeofday(&end_t, NULL);
			end_time = clock();
			take_time += (end_time - begin_time);

			timersub(&end_t, &start_t, &temp_t); timeradd(&temp_t, result_t, result_t);
			
			// Call JAVA method for notice percentage
			(*env)->CallVoidMethod(env, thiz, publish, 10, i + k * num_trans);
		}
	}

	return take_time;
}

JNIEXPORT jlong Java_com_andromeda_androbenchplus_Result_PM_1READ(JNIEnv * env,
		jobject thiz, jstring path, jint num_dir, jint num_file, jint num_trans,
		jint filesize_min, jint filesize_max, jint buffersize, struct timeval* result_t) {
	int fd;
	char *tmp;
	char filename[MAX_FILENAME_SIZE];
	int i;
	int j;
	int k;
	int rnd;
	int rnd_file;
	int rnd_dir;
	clock_t begin_time, end_time, take_time;
	struct timeval start_t, end_t, temp_t;

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env, thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, 11, 0);

	if ((tmp = (char *) malloc((size_t) buffersize)) == NULL) {
		return ERROR_NOT_ALLOCATION;
	}

	srand(0);

	take_time = 0;
	const char *mPath = (*env)->GetStringUTFChars(env, path, 0);

	result_t->tv_sec=0; result_t->tv_usec=0;
	
	for (i = 0; i < num_trans; i++) {
		rnd = rand() % (num_file * num_dir);
		rnd_file = rand() % num_file;
		rnd_dir = rand() % num_dir;
		sprintf(filename, "%s/%d/pm%d", mPath, rnd_dir, rnd_file);

		begin_time = clock();
		gettimeofday(&start_t, NULL);
		if ((fd = open(filename, O_RDWR, 0666)) < 0) {
			return ERROR_NOT_OPEN;
		}

		for (j = 0;	j < (__PM_GET_SIZE(rnd, filesize_min, filesize_max,	buffersize) / buffersize); j++) {
			read(fd, tmp, buffersize);
		}
		close(fd);
		gettimeofday(&end_t, NULL);
		end_time = clock();
		take_time += (end_time - begin_time);
		timersub(&end_t, &start_t, &temp_t); timeradd(&temp_t, result_t, result_t);

		// Call JAVA method for notice percentage
		(*env)->CallVoidMethod(env, thiz, publish, 11, i);
	}

	return take_time;
}

JNIEXPORT jlong Java_com_andromeda_androbenchplus_Result_PM_1APPEND(
		JNIEnv * env, jobject thiz, jstring path, jint num_dir, jint num_file,
		jint num_trans, jint filesize_min, jint filesize_max, jint buffersize, struct timeval* result_t) {
	int fd;
	char *tmp;
	char filename[MAX_FILENAME_SIZE];
	int i;
	int j;
	int rnd;
	int rnd_dir;
	clock_t begin_time, end_time, take_time;
	struct timeval start_t, end_t, temp_t;

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env, thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, 12, 0);

	if ((tmp = (char *) malloc((size_t) buffersize)) == NULL) {
		return ERROR_NOT_ALLOCATION;
	}
	memset(tmp, 0x22, buffersize);

	srand(0);

	take_time = 0;
	const char *mPath = (*env)->GetStringUTFChars(env, path, 0);

	result_t->tv_sec=0; result_t->tv_usec=0;
	
	for (i = 0; i < num_trans; i++) {
		rnd = rand() % num_file;
		rnd_dir = rand() % num_dir;
		sprintf(filename, "%s/%d/pm%d", mPath, rnd_dir, rnd);

		begin_time = clock();
		gettimeofday(&start_t, NULL);
		if ((fd = open(filename, O_RDWR | O_APPEND, 0666)) < 0) {
			return ERROR_NOT_OPEN;
		}
		for (j = 0;	j< (__PM_GET_SIZE(rnd, filesize_min, filesize_max,buffersize) / buffersize); j++) {
			__WRITE_HELPER(fd, tmp, buffersize);
		}
		fsync(fd);
		close(fd);
		gettimeofday(&end_t, NULL);
		end_time = clock();
		take_time += (end_time - begin_time);
		timersub(&end_t, &start_t, &temp_t); timeradd(&temp_t, result_t, result_t);

		// Call JAVA method for notice percentage
		(*env)->CallVoidMethod(env, thiz, publish, 12, i);
	}

	return take_time;
}

JNIEXPORT jlong Java_com_andromeda_androbenchplus_Result_PM_1REMOVEALL(
		JNIEnv * env, jobject thiz, jstring path, jint num_dir, jint num_file, struct timeval* result_t) {
	char filename[MAX_FILENAME_SIZE];
	int i;
	int j;
	clock_t begin_time, end_time, take_time;
	struct timeval start_t, end_t, temp_t;

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env, thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, 13, 0);

	take_time = 0;
	const char *mPath = (*env)->GetStringUTFChars(env, path, 0);

	result_t->tv_sec=0; result_t->tv_usec=0;
	
	for (j = 0; j < num_dir; j++) {
		for (i = 0; i < num_file; i++) {
			sprintf(filename, "%s/%d/pm%d", mPath, j, i);

			begin_time = clock();
			gettimeofday(&start_t, NULL);
			remove(filename);

			gettimeofday(&end_t, NULL);
			end_time = clock();
			take_time += (end_time - begin_time);
			timersub(&end_t, &start_t, &temp_t); timeradd(&temp_t, result_t, result_t);

			// Call JAVA method for notice percentage
			(*env)->CallVoidMethod(env, thiz, publish, 13, j * num_file + i);
		}
		sprintf(filename, "%s/%d", mPath, j);

		begin_time = clock();
		rmdir(filename);

		end_time = clock();
		take_time += (end_time - begin_time);
	}

	return take_time;
}
