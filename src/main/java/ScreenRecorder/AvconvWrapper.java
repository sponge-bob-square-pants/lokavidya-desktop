package ScreenRecorder;

import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.Insets;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import javax.swing.JFrame;

import com.iitb.lokavidya.core.utils.GeneralUtils;

import gui.Call;

public class AvconvWrapper {
	
	private String mPathExecutable;
	private int mWidth;
	private int mHeight;
	private String mScreenSize;
	private String mVideoURL;
	private String[] mCommand;
	private InterruptableProcess runProcess;
	private boolean mReadyForRecording;
	private boolean mIsRecording;
	
	public AvconvWrapper(String videoPath, String videoName, int startingX, int startingY, int width, int height){
		mPathExecutable = GeneralUtils.findAvconvPath();
		mVideoURL = new File(videoPath, videoName + ".mp4").getAbsolutePath();
		System.out.println("video url : " + mVideoURL);
		
		mWidth = width;
		mHeight = height;
		mScreenSize = ((int) mWidth) + "x" + ((int) mHeight);
		
		// delete a previous copy of the video, if it exists
		if (new File(mVideoURL).exists()){
			new File(mVideoURL).delete();
		}
		
		if (System.getProperty("os.name").contains("Linux")) {
			// Linux
			// avconv -y -f alsa -i pulse -f x11grab -r 3 -s 1280x720 -i :0.0+0,0 -acodec aac -vcodec libx264 -threads 0 Desktop/video.mkv
			mCommand = new String[] {
					mPathExecutable, 
					"-y", 
					"-f", 
					"alsa", 
					"-f",
					"x11grab", 
					"-r", 
					"3", 
					"-s", 
					mScreenSize, 
					"-i", 
					":0.0+0,0", 
					"-acodec", 
					"aac", 
					"-vcodec",
					"libx264", 
					"-threads", 
					"0", 
					mVideoURL
			};
			mReadyForRecording = true;
		} else if (System.getProperty("os.name").contains("Windows")) {
			// Windows
			mReadyForRecording = false;
		} else if (System.getProperty("os.name").startsWith("Mac")) {
			// MacOS
			mReadyForRecording = false;
		}
	}
	
	public AvconvWrapper(String videoPath, String videoName) {
		this(videoPath, videoName, 0, 0, getScreenWidth(), getScreenHeight());
	}
	
	public static int getScreenWidth() {
		Dimension screenDimensions = Toolkit.getDefaultToolkit().getScreenSize();
		return (int) screenDimensions.getWidth();
	}
	
	public static int getScreenHeight() {
		Dimension screenDimensions = Toolkit.getDefaultToolkit().getScreenSize();
		return (int) screenDimensions.getHeight();
	}
	
	
	public boolean startRecordingScreen() {
		if(mReadyForRecording) {
			runProcess = new InterruptableProcess(mCommand);
			mIsRecording = runProcess.start();
		} else {
			mIsRecording = false;
		} return mIsRecording;
	}
	
	// this method can be called from the listener when the user presses the button to stop recording
	public boolean stopRecordingScreen(){
		if(mIsRecording) {
			return runProcess.stop();
		} return true;
		
	}
	
	public static void main(String[] args) {
		AvconvWrapper avconvWrapper = new AvconvWrapper("/home/ironstein/Desktop", "abcdefgh");
		System.out.println("starting recording : " + avconvWrapper.startRecordingScreen());
		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("stop recording : " + avconvWrapper.stopRecordingScreen());
	}
	
}
