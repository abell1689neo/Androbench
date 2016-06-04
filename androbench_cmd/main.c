#include <stdio.h>
#include <time.h>
#include <sys/time.h>

#include "jni.h"
#include "iniparser.h"

struct _config {
	int readFileSize;
	int writeFileSize;
	int seqBufSize;
	int rndBufSize;
	int rndSeed;

	int pmNumDir;
	int pmNumFile;
	int pmNumTrans;
	int pmFileSizeMin;
	int pmFileSizeMax;
	int pmBufSize;
};

int parse_ini_file(char*, struct _config*);

int main (int argc, char** argv)
{
	long t;
	int status;
	struct _config config;
	double result_c;
	int iter;
	char commandBuf[1024];
	struct timeval result_t;


	if(argc!=2) {
		printf("Usage : %s [Target Path]\n", argv[0]);
		return -1;
	}
	jni_target_path = argv[1];

	// Parse parameters here..
	status = parse_ini_file("default.conf", &config);
	if(status<0) return -1;

	// Microbenchmark Start
	INIT_1READ (config.seqBufSize, config.readFileSize);
	drop_caches ();
	
	// Sequential Read
	t = SEQ_1READ (config.seqBufSize, config.readFileSize, &result_t);
	drop_caches ();
	result_c = (double)(t)/CLOCKS_PER_SEC;
	printf("SEQ_READ : %ld.%.6ld\n", result_t.tv_sec, result_t.tv_usec);

	// Sequential Write
	t = SEQ_1WRITE (config.seqBufSize, config.writeFileSize, &result_t);
	drop_caches ();
	result_c = (double)(t)/CLOCKS_PER_SEC;
	printf("SEQ_WRITE : %ld.%.6ld\n", result_t.tv_sec, result_t.tv_usec);

	// Random Read
	t = RND_1READ (config.rndBufSize, config.readFileSize, config.rndSeed, &result_t);
	drop_caches ();
	result_c = (double)(t)/CLOCKS_PER_SEC;
	printf("RND_READ : %ld.%.6ld\n", result_t.tv_sec, result_t.tv_usec);

	// Random Write
	t = RND_1WRITE (config.rndBufSize, config.writeFileSize, config.rndSeed, &result_t);
	drop_caches ();
	result_c = (double)(t)/CLOCKS_PER_SEC;
	printf("RND_WRITE : %ld.%.6ld\n", result_t.tv_sec, result_t.tv_usec);
	
	//Micro Benchmark finalize
	FINAL();
	
	// Postermark Start
	// Postermark INIT
	t = PM_1INITIALIZE(config.pmNumDir, config.pmNumFile, config.pmFileSizeMin, config.pmFileSizeMax, config.pmBufSize, &result_t);
	result_c = (double)(t)/CLOCKS_PER_SEC;
	printf("PM_INIT : %ld.%.6ld\n", result_t.tv_sec, result_t.tv_usec);
	drop_caches ();
	
	// Postermark Delete
	t = PM_1DELETE(config.pmNumDir, config.pmNumFile, config.pmNumTrans, &result_t);
	result_c = (double)(t)/CLOCKS_PER_SEC;
	printf("PM_DELETE : %ld.%.6ld\n", result_t.tv_sec, result_t.tv_usec);
	drop_caches ();
	
	// Postermark Create
	t = PM_1CREATE(config.pmNumDir, config.pmNumFile, config.pmNumTrans, config.pmFileSizeMin, config.pmFileSizeMax, config.pmBufSize, &result_t);
	result_c = (double)(t)/CLOCKS_PER_SEC;
	printf("PM_CREATE : %ld.%.6ld\n", result_t.tv_sec, result_t.tv_usec);
	drop_caches ();
	
	// Postermark Read
	t = PM_1READ(config.pmNumDir, config.pmNumFile, config.pmNumTrans, config.pmFileSizeMin, config.pmFileSizeMax, config.pmBufSize, &result_t);
	result_c = (double)(t)/CLOCKS_PER_SEC;
	printf("PM_READ : %ld.%.6ld\n", result_t.tv_sec, result_t.tv_usec);
	drop_caches ();
	
	// Postermark Append
	t = PM_1APPEND(config.pmNumDir, config.pmNumFile, config.pmNumTrans, config.pmFileSizeMin, config.pmFileSizeMax, config.pmBufSize, &result_t);
	result_c = (double)(t)/CLOCKS_PER_SEC;
	printf("PM_APPEND : %ld.%.6ld\n", result_t.tv_sec, result_t.tv_usec);
	drop_caches ();
	
	// Postermark Remove All
	t = PM_1REMOVEALL(config.pmNumDir, config.pmNumFile, &result_t);
	result_c = (double)(t)/CLOCKS_PER_SEC;
	printf("PM_REMOVEALL : %ld.%.6ld\n", result_t.tv_sec, result_t.tv_usec);
	drop_caches ();


	sprintf(commandBuf,"rm -rf %s", jni_target_path);
	system(commandBuf);
}

int parse_ini_file(char* ini_name, struct _config* config) {
	dictionary* ini;
	int test;

	ini = iniparser_load(ini_name);
	if(ini==NULL) {
		fprintf(stderr, "cannot parse file: %s\n", ini_name);
		return -1;
	}
	
  config->readFileSize	= iniparser_getint(ini, "MicroBench:ReadFileSize", -1) * 1024;
  config->writeFileSize = iniparser_getint(ini, "MicroBench:WriteFileSize", -1) * 1024;
  config->seqBufSize		= iniparser_getint(ini, "MicroBench:SeqBufferSize", -1) * 1024;
  config->rndBufSize		= iniparser_getint(ini, "MicroBench:RndBufferSize", -1) * 1024;
  config->rndSeed				= iniparser_getint(ini, "MicroBench:RndSeed", -1);
	
	config->pmNumDir			= iniparser_getint(ini, "Postermark:NumDirs", -1);
	config->pmNumFile			= iniparser_getint(ini, "Postermark:NumFiles", -1);
	config->pmNumTrans		= iniparser_getint(ini, "Postermark:NumTrans", -1);
	config->pmFileSizeMin	= iniparser_getint(ini, "Postermark:FileSizeMin", -1) * 1024;
	config->pmFileSizeMax	= iniparser_getint(ini, "Postermark:FileSizeMax", -1) * 1024;
	config->pmBufSize			= iniparser_getint(ini, "Postermark:BufferSize", -1) * 1024;

	iniparser_freedict(ini);
	return 0;
}
