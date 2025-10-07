import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;


public class Client {

    public static void main(String[] args) throws Exception {
        Socket sc = new Socket(InetAddress.getLocalHost(), 9090);


        BufferedReader reader = new BufferedReader(new InputStreamReader(sc.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sc.getOutputStream()));
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        Scanner scanner = new Scanner(System.in);

        new Thread(() -> {
            try {
                String msg;
                while ((msg = reader.readLine()) != null) {
                    System.out.println(msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        String initialPrompt = reader.readLine();
        System.out.println(initialPrompt);
        String username = scanner.nextLine();
        writer.write(username);
        writer.newLine();
        writer.flush();

        // Menú interactivo
        while (true) {
            System.out.println("\n=== MENÚ ===");
            System.out.println("1. Enviar mensaje privado");
            System.out.println("2. Crear grupo");
            System.out.println("3. Unirse a grupo");
            System.out.println("4. Enviar mensaje a grupo");
            System.out.println("5. Enviar audio");
            System.out.println("6. Salir");
            System.out.print("Selecciona una opción: ");

            String option = scanner.nextLine();

            switch (option) {
                case "1":
                    System.out.print("Destinatario: ");
                    String dest = scanner.nextLine();
                    System.out.print("Mensaje: ");
                    String msg = scanner.nextLine();
                    writer.write("/mgs " + dest + " " + msg);
                    writer.newLine();
                    writer.flush();
                    break;

                case "2":
                    System.out.print("Nombre del grupo: ");
                    String groupCreate = scanner.nextLine();
                    writer.write("/group crear " + groupCreate);
                    writer.newLine();
                    writer.flush();
                    break;

                case "3":
                    System.out.print("Nombre del grupo: ");
                    String groupJoin = scanner.nextLine();
                    writer.write("/group join " + groupJoin);
                    writer.newLine();
                    writer.flush();
                    break;

                case "4":
                    System.out.print("Nombre del grupo: ");
                    String groupSend = scanner.nextLine();
                    System.out.print("Mensaje: ");
                    String msgGroup = scanner.nextLine();
                    writer.write("/sendgroup " + groupSend + " " + msgGroup);
                    writer.newLine();
                    writer.flush();
                    break;

                case "5":
                    System.out.print("Grabando audio");
                    String generalMsg = scanner.nextLine();
                    writer.write(generalMsg);
                    writer.newLine();
                    writer.flush();
                    break;

                case "6":
                    System.out.println("Desconectando...");
                    sc.close();
                    System.exit(0);
                    break;

                default:
                    System.out.println("Opción no válida.");
            }
        }
    }
}

