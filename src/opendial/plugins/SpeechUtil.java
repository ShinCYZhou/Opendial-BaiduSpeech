package opendial.plugins;

import java.util.logging.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;  
import java.io.InputStream; 
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.values.StringVal;
import opendial.bn.values.Value;
import opendial.datastructs.SpeechData;
import opendial.gui.GUIFrame;
import opendial.modules.Module;
//import opendial.modules.AudioModule;
import opendial.utils.InferenceUtils;



//
import com.baidu.aip.speech.AipSpeech;
import com.baidu.aip.speech.TtsResponse;
import com.baidu.aip.util.Util;


import javazoom.jl.decoder.JavaLayerException;
//import javazoom.jl.player.Player;


import javax.sound.sampled.AudioFileFormat;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javazoom.jl.player.Player;

import org.json.JSONArray;
import org.json.JSONObject;
import com.alibaba.fastjson.JSON;


/**
 * Plugin to access the Nuance Speech API for both speech recognition and synthesis.
 * The plugin necessitates the specification of an application ID and key [see the
 * README file for the plugin for details]. The API offers a cloud-based access to
 * the Nuance Mobile developer platform.
 * 
 * <p>
 * To enhance the recognition accuracy, it is recommended to provide the system with
 * custom vocabularies (see the documentation on the Nuance website).
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class SpeechUtil implements Module {

	 public static final String APP_ID = "11679901";

	    public static final String API_KEY = "FMkPBfeCmc7kGQmhHr3prGzN";

	    public static final String SECRET_KEY = "WpWbnNu9SDUscwWTs2sQRtw1WXvGssCg";
	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	/** dialogue state */
	DialogueSystem system;

	/** HTTP client and URI for the speech recognition */
	 //CloseableHttpClient asrClient;
	//URI asrURI;
    AipSpeech client;

	/** HTTP client and URI for the speech synthesis */
	//CloseableHttpClient ttsClient;
	//URI ttsURI;
    //AipSpeech clientS;

	/** Cache of previously synthesised system utterances */
	Map<String, SpeechData> ttsCache;

	/** List of speech outputs currently being synthesised */
	List<SpeechData> currentSynthesis;

	/** whether the system is paused or active */
	boolean paused = true;

	/**
	 * Creates a new plugin, attached to the dialogue system
	 * 
	 * @param system the dialogue system to attach @ in case of missing parameters
	 */
	public SpeechUtil(DialogueSystem system) {
		this.system = system;
		List<String> missingParams =
				new LinkedList<String>(Arrays.asList("id", "key",  "secret_key", "lang"));
		missingParams.removeAll(system.getSettings().params.keySet());
		if (!missingParams.isEmpty()) {
			throw new RuntimeException("Missing parameters: " + missingParams);
		}

		currentSynthesis = new ArrayList<SpeechData>();
		client=buildClient();

		system.enableSpeech(true);
	}

	/**
	 * Starts the baidu speech plugin.
	 */
	@Override
	public void start() {
		paused = false;
		GUIFrame gui = system.getModule(GUIFrame.class);
		if (gui == null) {
			throw new RuntimeException(
					"Baidu connection requires access to the GUI");
		}
		ttsCache = new HashMap<String, SpeechData>();
	}

	/**
	 * Pauses the plugin.
	 */
	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}

	/**
	 * Returns true if the plugin has been started and is not paused.
	 */
	@Override
	public boolean isRunning() {
		return !paused;
	}

	/**
	 * If the system output has been updated, trigger the speech synthesis.
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {

		String userSpeechVar = system.getSettings().userSpeech;
		String outputVar = system.getSettings().systemOutput;

		
		// if a new user speech is detected, start the speech recognition
		if (updatedVars.contains(userSpeechVar) && state.hasChanceNode(userSpeechVar)
				&& !paused) {

			Value speechVal = system.getContent(userSpeechVar).getBest();
			if (speechVal instanceof SpeechData) {
				Thread t = new Thread(() -> recognise((SpeechData) speechVal));
				t.start();
				
			}
		}

		// if a new system speech is detected, start speech synthesis
		else if (updatedVars.contains(outputVar) && state.hasChanceNode(outputVar)
				&& !paused) {
			Value utteranceVal = system.getContent(outputVar).getBest();
			
			if (utteranceVal instanceof StringVal) {
				synthesise(utteranceVal.toString());
			}
		}
	}

	/**
	 * Processes the audio data contained in tempFile (based on the recognition
	 * grammar whenever provided) and updates the dialogue state with the new user
	 * inputs.
	 * 
	 * @param stream the speech stream containing the audio data
	 */
	private void recognise(SpeechData stream) {

	
		try {
		
			Thread.sleep(2000);

	        // 可选：设置网络连接参数
	        //client.setConnectionTimeoutInMillis(2000);
	        //client.setSocketTimeoutInMillis(60000);
	        
	        byte[] data=stream.toByteArray();
			
            //System.out.println(" Speechrecognition : " + stream.toString());
            //System.out.println(" Speechrecognition : " + stream);

			
           // Thread.sleep(1000);
            
            //参数设置：英文
            HashMap<String, Object> paramMap = new HashMap<String, Object>();
    		paramMap.put("dev_pid",1737);
    		
	        JSONObject res = client.asr(data, "pcm", 16000, paramMap);
	        JSONArray  r=res.getJSONArray("result");
	        String str=r.toString();
	        String subStr=str.substring(2, str.length()-2);
	   
	        //test
	        System.out.println(" Speechrecognition : " + res.toString());
	        System.out.println(" Speechrecognition : " + subStr);
	        Util.writeBytesToFileSystem(data, "d:/1.pcm");
	        //new Player(new BufferedInputStream(new FileInputStream(new File("d:/1.mp3")))).play();
	        
	        InputStream ss = new ByteArrayInputStream(subStr.getBytes());
	        BufferedReader reader = new BufferedReader(
					new InputStreamReader(ss));

			

	        String sentence;
			Map<String, Double> lines = new HashMap<String, Double>();
			while ((sentence = reader.readLine() ) != null) {//reader.readLine()
				lines.put(sentence, 1.0 / (lines.size() + 1));
			}
			lines = InferenceUtils.normalise(lines);
			for (String s : new ArrayList<String>(lines.keySet())) {
				lines.put(s, ((int) (lines.get(s) * 100)) / 100.0);
			}

			log.fine("recognition results: " + lines);
			reader.close();
			if (!lines.isEmpty()) {
				system.addUserInput(lines);
			}
			//httppost.releaseConnection();
		}
		catch (Exception e) {
			log.warning("could not extract ASR results: " + e);
		}
	}

	/**
	 * Synthesises the provided utterance (first looking at the cache of existing
	 * synthesised speech, and starting the generation if no one is already present).
	 * 
	 * @param utterance the utterance to synthesise
	 */
	private void synthesise(String utterance) {

		String systemSpeechVar = system.getSettings().systemSpeech;

		SpeechData outputSpeech;
		if (ttsCache.containsKey(utterance)) {
			outputSpeech = ttsCache.get(utterance);
			outputSpeech.rewind();
		}
		else {
			AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
			outputSpeech = new SpeechData(format);
			new Thread(() -> synthesise(utterance, outputSpeech)).start();
		}

		currentSynthesis.add(outputSpeech);
		new Thread(() -> {
			while (!currentSynthesis.get(0).equals(outputSpeech)) {
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
				}
			}
			system.addContent(systemSpeechVar, outputSpeech);
			currentSynthesis.remove(0);
		}).start();
	}
	
	
	/**
	 * Synthesises the provided utterance and adds the resulting stream of audio data
	 * to the SpeechData object.
	 * 
	 * @param utterance the utterance to synthesise
	 * @param output the speech data in which to write the generated audio
	 * @throws JavaLayerException 
	 */
	private void synthesise(String utterance, SpeechData output) {

		try {
			log.fine("calling baidu server to synthesise utterance \"" + utterance
					+ "\"");

			   /*
	        	最长的长度
	         
	        int maxLength = 1024;
	        if (utterance.getBytes().length >= maxLength) {
	            log.warning("The sentence is too long!");
	        }
	      */
	        // 可选：设置网络连接参数
	       // client.setConnectionTimeoutInMillis(2000);
	        //client.setSocketTimeoutInMillis(60000);

	        // 可选：设置代理服务器地址, http和socket二选一，或者均不设置
//	        client.setHttpProxy("proxy_host", proxy_port);  // 设置http代理
//	        client.setSocketProxy("proxy_host", proxy_port);  // 设置socket代理

	        // 调用接口
	        //lang: "zh"/ "en" 
	        TtsResponse res = client.synthesis(utterance,system.getSettings().params.getProperty("lang"), 1, null);
	        //
	        byte[] data = res.getData();
	        ///////////////////byte[] data =res.toString().getBytes();
	        //AudioSystem.write(data, AudioFileFormat.Type.WAVE, "d:/SpeechSynthesizer.pcm");
	        Util.writeBytesToFileSystem(data, "d:/SpeechSynthesizer.mp3");
	        convertMP32Pcm("d:/SpeechSynthesizer.mp3","d:/SpeechSynthesizer.pcm");
	        byte[] data2 = Util.readFileByBytes("d:/SpeechSynthesizer.pcm");
	        //new Player(new BufferedInputStream(new FileInputStream(new File("d:/SpeechSynthesizer.mp3")))).play();
	        
	        InputStream s = new ByteArrayInputStream(data2);
	        
	        //String ss=s.toString();

	      
	         //问题   
				output.write(s);
				
				output.setAsFinal();
				ttsCache.put(utterance, output);
				//语言朗读
				//AudioModule audio=new AudioModule(system);
				//audio.playSpeech(output);
				
				log.fine("... Speech synthesis completed!");
	        
		}
		catch (Exception e) {
			e.printStackTrace();
		}
			
	}

	/**
	 * Builds the REST clients for speech recognition and synthesis.
	 * 
	 * @public void
	 */
	  public AipSpeech buildClient() {
		try {
			
			 if (client==null){
		            synchronized (AipSpeech.class){
		                if (client==null) {
		                    client = new AipSpeech(system.getSettings().params.getProperty("id"), system.getSettings().params.getProperty("key"), system.getSettings().params.getProperty("secret_key"));
		                	
		                    //client = new AipSpeech(APP_ID, API_KEY, SECRET_KEY);  
		                }
		            }
		        }
			 return client;
		}
		catch (Exception e) {
			throw new RuntimeException("cannot build client: " + e);
		}
	}

	  /**
	     *  mp3转pcm
	     * @param mp3filepath MP3文件存放路径
	     * @param pcmfilepath pcm文件保存路径
	     * @return
	     */
	    public static boolean convertMP32Pcm(String mp3filepath, String pcmfilepath){
	        try {
	            //获取文件的音频流，pcm的格式
	            AudioInputStream audioInputStream = getPcmAudioInputStream(mp3filepath);
	            //将音频转化为  pcm的格式保存下来
	            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File(pcmfilepath));
	            return true;
	        } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	            return false;
	        }
	    }

	  /**
	     * 获得pcm文件的音频流
	     * @param mp3filepath
	     * @return
	     */
	    private static AudioInputStream getPcmAudioInputStream(String mp3filepath) {
	        File mp3 = new File(mp3filepath);
	        AudioInputStream audioInputStream = null;
	        AudioFormat targetFormat = null;
	        try {
	            AudioInputStream in = null;
	            MpegAudioFileReader mp = new MpegAudioFileReader();
	            in = mp.getAudioInputStream(mp3);
	            AudioFormat baseFormat = in.getFormat();
	            targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16,
	                    baseFormat.getChannels(), baseFormat.getChannels()*2, baseFormat.getSampleRate(), false);
	            audioInputStream = AudioSystem.getAudioInputStream(targetFormat, in);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return audioInputStream;
	    }

}