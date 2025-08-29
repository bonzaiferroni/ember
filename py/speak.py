#!/usr/bin/env python3
# tts_stdout.py — write PCM16 WAV to stdout
import sys, argparse, io, numpy as np
from process import generate_from_text

def write_wav_unseekable_pcm16(fp, data: np.ndarray, rate: int):
    # ensure int16 little-endian
    if data.dtype != np.int16:
        d = np.asarray(data, dtype=np.float32)
        d = np.clip(d, -1.0, 1.0)
        data16 = (d * 32767.0).astype("<i2", copy=False)
    else:
        data16 = data.astype("<i2", copy=False)

    # header
    import struct
    ch = 1 if data16.ndim == 1 else data16.shape[1]
    nframes = data16.shape[0]
    bps = 2
    block_align = ch * bps
    data_bytes = nframes * block_align
    byte_rate = rate * block_align

    fp.write(b"RIFF")
    fp.write(struct.pack("<I", 36 + data_bytes))
    fp.write(b"WAVEfmt ")
    fp.write(struct.pack("<IHHIIHH", 16, 1, ch, rate, byte_rate, block_align, 16))
    fp.write(b"data")
    fp.write(struct.pack("<I", data_bytes))
    fp.write(data16.tobytes(order="C"))

def main(text, voice=None, rate=24000):
    audio = generate_from_text(text, voice)  # float32 [-1,1] typical
    write_wav_unseekable_pcm16(sys.stdout.buffer, audio, rate)

# optional: return bytes for JEP
def tts_bytes(text, voice=None, rate=24000):
    audio = generate_from_text(text, voice)
    buf = io.BytesIO()
    write_wav_unseekable_pcm16(buf, audio, rate)
    return buf.getvalue()

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("text")
    ap.add_argument("voice", nargs="?", help="Voice name (optional)")
    ap.add_argument("-r", "--rate", type=int, default=24000)
    ns = ap.parse_args()
    text, voice, rate = ns.text, ns.voice, ns.rate
    main(text, voice, rate)

