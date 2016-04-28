#include <stdlib.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <pthread.h>
#include "audio.h"

/* #include <android/log.h> */

static SLObjectItf engine_obj;
static SLEngineItf engine_engine;
static SLObjectItf output_mix_obj;
static SLObjectItf player_obj;
static SLPlayItf player_play;
static SLVolumeItf player_vol;
static SLAndroidSimpleBufferQueueItf buffer_queue;
static char *buffer;
static int buffer_num;
static int buffer_size;
static volatile int first_free, last_free;
static int playing;
static pthread_mutex_t _lock;

#define TAG "Xmp"
#define BUFFER_TIME 40

#define lock()   pthread_mutex_lock(&_lock)
#define unlock() pthread_mutex_unlock(&_lock)


static void player_callback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
	INC(last_free, buffer_num);

	/* underrun, shouldn't happen */
	if (last_free == first_free) {
		DEC(last_free, buffer_num);
	}
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

	/* initialize lock */
	if (pthread_mutex_init(&_lock, NULL) != 0) {
		return -1;
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

	SLDataSource audio_source = { &loc_bufq, &format_pcm };

	/* configure audio sink */
	SLDataLocator_OutputMix loc_outmix = {
		SL_DATALOCATOR_OUTPUTMIX,
		output_mix_obj
	};

	SLDataSink audio_sink = { &loc_outmix, NULL };

	/* create audio player */
	const SLInterfaceID ids1[] = {
		SL_IID_VOLUME, SL_IID_ANDROIDSIMPLEBUFFERQUEUE
	};
	const SLboolean req1[] = {
		SL_BOOLEAN_TRUE
	};

	r = (*engine_engine)->CreateAudioPlayer(engine_engine, &player_obj,
			&audio_source, &audio_sink, 2, ids1, req1);
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

	/* get volume interface */
	r = (*player_obj)->GetInterface(player_obj, SL_IID_VOLUME, &player_vol);
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
	pthread_mutex_destroy(&_lock);
	return -1;
}

static void opensl_close()
{
	lock();

	if (player_obj != NULL)
		(*player_obj)->Destroy(player_obj);
	if (output_mix_obj != NULL)
		(*output_mix_obj)->Destroy(output_mix_obj);
	if (engine_obj != NULL)
		(*engine_obj)->Destroy(engine_obj);

	player_obj = NULL;
	output_mix_obj = NULL;
	engine_obj = NULL;

	player_play = NULL;
	buffer_queue = NULL;

	unlock();
	pthread_mutex_destroy(&_lock);
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

	if (buffer_num < 3)
		buffer_num = 3;

	buffer = malloc(buffer_size * buffer_num);
	if (buffer == NULL)
		return -1;

	ret = opensl_open(rate, buffer_num);
	if (ret < 0)
		return ret;

	first_free = 0;
	last_free = buffer_num - 1;

	return buffer_num;
}

void flush_audio()
{
	SLAndroidSimpleBufferQueueState state;

	lock();
	if (buffer_queue != NULL) {
		(*buffer_queue)->GetState(buffer_queue, &state);
	}

	while (state.count != 0) {
		usleep(10000);
		if (buffer_queue != NULL) {
			(*buffer_queue)->GetState(buffer_queue, &state);
		}

	}
	unlock();
}

void drop_audio()
{
	lock();

	if (buffer_queue != NULL) {
		(*buffer_queue)->Clear(buffer_queue);
	}

	first_free = 0;
	last_free = buffer_num - 1;

	unlock();
}

int play_audio()
{
	SLresult r = SL_RESULT_SUCCESS;

	flush_audio();

	/* set player state to playing */
	if (restart_audio() < 0)
		return -1;

	return 0;
}

int has_free_buffer()
{
	return last_free != first_free;
}

int fill_buffer(int looped)
{
	int ret;

	/* fill and enqueue buffer */
	char *b = &buffer[first_free * buffer_size];
	INC(first_free, buffer_num);

	ret = play_buffer(b, buffer_size, looped);
	lock();
	if (buffer_queue != NULL) {
		(*buffer_queue)->Enqueue(buffer_queue, b, buffer_size);
	}
	unlock();

	return ret;
}

int restart_audio()
{
	int i, ret;

	/* enqueue initial buffers */
	while (has_free_buffer()) {
		fill_buffer(0);
	}

	lock();
	if (player_play != NULL) {
		ret = (*player_play)->SetPlayState(player_play,
					SL_PLAYSTATE_PLAYING);
	}
	unlock();

	return ret == SL_RESULT_SUCCESS ? 0 : -1;
}

int stop_audio()
{
	int ret;

	drop_audio();

	lock();
	if (player_play != NULL) {
		ret = (*player_play)->SetPlayState(player_play,
					SL_PLAYSTATE_STOPPED);
	}
	unlock();

	return ret == SL_RESULT_SUCCESS ? 0 : -1;
}

int get_volume()
{
	SLmillibel vol;
	SLresult r;

	r = (*player_vol)->GetVolumeLevel(player_vol, &vol);
	return r == SL_RESULT_SUCCESS ? -vol : -1;
}

int set_volume(int vol)
{
	SLresult r;
	r = (*player_vol)->SetVolumeLevel(player_vol, (SLmillibel)-vol);
	return r == SL_RESULT_SUCCESS ? 0 : -1;
}
