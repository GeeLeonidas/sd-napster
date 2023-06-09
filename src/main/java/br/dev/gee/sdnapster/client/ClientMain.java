package br.dev.gee.sdnapster.client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import br.dev.gee.sdnapster.common.Constants;

public class ClientMain {
    public static void main(String[] args) {
        try (Socket serverSocket = new Socket("localhost", Constants.SERVER_PORT)) {
            Thread clientCommsThread = new Thread(new ClientCommsRunnable(serverSocket));
            clientCommsThread.start();
            clientCommsThread.join();
        } catch (UnknownHostException exception) {
            exception.printStackTrace();
        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
    }
}
