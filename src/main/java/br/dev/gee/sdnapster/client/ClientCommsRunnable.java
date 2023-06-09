package br.dev.gee.sdnapster.client;

import java.net.Socket;

public class ClientCommsRunnable implements Runnable {
    private Socket serverSocket;

    public ClientCommsRunnable(Socket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        
    }
}
