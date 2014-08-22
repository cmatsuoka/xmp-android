#ifndef XMP_JNI_AUDIO_H
#define XMP_JNI_AUDIO_H

int open_audio(int, int);
void close_audio(void);
int play_audio(void);
void stop_audio(void);
int play_buffer(void *, int);

#endif
