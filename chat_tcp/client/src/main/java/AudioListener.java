import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class AudioListener {
    public static void main(String[] args) throws Exception {
        AudioFormat format = new AudioFormat(44100, 16, 2, true, true);

        DatagramSocket socket = new DatagramSocket();

        DataLine.Info infoSpeaker=new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine speaker= (SourceDataLine) AudioSystem.getLine(infoSpeaker);
        speaker.open(format);
        speaker.start();

        byte[] buffer=new byte[1024];
        DatagramPacket packet=new DatagramPacket(buffer,buffer.length);

        while(true){
            socket.receive(packet);
            speaker.write(packet.getData(), 0,packet.getLength());
        }

    }
}
