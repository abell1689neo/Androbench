#include "jni.h"
#include <stdlib.h>

//#define PREFIX	Java_com_andromeda_androbenchplus_Result

jclass jniGetObjectClass (JNIEnv *env, jobject thiz)
{
	    return 0;
}

jmethodID jniGetMethodID (JNIEnv *env, jclass targetClass, jstring name, jstring proto)
{
	    return 0;
}


void jniCallback (JNIEnv *env, jobject thiz, jmethodID publish, jint x, jint y)
{
//    printf ("publish = %d, x = %d, y = %d\n", publish, x, y);
}

char *jniGetStringUTFChars (JNIEnv *env, jstring path, jint x)
{
	    return jni_target_path;
}


struct jnienv_struct jnienv = {
		jniGetObjectClass,
		jniGetMethodID,
		jniGetStringUTFChars,
		jniCallback
};

JNIEnv jenv = &jnienv;


int INIT_1READ (int reclen, int filesize)
{
	return JNI(INIT_1READ) (&jenv, NULL, NULL, reclen, filesize);
}

int FINAL ()
{
	return JNI(FINAL) (&jenv, NULL, NULL);
}

long SEQ_1READ (int reclen, int filesize, struct timeval* result_t)
{
	return JNI(SEQ_1READ) (&jenv, NULL, NULL, reclen, filesize, result_t);
}


long SEQ_1WRITE (int reclen, int filesize, struct timeval* result_t)
{
	return JNI(SEQ_1WRITE) (&jenv, NULL, NULL, reclen, filesize, result_t);
}

long RND_1READ (int reclen, int filesize, int rndSeed, struct timeval* result_t)
{
	return JNI(RND_1READ) (&jenv, NULL, NULL, reclen, filesize, rndSeed, result_t);
}

long RND_1WRITE (int reclen, int filesize, int rndSeed, struct timeval* result_t)
{
	return JNI(RND_1WRITE) (&jenv, NULL, NULL, reclen, filesize, rndSeed, result_t);
}

long PM_1INITIALIZE (int num_dir, int num_file, int filesize_min, int filesize_max, int buffersize, struct timeval* result_t)
{
	return JNI(PM_1INITIALIZE) (&jenv, NULL, NULL, num_dir, num_file, filesize_min, filesize_max, buffersize, result_t);
}

long PM_1DELETE (int num_dir, int num_file, int num_trans, struct timeval* result_t)
{
	return JNI(PM_1DELETE) (&jenv, NULL, NULL, num_dir, num_file, num_trans, result_t);
}

long PM_1CREATE (int num_dir, int num_file, int num_trans, int filesize_min, int filesize_max, int buffersize, struct timeval* result_t)
{
	return JNI(PM_1CREATE) (&jenv, NULL, NULL, num_dir, num_file, num_trans, filesize_min, filesize_max, buffersize, result_t);
}

long PM_1READ (int num_dir, int num_file, int num_trans, int filesize_min, int filesize_max, int buffersize, struct timeval* result_t)
{
	return JNI(PM_1READ) (&jenv, NULL, NULL, num_dir, num_file, num_trans, filesize_min, filesize_max, buffersize, result_t);
}

long PM_1APPEND (int num_dir, int num_file, int num_trans, int filesize_min, int filesize_max, int buffersize, struct timeval* result_t)
{
	return JNI(PM_1APPEND) (&jenv, NULL, NULL, num_dir, num_file, num_trans, filesize_min, filesize_max, buffersize, result_t);
}

long PM_1REMOVEALL (int num_dir, int num_file, struct timeval* result_t)
{
	return JNI(PM_1REMOVEALL) (&jenv, NULL, NULL, num_dir, num_file, result_t);
}

void drop_caches ()
{
	system ("sync; echo 3 > /proc/sys/vm/drop_caches; sync; echo 0 > /proc/sys/vm/drop_caches; sync;");
}

	

