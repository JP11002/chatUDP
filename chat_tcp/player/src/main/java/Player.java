import javax.sound.sampled.AudioFormat;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Player {
    public static void main(String[] ars) throws Exception{
        AudioFormat format=new AudioFormat(44100, 16, 1, true, true);

        PlayerThread thread=new PlayerThread(format);
        thread.start();
        thread.setPlay(true);

        DatagramSocket socket=new DatagramSocket(9090);
        byte[] data=new byte[1024];

        DatagramPacket packet=new DatagramPacket(data,data.length);
        socket.receive(packet);
        thread.play(data);

    }
}
