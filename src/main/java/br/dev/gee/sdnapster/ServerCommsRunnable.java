package br.dev.gee.sdnapster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerCommsRunnable implements Runnable {
    private Socket clientSocket;

    public ServerCommsRunnable(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ) {
            String receivedMessage = in.readLine();
            if (receivedMessage != Main.MAGIC_STRING)
                return;
            while ((receivedMessage = in.readLine()) != null) {
                if (receivedMessage == Main.GOODBYE_STRING)
                    return;
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
