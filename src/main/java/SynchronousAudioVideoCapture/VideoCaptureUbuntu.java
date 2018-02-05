package SynchronousAudioVideoCapture;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.iitb.lokavidya.core.utils.FFMPEGWrapper;
import com.iitb.lokavidya.core.utils.RandomStringUtils;

import gui.Call;

public class VideoCaptureUbuntu extends VideoCapture {
	
	private AvconvWrapper mAvconvWrapper;
	private int mFrameRate;
	private ArrayList<String> mTempFiles;
	private String mCurrentTempFile;
	private String mVideoPath;
	private SynchronousAudioVideoCapture mCapture;
	private boolean mIsBusy;
	private FFMPEGWrapper mFfmpegWrapper;
	
	public VideoCaptureUbuntu(String videoPath, int videoFrameRate, SynchronousAudioVideoCapture capture) {
		System.out.println("videoCaptureUbuntu : called");
		mVideoPath = videoPath;
		mFrameRate = videoFrameRate;
		mCapture = capture;
		mTempFiles = new ArrayList<String>();
		mCurrentTempFile = getNewTempFile("f4v");
		mIsBusy = false;
		mFfmpegWrapper = new FFMPEGWrapper();
		mAvconvWrapper = new AvconvWrapper(videoPath);
	}
	
	public String getNewTempFile(String extension) {
		String f = (new File(System.getProperty("java.io.tmpdir"), RandomStringUtils.getSystemTimeString() + "." + extension)).getAbsolutePath();
		System.out.println("getNewTempFile : " + f);
		return f;
	}

	@Override
	public int getFrameRate() {
		return mFrameRate;
	}

	@Override
	public void setFrameRate(int frameRate) {
		mFrameRate = frameRate;
	}

	@Override
	public void stop() {
		mIsBusy = true;
		pause();
		System.out.println("Avconv recording stopped");
		mFfmpegWrapper.stitchVideo(mTempFiles, getNewTempFile("txt"), mVideoPath);
		mIsBusy = false;
	}
	
	public void pause(){
		//PAUSED
		System.out.println("videoCapture pausing");
		// cancel schedule for next call
		mCapture.getVideoCaptureScheduler().cancel(true);
		// stop screen recording
		mAvconvWrapper.stopRecordingScreen();
		// add the current temp file name to array
		mTempFiles.add(mCurrentTempFile);
		// create a new temp file
		mCurrentTempFile = getNewTempFile("f4v");
	}

	@Override
	public void run() {
		System.out.println("VideoCaptureUbuntu : run : called");
		
		if(mCapture.getState() == SynchronousAudioVideoCapture.States.PAUSED) {
			pause();
			System.out.println("new temp file : " + mCurrentTempFile);
		} else {
			System.out.println("videoCapture not pausing");
			System.out.println("-------------------------------------------");
			System.out.println("-------------------------------------------");
			System.out.println("is recording already : " + mAvconvWrapper.isRecording());
			System.out.println("-------------------------------------------");
			if(!mAvconvWrapper.isRecording()) {
				// not recording currently, so start recording
				System.out.println("videoCapture starting");
				mAvconvWrapper = new AvconvWrapper(new File(mCurrentTempFile).getPath(), Call.workspace.framerate, Call.workspace.x, Call.workspace.y, Call.workspace.recordingWidth, Call.workspace.recordingHeight);
				System.out.println(mAvconvWrapper.startRecordingScreen());
			} else {
				System.out.println("videoCapture not starting");
			}
		}
	}

	@Override
	public boolean isBusy() {
		return mIsBusy;
	}

}
