#ifndef XMP_JNI_AUDIO_H
#define XMP_JNI_AUDIO_H

#define INC(x,max) do { \
	if (((x) + 1) >= (max)) { (x) = 0; } \
	else { (x)++; } \
} while (0)

int open_audio(int, int);
void close_audio(void);
int play_audio(void);
void stop_audio(void);
void restart_audio(void);
int play_buffer(void *, int, int);
int has_free_buffer(void);
int fill_buffer(int);

#endif
