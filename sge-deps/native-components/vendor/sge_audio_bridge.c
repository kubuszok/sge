/*
 * SGE Audio Bridge — C wrapper around miniaudio for the SGE game engine.
 *
 * Exposes 37 sge_audio_* C ABI functions consumed by:
 *   - Desktop JVM via Panama FFM (java.lang.foreign)
 *   - Scala Native via @extern
 *
 * Architecture:
 *   Engine  → ma_engine (high-level miniaudio engine with built-in mixer)
 *   Sound   → ma_audio_buffer + ma_sound (PCM data in memory, multiple instances)
 *   Music   → ma_sound with MA_SOUND_FLAG_STREAM (file-based streaming)
 *   Device  → ma_device (raw PCM output for custom audio)
 *
 * Handle pattern: heap-allocated structs, returned as int64_t to caller.
 * 0 = null/invalid handle.
 *
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 * miniaudio is public domain / MIT-0 by David Reid.
 */

#define MINIAUDIO_IMPLEMENTATION
#include "miniaudio/miniaudio.h"

#include <stdlib.h>
#include <string.h>
#include <stdint.h>

/* ─── Helper: convert bit depth to ma_format ───────────────────────────── */

static ma_format bit_depth_to_format(int bit_depth) {
    switch (bit_depth) {
        case 8:  return ma_format_u8;
        case 16: return ma_format_s16;
        case 24: return ma_format_s24;
        case 32: return ma_format_s32;
        default: return ma_format_s16;
    }
}

/* ─── SgeSound: owns decoded PCM data for creating playback instances ──── */

typedef struct {
    void*     pPCMData;        /* heap-allocated decoded PCM data             */
    ma_uint64 frameCount;      /* number of frames in pPCMData                */
    ma_format format;          /* sample format (e.g. ma_format_s16)          */
    ma_uint32 channels;        /* number of channels                          */
    ma_uint32 sampleRate;      /* sample rate in Hz                           */
    ma_engine* pEngine;        /* back-reference for creating instances       */
} SgeSound;

/* ─── SgeSoundInstance: independent playback with its own audio buffer ──── */

typedef struct {
    ma_audio_buffer* pBuffer;  /* per-instance buffer (owns cursor)           */
    ma_sound         sound;    /* sound node attached to engine endpoint      */
} SgeSoundInstance;

/* ─── SgeMusic: streaming sound from file ──────────────────────────────── */

typedef struct {
    ma_sound  sound;
    ma_engine* pEngine;
    float     volume; /* tracked separately for pan computation */
} SgeMusic;

/* ─── SgePcmDevice: raw PCM output device ──────────────────────────────── */

typedef struct {
    ma_device device;
    /* Ring buffer for write_device → callback transfer */
    ma_pcm_rb ringBuffer;
    int       channels;
    float     volume;
} SgePcmDevice;

/* ─── PCM device callback ──────────────────────────────────────────────── */

static void pcm_device_callback(ma_device* pDevice, void* pOutput, const void* pInput, ma_uint32 frameCount) {
    SgePcmDevice* dev = (SgePcmDevice*)pDevice->pUserData;
    if (dev == NULL) return;

    ma_uint32 framesRead = 0;
    void* pReadBuf;
    ma_result result = ma_pcm_rb_acquire_read(&dev->ringBuffer, &frameCount, &pReadBuf);
    if (result == MA_SUCCESS && frameCount > 0) {
        /* Apply volume scaling */
        ma_uint32 sampleCount = frameCount * (ma_uint32)dev->channels;
        const int16_t* src = (const int16_t*)pReadBuf;
        int16_t* dst = (int16_t*)pOutput;
        for (ma_uint32 i = 0; i < sampleCount; i++) {
            dst[i] = (int16_t)(src[i] * dev->volume);
        }
        framesRead = frameCount;
        ma_pcm_rb_commit_read(&dev->ringBuffer, framesRead);
    }
    /* Fill remainder with silence */
    ma_uint32 totalSamples = frameCount * (ma_uint32)dev->channels;
    ma_uint32 readSamples = framesRead * (ma_uint32)dev->channels;
    if (readSamples < totalSamples) {
        memset((int16_t*)pOutput + readSamples, 0, (totalSamples - readSamples) * sizeof(int16_t));
    }

    (void)pInput;
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  Engine lifecycle
 * ═══════════════════════════════════════════════════════════════════════════ */

int64_t sge_audio_init_engine(int simultaneous_sources, int buffer_size, int buffer_count) {
    ma_engine* pEngine = (ma_engine*)malloc(sizeof(ma_engine));
    if (pEngine == NULL) return 0;

    ma_engine_config config = ma_engine_config_init();
    /* miniaudio manages its own threading; these hints are noted but
       the engine's internal mixer handles source limits automatically. */
    (void)simultaneous_sources;
    (void)buffer_size;
    (void)buffer_count;

    ma_result result = ma_engine_init(&config, pEngine);
    if (result != MA_SUCCESS) {
        free(pEngine);
        return 0;
    }
    return (int64_t)(uintptr_t)pEngine;
}

void sge_audio_shutdown_engine(int64_t engine_handle) {
    ma_engine* pEngine = (ma_engine*)(uintptr_t)engine_handle;
    if (pEngine == NULL) return;
    ma_engine_uninit(pEngine);
    free(pEngine);
}

void sge_audio_update_engine(int64_t engine_handle) {
    /* miniaudio runs its own audio thread — no manual update needed. */
    (void)engine_handle;
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  Sound (PCM data in memory)
 * ═══════════════════════════════════════════════════════════════════════════ */

int64_t sge_audio_create_sound(
    int64_t engine_handle,
    const uint8_t* pcm_data, int data_len,
    int channels, int bit_depth, int sample_rate
) {
    ma_engine* pEngine = (ma_engine*)(uintptr_t)engine_handle;
    if (pEngine == NULL || pcm_data == NULL || data_len <= 0) return 0;

    SgeSound* sge = (SgeSound*)malloc(sizeof(SgeSound));
    if (sge == NULL) return 0;
    memset(sge, 0, sizeof(SgeSound));
    sge->pEngine = pEngine;

    /* Try to decode as an encoded audio file (WAV, MP3, OGG, FLAC).
     * If decoding fails, fall back to treating the data as raw PCM
     * using the caller-provided format parameters. */
    ma_decoder decoder;
    ma_decoder_config decoderConfig = ma_decoder_config_init(ma_format_s16, 0, 0);
    ma_result result = ma_decoder_init_memory(pcm_data, (size_t)data_len, &decoderConfig, &decoder);

    ma_format format;
    ma_uint32 decodedChannels;
    ma_uint32 decodedSampleRate;
    void* pDecodedPCM = NULL;
    ma_uint64 frameCount;

    if (result == MA_SUCCESS) {
        /* Successfully opened as encoded file — read all decoded frames */
        format = decoder.outputFormat;
        decodedChannels = decoder.outputChannels;
        decodedSampleRate = decoder.outputSampleRate;

        /* Get total frame count for allocation */
        ma_uint64 totalFrames = 0;
        ma_decoder_get_length_in_pcm_frames(&decoder, &totalFrames);
        if (totalFrames == 0) totalFrames = (ma_uint64)data_len; /* estimate fallback */

        size_t bytesPerFrame = ma_get_bytes_per_sample(format) * decodedChannels;
        pDecodedPCM = malloc((size_t)totalFrames * bytesPerFrame);
        if (pDecodedPCM == NULL) {
            ma_decoder_uninit(&decoder);
            free(sge);
            return 0;
        }

        ma_uint64 framesRead = 0;
        ma_decoder_read_pcm_frames(&decoder, pDecodedPCM, totalFrames, &framesRead);
        ma_decoder_uninit(&decoder);
        frameCount = framesRead;

        /* Store decoded PCM data in SgeSound for per-instance buffer creation */
        sge->pPCMData    = pDecodedPCM; /* ownership transferred to SgeSound */
        sge->frameCount  = frameCount;
        sge->format      = format;
        sge->channels    = decodedChannels;
        sge->sampleRate  = decodedSampleRate;
    } else {
        /* Decoding failed — treat as raw PCM with caller-provided format */
        format = bit_depth_to_format(bit_depth);
        int bytesPerSample = ma_get_bytes_per_sample(format);
        int bytesPerFrameRaw = bytesPerSample * channels;
        frameCount = (ma_uint64)data_len / (ma_uint64)bytesPerFrameRaw;

        /* Copy raw PCM data so it outlives the caller's buffer */
        pDecodedPCM = malloc((size_t)data_len);
        if (pDecodedPCM == NULL) {
            free(sge);
            return 0;
        }
        memcpy(pDecodedPCM, pcm_data, (size_t)data_len);

        sge->pPCMData    = pDecodedPCM;
        sge->frameCount  = frameCount;
        sge->format      = format;
        sge->channels    = (ma_uint32)channels;
        sge->sampleRate  = (ma_uint32)sample_rate;
    }

    return (int64_t)(uintptr_t)sge;
}

void sge_audio_dispose_sound(int64_t sound_handle) {
    SgeSound* sge = (SgeSound*)(uintptr_t)sound_handle;
    if (sge == NULL) return;
    free(sge->pPCMData);
    free(sge);
}

int64_t sge_audio_play_sound(
    int64_t sound_handle,
    float volume, float pitch, float pan, int loop
) {
    SgeSound* sge = (SgeSound*)(uintptr_t)sound_handle;
    if (sge == NULL) return 0;

    /* Each instance gets its own audio buffer + sound node so playback
       cursors are independent and multiple instances can play concurrently. */
    SgeSoundInstance* inst = (SgeSoundInstance*)malloc(sizeof(SgeSoundInstance));
    if (inst == NULL) return 0;

    ma_audio_buffer_config bufConfig = ma_audio_buffer_config_init(
        sge->format, sge->channels, sge->frameCount, sge->pPCMData, NULL
    );
    bufConfig.sampleRate = sge->sampleRate;

    ma_result result = ma_audio_buffer_alloc_and_init(&bufConfig, &inst->pBuffer);
    if (result != MA_SUCCESS) {
        free(inst);
        return 0;
    }

    result = ma_sound_init_from_data_source(
        sge->pEngine, inst->pBuffer, 0, NULL, &inst->sound
    );
    if (result != MA_SUCCESS) {
        ma_audio_buffer_uninit_and_free(inst->pBuffer);
        free(inst);
        return 0;
    }

    ma_sound_set_volume(&inst->sound, volume);
    ma_sound_set_pitch(&inst->sound, pitch);
    ma_sound_set_pan(&inst->sound, pan);
    ma_sound_set_looping(&inst->sound, loop != 0 ? MA_TRUE : MA_FALSE);
    ma_sound_start(&inst->sound);

    return (int64_t)(uintptr_t)inst;
}

void sge_audio_stop_sound(int64_t instance_id) {
    SgeSoundInstance* inst = (SgeSoundInstance*)(uintptr_t)instance_id;
    if (inst == NULL) return;
    ma_sound_stop(&inst->sound);
    ma_sound_uninit(&inst->sound);
    ma_audio_buffer_uninit_and_free(inst->pBuffer);
    free(inst);
}

void sge_audio_pause_sound(int64_t instance_id) {
    SgeSoundInstance* inst = (SgeSoundInstance*)(uintptr_t)instance_id;
    if (inst == NULL) return;
    ma_sound_stop(&inst->sound);
}

void sge_audio_resume_sound(int64_t instance_id) {
    SgeSoundInstance* inst = (SgeSoundInstance*)(uintptr_t)instance_id;
    if (inst == NULL) return;
    ma_sound_start(&inst->sound);
}

void sge_audio_stop_all_instances(int64_t sound_handle) {
    /* Individual instance tracking would be needed for full support.
       For now, this is a no-op — callers should track and stop instances. */
    (void)sound_handle;
}

void sge_audio_pause_all_instances(int64_t sound_handle) {
    (void)sound_handle;
}

void sge_audio_resume_all_instances(int64_t sound_handle) {
    (void)sound_handle;
}

void sge_audio_set_sound_volume(int64_t instance_id, float volume) {
    SgeSoundInstance* inst = (SgeSoundInstance*)(uintptr_t)instance_id;
    if (inst == NULL) return;
    ma_sound_set_volume(&inst->sound, volume);
}

void sge_audio_set_sound_pitch(int64_t instance_id, float pitch) {
    SgeSoundInstance* inst = (SgeSoundInstance*)(uintptr_t)instance_id;
    if (inst == NULL) return;
    ma_sound_set_pitch(&inst->sound, pitch);
}

void sge_audio_set_sound_pan(int64_t instance_id, float pan, float volume) {
    SgeSoundInstance* inst = (SgeSoundInstance*)(uintptr_t)instance_id;
    if (inst == NULL) return;
    ma_sound_set_pan(&inst->sound, pan);
    ma_sound_set_volume(&inst->sound, volume);
}

void sge_audio_set_sound_looping(int64_t instance_id, int looping) {
    SgeSoundInstance* inst = (SgeSoundInstance*)(uintptr_t)instance_id;
    if (inst == NULL) return;
    ma_sound_set_looping(&inst->sound, looping != 0 ? MA_TRUE : MA_FALSE);
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  Music (streaming from file)
 * ═══════════════════════════════════════════════════════════════════════════ */

int64_t sge_audio_create_music(int64_t engine_handle, const char* file_path) {
    ma_engine* pEngine = (ma_engine*)(uintptr_t)engine_handle;
    if (pEngine == NULL || file_path == NULL) return 0;

    SgeMusic* mus = (SgeMusic*)malloc(sizeof(SgeMusic));
    if (mus == NULL) return 0;
    memset(mus, 0, sizeof(SgeMusic));
    mus->pEngine = pEngine;
    mus->volume = 1.0f;

    ma_result result = ma_sound_init_from_file(
        pEngine, file_path,
        MA_SOUND_FLAG_STREAM | MA_SOUND_FLAG_NO_SPATIALIZATION,
        NULL, NULL, &mus->sound
    );
    if (result != MA_SUCCESS) {
        free(mus);
        return 0;
    }

    return (int64_t)(uintptr_t)mus;
}

void sge_audio_dispose_music(int64_t music_handle) {
    SgeMusic* mus = (SgeMusic*)(uintptr_t)music_handle;
    if (mus == NULL) return;
    ma_sound_uninit(&mus->sound);
    free(mus);
}

void sge_audio_play_music(int64_t music_handle) {
    SgeMusic* mus = (SgeMusic*)(uintptr_t)music_handle;
    if (mus == NULL) return;
    ma_sound_seek_to_pcm_frame(&mus->sound, 0);
    ma_sound_start(&mus->sound);
}

void sge_audio_pause_music(int64_t music_handle) {
    SgeMusic* mus = (SgeMusic*)(uintptr_t)music_handle;
    if (mus == NULL) return;
    ma_sound_stop(&mus->sound);
}

void sge_audio_stop_music(int64_t music_handle) {
    SgeMusic* mus = (SgeMusic*)(uintptr_t)music_handle;
    if (mus == NULL) return;
    ma_sound_stop(&mus->sound);
    ma_sound_seek_to_pcm_frame(&mus->sound, 0);
}

int sge_audio_is_music_playing(int64_t music_handle) {
    SgeMusic* mus = (SgeMusic*)(uintptr_t)music_handle;
    if (mus == NULL) return 0;
    return ma_sound_is_playing(&mus->sound) ? 1 : 0;
}

float sge_audio_get_music_volume(int64_t music_handle) {
    SgeMusic* mus = (SgeMusic*)(uintptr_t)music_handle;
    if (mus == NULL) return 0.0f;
    return mus->volume;
}

void sge_audio_set_music_volume(int64_t music_handle, float volume) {
    SgeMusic* mus = (SgeMusic*)(uintptr_t)music_handle;
    if (mus == NULL) return;
    mus->volume = volume;
    ma_sound_set_volume(&mus->sound, volume);
}

void sge_audio_set_music_pitch(int64_t music_handle, float pitch) {
    SgeMusic* mus = (SgeMusic*)(uintptr_t)music_handle;
    if (mus == NULL) return;
    ma_sound_set_pitch(&mus->sound, pitch);
}

void sge_audio_set_music_pan(int64_t music_handle, float pan, float volume) {
    SgeMusic* mus = (SgeMusic*)(uintptr_t)music_handle;
    if (mus == NULL) return;
    ma_sound_set_pan(&mus->sound, pan);
    mus->volume = volume;
    ma_sound_set_volume(&mus->sound, volume);
}

int sge_audio_is_music_looping(int64_t music_handle) {
    SgeMusic* mus = (SgeMusic*)(uintptr_t)music_handle;
    if (mus == NULL) return 0;
    return ma_sound_is_looping(&mus->sound) ? 1 : 0;
}

void sge_audio_set_music_looping(int64_t music_handle, int looping) {
    SgeMusic* mus = (SgeMusic*)(uintptr_t)music_handle;
    if (mus == NULL) return;
    ma_sound_set_looping(&mus->sound, looping != 0 ? MA_TRUE : MA_FALSE);
}

void sge_audio_set_music_position(int64_t music_handle, float position) {
    SgeMusic* mus = (SgeMusic*)(uintptr_t)music_handle;
    if (mus == NULL) return;
    /* Convert seconds to PCM frames using the sound's data source sample rate */
    ma_engine* pEngine = mus->pEngine;
    ma_uint32 sampleRate = ma_engine_get_sample_rate(pEngine);
    ma_uint64 frame = (ma_uint64)(position * (float)sampleRate);
    ma_sound_seek_to_pcm_frame(&mus->sound, frame);
}

float sge_audio_get_music_position(int64_t music_handle) {
    SgeMusic* mus = (SgeMusic*)(uintptr_t)music_handle;
    if (mus == NULL) return 0.0f;
    float cursor = 0.0f;
    ma_sound_get_cursor_in_seconds(&mus->sound, &cursor);
    return cursor;
}

float sge_audio_get_music_duration(int64_t music_handle) {
    SgeMusic* mus = (SgeMusic*)(uintptr_t)music_handle;
    if (mus == NULL) return 0.0f;
    float length = 0.0f;
    ma_sound_get_length_in_seconds(&mus->sound, &length);
    return length;
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  AudioDevice (raw PCM output)
 * ═══════════════════════════════════════════════════════════════════════════ */

int64_t sge_audio_create_device(int64_t engine_handle, int sample_rate, int is_mono) {
    (void)engine_handle; /* device is independent of engine */

    SgePcmDevice* dev = (SgePcmDevice*)malloc(sizeof(SgePcmDevice));
    if (dev == NULL) return 0;
    memset(dev, 0, sizeof(SgePcmDevice));
    dev->channels = is_mono ? 1 : 2;
    dev->volume = 1.0f;

    /* Initialize ring buffer (1 second of audio) */
    ma_uint32 bufferFrames = (ma_uint32)sample_rate;
    ma_result result = ma_pcm_rb_init(
        ma_format_s16, (ma_uint32)dev->channels, bufferFrames, NULL, NULL, &dev->ringBuffer
    );
    if (result != MA_SUCCESS) {
        free(dev);
        return 0;
    }

    ma_device_config config = ma_device_config_init(ma_device_type_playback);
    config.playback.format = ma_format_s16;
    config.playback.channels = (ma_uint32)dev->channels;
    config.sampleRate = (ma_uint32)sample_rate;
    config.dataCallback = pcm_device_callback;
    config.pUserData = dev;

    result = ma_device_init(NULL, &config, &dev->device);
    if (result != MA_SUCCESS) {
        ma_pcm_rb_uninit(&dev->ringBuffer);
        free(dev);
        return 0;
    }

    result = ma_device_start(&dev->device);
    if (result != MA_SUCCESS) {
        ma_device_uninit(&dev->device);
        ma_pcm_rb_uninit(&dev->ringBuffer);
        free(dev);
        return 0;
    }

    return (int64_t)(uintptr_t)dev;
}

void sge_audio_dispose_device(int64_t device_handle) {
    SgePcmDevice* dev = (SgePcmDevice*)(uintptr_t)device_handle;
    if (dev == NULL) return;
    ma_device_uninit(&dev->device);
    ma_pcm_rb_uninit(&dev->ringBuffer);
    free(dev);
}

void sge_audio_write_device(int64_t device_handle, const uint8_t* data, int offset, int length) {
    SgePcmDevice* dev = (SgePcmDevice*)(uintptr_t)device_handle;
    if (dev == NULL || data == NULL || length <= 0) return;

    int bytesPerFrame = 2 * dev->channels; /* s16 format */
    ma_uint32 frameCount = (ma_uint32)(length / bytesPerFrame);
    const uint8_t* src = data + offset;

    void* pWriteBuf;
    ma_result result = ma_pcm_rb_acquire_write(&dev->ringBuffer, &frameCount, &pWriteBuf);
    if (result == MA_SUCCESS && frameCount > 0) {
        memcpy(pWriteBuf, src, frameCount * (ma_uint32)bytesPerFrame);
        ma_pcm_rb_commit_write(&dev->ringBuffer, frameCount);
    }
}

void sge_audio_set_device_volume(int64_t device_handle, float volume) {
    SgePcmDevice* dev = (SgePcmDevice*)(uintptr_t)device_handle;
    if (dev == NULL) return;
    dev->volume = volume;
}

void sge_audio_pause_device(int64_t device_handle) {
    SgePcmDevice* dev = (SgePcmDevice*)(uintptr_t)device_handle;
    if (dev == NULL) return;
    ma_device_stop(&dev->device);
}

void sge_audio_resume_device(int64_t device_handle) {
    SgePcmDevice* dev = (SgePcmDevice*)(uintptr_t)device_handle;
    if (dev == NULL) return;
    ma_device_start(&dev->device);
}

int sge_audio_get_device_latency(int64_t device_handle) {
    SgePcmDevice* dev = (SgePcmDevice*)(uintptr_t)device_handle;
    if (dev == NULL) return 0;
    /* Return available frames in the ring buffer as a latency estimate */
    return (int)ma_pcm_rb_available_read(&dev->ringBuffer);
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  Output device enumeration
 * ═══════════════════════════════════════════════════════════════════════════ */

char** sge_audio_get_output_devices(int64_t engine_handle, int* count_out) {
    if (count_out != NULL) *count_out = 0;

    ma_engine* pEngine = (ma_engine*)(uintptr_t)engine_handle;
    if (pEngine == NULL) return NULL;

    ma_device* pDevice = ma_engine_get_device(pEngine);
    if (pDevice == NULL) return NULL;

    ma_context* pContext = ma_device_get_context(pDevice);
    if (pContext == NULL) return NULL;

    ma_device_info* pPlaybackInfos = NULL;
    ma_uint32 playbackCount = 0;
    ma_result result = ma_context_get_devices(
        pContext, &pPlaybackInfos, &playbackCount, NULL, NULL
    );
    if (result != MA_SUCCESS || playbackCount == 0) return NULL;

    char** names = (char**)malloc(sizeof(char*) * playbackCount);
    if (names == NULL) return NULL;

    for (ma_uint32 i = 0; i < playbackCount; i++) {
        size_t len = strlen(pPlaybackInfos[i].name);
        names[i] = (char*)malloc(len + 1);
        if (names[i] != NULL) {
            memcpy(names[i], pPlaybackInfos[i].name, len + 1);
        }
    }

    if (count_out != NULL) *count_out = (int)playbackCount;
    return names;
}

int sge_audio_switch_output_device(int64_t engine_handle, const char* device_name) {
    /* miniaudio doesn't support hot-switching output devices on the engine.
       A full implementation would require uninit + reinit with a device ID.
       For now, return 0 (failure). */
    (void)engine_handle;
    (void)device_name;
    return 0;
}

void sge_audio_free_output_devices(char** devices, int count) {
    if (devices == NULL) return;
    for (int i = 0; i < count; i++) {
        free(devices[i]);
    }
    free(devices);
}
