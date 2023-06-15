package br.dev.gee.sdnapster.tracker;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;

public class TrackerMain {

    private TrackerMain() {
        try {
            RemoteChunkMapImpl remoteChunkMapImpl = new RemoteChunkMapImpl();
            Naming.rebind("RMI://127.0.0.1:1020/ChunkMapService", remoteChunkMapImpl); 
        } catch (RemoteException exception) {
            exception.printStackTrace();
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new TrackerMain();
    }
}
