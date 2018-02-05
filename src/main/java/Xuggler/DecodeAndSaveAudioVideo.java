/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Xuggler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IAudioSamples.Format;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;


public class DecodeAndSaveAudioVideo {

 public static void convertFormat(String fromFileName,String toFileName) {
	 System.out.println("Converting from "+fromFileName+" to "+toFileName);
	 IMediaReader reader = ToolFactory.makeReader(fromFileName);
	 reader.addListener(ToolFactory.makeWriter(toFileName, reader));
	 while (reader.readPacket() == null);
 }

 public static void main(String[] args) {}

 public static void stitch(String filenamevideo,String filenameaudio,final String outputName) {
	 	
	 	IMediaWriter mWriter = ToolFactory.makeWriter(outputName); //output file
	 	
	 	IContainer containerVideo = IContainer.make();
	 	IContainer containerAudio = IContainer.make();
	 	
	 	System.out.println(filenamevideo + "\n" + filenameaudio);
	 	if (containerVideo.open(filenamevideo, IContainer.Type.READ, null) < 0)
	        throw new IllegalArgumentException("Cant find " + filenamevideo);
	 	if (containerAudio.open(filenameaudio, IContainer.Type.READ, null) < 0)
	        throw new IllegalArgumentException("Cant find " + filenameaudio);
	 	
	 	int numStreamVideo = containerVideo.getNumStreams();
	 	int numStreamAudio = containerAudio.getNumStreams();
	 	System.out.println("Number of video streams: "+numStreamVideo + "\n" + "Number of audio streams: "+numStreamAudio );
	 	
	 	int videostreamt = -1; 
	 	int audiostreamt = -1;
	 	
	 	IStreamCoder videocoder = null;
	    for(int i=0; i<numStreamVideo; i++) {
	        IStream stream = containerVideo.getStream(i);
	        IStreamCoder code = stream.getStreamCoder();

	        if(code.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
	            videostreamt = i;
	            videocoder = code;
	            break;
	        }
	    }
	    System.out.println("video coder: " + videocoder);
	    	    
	    IStreamCoder audiocoder = null;
	    for(int i=0; i<numStreamAudio; i++) {
	        IStream stream = containerAudio.getStream(i);
	        IStreamCoder code = stream.getStreamCoder();
	        if(code.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
	            audiocoder = code;
	        	audiostreamt = i;
	            break;
	        }
	    }
	    System.out.println("audio coder: "+audiocoder);
	    
	    if (videostreamt == -1) throw new RuntimeException("No video stream found");
	    if (audiostreamt == -1) throw new RuntimeException("No audio stream found");
	    
	    if(videocoder.open(null, null)<0 ) throw new RuntimeException("Cant open video coder");
	    if(audiocoder.open(null, null)<0 ) throw new RuntimeException("Cant open audio coder");
	    
	    mWriter.addAudioStream(1, 1, audiocoder.getChannels(), audiocoder.getSampleRate());
	    mWriter.addVideoStream(0, 0, videocoder.getWidth(), videocoder.getHeight());
	    
	    IPacket packetaudio = IPacket.make();
	    IPacket packetvideo = IPacket.make();
	    
	 	/*IContainer cont = IContainer.make();
	 	IContainerFormat cf = IContainerFormat.make();
	 	cf.setOutputFormat("flv", outputName, "video/x-flv");
	 	cont.open(outputName, IContainer.Type.WRITE, cf);
	 	
	 	ICodec nCodec = ICodec.findEncodingCodec(ICodec.ID.CODEC_ID_FLV1);
	 	IStream nVideoStream = cont.addNewStream(nCodec);
	 	IStreamCoder nVideoCoder = nVideoStream.getStreamCoder();
	 	
	 	nVideoCoder.setNumPicturesInGroupOfPictures(videocoder.getNumPicturesInGroupOfPictures());
	 	nVideoCoder.setBitRate(videocoder.getBitRate());
	 	nVideoCoder.setBitRateTolerance(videocoder.getBitRateTolerance());
	 	nVideoCoder.setPixelType(videocoder.getPixelType());
	 	nVideoCoder.setHeight(videocoder.getHeight());
	 	nVideoCoder.setWidth(videocoder.getWidth());
	 	nVideoCoder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, true);
	 	nVideoCoder.setGlobalQuality(videocoder.getGlobalQuality());
	 	
	 	//IRational nFrameRate = IRational.make(3,1);
	 	nVideoCoder.setFrameRate(videocoder.getFrameRate());
	 	nVideoCoder.setTimeBase(videocoder.getTimeBase());
	 	
	 	nVideoCoder.open(null, null);
	 	
	 	IStream nAudioStream = cont.addNewStream(ICodec.ID.CODEC_ID_MP3);
	 	IStreamCoder nAudioCoder = nAudioStream.getStreamCoder();
	 	nAudioCoder.setChannels(audiocoder.getChannels());
	 	nAudioCoder.setSampleFormat(audiocoder.getSampleFormat());
	 	nAudioCoder.setSampleRate(audiocoder.getSampleRate());
	 	
	 	nAudioCoder.open(null, null);
	 	
	 	cont.writeHeader();
	 	
	 	System.out.println(cont.getContainerFormat());
	 	System.out.println(nVideoCoder.getCodec());
	 	
	 	//---------------------------------------------------
	 	
	 	IVideoResampler resampler = null;
	 	if (videocoder.getPixelType() != IPixelFormat.Type.YUV420P){
            // if this stream is not in BGR24, we're going to need to
            // convert it. The VideoResampler does that for us.
	 		System.out.println("resampling");
            resampler = IVideoResampler.make(videocoder.getWidth(),
                    videocoder.getHeight(), IPixelFormat.Type.YUV420P,
                    videocoder.getWidth(), videocoder.getHeight(),
                    videocoder.getPixelType());
	 	}*/
	    
	 	while(containerVideo.readNextPacket(packetvideo) >= 0 ||
	            containerAudio.readNextPacket(packetaudio) >= 0){
	        if(packetvideo.getStreamIndex() == videostreamt){
	        	System.out.println("------video packet----------");
	            //video packet
	            IVideoPicture picture = IVideoPicture.make(videocoder.getPixelType(),
	                    videocoder.getWidth(),
	                    videocoder.getHeight());
	            int offset = 0;
	            while (offset < packetvideo.getSize()){
	                int bytesDecoded = videocoder.decodeVideo(picture, 
	                        packetvideo, 
	                        offset);
	                if(bytesDecoded < 0) throw new RuntimeException("bytesDecoded not working");
	                offset += bytesDecoded;
	                if(picture.isComplete()){
	                	System.out.println(picture.getPixelType());
	                	mWriter.encodeVideo(0, picture);
	                	/*IVideoPicture newpic = picture;
	                	if(resampler!=null){
	                		newpic = IVideoPicture.make(resampler.getOutputPixelFormat(), picture.getWidth(), picture.getHeight());
	                	}
	                	nVideoCoder.encodeVideo(packetvideo, newpic, 0);
	                    */
	                    
	                }
	            }
	        } 

	        if(packetaudio.getStreamIndex() == audiostreamt){   
	        //audio packet
	        	System.out.println("------audio packet----------");
	            IAudioSamples samples = IAudioSamples.make(512, 
	                    audiocoder.getChannels(),
	                    IAudioSamples.Format.FMT_S32);  
	            int offset = 0;
	            while(offset<packetaudio.getSize())
	            {
	                int bytesDecodedaudio = audiocoder.decodeAudio(samples, 
	                        packetaudio,
	                        offset);
	                if (bytesDecodedaudio < 0)
	                    throw new RuntimeException("could not detect audio");
	                offset += bytesDecodedaudio;
	                if (samples.isComplete()){
	                	mWriter.encodeAudio(1, samples);
	                	//nAudioCoder.encodeAudio(packetvideo, samples, 0); 
	                	
	        		}
	            }
	    	}
	        
	        //if (packetvideo.isComplete()){cont.writePacket(packetvideo);}
	    }
	    /*cont.flushPackets();
	    videocoder.close();
	    audiocoder.close();
	    cont.writeTrailer();
	    cont.close();*/
 	}
}