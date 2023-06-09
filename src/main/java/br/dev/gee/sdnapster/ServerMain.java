package br.dev.gee.sdnapster;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import br.dev.gee.sdnapster.common.Constants;

public class ServerMain {
    public static void main(String[] args) {
        try (
            ServerSocket serverSocket = new ServerSocket(Constants.SERVER_PORT);
        ) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ServerCommsRunnable(clientSocket)).start();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
