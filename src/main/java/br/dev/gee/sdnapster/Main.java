package br.dev.gee.sdnapster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static final int SERVER_PORT = 31337;
    public static final String MAGIC_STRING     = "SEVMTE8gVEhFUkUhIE1BWSBJIENPTUUgSU4/",
                               GOODBYE_STRING   = "RkFSRVdFTEwsIEZSSUVORC4uLiBVTlRJTCBORVhUIFRJTUUh";

    public static void main(String[] args) {
        try (
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
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
