package client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Simplified JavaFX controller that supports:
 * - connect to server and set username
 * - create/join group
 * - send message to user or group
 * - record a short voice note (5s) and send to user or group via TCP
 * - basic local history append (JSON)
 */
public class ChatController {

    @FXML private TextArea chatArea;
    @FXML private TextField nameField;
    @FXML private Button connectBtn;
    @FXML private TextField targetField;
    @FXML private ChoiceBox<String> targetType; // User or Group
    @FXML private TextField messageField;
    @FXML private Button sendBtn;
    @FXML private Button createGroupBtn;
    @FXML private Button joinGroupBtn;
    @FXML private Button recordVoiceBtn;
    @FXML private Button sendVoiceBtn;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private Consumer<String> uiUpdater;
    private File lastVoiceFile;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File historyFile = new File("client_history_" + System.currentTimeMillis() + ".json");

    @FXML public void initialize() {
        targetType.getItems().addAll("user","group");
        targetType.setValue("user");
        sendBtn.setDisable(true);
        recordVoiceBtn.setDisable(true);
        sendVoiceBtn.setDisable(true);
    }

    @FXML public void connect() {
        try {
            username = nameField.getText().trim();
            if (username.isEmpty()) return;
            socket = new Socket("localhost", 5000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);


            String welcome = in.readLine();
            appendToChat(welcome);
            out.println(username);
            appendToChat("Connected as: " + username);


            new Thread(this::listenServer).start();

            sendBtn.setDisable(false);
            recordVoiceBtn.setDisable(false);
            connectBtn.setDisable(true);
            nameField.setDisable(true);
        } catch (Exception e) {
            appendToChat("Connection error: " + e.getMessage());
        }
    }

    private void listenServer() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                final String l = line;
                Platform.runLater(() -> appendToChat(l));
            }
        } catch (IOException e) {
            Platform.runLater(() -> appendToChat("Disconnected from server."));
        }
    }

    @FXML public void createGroup() {
        String g = targetField.getText().trim();
        if (g.isEmpty()) { appendToChat("Enter group name in target field"); return; }
        out.println("/create " + g);
    }

    @FXML public void joinGroup() {
        String g = targetField.getText().trim();
        if (g.isEmpty()) { appendToChat("Enter group name in target field"); return; }
        out.println("/join " + g);
    }

    @FXML public void sendMessage() {
        String target = targetField.getText().trim();
        String type = targetType.getValue();
        String msg = messageField.getText().trim();
        if (target.isEmpty() || msg.isEmpty()) { appendToChat("Target and message required"); return; }
        if (type.equals("user")) {
            out.println("/msg " + target + " " + msg);
        } else {
            out.println("/gmsg " + target + " " + msg);
        }
        appendHistory("TEXT", type + ":" + target, msg, null);
        messageField.clear();
    }

    @FXML public void recordVoice() {
        // record 5 seconds and save wav
        try {
            AudioFormat fmt = new AudioFormat(16000f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, fmt);
            if (!AudioSystem.isLineSupported(info)) {
                appendToChat("Microphone not supported for format.");
                return;
            }
            TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info);
            mic.open(fmt);
            mic.start();
            appendToChat("Recording 5 seconds...");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            long end = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < end) {
                int r = mic.read(buffer, 0, buffer.length);
                if (r > 0) baos.write(buffer,0,r);
            }
            mic.stop();
            mic.close();

            byte[] audioData = baos.toByteArray();
            File tmp = File.createTempFile("voicenote_", ".wav");
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                writeWavHeader(fos, audioData.length, fmt);
                fos.write(audioData);
            }
            lastVoiceFile = tmp;
            appendToChat("Recorded voice note: " + tmp.getName());
            sendVoiceBtn.setDisable(false);
        } catch (Exception e) {
            appendToChat("Recording error: " + e.getMessage());
        }
    }

    @FXML public void sendVoiceNote() {
        if (lastVoiceFile == null || !lastVoiceFile.exists()) { appendToChat("No voice file recorded"); return; }
        String target = targetField.getText().trim();
        String type = targetType.getValue();
        if (target.isEmpty()) { appendToChat("Target required"); return; }
        try {

            byte[] bytes = java.nio.file.Files.readAllBytes(lastVoiceFile.toPath());
            out.println("/voicenote " + type + " " + target + " " + lastVoiceFile.getName() + " " + bytes.length);

            socket.getOutputStream().write(bytes);
            socket.getOutputStream().flush();
            appendToChat("Sent voice note to " + type + ":" + target);
            appendHistory("AUDIO", type + ":" + target, "voice:" + lastVoiceFile.getName(), lastVoiceFile.getName());
        } catch (Exception e) {
            appendToChat("Send voice error: " + e.getMessage());
        }
    }

    private void appendToChat(String s) {
        chatArea.appendText(s + "\n");
    }

    private void appendHistory(String type, String target, String content, String audioFile) {
        try {
            HistoryRecord rec = new HistoryRecord(type, target, content, audioFile);
            java.util.List<HistoryRecord> list = new java.util.ArrayList<>();
            if (historyFile.exists()) {
                HistoryRecord[] arr = gson.fromJson(new FileReader(historyFile), HistoryRecord[].class);
                if (arr != null) list.addAll(java.util.Arrays.asList(arr));
            }
            list.add(rec);
            try (FileWriter fw = new FileWriter(historyFile)) {
                gson.toJson(list, fw);
            }
        } catch (Exception e) {
            appendToChat("History write error: " + e.getMessage());
        }
    }


    private void writeWavHeader(OutputStream out, int dataLen, AudioFormat fmt) throws IOException {
        int sampleRate = (int) fmt.getSampleRate();
        int channels = fmt.getChannels();
        int byteRate = sampleRate * channels * 16/8;
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeBytes("RIFF");
        dos.writeInt(Integer.reverseBytes(36 + dataLen));
        dos.writeBytes("WAVE");
        dos.writeBytes("fmt ");
        dos.writeInt(Integer.reverseBytes(16));
        dos.writeShort(Short.reverseBytes((short)1));
        dos.writeShort(Short.reverseBytes((short)channels));
        dos.writeInt(Integer.reverseBytes(sampleRate));
        dos.writeInt(Integer.reverseBytes(byteRate));
        dos.writeShort(Short.reverseBytes((short)(channels * 16/8)));
        dos.writeShort(Short.reverseBytes((short)16));
        dos.writeBytes("data");
        dos.writeInt(Integer.reverseBytes(dataLen));
    }

    static class HistoryRecord {
        String type; String target; String content; String audioFile; long timestamp = System.currentTimeMillis();
        HistoryRecord(String t, String tg, String c, String af) { type=t; target=tg; content=c; audioFile=af; }
    }
}
