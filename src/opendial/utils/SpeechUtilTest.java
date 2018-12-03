package opendial.utils;

import com.alibaba.fastjson.JSON;
import com.baidu.aip.speech.AipSpeech;
import com.baidu.aip.speech.TtsResponse;
import com.baidu.aip.util.Util;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import lombok.extern.slf4j.Slf4j;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * 百度语音工具�?
 */
@Slf4j
public class SpeechUtilTest {


    public static final String APP_ID = "14359027";

    public static final String API_KEY = "4cOcaGTuBq6RKHtFiKR7YMTE";

    public static final String SECRET_KEY = "zVc6O7YserwWV0xVjGGz1lIZNEYZb79B";

    private static AipSpeech client;

    public static void main(String[] args) throws IOException, JavaLayerException, JSONException {
        SpeechSynthesizer("one two three", "d:/SpeechSynthesizer.mp3");
        new Player(new BufferedInputStream(new FileInputStream(new File("d:/SpeechSynthesizer.mp3")))).play();
        convertMP32Pcm("d:/SpeechSynthesizer.mp3","d:/SpeechSynthesizer.pcm");
        SpeechRecognition("d:/SpeechSynthesizer.pcm","pcm");
    }


    /**
     * 单例 懒加载模�? 返回实例
     * @return
     */
    public static AipSpeech getInstance(){
        if (client==null){
            synchronized (AipSpeech.class){
                if (client==null) {
                    client = new AipSpeech(APP_ID, API_KEY, SECRET_KEY);
                }
            }
        }
        return client;
    }

    /**
     * 语音合成
     * @param word 文字内容
     * @param outputPath 合成语音生成路径
     * @return
     */
    public static boolean SpeechSynthesizer(String word, String outputPath) {
        /*
        �?长的长度
         */
        int maxLength = 1024;
        if (word.getBytes().length >= maxLength) {
            return false;
        }
        // 初始化一个AipSpeech
        client = getInstance();

        // 可�?�：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);

        // 可�?�：设置代理服务器地�?, http和socket二�?�一，或者均不设�?
//        client.setHttpProxy("proxy_host", proxy_port);  // 设置http代理
//        client.setSocketProxy("proxy_host", proxy_port);  // 设置socket代理

        // 调用接口
        TtsResponse res = client.synthesis(word, "zh", 1, null);
        byte[] data = res.getData();
        org.json.JSONObject res1 = res.getResult();
        if (data != null) {
            try {
                Util.writeBytesToFileSystem(data, outputPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        if (res1 != null) {
        	System.out.println(" result : " + res1.toString());
        }
        return false;

    }

    /**
     * 语音识别
     * @param videoPath
     * @param videoType
     * @return
     * @throws JSONException 
     */
    public static String SpeechRecognition(String videoPath, String videoType) throws JSONException {
        // 初始化一个AipSpeech
        client = getInstance();

        // 可�?�：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);

        // 可�?�：设置代理服务器地�?, http和socket二�?�一，或者均不设�?
//        client.setHttpProxy("proxy_host", proxy_port);  // 设置http代理
//        client.setSocketProxy("proxy_host", proxy_port);  // 设置socket代理


        // 调用接口 log.info
        //英文
        HashMap<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("dev_pid",1737);
		
        JSONObject res = client.asr(videoPath, videoType, 16000, paramMap);
        JSONArray  r=res.getJSONArray("result");
        String s=r.toString();
        //String[] r=res.getStringArray("result");
        System.out.println(" SpeechRecognition : " + s.substring(2, s.length()-2));
        return res.toString();
        //System.out.println(res.toString(2));//
    }


    /**
     *  mp3转pcm
     * @param mp3filepath MP3文件存放路径
     * @param pcmfilepath pcm文件保存路径
     * @return
     */
    public static boolean convertMP32Pcm(String mp3filepath, String pcmfilepath){
        try {
            //获取文件的音频流，pcm的格�?
            AudioInputStream audioInputStream = getPcmAudioInputStream(mp3filepath);
            //将音频转化为  pcm的格式保存下�?
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
