import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.LinkedList;
import java.util.Queue;

public class PlayerThread extends Thread {
    private static Queue<byte[]> audioBytes = new LinkedList<>();
    private boolean isPlaying;
    private DataLine.Info infoSpeaker;
    private SourceDataLine speaker;

    public PlayerThread(AudioFormat format) throws Exception {
        infoSpeaker=new DataLine.Info(SourceDataLine.class,format);
        speaker=(SourceDataLine) AudioSystem.getLine(infoSpeaker);


    }

    public void setPlay(boolean isPlaying){
        this.isPlaying = isPlaying;
    }

    public void play(byte[] batch){
        audioBytes.add(batch);
    }

    @Override
    public void run() {
        while (true){
            try {
               if (isPlaying){
                   if(!audioBytes.isEmpty()){
                       byte[] current=audioBytes.poll();
                       speaker.write(current,0,current.length);
                   }else {
                       Thread.yield();
                   }
               }else{
                   Thread.sleep(5000);
               }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
