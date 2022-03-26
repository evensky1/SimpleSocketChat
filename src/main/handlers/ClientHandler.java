package main.handlers;

import main.entity.FileInfo;
import main.exceptions.SuchUserAlsoExistsException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class ClientHandler implements Runnable {

    private static final ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private static final ArrayList<FileInfo> files = new ArrayList<>();
    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private String clientUsername;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.dataInputStream = new DataInputStream(socket.getInputStream());
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());

            int usernameLength = dataInputStream.readInt();
            this.clientUsername = new String(dataInputStream.readNBytes(usernameLength), StandardCharsets.UTF_8);

            if (clientHandlers.stream().anyMatch(user -> user.clientUsername.equals(this.clientUsername))) {
                throw new SuchUserAlsoExistsException("SERVER: User with such name also exists");
            } else {
                clientHandlers.add(this);
                String congratsMessage = "SERVER: You were successfully connected";
                dataOutputStream.writeInt(congratsMessage.length());
                dataOutputStream.write(congratsMessage.getBytes(StandardCharsets.UTF_8));
                dataOutputStream.flush();
                broadcastMessage("SERVER: " + this.clientUsername + " has entered the chat");
            }
        } catch (IOException e) {
            closeEverything(socket, dataInputStream, dataOutputStream);
        } catch (SuchUserAlsoExistsException e) {
            closeEverything(socket, dataInputStream, dataOutputStream);
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        while (socket.isConnected()) {
            try {
                int messageLength = dataInputStream.readInt();
                String messageFromClient = new String(dataInputStream.readNBytes(messageLength), StandardCharsets.UTF_8);

                if (messageFromClient.equals("-f")) {

                    int fileNameLength = dataInputStream.readInt();
                    String fileName = new String(dataInputStream.readNBytes(fileNameLength), StandardCharsets.UTF_8);

                    int fileContentLength = dataInputStream.readInt();
                    byte[] fileContent = dataInputStream.readNBytes(fileContentLength);

                    files.add(new FileInfo(fileName, fileContent));
                    String serverMessage = "SERVER: " + this.clientUsername + " just send " + fileName;
                    broadcastMessage(serverMessage);

                } else if (messageFromClient.matches(".+: -sf$")) {

                    for (FileInfo file : files) {
                        byte[] rawFileName = file.getName().getBytes(StandardCharsets.UTF_8);
                        this.dataOutputStream.writeInt(rawFileName.length);
                        this.dataOutputStream.write(rawFileName);
                        this.dataOutputStream.flush();
                    }

                } else if (messageFromClient.matches(".+: -d .+")) {

                    String fileName = messageFromClient.replaceFirst(".+: -d ", "");

                    FileInfo fileToSend = files
                            .stream()
                            .filter(fileInfo -> fileInfo.getName().equals(fileName))
                            .findFirst()
                            .orElseThrow(IOException::new);

                    sendFile(fileToSend);
                } else {
                    broadcastMessage(messageFromClient);
                }
            } catch (IOException e) {
                closeEverything(socket, dataInputStream, dataOutputStream);
                break;
            }
        }
    }

    private void sendFile(FileInfo fileToSend) throws IOException {
        dataOutputStream.writeInt(2);
        dataOutputStream.writeBytes("-d");
        dataOutputStream.flush();

        byte[] rawFileName = fileToSend.getName().getBytes(StandardCharsets.UTF_8);
        this.dataOutputStream.writeInt(rawFileName.length);
        this.dataOutputStream.write(rawFileName);

        byte[] rawFileContent = fileToSend.getData();
        this.dataOutputStream.writeInt(rawFileContent.length);
        this.dataOutputStream.write(rawFileContent);

        this.dataOutputStream.flush();
    }

    private void closeEverything(Socket socket,
                                 DataInputStream dataInputStream,
                                 DataOutputStream dataOutputStream) {

        removeClientHandler();
        try {
            if (socket != null) {
                socket.close();
            }
            if (dataInputStream != null) {
                dataInputStream.close();
            }
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastMessage(String message) {
        clientHandlers.stream()
                .filter(clientHandler -> !clientHandler.clientUsername.equals(this.clientUsername))
                .forEach(clientHandler -> {
                    try {
                        clientHandler.dataOutputStream.writeInt(message.getBytes(StandardCharsets.UTF_8).length);
                        clientHandler.dataOutputStream.write(message.getBytes(StandardCharsets.UTF_8));
                        clientHandler.dataOutputStream.flush();
                    } catch (IOException e) {
                        closeEverything(socket, dataInputStream, dataOutputStream);
                    }
                });
    }

    private void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessage("SERVER: " + clientUsername + " has left the chat");
    }
}