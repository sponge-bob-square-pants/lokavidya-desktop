package com.iitb.lokavidya.core.operations;

import gui.Call;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.RandomStringUtils;

import Xuggler.DecodeAndSaveAudioVideo;

import com.iitb.lokavidya.core.data.Audio;
import com.iitb.lokavidya.core.data.Project;
import com.iitb.lokavidya.core.data.Segment;
import com.iitb.lokavidya.core.data.Video;
import com.iitb.lokavidya.core.utils.FFMPEGWrapper;
import com.iitb.lokavidya.core.utils.GeneralUtils;
import com.iitb.lokavidya.core.utils.SoundCapture;
import com.sun.star.setup.CopyFileAction;

import SynchronousAudioVideoCapture.AudioCapture;
import SynchronousAudioVideoCapture.SynchronousAudioVideoCapture;
import SynchronousAudioVideoCapture.VideoCapture;

public class ProjectOperations {
	static SoundCapture currentSound = null;
	static SynchronousAudioVideoCapture currentCapture = null;
	static String tempAudioURL;
	static String tempVideoURL;

	public static void stitch(Project project) {
		FFMPEGWrapper ffmpegWrapper = new FFMPEGWrapper();
		ArrayList<String> videoPaths = new ArrayList<String>();
		Iterator<Segment> iterator = project.getOrderedSegmentList().iterator();
		String tmpPath = System.getProperty("java.io.tmpdir");
		double divider = (double)60/(double)project.getOrderedSegmentList().size();
		int value =10;
		int i=0;
		while (iterator.hasNext()) {
			Segment segment = iterator.next();
			// TODO Check audio format before attempting convert
			if (segment.getSlide() != null){
			if (segment.getSlide().getAudio() != null) {
				try {
					String audioURL = segment.getSlide().getAudio()
							.getAudioURL();
					System.out.println("Audio URL: " + audioURL);
					String imageURL = segment.getSlide().getImageURL();
					System.out.println("Image URL: " + imageURL);
					/*String audioName = RandomStringUtils.randomAlphanumeric(10)
							.toLowerCase();
					String mp3Path = project.getProjectURL() + File.separator
							+ audioName + ".mp3";
					ffmpegWrapper.convertWavToMp3(audioURL, mp3Path);
					// Update the audio properties
					System.out.println("outside wrapper..");
					Audio tempAudio = new Audio(mp3Path,
							project.getProjectURL());
					segment.getSlide().setAudio(tempAudio);
					segment.getSlide().getAudio().setAudioProperties();
					System.out.println("audio conversion complete");
					System.out.println("audio format: "
							+ segment.getSlide().getAudio().getAudioFormat());
					*/
					System.out.println("Video creation... ");
					String tempPath = new File(tmpPath, "temp.mp4")
							.getAbsolutePath();
					System.out.println("Temp path: " + tempPath);
					Video video = new Video(project.getProjectURL());
					System.out.println("VIDEO URL: " + video.getVideoURL());
					ffmpegWrapper.stitchImageAndAudio(imageURL, segment
							.getSlide().getAudio().getAudioURL(),
							video.getVideoURL());
					video.setVideoProperties();
					segment.setVideo(video);
					
					System.out.println(video.getVideoFormat());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			} 
			if(segment.getVideo()!=null)
			{
				segment.getVideo().setVideoProperties();
				videoPaths.add(segment.getVideo().getVideoURL());
			}
			value = (int)(10+(double)(i+1)*(divider));
			Call.workspace.progressBar.setValue(value);
			i++;
		}
		System.out.println("Video list :");
		Iterator<String> iter = videoPaths.iterator();
		while (iter.hasNext()) {
			System.out.println(iter.next());
		}
		
		// Begin concatenation
		String finalPath = new File(project.getProjectURL(), project.getProjectName() + ".mp4").getAbsolutePath();
		ffmpegWrapper.stitchVideo(videoPaths, new File("concat.txt").getAbsolutePath(), finalPath);
		Call.workspace.progressBar.setValue(80);
		for(int j=0; j<videoPaths.size(); j++) {
			if(j == 0) {
				
			}
		}
	}
	
	static Audio globalAudio;
	static Segment segment;
	static Video globalVideo;

	public static void startAudioRecording(Project project, Segment segmentA) {
		Call.workspace.startOperation();
		SegmentService.deleteVideo(project, segmentA);
		SegmentService.deleteAudio(project, segmentA);
		segment=segmentA;
		globalAudio = new Audio(project.getProjectURL());
		currentSound = new SoundCapture(globalAudio.getAudioURL());
		currentSound.startRecording();
	}

	public void startToggleSlideRecording(Project project, Segment segmentA) {
		Call.workspace.startOperation();
		SegmentService.deleteVideo(project, segmentA);
		SegmentService.deleteAudio(project, segmentA);
		segment=segmentA;
		tempAudioURL=new File(project.getProjectURL(),(RandomStringUtils.randomAlphanumeric(10).toLowerCase()+".wav")).getAbsolutePath();
		tempVideoURL=new File((RandomStringUtils.randomAlphanumeric(10).toLowerCase()+".flv")).getAbsolutePath();
		try {
			currentCapture = new SynchronousAudioVideoCapture(tempAudioURL, tempVideoURL);
			currentCapture.startRecording();
		} catch (java.lang.Exception e) {
			// TODO show dialogue (could not start recording)
			e.printStackTrace();
		}
	}

	public static void playSlideRecording() {

	}

	public static void stopPlaySlideRecording() {

	}

	public static void stopAudioRecording(Project project) {
		List<Segment> slist = project.getOrderedSegmentList();
		for(Segment s:slist){
			if(s.getSlide()!=null){
				if(s.getSlide().getAudio()!=null && s.getVideo()==null){
					System.out.println("Checking:"+s.getSlide().getAudio().getAudioURL());
					FFMPEGWrapper ffmpegwrapper = new FFMPEGWrapper();
					long duration = ffmpegwrapper.getDuration(s.getSlide().getAudio().getAudioURL());
					System.out.println("Stopping Duration: "+duration);
					s.setTime(duration);
				}
			}
		}
		Call.workspace.endOperation();
	}
	
	public static void stopAudioToggleRecording(){
		currentSound.stopRecording();
		segment.getSlide().setAudio(globalAudio);
	}

	public static void discardSlideRecording(Project p) {
		currentSound.stopRecording();
		SegmentService.deleteAudio(p, segment);
//		Call.workspace.cancelOperation();
	}

	public void discardToggleSlideRecording(Project project) {
//		currentAudio.stopRecording();
//        currentMuteVideo.stop();
		currentCapture.stopRecording();
		SegmentService.deleteVideo(project,segment );
		SegmentService.deleteAudio(project, segment);
//        Call.workspace.cancelOperation();
	}

	public void stopToggleSlideRecording(Project project) {
		// TODO show some dialog here saying something like "Please Wait"
		// Since the "currentCapture.stopRecording()" method will take a lot of time
		// After the stack thingy is implemented
		currentCapture.stopRecording();
        segment.getSlide().setTempAudioURL(FilenameUtils.removeExtension(tempAudioURL)+".mp3");
        segment.getSlide().setTempMuteVideoURL(tempVideoURL); 
	}

	public static void stopSlideRecording(Project project) {
		
		List<Segment> slist = project.getOrderedSegmentList();
		for(Segment s:slist){
			if(s.getSlide()!=null) {
				// consider only the silde that has been altered (slide recording)
				if(s.getSlide().getTempAudioURL()!=null && s.getSlide().getTempMuteVideoURL()!=null){
					// saving temp video as an flv file in lokavidya folder
					File originalTempVideo = new File(s.getSlide().getTempMuteVideoURL());
					System.out.println("originalTempVideo : Saving at : " + originalTempVideo.getAbsolutePath());
					
					// stitching audio and video files, and generating a flv file as output
					File tempVideo = new File(project.getProjectURL(), RandomStringUtils.randomAlphanumeric(10).toLowerCase()+".flv");
					System.out.println("tempVideo : Saving at : " + tempVideo.getAbsolutePath());
					DecodeAndSaveAudioVideo.stitch(originalTempVideo.getAbsolutePath(),new File(s.getSlide().getTempAudioURL()).getAbsolutePath(),tempVideo.getAbsolutePath());
					
					// converting tempVideo, and saving it to globalVideo.getVideoURL() path
					// ffmpeg -i inputVideo.flv -c:v libxvid -c:a aac -strict experimental outputVideo.mp4
					globalVideo = new Video(project.getProjectURL());
					System.out.println("converting " + tempVideo.getAbsolutePath() + " to " + globalVideo.getVideoURL());
					FFMPEGWrapper wrapper = new FFMPEGWrapper();
					String tmpVideoPath = new File(System.getProperty("java.io.tmpdir"), "tempVideoBeforeSaving.flv").getAbsolutePath();
					
					String[] command;
					
					command = new String[] {
						wrapper.pathExecutable, 
						"-i", 
						tempVideo.getAbsolutePath(),
						"-y",
						"-c:v",
						wrapper.encoding,
						"-c:a",
						"aac",
//						"-crf",
//						"22",
						"-strict",
						"experimental",
						globalVideo.getVideoURL()
					};
					GeneralUtils.runProcess(command);
							
//					if(System.getProperty("os.name").toLowerCase().contains("mac")) {
//						command = new String[] {
//							wrapper.pathExecutable, 
//							"-i", 
//							tempVideo.getAbsolutePath(),
//							"-y",
//							"-c:v",
//							"libx264",
//							"-c:a",
//							"aac",
//							"-strict",
//							"experimental",
//							globalVideo.getVideoURL()
//						};
//					} else {
//						command = new String[] {
//							wrapper.pathExecutable, 
//							"-i", 
//							tempVideo.getAbsolutePath(),
//							"-y",
//							"-c:v",
//							"libxvid",
//							"-c:a",
//							"aac",
//							"-strict",
//							"experimental",
//							globalVideo.getVideoURL()
//						};
//					}
//					GeneralUtils.runProcess(command);
//					
//					// convert globalVideo encoding to libx264
//					if(!System.getProperty("os.name").toLowerCase().contains("mac")) {
//						command = new String[] {
//								wrapper.pathExecutable, 
//								"-i", 
//								tmpVideoPath,
//								"-y",
//								"-c:v",
//								"libx264",
//								"-preset",
//								"slow",
//								"-crf",
//								"22",
//								"-c:a",
//								"copy",
//								globalVideo.getVideoURL()
//							};
//						GeneralUtils.runProcess(command);
//					}
					
//					new File(tmpVideoPath).delete();
					
					Video screenVideo = new Video(globalVideo.getVideoURL(), project.getProjectURL());

					// get the duration of the output video
					SegmentService.addVideo(project, s ,screenVideo);
					FFMPEGWrapper ffmpegwrapper = new FFMPEGWrapper();
					long duration = ffmpegwrapper.getDuration(s.getVideo().getVideoURL());
					s.setTime(duration);
				
					// Call.workspace.deleteList.add(f);
					Call.workspace.deleteList.add(new File(s.getSlide().getTempAudioURL()));
					Call.workspace.deleteList.add(tempVideo);
				
					s.getSlide().setTempAudioToNull();
					s.getSlide().setTempMuteVideoToNull();
					System.out.println("StopSlideRecording : slide");
				}
			}
		}
		System.out.println("StopSlideRecording : final");
		Call.workspace.endOperation();
	}
}
