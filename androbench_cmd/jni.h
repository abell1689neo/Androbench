#include <stdio.h>
#include <sys/time.h>

#define JNIEXPORT	
#define JNICALL

char *jni_target_path;

typedef int 	jint;
typedef void *	jobject;
typedef char *	jstring;
typedef long	jlong;

typedef int		jmethodID;
typedef int 	jclass;

typedef struct jnienv_struct * JNIEnv;

struct jnienv_struct {
	jclass	(*GetObjectClass) (JNIEnv *env, jobject thiz);
	jmethodID	(*GetMethodID) (JNIEnv *env, jclass targetClass, jstring name, jstring proto);
	char * (*GetStringUTFChars) (JNIEnv *env, jstring path, jint x);
	void	(*CallVoidMethod) (JNIEnv *env, jobject thiz, jmethodID publish, jint x, jint y);

};

extern JNIEnv jenv;

extern jint Java_com_andromeda_androbenchplus_Result_INIT_1READ (JNIEnv *, jobject, jstring, jint, jint);
extern jint Java_com_andromeda_androbenchplus_Result_FINAL (JNIEnv *, jobject, jstring);
extern jlong Java_com_andromeda_androbenchplus_Result_SEQ_1READ (JNIEnv *, jobject, jstring, jint, jint, struct timeval*);

extern jlong Java_com_andromeda_androbenchplus_Result_SEQ_1WRITE (JNIEnv *, jobject, jstring, jint, jint, struct timeval*);
extern jlong Java_com_andromeda_androbenchplus_Result_RND_1READ (JNIEnv *, jobject, jstring, jint, jint, jint, struct timeval*);
extern jlong Java_com_andromeda_androbenchplus_Result_RND_1WRITE (JNIEnv *, jobject, jstring, jint, jint, jint, struct timeval*);
extern jlong Java_com_andromeda_androbenchplus_Result_PM_1INITIALIZE (JNIEnv *, jobject, jstring, jint, jint, jint, jint, jint, struct timeval*);
extern jlong Java_com_andromeda_androbenchplus_Result_PM_1DELETE (JNIEnv *, jobject, jstring, jint, jint, jint, struct timeval*);
extern jlong Java_com_andromeda_androbenchplus_Result_PM_1CREATE (JNIEnv *, jobject, jstring, jint, jint, jint, jint, jint, jint, struct timeval*);
extern jlong Java_com_andromeda_androbenchplus_Result_PM_1READ (JNIEnv *, jobject, jstring, jint, jint, jint, jint, jint, jint, struct timeval*);
extern jlong Java_com_andromeda_androbenchplus_Result_PM_1APPEND (JNIEnv *, jobject, jstring, jint, jint, jint, jint, jint, jint, struct timeval*);
extern jlong Java_com_andromeda_androbenchplus_Result_PM_1REMOVEALL (JNIEnv *, jobject, jstring, jint, jint, struct timeval*);

#define JNI(f)	Java_com_andromeda_androbenchplus_Result_##f

extern int INIT_1READ (int reclen, int filesize);
extern int FINAL ();
extern long SEQ_1READ (int reclen, int filesize, struct timeval* result_t);
extern long SEQ_1WRITE (int reclen, int filesize, struct timeval* result_t);
extern long RND_1READ (int reclen, int filesize, int rndSeed, struct timeval* result_t);
extern long RND_1WRITE (int reclen, int filesize, int rndSeed, struct timeval* result_t);
extern long PM_1INITIALIZE (int num_dir, int num_file, int filesize_min, int filesize_max, int buffersize, struct timeval* result_t);
extern long PM_1DELETE (int num_dir, int num_file, int num_trans, struct timeval* result_t);
extern long PM_1CREATE (int num_dir, int num_file, int num_trans, int filesize_min, int filesize_max, int buffersize, struct timeval* result_t);
extern long PM_1READ (int num_dir, int num_file, int num_trans, int filesize_min, int filesize_max, int buffersize, struct timeval* result_t);
extern long PM_1APPEND (int num_dir, int num_file, int num_trans, int filesize_min, int filesize_max, int buffersize, struct timeval* result_t);
extern long PM_1REMOVEALL (int num_dir, int num_file, struct timeval* result_t);

extern void drop_caches ();

