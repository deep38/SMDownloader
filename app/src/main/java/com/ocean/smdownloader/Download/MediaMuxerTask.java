package com.ocean.smdownloader.Download;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.AsyncTask;
import android.util.Log;

import com.ocean.smdownloader.Algorithms;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class MediaMuxerTask extends AsyncTask<String, Integer, Boolean> {
    String tag = "Muxer";

    public static final String[] WEBM_FORMATS = new String[]{MediaFormat.MIMETYPE_VIDEO_VP8, MediaFormat.MIMETYPE_VIDEO_VP9, MediaFormat.MIMETYPE_AUDIO_VORBIS, MediaFormat.MIMETYPE_AUDIO_OPUS};
    public static final String[] MPEG4_FORMATS = new String[]{MediaFormat.MIMETYPE_VIDEO_AVC, MediaFormat.MIMETYPE_VIDEO_HEVC, MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION, MediaFormat.MIMETYPE_VIDEO_MPEG4, MediaFormat.MIMETYPE_AUDIO_AAC};

    private MediaMuxerTaskListener listener;
    private DownloadTaskData taskData;
    private String videoFilePath;
    private String audioFilePath;
    private String outputFilePath;

    private String error;
    private long lastProgressUpdateTime;

    public MediaMuxerTask(MediaMuxerTaskListener listener, DownloadTaskData taskData, String videoFilePath, String audioFilePath, String outputFilePath) {
        this.listener = listener;
        this.taskData = taskData;
        this.videoFilePath = videoFilePath;
        this.audioFilePath = audioFilePath;
        this.outputFilePath = outputFilePath;

        Log.d("MuxerTask", videoFilePath + " \n" +audioFilePath + " \n" +outputFilePath + " \n");
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        listener.onMuxStart(taskData);
    }

    @SuppressLint("WrongConstant")
    @Override
    protected Boolean doInBackground(String... strings) {
        try {

            MediaExtractor videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(videoFilePath);

            MediaExtractor audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(audioFilePath);

            videoExtractor.selectTrack(0);
            MediaFormat videoFormat = videoExtractor.getTrackFormat(0);
            audioExtractor.selectTrack(0);
            MediaFormat audioFormat = audioExtractor.getTrackFormat(0);

            boolean isWebmFormat = isWebmFormat(videoFormat.getString(MediaFormat.KEY_MIME));
            boolean isAudioFormatSame = (isWebmFormat && isWebmFormat(audioFormat.getString(MediaFormat.KEY_MIME)) || (!isWebmFormat && !isWebmFormat(audioFormat.getString(MediaFormat.KEY_MIME))));
            MediaMuxer muxer = new MediaMuxer(outputFilePath, isWebmFormat ? MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM : MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            int videoTrack = muxer.addTrack(videoFormat);
            Log.d(tag, "video added: " + videoTrack + " " + videoFormat.toString());

            int audioTrack;
            MediaCodec decoder = null, encoder = null;
            if (isAudioFormatSame) {
                audioTrack = muxer.addTrack(audioFormat);
            } else {
                decoder = MediaCodec.createDecoderByType(audioFormat.getString(MediaFormat.KEY_MIME));
                decoder.configure(audioFormat, null, null, 0);
                decoder.start();

                MediaFormat neededFormat = new MediaFormat();
                neededFormat.setString(MediaFormat.KEY_MIME, isWebmFormat ? MediaFormat.MIMETYPE_AUDIO_OPUS : MediaFormat.MIMETYPE_AUDIO_AAC);
                neededFormat.setInteger(MediaFormat.KEY_PROFILE, isWebmFormat ? MediaCodecInfo.CodecProfileLevel.VP9Profile3 : MediaCodecInfo.CodecProfileLevel.AACObjectLC);
                neededFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
                neededFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128 * 1024);
                neededFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                neededFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1048576);

                encoder = MediaCodec.createEncoderByType(neededFormat.getString(MediaFormat.KEY_MIME));
                encoder.configure(neededFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                encoder.start();

                audioTrack = muxer.addTrack(neededFormat);
            }
            Log.d(tag, "audio added: " + audioTrack + " " + audioFormat.toString());

            boolean sawEOS = false;
            int frameCount = 0;
            int offset = 100;
            int sampleSize = 256 * 1024;
            ByteBuffer videoBuf = ByteBuffer.allocate(sampleSize);
            ByteBuffer audioBuf = ByteBuffer.allocate(sampleSize);
            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();

            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

            muxer.start();

            long videoSize = 0, audioSize;

            while (!sawEOS) {
                videoBufferInfo.offset = offset;
                videoBufferInfo.size = videoExtractor.readSampleData(videoBuf, offset);

                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
                    Log.d(tag, "Saw input EOS.");
                    sawEOS = true;
                    videoBufferInfo.size = 0;
                } else {
                    videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
                    videoBufferInfo.flags = videoExtractor.getSampleFlags();
                    muxer.writeSampleData(videoTrack, videoBuf, videoBufferInfo);
                    videoExtractor.advance();
                    frameCount++;

                    videoSize += videoBufferInfo.size;
                    publishProgress(Algorithms.getProgressPercentage(videoSize, taskData.getTotalSize()));

                    Log.d(tag, "Frame (" + frameCount + ") Video PresentationTimeUS: " + videoBufferInfo.presentationTimeUs + " Flags:" + videoBufferInfo.flags + " Size(kb) " + videoBufferInfo.size / 1024);
                    Log.d(tag, "Frame (" + frameCount + ") Audio PresentationTimeUS: " + audioBufferInfo.presentationTimeUs + " Flags:" + audioBufferInfo.flags + " Size(kb) " + audioBufferInfo.size / 1024);
                }
            }

            audioSize = videoSize;
            if (isAudioFormatSame) {
                sawEOS = false;
                frameCount = 0;
                while (!sawEOS) {
                    frameCount++;

                    audioBufferInfo.offset = offset;
                    audioBufferInfo.size = audioExtractor.readSampleData(audioBuf, offset);

                    if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
                        Log.d(tag, "EOS");
                        sawEOS = true;
                        audioBufferInfo.size = 0;
                    } else {
                        audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
                        audioBufferInfo.flags = audioExtractor.getSampleFlags();
                        muxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo);
                        audioExtractor.advance();

                        audioSize += audioBufferInfo.size;
                        publishProgress(Algorithms.getProgressPercentage(audioSize, taskData.getTotalSize()));

                        Log.d(tag, "Frame2 (" + frameCount + ") Video PresentationTimeUS: " + videoBufferInfo.presentationTimeUs + " Flags:" + videoBufferInfo.flags + " Size(kb) " + videoBufferInfo.size / 1024.0f);
                        Log.d(tag, "Frame2 (" + frameCount + ") Audio PresentationTimeUS: " + audioBufferInfo.presentationTimeUs + " Flags:" + audioBufferInfo.flags + " Size(kb) " + audioBufferInfo.size / 1024.0f);
                    }
                }
            } else {
                boolean allInputExtracted = false;
                boolean allInputDecoded = false;
                boolean allOutputEncoded = false;

                long timeoutUs = 10000L;
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                long totalSize = 0, totalTime = 0;
                while (!allOutputEncoded) {

                    if (!allInputExtracted) {
                        int inBufferId = decoder.dequeueInputBuffer(timeoutUs);
                        if (inBufferId >= 0) {
                            ByteBuffer buffer = decoder.getInputBuffer(inBufferId);
                            sampleSize = audioExtractor.readSampleData(buffer, 0);
                            totalSize += bufferInfo.size;
                            totalTime += audioExtractor.getSampleTime();
                            Log.i(tag, "Extracting input: " + Algorithms.getSize(totalSize) + " Time: " + totalTime);

                            if (sampleSize >= 0) {
                                decoder.queueInputBuffer(
                                        inBufferId, 0, sampleSize,
                                        audioExtractor.getSampleTime(), audioExtractor.getSampleFlags()
                                );

                                audioExtractor.advance();
                            } else {
                                decoder.queueInputBuffer(
                                        inBufferId, 0, 0,
                                        0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                );
                                allInputExtracted = true;
                            }
                        }
                    }

                    boolean encoderOutputAvailable = true;
                    boolean decoderOutputAvailable = !allInputDecoded;

                    while (encoderOutputAvailable || decoderOutputAvailable) {

                        int outBufferId = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs);
                        if (outBufferId >= 0) {
                            ByteBuffer encodedBuffer = encoder.getOutputBuffer(outBufferId);
                            muxer.writeSampleData(audioTrack, encodedBuffer, bufferInfo);
                            encoder.releaseOutputBuffer(outBufferId, false);

                            Log.i(tag, "Writing in muxer: " + bufferInfo.size);
                            audioSize += bufferInfo.size;
                            publishProgress(Algorithms.getProgressPercentage(audioSize, taskData.getTotalSize()));

                            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                allOutputEncoded = true;
                                break;
                            }
                        } else if (outBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            encoderOutputAvailable = false;
                        } else if (outBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                        }

                        if (outBufferId != MediaCodec.INFO_TRY_AGAIN_LATER)
                            continue;

                        if (!allInputDecoded) {
                            outBufferId = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs);
                            if (outBufferId >= 0) {
                                ByteBuffer outBuffer = decoder.getOutputBuffer(outBufferId);

                                int inBufferId = encoder.dequeueInputBuffer(timeoutUs);
                                ByteBuffer inBuffer = encoder.getInputBuffer(inBufferId);

                                inBuffer.put(outBuffer);

                                encoder.queueInputBuffer(
                                        inBufferId, bufferInfo.offset, bufferInfo.size,
                                        bufferInfo.presentationTimeUs, bufferInfo.flags
                                );

                                decoder.releaseOutputBuffer(outBufferId, false);

                                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                                    allInputDecoded = true;

                            } else if (outBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                decoderOutputAvailable = false;
                            }
                        }
                    }
                }

            }

            audioExtractor.release();

            if (decoder != null) {
                decoder.stop();
                decoder.release();

                encoder.stop();
                encoder.release();
            }

            muxer.stop();
            muxer.release();
            Log.d(tag, "Done: Audio: " + Algorithms.getSize(audioSize) + " Video: " + Algorithms.getSize(videoSize));
            return true;
        } catch (Exception e) {
            Log.e(tag, e.getMessage());
            error = e.getMessage();
            return false;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (System.currentTimeMillis() - lastProgressUpdateTime >= 1000) {
            listener.onMuxProgress(taskData, values[0]);
            lastProgressUpdateTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        Log.d(tag, "PostExecute: " + aBoolean);
        if (aBoolean) {
            listener.onMuxComplete(taskData);
        } else {
            listener.onMuxFailed(taskData, error);
        }
    }

    private boolean isWebmFormat(String requiredMime) {
        for (String mime : WEBM_FORMATS) {
            if (mime.equals(requiredMime))
                return true;
        }
        return false;
    }
}
