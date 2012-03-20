/* Simple interface adaptor for jni */

#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include "xmp.h"

/* #include <android/log.h> */

static xmp_context ctx = NULL;
static struct xmp_module_info mi;

static int _playing = 0;
static int _cur_vol[XMP_MAX_CHANNELS];
static int _hold_vol[XMP_MAX_CHANNELS];
static int _pan[XMP_MAX_CHANNELS];
static int _ins[XMP_MAX_CHANNELS];
static int _key[XMP_MAX_CHANNELS];
static int _period[XMP_MAX_CHANNELS];
static int _finalvol[XMP_MAX_CHANNELS];
static int _last_key[XMP_MAX_CHANNELS];
static int _pos[XMP_MAX_CHANNELS];
static int _decay = 4;

#define MAX_BUFFER_SIZE 256
static char _buffer[MAX_BUFFER_SIZE];


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
	/* xmp_free_context(ctx); */
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

	xmp_player_get_info(ctx, &mi);

	return res;
}

JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_testModule(JNIEnv *env, jobject obj, jstring name, jobject info)
{
	const char *filename;
	int i, res;
	struct xmp_test_info ti;

	filename = (*env)->GetStringUTFChars(env, name, NULL);
	/* __android_log_print(ANDROID_LOG_DEBUG, "libxmp", "%s", filename); */
	res = xmp_test_module((char *)filename, &ti);

	/* If the module title is empty, use the file basename */
	for (i = strlen(ti.name) - 1; i >= 0; i--) {
		if (ti.name[i] == ' ') {
			ti.name[i] = 0;
		} else {
			break;
		}
	}
	if (strlen(ti.name) == 0) {
		const char *x = strrchr(filename, '/');
		if (x == NULL) {
			x = filename;
		}
		strncpy(ti.name, x + 1, XMP_NAME_SIZE);
	}

	(*env)->ReleaseStringUTFChars(env, name, filename);

	if (res == 0) {
		if (info != NULL) {
			jclass modInfoClass = (*env)->FindClass(env,
	                        	"org/helllabs/android/xmp/ModInfo");
			jfieldID field;
	
			if (modInfoClass == NULL)
				return JNI_FALSE;
			
			field = (*env)->GetFieldID(env, modInfoClass, "name",
	                        	"Ljava/lang/String;");
			if (field == NULL)
				return JNI_FALSE;
			(*env)->SetObjectField(env, info, field,
					(*env)->NewStringUTF(env, ti.name));
	
			field = (*env)->GetFieldID(env, modInfoClass, "type",
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

	for (i = 0; i < XMP_MAX_CHANNELS; i++) {
		_key[i] = -1;
		_last_key[i] = -1;
	}

	_playing = 1;
	return xmp_player_start(ctx, rate, flags);
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
	return xmp_seek_time(ctx, time);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_time(JNIEnv *env, jobject obj)
{
	return _playing ? mi.time : -1;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_mute(JNIEnv *env, jobject obj, jint chn, jint status)
{
	return xmp_channel_mute(ctx, chn, status);
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getInfo(JNIEnv *env, jobject obj, jintArray values)
{
	int v[7];

	v[0] = mi.order;
	v[1] = mi.pattern;
	v[2] = mi.row;
	v[3] = mi.num_rows;
	v[4] = mi.frame;
	v[5] = mi.speed;
	v[6] = mi.bpm;

	(*env)->SetIntArrayRegion(env, values, 0, 7, v);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_setMixerAmp(JNIEnv *env, jobject obj, jint amp)
{
	return xmp_mixer_amp(ctx, amp);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_setMixerMix(JNIEnv *env, jobject obj, jint mix)
{
	return xmp_mixer_mix(ctx, mix);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getPlaySpeed(JNIEnv *env, jobject obj)
{
	return mi.speed;
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
	return mi.pattern;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getLoopCount(JNIEnv *env, jobject obj)
{
	return mi.loop_count;
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getModVars(JNIEnv *env, jobject obj, jintArray vars)
{
	int v[6];

	v[0] = mi.total_time;
	v[1] = mi.mod->len;
	v[2] = mi.mod->pat;
	v[3] = mi.mod->chn;
	v[4] = mi.mod->ins;
	v[5] = mi.mod->smp;

	(*env)->SetIntArrayRegion(env, vars, 0, 6, v);
}

JNIEXPORT jstring JNICALL
Java_org_helllabs_android_xmp_Xmp_getVersion(JNIEnv *env, jobject obj)
{
	char buf[20];
	snprintf(buf, 20, "%d.%d.%d",
		(xmp_version & 0x00ff0000) >> 16,
		(xmp_version & 0x0000ff00) >> 8,
		(xmp_version & 0x000000ff));

	return (*env)->NewStringUTF(env, buf);
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
Java_org_helllabs_android_xmp_Xmp_getModName(JNIEnv *env, jobject obj)
{
	return (*env)->NewStringUTF(env, mi.mod->name);
}

JNIEXPORT jstring JNICALL
Java_org_helllabs_android_xmp_Xmp_getModType(JNIEnv *env, jobject obj)
{
	return (*env)->NewStringUTF(env, mi.mod->type);
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

static struct xmp_subinstrument *get_subinstrument(int ins, int key)
{
	if (ins >= 0 && ins < mi.mod->ins) {
		if (mi.mod->xxi[ins].map[key].ins != 0xff) {
			int mapped = mi.mod->xxi[ins].map[key].ins;
			return &mi.mod->xxi[ins].sub[mapped];
		}
	}

	return NULL;
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getChannelData(JNIEnv *env, jobject obj, jintArray vol, jintArray finalvol, jintArray pan, jintArray ins, jintArray key, jintArray period)
{
	struct xmp_subinstrument *sub;
	int chn = mi.mod->chn;
	int i;

	for (i = 0; i < chn; i++) {
                struct xmp_channel_info *ci = &mi.channel_info[i];

		if (ci->event.vol > 0) {
			_hold_vol[i] = ci->event.vol * 0x40 / mi.vol_base;
		}

		_cur_vol[i] -= _decay;
		if (_cur_vol[i] < 0) {
			_cur_vol[i] = 0;
		}

		if (ci->event.note > 0 && ci->event.note <= 0x80) {
			_key[i] = ci->event.note - 1;
			_last_key[i] = _key[i];
			sub = get_subinstrument(ci->instrument, _key[i]);
			if (sub != NULL) {
				_cur_vol[i] = sub->vol;
			}
		} else {
			_key[i] = -1;
		}

		if (ci->event.vol > 0) {
			_key[i] = _last_key[i];
			_cur_vol[i] = ci->event.vol * 0x40 / mi.vol_base;
		}

		_ins[i] = (signed char)ci->instrument;
		_finalvol[i] = ci->volume;
		_pan[i] = ci->pan;
		_period[i] = ci->period;
	}

	(*env)->SetIntArrayRegion(env, vol, 0, chn, _cur_vol);
	(*env)->SetIntArrayRegion(env, finalvol, 0, chn, _finalvol);
	(*env)->SetIntArrayRegion(env, pan, 0, chn, _pan);
	(*env)->SetIntArrayRegion(env, ins, 0, chn, _ins);
	(*env)->SetIntArrayRegion(env, key, 0, chn, _key);
	(*env)->SetIntArrayRegion(env, period, 0, chn, _period);
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getPatternRow(JNIEnv *env, jobject obj, jint pat, jint row, jbyteArray rowNotes, jbyteArray rowInstruments)
{
	struct xmp_pattern *xxp;
	unsigned char row_note[XMP_MAX_CHANNELS];
	unsigned char row_ins[XMP_MAX_CHANNELS];
	int chn;
	int i;

	if (mi.mod == NULL || pat > mi.mod->pat || row > mi.mod->xxp[pat]->rows)
		return;

 	xxp = mi.mod->xxp[pat];
	chn = mi.mod->chn;

	for (i = 0; i < chn; i++) {
		struct xmp_track *xxt = mi.mod->xxt[xxp->index[i]];
		struct xmp_event *e = &xxt->event[row];

		row_note[i] = e->note;
		row_ins[i] = e->ins;
	}

	(*env)->SetByteArrayRegion(env, rowNotes, 0, chn, row_note);
	(*env)->SetByteArrayRegion(env, rowInstruments, 0, chn, row_ins);
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getSampleData(JNIEnv *env, jobject obj, jint trigger, jint ins, jint key, jint period, jint chn, jint width, jbyteArray buffer)
{
	struct xmp_subinstrument *sub;
	struct xmp_sample *xxs;
	int i, pos, transient_size;
	int limit;
	int step, lps, lpe;

	if (width > MAX_BUFFER_SIZE) {
		width = MAX_BUFFER_SIZE;
	}

	if (period == 0) {
		goto err;
	}

	if (ins < 0 || ins > mi.mod->ins || key > 0x80) {
		goto err;
	}

	sub = get_subinstrument(ins, key);
	if (sub == NULL || sub->sid < 0 || sub->sid >= mi.mod->smp) {
		goto err;
	}

	xxs = &mi.mod->xxs[sub->sid];
	if (xxs == NULL || xxs->flg & XMP_SAMPLE_SYNTH) {
		goto err;
	}

	pos = _pos[chn];

	/* In case of new keypress, reset sample */
	if (trigger > 0) {
		pos = 0;
	}

	step = (XMP_PERIOD_BASE << 5) / period;
	lps = xxs->lps << 5;
	lpe = xxs->lpe << 5;

	/* Limit is the buffer size or the remaining transient size */
	if (xxs->flg & XMP_SAMPLE_LOOP) {
		transient_size = (xxs->lps - pos) / step;
	} else {
		transient_size = (xxs->len - pos) / step;
	}
	if (transient_size < 0) {
		transient_size = 0;
	}

	limit = width;
	if (limit > transient_size) {
		limit = transient_size;
	}

	if (xxs->flg & XMP_SAMPLE_16BIT) {
		/* transient */
		for (i = 0; i < limit; i++) {
			_buffer[i] = ((short *)&xxs->data)[pos >> 5] / 256;
			pos += step;
		}

		/* loop */
		if (xxs->flg & XMP_SAMPLE_LOOP) {
			for (i = limit; i < width; i++) {
				_buffer[i] = ((short *)xxs->data)[pos >> 5];	
				pos += step;
				if (pos >= xxs->lpe) {
					pos = xxs->lps + pos - xxs->lpe;
				}
			}
		} else {
			for (i = limit; i < width; i++) {
				_buffer[i] = 0;	
			}
		}
	} else {
		/* transient */
		for (i = 0; i < limit; i++) {
			_buffer[i] = xxs->data[pos >> 5];
			pos += step;
		}

		/* loop */
		if (xxs->flg & XMP_SAMPLE_LOOP) {
			for (i = limit; i < width; i++) {
				_buffer[i] = xxs->data[pos >> 5];
				pos += step;
				if (pos >= xxs->lpe) {
					pos = xxs->lps + pos - xxs->lpe;
				}
			}
		} else {
			for (i = limit; i < width; i++) {
				_buffer[i] = 0;	
			}
		}
	}

	_pos[chn] = pos;

	(*env)->SetByteArrayRegion(env, buffer, 0, width, _buffer);
	return;

    err:
	memset(_buffer, 0, width);
	(*env)->SetByteArrayRegion(env, buffer, 0, width, _buffer);
}
