#include <stdlib.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

static SLObjectItf engine_obj;
static SLEngineItf engine_engine;
static SLObjectItf output_mix_obj;
static SLObjectItf player_obj;
static SLPlayItf player_play;
static SLAndroidSimpleBufferQueueItf buffer_queue;
static int currentOutputIndex;
static int currentOutputBuffer;
static short *outputBuffer[2];
static int outBufSamples;
static void* outlock;
static double time;


static void player_callback(SLAndroidSimpleBufferQueueItf bq, void *context)
{

}

static int opensl_init()
{
	SLresult r;

	r = slCreateEngine(&engine_obj, 0, NULL, 0, NULL, NULL);
	if (r != SL_RESULT_SUCCESS) 
		return -1;

	r = (*engine_obj)->Realize(engine_obj, SL_BOOLEAN_FALSE);
	if (r != SL_RESULT_SUCCESS) 
		return -1;

	r = (*engine_obj)->GetInterface(engine_obj, SL_IID_ENGINE,
				&engine_engine);
	if (r != SL_RESULT_SUCCESS) 
		return -1;

	return 0;
}


static int opensl_open(int sr)
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
		return -1;
	}

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
		return -1;
	
	/* realize output mix */
	r = (*output_mix_obj)->Realize(output_mix_obj, SL_BOOLEAN_FALSE);
	if (r != SL_RESULT_SUCCESS) 
		return -1;

	SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {
		SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2
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
		return -1;

	/* realize player */
	r = (*player_obj)->Realize(player_obj, SL_BOOLEAN_FALSE);
	if (r != SL_RESULT_SUCCESS) 
		return -1;

	/* get play interface */
	r = (*player_obj)->GetInterface(player_obj, SL_IID_PLAY, &player_play);
	if (r != SL_RESULT_SUCCESS) 
		return -1;

	/* get buffer queue interface */
	r = (*player_obj)->GetInterface(player_obj,
			SL_IID_ANDROIDSIMPLEBUFFERQUEUE, &buffer_queue);
	if (r != SL_RESULT_SUCCESS) 
		return -1;

	/* register callback on buffer queue */
	r = (*buffer_queue)->RegisterCallback(buffer_queue, player_callback, 0);
	if (r != SL_RESULT_SUCCESS) 
		return -1;

	/* set player state to playing */
	r = (*player_play)->SetPlayState(player_play, SL_PLAYSTATE_PLAYING);
	if (r != SL_RESULT_SUCCESS) 
		return -1;

	return 0;
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
	free(outputBuffer[0]);
	free(outputBuffer[1]);
}

int open_audio(int rate, int frames)
{
	if (opensl_init() < 0) {
		goto err;
	}

	if (opensl_open(rate) < 0) {
		goto err;
	}

	return 0;

    err:
	close_audio();
	return -1;
}

