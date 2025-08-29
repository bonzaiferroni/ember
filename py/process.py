import torch
import numpy as np
import os
from kokoro import KPipeline
import soundfile as sf
# 🇺🇸 'a' => American English, 🇬🇧 'b' => British English
# 🇯🇵 'j' => Japanese: pip install misaki[ja]
# 🇨🇳 'z' => Mandarin Chinese: pip install misaki[zh]
# 4️⃣ Generate, display, and save audio files in a loop.

def split_text_into_segments(full_text, max_length=500):
    """Split text into segments of max_length, ending at the next period."""
    segments = []
    while len(full_text) > max_length:
        # Find the next period within the max_length
        end_idx = full_text[:max_length].rfind('.')
        if end_idx == -1:  # No period found, split at max_length
            end_idx = max_length
        segments.append(full_text[:end_idx + 1].strip())
        full_text = full_text[end_idx + 1:].strip()
    if full_text:
        segments.append(full_text)  # Add the remaining text
    return segments

def generate_audio_from_text_segments(segments, voice_name, lang_code):
    """Generate audio for each text segment and concatenate into one audio file."""
    pipeline = KPipeline(lang_code=lang_code) # <= make sure lang_code matches voice
    audio_segments = []
    for i, segment in enumerate(segments):
        generator = pipeline(
            segment, voice=voice_name, # <= change voice here
            speed=1, split_pattern=r'\n+'
        )

        for i, (gs, ps, audio) in enumerate(generator):
            print(i)  # i => index
            print(gs) # gs => graphemes/text
            print(ps) # ps => phonemes
            audio_segments.append(audio)

    # Concatenate all audio segments
    return np.concatenate(audio_segments)

def generate_from_text(text: str, voice_arg: str | None = None):
    # Initialize model and voice
    voice_name = voice_arg or VOICES[8]
    print(f'Loaded voice: {voice_name}')
    lang_code = voice_name[0]
    pipeline = KPipeline(lang_code=lang_code) # <= make sure lang_code matches voice
    generator = pipeline(
        text, voice=voice_name, # <= change voice here
        speed=1, split_pattern=r'\n+'
    )

    audio_segments = []
    for i, (gs, ps, audio) in enumerate(generator):
        print(i)  # i => index
        print(gs) # gs => graphemes/text
        # print(ps) # ps => phonemes
        audio_segments.append(audio)

    # Generate audio
    return np.concatenate(audio_segments)

VOICES = [
    'af_alloy',
    'af_aoede',
    'af_bella',
    'af_heart',
    'af_jessica',
    'af_kore',
    'af_nicole',
    'af_nova',
    'af_river',
    'af_sarah',
    'af_sky',
    'am_adam',
    'am_echo',
    'am_eric',
    'am_fenrir',
    'am_liam',
    'am_michael',
    'am_onyx',
    'am_puck',
    'am_santa',
    'bf_alice',
    'bf_emma',
    'bf_isabella',
    'bf_lily',
    'bm_daniel',
    'bm_fable',
    'bm_george',
    'bm_lewis'
]