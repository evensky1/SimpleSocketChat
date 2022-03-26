package main.client;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {

    private final String relativePath = "C:\\Users\\fromt\\OneDrive\\Рабочий стол\\";
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private String username;

    public Client(Socket socket, String username) {
        try {
            this.socket = socket;
            this.dataInputStream = new DataInputStream(socket.getInputStream());
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
            this.username = username;
        } catch (IOException e) {
            closeEverything(socket, dataInputStream, dataOutputStream);
        }
    }

    public static void main(String[] args) throws IOException {
        var scanner = new Scanner(System.in);

        System.out.println("Enter your username: ");
        String username = scanner.nextLine();

        System.out.println("Enter your host name: ");
        String hostName = scanner.nextLine();

        System.out.println("Enter port");
        int port = scanner.nextInt();

        try {
            var socket = new Socket(hostName, port);
            var client = new Client(socket, username);
            client.listenForMessage();
            client.sendMessage();
        } catch (ConnectException e) {
            System.out.println("There is no server on such port");
        } catch (UnknownHostException e) {
            System.out.println("There is no such host");
        } catch (SocketException e) {
            System.out.println("There is a problem with creating user socket");
        }

    }

    private void sendMessage() {
        try {
            dataOutputStream.writeInt(username.getBytes(StandardCharsets.UTF_8).length);
            dataOutputStream.write(username.getBytes(StandardCharsets.UTF_8));
            dataOutputStream.flush();

            var scanner = new Scanner(System.in);
            while (socket.isConnected()) {
                String clientInput = scanner.nextLine();
                if (clientInput.matches("^-f.+")) {
                    clientInput = clientInput.replaceAll("^-f +| +$", "");
                    dataOutputStream.writeInt(2);
                    dataOutputStream.writeBytes("-f");
                    dataOutputStream.flush();

                    File fileToSend = new File(clientInput);
                    var fileInputStream = new FileInputStream(fileToSend.getAbsolutePath());

                    byte[] fileNameBytes = fileToSend.getName().getBytes(StandardCharsets.UTF_8);

                    byte[] fileContentBytes = new byte[(int) fileToSend.length()];
                    fileInputStream.read(fileContentBytes);

                    dataOutputStream.writeInt(fileNameBytes.length);
                    dataOutputStream.write(fileNameBytes);

                    dataOutputStream.writeInt(fileContentBytes.length);
                    dataOutputStream.write(fileContentBytes);

                } else {
                    String message = username + ": " + clientInput;
                    dataOutputStream.writeInt(message.getBytes(StandardCharsets.UTF_8).length);
                    dataOutputStream.write(message.getBytes(StandardCharsets.UTF_8));
                }
                dataOutputStream.flush();
            }
        } catch (IOException e) {
            closeEverything(socket, dataInputStream, dataOutputStream);
        }
    }

    private void closeEverything(Socket socket,
                                 DataInputStream dataInputStream,
                                 DataOutputStream dataOutputStream) {

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

    private void listenForMessage() {
        new Thread(() -> {
            while (socket.isConnected()) {
                try {
                    int messageLength = dataInputStream.readInt();
                    String msgFromGroupChat = new String(dataInputStream.readNBytes(messageLength), StandardCharsets.UTF_8);

                    if (msgFromGroupChat.equals("-d")) {
                        int fileNameLength = dataInputStream.readInt();
                        String fileName = new String(dataInputStream.readNBytes(fileNameLength), StandardCharsets.UTF_8);

                        int fileContentLength = dataInputStream.readInt();
                        byte[] fileContent = dataInputStream.readNBytes(fileContentLength);

                        var downloadedFile = new File(relativePath + fileName);
                        var fileOutputStream = new FileOutputStream(downloadedFile);
                        fileOutputStream.write(fileContent);

                        fileOutputStream.flush();
                    } else {
                        System.out.println(msgFromGroupChat);
                    }
                } catch (IOException e) {
                    closeEverything(socket, dataInputStream, dataOutputStream);
                }
            }
        }).start();
    }
}
