import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Capturer {
    public static void main(String[] args) throws Exception {
        AudioFormat format= new AudioFormat(44100,16,2,true,true);

        DatagramSocket sc=new DatagramSocket(9090);

        DataLine.Info infoSpeaker = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine speaker=(SourceDataLine) AudioSystem.getLine(infoSpeaker);



        DataLine.Info infoMic=new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine mic=(TargetDataLine) AudioSystem.getLine(infoMic);
        InetAddress address=InetAddress.getByName("192.168.131.42");

        mic.open();
        speaker.open();

        mic.start();
        speaker.start();

        byte[] buffer=new byte[1024];

        while(true){
            int resp= mic.read(buffer, 0, buffer.length);
            if(resp>0){
                DatagramPacket packet=new DatagramPacket(buffer,resp,address,9091);
                sc.send(packet);
            }
        }

    }
}