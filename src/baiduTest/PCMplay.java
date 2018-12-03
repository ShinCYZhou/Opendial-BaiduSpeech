package baiduTest;
import java.io.File;  
import java.io.FileInputStream;  
import java.io.FileNotFoundException;  
import java.io.IOException;  
import java.io.InputStream;  
  
import javax.sound.sampled.AudioFormat;  
import javax.sound.sampled.AudioSystem;  
import javax.sound.sampled.DataLine;  
import javax.sound.sampled.LineUnavailableException;  
import javax.sound.sampled.SourceDataLine;  

public class PCMplay {

    /** 
     * @param args 
     */  
    public static void main(String[] args) {  
        // TODO Auto-generated method stub  
  
        try {  
            File file = new File("d:/1.pcm");  
            System.out.println(file.length());  
            int offset = 0;  
            int bufferSize = Integer.valueOf(String.valueOf(file.length())) ;  
            byte[] audioData = new byte[bufferSize];  
            InputStream in = new FileInputStream(file);  
            in.read(audioData);  
  
              
              
            float sampleRate = 16000;  
            int sampleSizeInBits = 16;  
            int channels = 1;  
            boolean signed = true;  
            boolean bigEndian = false;  
            // sampleRate - ÿ���������  
            // sampleSizeInBits - ÿ�������е�λ��  
            // channels - �������������� 1 ���������� 2 ����  
            // signed - ָʾ�������з��ŵģ������޷��ŵ�  
            // bigEndian - ָʾ�Ƿ��� big-endian �ֽ�˳��洢���������е����ݣ�false ��ζ��  
            // little-endian����  
            AudioFormat af = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);  
            SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, af, bufferSize);  
            SourceDataLine sdl = (SourceDataLine) AudioSystem.getLine(info);  
            sdl.open(af);  
            sdl.start();  
            while (offset < audioData.length) {  
                offset += sdl.write(audioData, offset, bufferSize);  
            }  
        } catch (LineUnavailableException e) {  
            // TODO Auto-generated catch block  
            e.printStackTrace();  
        } catch (FileNotFoundException e) {  
            // TODO Auto-generated catch block  
            e.printStackTrace();  
        } catch (IOException e) {  
            // TODO Auto-generated catch block  
            e.printStackTrace();  
        }  
  
    }  
}
