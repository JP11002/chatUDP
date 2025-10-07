import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class Server {

    private static int acc=0;
    private static Semaphore semaphore=new Semaphore(1);
    private static Executor ex= Executors.newFixedThreadPool(15);
    private static ConcurrentHashMap<String,BufferedWriter> clients=new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Set<String>> groups=new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, java.net.SocketAddress> udpClients = new ConcurrentHashMap<>();


    public static void main(String[] args) throws Exception{

        InetAddress address= Inet4Address.getByName(InetAddress.getLocalHost().getHostAddress());
        final ServerSocket socket= new ServerSocket(9090,0,address);

        Runtime.getRuntime().addShutdownHook(new Thread(() ->{
            try{
            socket.close();
            } catch (Exception e){
                e.printStackTrace();
            } 
            
        }));

        while (true){
            Socket sc=socket.accept();
            ex.execute(()->{
                try{
                    responder(sc);
                } catch (Exception e){
                    e.printStackTrace();
                }
            });

        }




    }


    public static void responder(Socket sc) throws Exception{


        BufferedReader reader= new BufferedReader(new InputStreamReader(sc.getInputStream()));
        BufferedWriter writer= new BufferedWriter(new OutputStreamWriter(sc.getOutputStream()));

        writer.write("Ingresar tu nombre de usuario: ");
        writer.newLine();
        writer.flush();
        String username=reader.readLine();

        clients.put(username, writer);
        System.out.println("Usuario conectado: "+ username);

        String msg;
        while((msg=reader.readLine())!=null){
            if(msg.startsWith("/mgs")){

                String[] parts=msg.split(" ",3);
                if(parts.length==3){
                    String dest=parts[1];
                    String content=username+": "+parts[2];
                    sendToUser(dest,content);
                }
            } else if(msg.startsWith("/group")){
                String[] parts=msg.split(" ",3);
                if(parts.length>=3 && parts[1].equals("crear")){
                    groups.put(parts[2],ConcurrentHashMap.newKeySet());
                    writer.write("Grupo "+parts[2]+" creado");
                    writer.newLine();
                    writer.flush();
                } else if(parts.length>=3 && parts[1].equals("join")){
                    groups.computeIfAbsent(parts[2],k->ConcurrentHashMap.newKeySet()).add(username);
                    writer.write("Te uniste al grupo "+parts[2]);
                    writer.newLine();
                    writer.flush();
                }
            } else if (msg.startsWith("/sendgroup")) {
                String[] parts = msg.split(" ", 3);
                if (parts.length == 3) {
                    String group = parts[1];
                    String content = username + ": " + parts[2];
                    sendToGroup(group, content);
                }
            } else {
                System.out.println("Mensaje de " + username + ": " + msg);
                writer.write("Yo: " + msg);
                writer.newLine();
                writer.flush();
            }
        }

        clients.remove(username);
        System.out.println("Usuario desconectado: " + username);
        sc.close();

    }
    private static void sendToUser(String user, String msg) throws IOException {
        BufferedWriter writer = clients.get(user);
        if (writer != null) {
            writer.write(msg);
            writer.newLine();
            writer.flush();
        }
    }
    private static void sendToGroup(String group, String msg) throws IOException {
        Set<String> members = groups.get(group);
        if (members != null) {
            for (String u : members) {
                sendToUser(u, msg);
            }
        }
    }
    private static void sendAudioToUser(String user, byte[] audioData, DatagramSocket udpSocket) throws IOException {
        java.net.SocketAddress addr = udpClients.get(user);
        if (addr != null) {
            DatagramPacket packet = new DatagramPacket(audioData, audioData.length, addr);
            udpSocket.send(packet);
        }
    }

    private static void sendAudioToGroup(String group, byte[] audioData, DatagramSocket udpSocket) throws IOException {
        Set<String> members = groups.get(group);
        if (members != null) {
            for (String u : members) {
                sendAudioToUser(u, audioData, udpSocket);
            }
        }
    }


}
