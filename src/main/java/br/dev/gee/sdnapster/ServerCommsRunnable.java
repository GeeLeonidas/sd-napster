package br.dev.gee.sdnapster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;

public class ServerCommsRunnable implements Runnable {
    private Socket clientSocket;

    public ServerCommsRunnable(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        Logger log = Logger.getLogger("ServerComms (id %d)".formatted(Thread.currentThread().getId()));
        try (
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ) {
            String receivedMessage = in.readLine();
            if (receivedMessage != ServerMain.MAGIC_STRING) {
                log.severe("Received a first message that isn't magical :(");
                return;
            }
            while ((receivedMessage = in.readLine()) != null) {
                if (receivedMessage == ServerMain.GOODBYE_STRING)
                    return;
                final String messageCode = receivedMessage.substring(0, 3),
                             messageContent = receivedMessage.substring(3);
                switch (messageCode) {
                    case "NEW":
                        final String[] args = messageContent.split("//");
                        if (args.length != 2) {
                            log.warning("Received borked message content (code '%s'): args.length != 2".formatted(messageCode));
                            break;
                        }
                        break;
                    default:
                        log.warning("Received unknown message code: '%s'".formatted(messageCode));
                        break;
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
