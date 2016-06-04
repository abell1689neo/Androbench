#define _GNU_SOURCE
#include <jni.h>
#include <stdio.h>
#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>
#include <time.h>
#include <string.h>
#include <errno.h>


#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <linux/fs.h>

#include <android/log.h>
#define LOG_TAG "Shuffle Data"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);

#define MAX_FILENAME_SIZE	1024
#define KILOBYTE			1024		// 1KB
#define MEGABYTE			1024*1024	// 10MB
#define NS_PER_S			1000000000

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
typedef struct __INS_MACRO{
	int IOtype;
	int IOsize;
	char IOfn[MAX_FILENAME_SIZE];
	int IOnum;
}ST_INS_MACRO;

// Write system call helper
int __WRITE_HELPER(int fd, const char *strTarget, int iSize);
int __SHUFFLE(unsigned short* Src, int recs, int rndSeed);

// Default Page Size for O_DIRECT (Memory Align)
long long page_size = 4096;

// Environment of calling android in JAVA Class's method
jclass targetClass;
jmethodID publish;

/*******************************************************************************************
* Micro benchmarks :: Initializing for sequential & random read benchmarks 
*******************************************************************************************/
JNIEXPORT jint Java_com_andromeda_androbench2_Result_INIT_1READ
   (JNIEnv * env, jobject thiz, jint target, jstring path, jint file_flag, jint reclen, jint filesize)
{

	int fd;
	char *tmp, *stmp;
	char filename[MAX_FILENAME_SIZE];
	int i;
	int recs;

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env,thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");

	// set benchmark path (/data, /mnt/sdcard, custom path for external sdcard)
	if(target == TEST_TARGET_DATA){
		strcpy(filename, "/data/data/com.andromeda.androbench2/files/disk_perf_test_read.tmp");
	}else if(target == TEST_TARGET_SDCARD){
		mkdir("/sdcard/.androbench2", 0777);
		strcpy(filename, "/sdcard/.androbench2/disk_perf_test_read.tmp");
	}else if(target == TEST_TARGET_EXTSD){
		const char *extPath = (*env)->GetStringUTFChars(env, path, 0);
		mkdir(extPath, 0777);
		strcpy(filename, extPath);
		strcat(filename, "/disk_perf_test_read.tmp");
	}

	if((fd = open(filename, O_RDWR | O_CREAT | O_SYNC | O_TRUNC, 0666)) < 0){
		return ERROR_NOT_OPEN;
	}

	if((tmp = (char *)malloc((size_t)reclen)) == NULL){
		return ERROR_NOT_ALLOCATION;
	}
	memset(tmp, 0x11, reclen);


	(*env)->CallVoidMethod(env, thiz, publish, 0, 0);
	recs = filesize/reclen;
	for(i=0; i<recs; i++){
		if(__WRITE_HELPER(fd, tmp, reclen) < 0)
			return ERROR_NOT_WRITE;

		fsync(fd);

		// Call JAVA method for percentage
		(*env)->CallVoidMethod(env, thiz, publish, 0, reclen * i);
	}
	(*env)->CallVoidMethod(env, thiz, publish, 0, filesize);

	free(stmp);
	close(fd);

	return 0;
}

/*******************************************************************************************
* Micro benchmarks :: Initilizing for purge cache 
*******************************************************************************************/
JNIEXPORT jint Java_com_andromeda_androbench2_Result_INIT_1PURGE_1CACHE
  (JNIEnv * env, jobject thiz, jint file_size)
{
	int fd;
	char *tmp, *stmp;
	char filename[MAX_FILENAME_SIZE];
	int i;
	int recs;
	int reclen;
	int processing = 1;
	struct stat s;

	mkdir("/sdcard/.androbench2", 0777);
	strcpy(filename, "/sdcard/.androbench2/purge_buffer.tmp");

	if(lstat(filename, &s)<0){
		processing = 1;
	}else{
		if(s.st_size >= file_size){
			processing = 0;
		}else{
			processing = 1;
		}
	}

	if(processing == 1){
		// set environment of calling android in JAVA Class's method
		targetClass = (*env)->GetObjectClass(env,thiz);
		publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");

		reclen = MEGABYTE;

		if((fd = open(filename, O_RDWR | O_CREAT | O_SYNC | O_TRUNC, 0666)) < 0){
			return ERROR_NOT_OPEN;
		}

		if((tmp = (char *)malloc((size_t)reclen)) == NULL){
			return ERROR_NOT_ALLOCATION;
		}
		memset(tmp, 0x11, reclen);

		(*env)->CallVoidMethod(env, thiz, publish, 0, 0);
		recs = file_size/reclen;
		for(i=0; i<recs; i++){
			if(__WRITE_HELPER(fd, tmp, reclen) < 0)
				return ERROR_NOT_WRITE;

			fsync(fd);
			// Call JAVA method for percentage
			(*env)->CallVoidMethod(env, thiz, publish, 6, reclen*i);
			
		}
		(*env)->CallVoidMethod(env, thiz, publish, 6, file_size);

		free(stmp);
		close(fd);
	}
}

/*******************************************************************************************
* Micro benchmarks :: Purge cache 
*******************************************************************************************/
JNIEXPORT void Java_com_andromeda_androbench2_Result_PURGE_1CACHE
  (JNIEnv * env, jobject thiz, jint file_size)
{
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

	strcpy(filename, "/sdcard/.androbench2/purge_buffer.tmp");
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
}

/*******************************************************************************************
* Micro benchmarks :: Delete temporary files
*******************************************************************************************/
JNIEXPORT jint JNICALL Java_com_andromeda_androbench2_Result_FINAL
  (JNIEnv * env, jobject thiz, jint target, jstring path)
{

	if(target == TEST_TARGET_DATA){
		unlink("/data/data/com.andromeda.androbench2/files/disk_perf_test_read.tmp");
		unlink("/data/data/com.andromeda.androbench2/files/disk_perf_test.tmp");
	}else if(target == TEST_TARGET_SDCARD){
		unlink("/sdcard/.androbench2/disk_perf_test_read.tmp");
		unlink("/sdcard/.androbench2/disk_perf_test.tmp");
	}else if(target == TEST_TARGET_EXTSD){
		char filename_r[MAX_FILENAME_SIZE];
		char filename_w[MAX_FILENAME_SIZE];
		const char *extPath = (*env)->GetStringUTFChars(env, path, 0);
		strcpy(filename_r, extPath);
		strcat(filename_r, "/disk_perf_test_read.tmp");
		strcpy(filename_w, extPath);
		strcat(filename_w, "/disk_perf_test.tmp");
		unlink(filename_r);
		unlink(filename_w);
	}
}

/*******************************************************************************************
* Micro benchmarks :: Sequential read benchmarks 
*******************************************************************************************/
JNIEXPORT jlong Java_com_andromeda_androbench2_Result_SEQ_1READ
  (JNIEnv * env, jobject thiz, jint target, jstring path, jint file_flag, jint reclen, jint filesize)
{
	int fd;
	int recs;
	int i;
	int percent;
	char *tmp, *stmp;
	char filename[MAX_FILENAME_SIZE];
	struct timespec tv_start, tv_stop;
	long take_time;

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env,thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, 1, 0);

	char *buf = (char*)malloc(128);

	take_time = 0;

	if(target == TEST_TARGET_DATA){
		strcpy(filename, "/data/data/com.andromeda.androbench2/files/disk_perf_test_read.tmp");
	}else if(target == TEST_TARGET_SDCARD){
		strcpy(filename, "/sdcard/.androbench2/disk_perf_test_read.tmp");
	}else if(target == TEST_TARGET_EXTSD){
		const char *extPath = (*env)->GetStringUTFChars(env, path, 0);
		strcpy(filename, extPath);
		strcat(filename, "/disk_perf_test_read.tmp");
	}

	if(file_flag == FILE_FLAG_SYNC){
		if((fd = open(filename, O_RDONLY)) < 0){
			return ERROR_NOT_OPEN;
		}
	}else if(file_flag == FILE_FLAG_DIRECT){
		if((fd = open(filename, O_RDONLY | O_DIRECT)) < 0){
			return ERROR_NOT_OPEN;
		}
	}

	page_size = getpagesize();
	if((tmp = (char *)malloc((size_t)reclen + page_size)) == NULL){
		return ERROR_NOT_ALLOCATION;
	}
	stmp = (char *)(((long)tmp+(long)page_size) & (long)~(page_size-1));
	
	recs = filesize/reclen;

	for(i=0; i<recs; i++){
		clock_gettime(CLOCK_MONOTONIC, &tv_start);
		if(read(fd,stmp,reclen) != reclen){
			return ERROR_NOT_READ;
		}
		clock_gettime(CLOCK_MONOTONIC, &tv_stop);
		take_time += (tv_stop.tv_sec - tv_start.tv_sec) * CLOCKS_PER_SEC + (tv_stop.tv_nsec - tv_start.tv_nsec)/(NS_PER_S/CLOCKS_PER_SEC);
		
		// Call JAVA method for notice percentage
		(*env)->CallVoidMethod(env, thiz, publish, 1, reclen*i);
		
	}
	(*env)->CallVoidMethod(env, thiz, publish, 1, filesize);

	free(tmp);
	close(fd);
	return take_time;

}

/*******************************************************************************************
* Micro benchmarks :: Sequential write benchmarks 
*******************************************************************************************/
JNIEXPORT jlong Java_com_andromeda_androbench2_Result_SEQ_1WRITE
  (JNIEnv * env, jobject thiz, jint target, jstring path, jint file_flag, jint reclen, jint filesize)
{
	int fd;
	int recs;
	int i;
	char *tmp, *stmp;
	char filename[MAX_FILENAME_SIZE];
	struct timespec tv_start, tv_stop;
	long take_time;

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env,thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, 2, 0);

	take_time = 0;
	
	if(target == TEST_TARGET_DATA){
		strcpy(filename, "/data/data/com.andromeda.androbench2/files/disk_perf_test.tmp");
	}else if(target == TEST_TARGET_SDCARD){
		mkdir("/sdcard/.androbench2", 0777);
		strcpy(filename, "/sdcard/.androbench2/disk_perf_test.tmp");
	}else if(target == TEST_TARGET_EXTSD){
		const char *extPath = (*env)->GetStringUTFChars(env, path, 0);
		strcpy(filename, extPath);
		strcat(filename, "/disk_perf_test.tmp");
	}

	if((fd = open(filename, O_RDWR | O_CREAT | O_SYNC | O_TRUNC, 0666)) < 0){
		return ERROR_NOT_OPEN;
	}

	if((tmp = (char *)malloc((size_t)reclen)) == NULL){
		return ERROR_NOT_ALLOCATION;
	}
	memset(tmp, 0x11, reclen);
	recs = filesize/reclen;

	//system("sync");
	sync();

	for(i=0; i<recs; i++){
		clock_gettime(CLOCK_MONOTONIC, &tv_start);
		if(__WRITE_HELPER(fd, tmp, reclen) < 0)
			return ERROR_NOT_WRITE;
		fsync(fd);
		clock_gettime(CLOCK_MONOTONIC, &tv_stop);
		take_time += (tv_stop.tv_sec - tv_start.tv_sec) * CLOCKS_PER_SEC + (tv_stop.tv_nsec - tv_start.tv_nsec)/(NS_PER_S/CLOCKS_PER_SEC);

		// Call JAVA method for notice percentage
		 (*env)->CallVoidMethod(env, thiz, publish, 2, reclen*i);
	}
	(*env)->CallVoidMethod(env, thiz, publish, 2, filesize);

	free(tmp);
	close(fd);

	return take_time;
}

/*******************************************************************************************
* Micro benchmarks :: Random read benchmarks 
*******************************************************************************************/
JNIEXPORT jlong Java_com_andromeda_androbench2_Result_RND_1READ
  (JNIEnv * env, jobject thiz, jint target, jstring path, jint file_flag, jint reclen, jint filesize, jint rndSeed)
{
	int fd;
	int recs;
	int i, j;
	int lpNum;
	char *tmp, *stmp;
	char filename[MAX_FILENAME_SIZE];
	struct timespec tv_start, tv_stop;
	long take_time;
	unsigned long long big_rand;
	unsigned short *offset;

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env,thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, 3, 0);

	take_time = 0;

	if(target == TEST_TARGET_DATA){
		strcpy(filename, "/data/data/com.andromeda.androbench2/files/disk_perf_test_read.tmp");
	}else if(target == TEST_TARGET_SDCARD){
		strcpy(filename, "/sdcard/.androbench2/disk_perf_test_read.tmp");
	}else if(target == TEST_TARGET_EXTSD){
		const char *extPath = (*env)->GetStringUTFChars(env, path, 0);
		strcpy(filename, extPath);
		strcat(filename, "/disk_perf_test_read.tmp");
	}

	if(file_flag == FILE_FLAG_SYNC){
		if((fd = open(filename, O_RDONLY)) < 0){
			return ERROR_NOT_OPEN;
		}
	}else if(file_flag == FILE_FLAG_DIRECT){
		if((fd = open(filename, O_RDONLY | O_DIRECT)) < 0){
			return ERROR_NOT_OPEN;
		}
	}

	page_size = getpagesize();
	if((tmp = (char *)malloc((size_t)reclen + page_size)) == NULL){
		return ERROR_NOT_ALLOCATION;
	}
	stmp = (char *)(((long)tmp+(long)page_size) & (long)~(page_size-1));
	
	recs = filesize/reclen;

	if(recs > 65535){
		return ERROR_NOT_CREATE_RNDNUM;
	}
	
	offset = (unsigned short*)malloc(sizeof(unsigned short)*recs);
	memset(offset, 0, sizeof(unsigned short)*recs);

	for(i=0; i<recs; i++)
		offset[i] = (unsigned short)i;

	__SHUFFLE(offset, recs, rndSeed);

	lpNum = 0;
	for(i = 0; i < ((recs-1)/RND_GRP_NUMBER)+1; i++){
		if(i == ((recs-1)/RND_GRP_NUMBER))
			lpNum = recs%RND_GRP_NUMBER;
		else
			lpNum = RND_GRP_NUMBER;

		clock_gettime(CLOCK_MONOTONIC, &tv_start);
		for(j = 0; j < lpNum; j++){
			lseek(fd, (offset[(i*RND_GRP_NUMBER)+j] * reclen), SEEK_SET);
			if(read(fd, stmp, reclen) != reclen){
				return ERROR_NOT_READ;
			}
		}
		clock_gettime(CLOCK_MONOTONIC, &tv_stop);
		take_time += (tv_stop.tv_sec - tv_start.tv_sec) * CLOCKS_PER_SEC + (tv_stop.tv_nsec - tv_start.tv_nsec)/(NS_PER_S/CLOCKS_PER_SEC);

		// Call JAVA method for notice percentage
		(*env)->CallVoidMethod(env, thiz, publish, 3, reclen*(i*RND_GRP_NUMBER));
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
JNIEXPORT jlong Java_com_andromeda_androbench2_Result_RND_1WRITE
  (JNIEnv * env, jobject thiz, jint target, jstring path, jint file_flag, jint reclen, jint filesize, jint rndSeed)
{
	int fd;
	int recs;
	int i, j;
	int lpNum;
	char *tmp, *stmp;
	char filename[MAX_FILENAME_SIZE];
	struct timespec tv_start, tv_stop;
	long take_time;
	unsigned long long big_rand;
	unsigned short *offset;

	// set environment of calling android in JAVA Class's method
	targetClass = (*env)->GetObjectClass(env,thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, 4, 0);

	take_time = 0;

	srand48(rndSeed);
	
	if(target == TEST_TARGET_DATA){
		strcpy(filename, "/data/data/com.andromeda.androbench2/files/disk_perf_test.tmp");
	}else if(target == TEST_TARGET_SDCARD){
		strcpy(filename, "/sdcard/.androbench2/disk_perf_test.tmp");
	}else if(target == TEST_TARGET_EXTSD){
		const char *extPath = (*env)->GetStringUTFChars(env, path, 0);
		strcpy(filename, extPath);
		strcat(filename, "/disk_perf_test.tmp");
	}

	if((fd = open(filename, O_RDWR | O_CREAT | O_SYNC | O_TRUNC, 0666)) < 0){
		return ERROR_NOT_OPEN;
	}

	if((tmp = (char *)malloc((size_t)reclen)) == NULL){
		return ERROR_NOT_ALLOCATION;
	}
	memset(tmp, 0x11, reclen);

	recs = filesize/reclen;

	if(recs > 65535){
		return ERROR_NOT_CREATE_RNDNUM;
	}

	offset = (unsigned short*)malloc(sizeof(unsigned short)*recs);
	memset(offset, 0, sizeof(unsigned short)*recs);

	for(i=0; i<recs; i++)
		offset[i] = (unsigned short)i;

	__SHUFFLE(offset, recs, rndSeed);

	lpNum = 0;
	for(i = 0; i < ((recs-1)/RND_GRP_NUMBER)+1; i++){
		if(i == ((recs-1)/RND_GRP_NUMBER))
			lpNum = recs%RND_GRP_NUMBER;
		else
			lpNum = RND_GRP_NUMBER;

		clock_gettime(CLOCK_MONOTONIC, &tv_start);
		for(j = 0; j < lpNum; j++){
			lseek(fd, (offset[(i*RND_GRP_NUMBER)+j] * reclen), SEEK_SET);
			if(__WRITE_HELPER(fd, tmp, reclen) < 0)
				return ERROR_NOT_WRITE;
		}
		clock_gettime(CLOCK_MONOTONIC, &tv_stop);
		take_time += (tv_stop.tv_sec - tv_start.tv_sec) * CLOCKS_PER_SEC + (tv_stop.tv_nsec - tv_start.tv_nsec)/(NS_PER_S/CLOCKS_PER_SEC);

		// Call JAVA method for notice percentage
		(*env)->CallVoidMethod(env, thiz, publish, 4, reclen*(i*RND_GRP_NUMBER));
	}
	(*env)->CallVoidMethod(env, thiz, publish, 4, filesize);

	free(tmp);
	close(fd);

	return take_time;
}

/*******************************************************************************************
* Macro benchmarks :: Replay script files
*******************************************************************************************/
JNIEXPORT jlong JNICALL Java_com_andromeda_androbench2_Result_MACRO
  (JNIEnv * env, jobject thiz, jint target, jstring path, jstring scriptName, jint numScript, jint scriptLine)
{
	int fd;
	char *tmp;
	int i = 0;
	int j = 0;

	struct timespec tv_start, tv_stop;
	long take_time;

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
	targetClass = (*env)->GetObjectClass(env,thiz);
	publish = (*env)->GetMethodID(env, targetClass, "changeDialog", "(II)V");
	(*env)->CallVoidMethod(env, thiz, publish, 12, ((numScript-1)*2) + 0);

	strcpy(filename, "/data/data/com.andromeda.androbench2/files/");
	const char *scriptFileName = (*env)->GetStringUTFChars(env, scriptName, 0);
	strcat(filename, scriptFileName);

	if(target == TEST_TARGET_DATA){
		strcpy(targetPathName, "/data/data/com.andromeda.androbench2/files/script");
	}else if(target == TEST_TARGET_SDCARD){
		system("mkdir /sdcard/.androbench2");
		strcpy(targetPathName, "/sdcard/.androbench2/script");
	}else if(target == TEST_TARGET_EXTSD){
		const char *extPath = (*env)->GetStringUTFChars(env, path, 0);
		strcpy(targetPathName, extPath);
		strcpy(funcDirName, "mkdir ");
		strcat(funcDirName, targetPathName);
		system(funcDirName);
		strcat(targetPathName, "/script");
	}

	strcpy(funcDirName, "mkdir ");
	strcat(funcDirName, targetPathName);
	system(funcDirName);

	scriptFile = fopen(filename, "r");

	instruction = (ST_INS_MACRO *)malloc(sizeof(ST_INS_MACRO)*scriptLine);

	cnt = 0;
	while(!feof(scriptFile)){
		printf("Size of ST_INS_MACRO: %d\n", sizeof(ST_INS_MACRO));
		printf("Step2\n");
		fscanf(scriptFile, "%d %d %s %d", &(instruction[cnt].IOtype), &(instruction[cnt].IOsize), targetFileName, &(instruction[cnt].IOnum));
		sprintf(instruction[cnt].IOfn, "%s%s%s", targetPathName, "/", targetFileName);

		if(maxBufferSize < instruction[cnt].IOsize)
			maxBufferSize = instruction[cnt].IOsize;

		cnt++;
	}

	fclose(scriptFile);

	if((tmp = (char *)malloc((size_t)maxBufferSize)) == NULL)
		return ERROR_NOT_ALLOCATION;
	memset(tmp, 0x11, maxBufferSize);


	(*env)->CallVoidMethod(env, thiz, publish, 12, ((numScript-1)*2) + 1);


	sync();

	clock_gettime(CLOCK_MONOTONIC, &tv_start);

	for(i = 0; i < cnt; i++){
		for(j = 0; j < instruction[i].IOnum; j++){		// Multiple instruction
			switch(instruction[i].IOtype){
			case 1:
				if((fd = open(instruction[i].IOfn, O_WRONLY | O_CREAT | O_TRUNC | O_LARGEFILE, 0666)) < 0)
					return ERROR_NOT_OPEN;
				break;
			case 2:
				//fsync(fd);
				if(close(fd) < 0)
					return ERROR_NOT_CLOSE;
				break;
			case 3:
				break;
			case 4:
				if(__WRITE_HELPER(fd, tmp, instruction[i].IOsize) < 0)
					return ERROR_NOT_WRITE;
				break;
			default:
				break;
			}
		}
	}

	sync();
	
	clock_gettime(CLOCK_MONOTONIC, &tv_stop);
	take_time += (tv_stop.tv_sec - tv_start.tv_sec) * CLOCKS_PER_SEC + (tv_stop.tv_nsec - tv_start.tv_nsec)/(NS_PER_S/CLOCKS_PER_SEC);

	free(tmp);
	free(instruction);

	strcpy(funcDirName, "rmdir -r ");
	strcat(funcDirName, targetPathName);
	system(funcDirName);

	(*env)->CallVoidMethod(env, thiz, publish, 12, ((numScript-1)*2) + 2);

	return take_time;

}

int __WRITE_HELPER(int fd, const char *strTarget, int iSize){
	int total = 0;
	int ret = 0;

	while (total < iSize){
		ret = write (fd, strTarget, (size_t)iSize);
		if (ret < 0) return -1;
		total += ret;
	}

	return total;
}

int __SHUFFLE(unsigned short* Src, int recs, int rndSeed){
	int i=0;
	int big_rand_fst = 0;
	int big_rand_snd = 0;
	unsigned short tmp = 0;

	srand48(rndSeed);
	for(i=0; i < recs; i++){
		big_rand_fst = i;
		big_rand_snd = lrand48()%recs;

		tmp = Src[big_rand_fst];
		Src[big_rand_fst] = Src[big_rand_snd];
		Src[big_rand_snd] = tmp;
	}
}
