package br.dev.gee.sdnapster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;
import java.util.HashMap;
import java.util.logging.Logger;

import br.dev.gee.sdnapster.common.Constants;

public class ServerCommsRunnable implements Runnable {
    private Socket clientSocket;
    private HashMap<String, HashMap<String, Integer>> addressToFileInfoMap;

    public ServerCommsRunnable(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.addressToFileInfoMap = new HashMap<>();
    }

    @Override
    public void run() {
        Logger log = Logger.getLogger("ServerComms (id %d)".formatted(Thread.currentThread().getId()));
        try (
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ) {
            String receivedMessage = in.readLine();
            if (receivedMessage != Constants.MAGIC_STRING) {
                log.severe("Received a first message that isn't magical :(");
                return;
            }
            while ((receivedMessage = in.readLine()) != null) {
                if (receivedMessage == Constants.GOODBYE_STRING)
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
                        /***
                         * fileName: Nome do arquivo que é disponibilizado pelo peer
                         * fileLastIdx: Index do último pedaço disponível do arquivo (assume que todos os anteriores estão também disponíveis)
                         * address: Utilizado como identificador da máquina do peer
                         ***/
                        final String fileName = new String(Base64.getUrlDecoder().decode(args[0]));
                        final int fileLastIdx = Math.max(0, Integer.parseInt(args[1]));
                        final String address = clientSocket.getInetAddress().getHostAddress();
                        
                        HashMap<String, Integer> fileToIdxMap = addressToFileInfoMap.getOrDefault(address, new HashMap<>());
                        fileToIdxMap.put(fileName, fileLastIdx);
                        addressToFileInfoMap.put(address, fileToIdxMap);
                        if (fileToIdxMap.size() == 1)
                            log.info("Created file info table for address '%s'!".formatted(address));
                        log.info("Updated file for address '%s'! Name (index '%d'): '%s'".formatted(address, fileLastIdx, fileName));
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
