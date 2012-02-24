/* Simple interface adaptor for jni */

#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include "xmp.h"

/* #include <android/log.h> */

#define NCH 64

extern struct xmp_drv_info drv_smix;

static xmp_context ctx = NULL;
static struct xmp_module_info mi;

static int _playing = 0;
static int _cur_vol[NCH];
static int _ins[NCH];
static int _key[NCH];
static int _decay = 8;


/* For ModList */
JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_init(JNIEnv *env, jobject obj)
{
	if (ctx != NULL)
		return;

	ctx = xmp_create_context();
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_deinit(JNIEnv *env, jobject obj)
{
	xmp_free_context(ctx);
	return 0;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_loadModule(JNIEnv *env, jobject obj, jstring name)
{
	const char *filename;
	int res;

	filename = (*env)->GetStringUTFChars(env, name, NULL);
	/* __android_log_print(ANDROID_LOG_DEBUG, "libxmp", "%s", filename); */
	res = xmp_load_module(ctx, (char *)filename);
	(*env)->ReleaseStringUTFChars(env, name, filename);

	return res;
}

JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_testModule(JNIEnv *env, jobject obj, jstring name, jobject info)
{
	const char *filename;
	int res;
	struct xmp_test_info ti;

	filename = (*env)->GetStringUTFChars(env, name, NULL);
	/* __android_log_print(ANDROID_LOG_DEBUG, "libxmp", "%s", filename); */
	res = xmp_test_module((char *)filename, &ti);
	(*env)->ReleaseStringUTFChars(env, name, filename);

	if (res == 0) {
		if (info != NULL) {
			jclass testInfoClass = (*env)->FindClass(env,
	                        	"org/helllabs/java/xmp/Xmp.ModuleInfo");
			jfieldID field;
	
			if (testInfoClass == NULL)
				return JNI_FALSE;
			
			field = (*env)->GetFieldID(env, testInfoClass, "name",
	                        	"Ljava/lang/String;");
			if (field == NULL)
				return JNI_FALSE;
			(*env)->SetObjectField(env, info, field,
					(*env)->NewStringUTF(env, ti.name));
	
			field = (*env)->GetFieldID(env, testInfoClass, "type",
	                        	"Ljava/lang/String;");
			if (field == NULL)
				return JNI_FALSE;
			(*env)->SetObjectField(env, info, field,
					(*env)->NewStringUTF(env, ti.type));
		}

		return JNI_TRUE;
	}

	return JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_releaseModule(JNIEnv *env, jobject obj)
{
	xmp_release_module(ctx);
	return 0;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_startPlayer(JNIEnv *env, jobject obj, jint start, jint rate, jint flags)
{
	int i;

	for (i = 0; i < NCH; i++) {
		_key[i] = -1;
	}

	_playing = 1;
	return xmp_player_start(ctx, start, rate, flags);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_endPlayer(JNIEnv *env, jobject obj)
{
	_playing = 0;
	xmp_player_end(ctx);
	return 0;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_playFrame(JNIEnv *env, jobject obj)
{
	int i, ret;

	ret = xmp_player_frame(ctx);
	xmp_player_get_info(ctx, &mi);

	return ret;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getBuffer(JNIEnv *env, jobject obj, jshortArray buffer)
{
	(*env)->SetShortArrayRegion(env, buffer, 0, mi.buffer_size, mi.buffer);
	return mi.buffer_size / 2;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_nextOrd(JNIEnv *env, jobject obj)
{
	return xmp_ord_next(ctx);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_prevOrd(JNIEnv *env, jobject obj)
{
	return xmp_ord_prev(ctx);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_setOrd(JNIEnv *env, jobject obj, jint n)
{
	return xmp_ord_set(ctx, n);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_stopModule(JNIEnv *env, jobject obj)
{
	return xmp_mod_stop(ctx);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_restartModule(JNIEnv *env, jobject obj)
{
	return xmp_mod_restart(ctx);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_incGvol(JNIEnv *env, jobject obj)
{
	return xmp_gvol_inc(ctx);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_decGvol(JNIEnv *env, jobject obj)
{
	return xmp_gvol_dec(ctx);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_seek(JNIEnv *env, jobject obj, jint time)
{
	return xmp_seek_time(ctx, time * 100);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_time(JNIEnv *env, jobject obj)
{
	return _playing ? mi.time : -1;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getPlayTempo(JNIEnv *env, jobject obj)
{
	return mi.tempo;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getPlayBpm(JNIEnv *env, jobject obj)
{
	return mi.bpm;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getPlayPos(JNIEnv *env, jobject obj)
{
	return mi.order;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getPlayPat(JNIEnv *env, jobject obj)
{
	return mi.mod->pat;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getLoopCount(JNIEnv *env, jobject obj)
{
	return mi.loop_count;
}

JNIEXPORT jstring JNICALL
Java_org_helllabs_android_xmp_Xmp_getVersion(JNIEnv *env, jobject obj)
{
	return (*env)->NewStringUTF(env, VERSION);
}

JNIEXPORT jobjectArray JNICALL
Java_org_helllabs_android_xmp_Xmp_getFormats(JNIEnv *env, jobject obj)
{
	jstring s;
	jclass stringClass;
	jobjectArray stringArray;
	int i, num;
	char **list;
	char buf[80];

	list = xmp_get_format_list();
	for (num = 0; list[num] != NULL; num++);

	stringClass = (*env)->FindClass(env,"java/lang/String");
	if (stringClass == NULL)
		return NULL;

	stringArray = (*env)->NewObjectArray(env, num, stringClass, NULL);
	if (stringArray == NULL)
		return NULL;

	for (i = 0; i < num; i++) {
		s = (*env)->NewStringUTF(env, list[i]);
		(*env)->SetObjectArrayElement(env, stringArray, i, s);
		(*env)->DeleteLocalRef(env, s);
	}

	return stringArray;
}

JNIEXPORT jstring JNICALL
Java_org_helllabs_android_xmp_Xmp_getTitle(JNIEnv *env, jobject obj)
{
	return (*env)->NewStringUTF(env, mi.mod->name);
}

JNIEXPORT jobjectArray JNICALL
Java_org_helllabs_android_xmp_Xmp_getInstruments(JNIEnv *env, jobject obj)
{
	jstring s;
	jclass stringClass;
	jobjectArray stringArray;
	int i;
	char buf[80];

	stringClass = (*env)->FindClass(env,"java/lang/String");
	if (stringClass == NULL)
		return NULL;

	xmp_player_get_info(ctx, &mi);

	stringArray = (*env)->NewObjectArray(env, mi.mod->ins, stringClass, NULL);
	if (stringArray == NULL)
		return NULL;

	for (i = 0; i < mi.mod->ins; i++) {
		snprintf(buf, 80, "%s", mi.mod->xxi[i].name);
		s = (*env)->NewStringUTF(env, buf);
		(*env)->SetObjectArrayElement(env, stringArray, i, s);
		(*env)->DeleteLocalRef(env, s);
	}

	return stringArray;
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getChannelData(JNIEnv *env, jobject obj, jintArray vol, jintArray ins, jintArray key)
{
	int i;

	for (i = 0; i < NCH; i++) {
		if (_key[i] > 0) {
			_cur_vol[i] = mi.channel_info[i].volume;
			_key[i] = -1;
		} else {
			_cur_vol[i] -= _decay;
			if (_cur_vol[i] < 0)
				_cur_vol[i] = 0;
		}
	}

	(*env)->SetIntArrayRegion(env, vol, 0, NCH, _cur_vol);
	(*env)->SetIntArrayRegion(env, ins, 0, NCH, _ins);
	(*env)->SetIntArrayRegion(env, key, 0, NCH, _key);
}
