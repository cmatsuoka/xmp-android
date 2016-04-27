#ifndef XMP_JNI_AUDIO_H
#define XMP_JNI_AUDIO_H

#define INC(x,max) do { \
	if (++(x) >= (max)) { (x) = 0; } \
} while (0)

#define DEC(x,max) do { \
	if (--(x) < 0) { (x) = (max) - 1; } \
} while (0)

int open_audio(int, int);
void close_audio(void);
int play_audio(void);
int stop_audio(void);
int restart_audio(void);
int play_buffer(void *, int, int);
int has_free_buffer(void);
int fill_buffer(int);
int get_volume(void);
int set_volume(int);

#endif
