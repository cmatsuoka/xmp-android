#include <stdlib.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include "audio.h"

/* #include <android/log.h> */

static SLObjectItf engine_obj;
static SLEngineItf engine_engine;
static SLObjectItf output_mix_obj;
static SLObjectItf player_obj;
static SLPlayItf player_play;
static SLAndroidSimpleBufferQueueItf buffer_queue;
static char *buffer;
static int buffer_num;
static int buffer_size;
static volatile int first_free, last_free;
static int playing;

#define TAG "Xmp"
#define BUFFER_TIME 40


static void player_callback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
	INC(last_free, buffer_num);
}

static int opensl_open(int sr, int num)
{
	SLresult r;
	SLuint32 rate;

	switch (sr) {
	case 8000:
		rate = SL_SAMPLINGRATE_8;
		break;
	case 22050:
		rate = SL_SAMPLINGRATE_22_05;
		break;
	case 44100:
		rate = SL_SAMPLINGRATE_44_1;
		break;
	case 48000:
		rate = SL_SAMPLINGRATE_48;
		break;
	default:
		goto err;
	}

	/* create engine */
	r = slCreateEngine(&engine_obj, 0, NULL, 0, NULL, NULL);
	if (r != SL_RESULT_SUCCESS) 
		goto err;

	r = (*engine_obj)->Realize(engine_obj, SL_BOOLEAN_FALSE);
	if (r != SL_RESULT_SUCCESS) 
		goto err;

	r = (*engine_obj)->GetInterface(engine_obj, SL_IID_ENGINE,
				&engine_engine);
	if (r != SL_RESULT_SUCCESS) 
		goto err1;

	/* create output mix */
	const SLInterfaceID ids[] = {
		SL_IID_VOLUME
	};
	const SLboolean req[] = {
		SL_BOOLEAN_FALSE
	};

	r = (*engine_engine)->CreateOutputMix(engine_engine,
				&output_mix_obj, 1, ids, req);
	if (r != SL_RESULT_SUCCESS) 
		goto err1;
	
	/* realize output mix */
	r = (*output_mix_obj)->Realize(output_mix_obj, SL_BOOLEAN_FALSE);
	if (r != SL_RESULT_SUCCESS) 
		goto err1;

	SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {
		SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, num
	};

	SLDataFormat_PCM format_pcm = {
		SL_DATAFORMAT_PCM, 2, rate,
		SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
		SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
		SL_BYTEORDER_LITTLEENDIAN
	};

	SLDataSource audioSrc = { &loc_bufq, &format_pcm };

	/* configure audio sink */
	SLDataLocator_OutputMix loc_outmix = {
		SL_DATALOCATOR_OUTPUTMIX,
		output_mix_obj
	};

	SLDataSink audioSnk = { &loc_outmix, NULL };

	/* create audio player */
	const SLInterfaceID ids1[] = {
		SL_IID_ANDROIDSIMPLEBUFFERQUEUE
	};
	const SLboolean req1[] = {
		SL_BOOLEAN_TRUE
	};

	r = (*engine_engine)->CreateAudioPlayer(engine_engine, &player_obj,
			&audioSrc, &audioSnk, 1, ids1, req1);
	if (r != SL_RESULT_SUCCESS) 
		goto err2;

	/* realize player */
	r = (*player_obj)->Realize(player_obj, SL_BOOLEAN_FALSE);
	if (r != SL_RESULT_SUCCESS) 
		goto err2;

	/* get play interface */
	r = (*player_obj)->GetInterface(player_obj, SL_IID_PLAY, &player_play);
	if (r != SL_RESULT_SUCCESS) 
		goto err3;

	/* get buffer queue interface */
	r = (*player_obj)->GetInterface(player_obj,
			SL_IID_ANDROIDSIMPLEBUFFERQUEUE, &buffer_queue);
	if (r != SL_RESULT_SUCCESS) 
		goto err3;

	/* register callback on buffer queue */
	r = (*buffer_queue)->RegisterCallback(buffer_queue, player_callback, 0);
	if (r != SL_RESULT_SUCCESS) 
		goto err3;

	return 0;

    err3:
	(*player_obj)->Destroy(player_obj);
    err2:
	(*output_mix_obj)->Destroy(output_mix_obj);
    err1:
	(*engine_obj)->Destroy(engine_obj);
    err:
	return -1;
}

static void opensl_close()
{
	(*player_obj)->Destroy(player_obj);
	(*output_mix_obj)->Destroy(output_mix_obj);
	(*engine_obj)->Destroy(engine_obj);
}

void close_audio()
{
	opensl_close();
	free(buffer);
}

int open_audio(int rate, int latency)
{
	int ret;

	buffer_num = latency / BUFFER_TIME;
	buffer_size = rate * 2 * 2 * BUFFER_TIME / 1000;
	buffer = malloc(buffer_size * buffer_num);
	if (buffer == NULL)
		return -1;

	ret = opensl_open(rate, buffer_num);
	if (ret < 0)
		return ret;

	return buffer_num;
}

int play_audio()
{
	SLresult r;
	int i;

	/* enqueue initial buffers */
	for (i = 0; i < buffer_num; i++) {
		char *b = &buffer[i * buffer_size];
		play_buffer(b, buffer_size);
		(*buffer_queue)->Enqueue(buffer_queue, b, buffer_size);
	}

	last_free = first_free = 0;

	/* set player state to playing */
	r = (*player_play)->SetPlayState(player_play, SL_PLAYSTATE_PLAYING);
	if (r != SL_RESULT_SUCCESS) 
		return -1;
}

int has_free_buffer()
{
	return last_free != first_free;
}

void fill_buffer()
{
	/* fill and enqueue buffer */
	char *b = &buffer[first_free * buffer_size];
	INC(first_free, buffer_num);

	play_buffer(b, buffer_size);
	(*buffer_queue)->Enqueue(buffer_queue, b, buffer_size);
}

void stop_audio()
{
	(*player_play)->SetPlayState(player_play, SL_PLAYSTATE_STOPPED);
}
