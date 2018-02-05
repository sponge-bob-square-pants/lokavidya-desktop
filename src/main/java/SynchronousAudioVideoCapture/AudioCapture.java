package SynchronousAudioVideoCapture;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.math.RandomUtils;

import com.iitb.lokavidya.core.utils.FFMPEGWrapper;
import com.iitb.lokavidya.core.utils.GeneralUtils;

import gui.Call;

public class AudioCapture implements Runnable {

	private final int mBufferSize = 16384;
	private String mAudioFilePath;

	/**
	 * An audio input stream is an input stream with a specified audio
	 * format and length. The length is expressed in sample frames, not
	 * bytes. Several methods are provided for reading a certain number of
	 * bytes from the stream, or an unspecified number of bytes. The audio
	 * input stream keeps track of the last byte that was read. You can skip
	 * over an arbitrary number of bytes to get to a later position for
	 * reading. An audio input stream may support marks. When you set a
	 * mark, the current position is remembered so that you can return to it
	 * later.
	 */
	private AudioInputStream mAudioInputStream;

	/*
	 * A target data line is a type of DataLine from which audio data
	 * can be read. The most common example is a data line that gets its
	 * data from an audio capture device. (The device is implemented as
	 * a mixer that writes to the target data line.)
	 */
	private TargetDataLine mLine;
	
	// line configuration
	private final AudioFormat.Encoding mEncoding = AudioFormat.Encoding.PCM_SIGNED;
	private final float mRate = 44100.0f;
	private final int mChannels = 2;
	private final int mFrameSize = 4;
	private final int mSampleSize = 16;
	private final boolean mIsBigEndian = true;
	private AudioFormat mFormat;
	private DataLine.Info mInfo;
	
	// TODO shift up
	private ByteArrayOutputStream mOut;
	private ByteArrayOutputStream mOut1;
	private int mFrameSizeInBytes;
	private int mBufferLengthInFrames;
	private int mBufferLengthInBytes;
	private byte[] mData;
	private int mNumBytesRead;
	private SynchronousAudioVideoCapture mCapture;
	private int mFrameRate;
	
	public int getFrameRate() {
		return mFrameRate;
	}
	
	public void setFrameRate(int frameRate) {
		mFrameRate = frameRate;
	}
	
	public AudioCapture(String audioFilePath, int frameRate, SynchronousAudioVideoCapture capture) throws UserDefinedExceptions.CouldNotStartRecordingException, LineUnavailableException {
		mAudioFilePath = audioFilePath;
		mCapture = capture;
		mFrameRate = frameRate;
		
		// define the required attributes for our line
		mFormat = new AudioFormat(mEncoding, mRate, mSampleSize, mChannels, (mSampleSize / 8) * mChannels, mRate, mIsBigEndian);
		mInfo = new DataLine.Info(TargetDataLine.class, mFormat);

		// make sure that this line is supported
		if (!AudioSystem.isLineSupported(mInfo)) {
			throw new UserDefinedExceptions.CouldNotStartRecordingException("Line matching " + mInfo + " not supported.");
		}
		
		mAudioInputStream = null;
		
		// get and open the target data line for capture.
		mLine = (TargetDataLine) AudioSystem.getLine(mInfo);
		mLine.open(mFormat, mLine.getBufferSize());

		// play back the captured audio data
		mOut = new ByteArrayOutputStream();
		mOut1 = new ByteArrayOutputStream();
		mFrameSizeInBytes = mFormat.getFrameSize();
		mBufferLengthInFrames = mLine.getBufferSize() / 8;
		mBufferLengthInBytes = mBufferLengthInFrames * mFrameSizeInBytes;
		mData = new byte[mBufferLengthInBytes];
		mLine.start();
	}

	public void run() {
		
		System.out.println("audio capture run called");
		
		if ((mNumBytesRead = mLine.read(mData, 0, mBufferLengthInBytes)) == -1) {
			System.out.println("Breaking from while ntz");
			return;
		}
	
		mOut.write(mData, 0, mNumBytesRead);
		mOut1.write(mData, 0, mNumBytesRead);
		
		if(mCapture.getState() == SynchronousAudioVideoCapture.States.PAUSED) {
			System.out.println("audio capture pausing");
			mCapture.getAudioCaptureScheduler().cancel(true);
		}
	}
	
	public void stop() throws IOException {
		// we reached the end of the stream.
		// stop and close the line.
		mLine.stop();
		mLine.close();
		mLine = null;
		
		// stop and close the output stream
		mOut.flush();
		mOut.close();
		mOut1.flush();
		mOut1.close();

		// load bytes into the audio input stream for playback

		byte audioBytes[] = mOut.toByteArray();
		ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
		mAudioInputStream = new AudioInputStream(bais, mFormat, audioBytes.length / mFrameSizeInBytes);

		AudioSystem.write(mAudioInputStream, AudioFileFormat.Type.WAVE, new File(mAudioFilePath));
		mAudioInputStream.reset();

		// convert the .wav file to .mp3 file (which is much much smaller in size than the .wav file)
		
		FFMPEGWrapper wrapper = new FFMPEGWrapper();
		// generate random string of 20 numbers
		String randomString = "";
		for(int i=0; i<20; i++) {
			randomString += RandomUtils.nextInt();
		}
		String newAudioFilePath = FilenameUtils.removeExtension(mAudioFilePath) + RandomUtils.nextInt() + randomString + ".mp3";
		mAudioFilePath = new File(mAudioFilePath).getAbsolutePath();
		newAudioFilePath = new File(newAudioFilePath).getAbsolutePath();
		String[] command = new String[] {
				wrapper.pathExecutable,
				"-i",
				mAudioFilePath,
				"-codec:a",
				"libmp3lame",
				"-qscale:a",
				"2",
				newAudioFilePath
		};
		
		GeneralUtils.runProcess(command);
		
		new File(mAudioFilePath).delete();
		new File(newAudioFilePath).renameTo(new File(FilenameUtils.removeExtension(mAudioFilePath)+".mp3"));
	}
	
}