package main.server;

import main.handlers.ClientHandler;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.Scanner;

public class Server {

    private final ServerSocket serverSocket;

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public static void main(String[] args) throws IOException {
        try {
            var scanner = new Scanner(System.in);
            System.out.println("Enter port number:");
            var serverSocket = new ServerSocket(scanner.nextInt());
            var server = new Server(serverSocket);
            server.startServer();
        } catch (BindException e) {
            System.out.println("This port is already in use, try another one");
        }
    }

    private void startServer() {
        try {
            System.out.println("Server is running . . .");
            while (!serverSocket.isClosed()) {
                var socket = serverSocket.accept();
                System.out.println("Someone has connected");
                var clientHandler = new ClientHandler(socket);
                var thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            closeServerSocket();
        }
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}