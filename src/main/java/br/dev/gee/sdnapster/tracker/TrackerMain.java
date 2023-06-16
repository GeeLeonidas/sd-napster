package br.dev.gee.sdnapster.tracker;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import br.dev.gee.sdnapster.common.ChunkMapService;
import br.dev.gee.sdnapster.common.MD5;

public class TrackerMain extends UnicastRemoteObject implements ChunkMapService {
    private static ConcurrentHashMap<String, HashMap<String, Set<MD5>>> chunkMapByHost;

    public TrackerMain() throws RemoteException {
        super();
    }

    @Override
    public void put(@Nonnull String fromFile, @Nonnull Set<MD5> chunkHashes) throws RemoteException {
        try {
            final String clientHost = RemoteServer.getClientHost();
            HashMap<String, Set<MD5>> newValue =
                chunkMapByHost.getOrDefault(clientHost, new HashMap<>());
            newValue.put(fromFile, chunkHashes);
            chunkMapByHost.put(clientHost, newValue);
        } catch (ServerNotActiveException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public @Nullable Set<MD5> get(@Nonnull String fromFile) throws RemoteException {
        try {
            final String clientHost = RemoteServer.getClientHost();
            if (chunkMapByHost.containsKey(clientHost))
                return chunkMapByHost.get(clientHost).get(fromFile);
        } catch (ServerNotActiveException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public @Nonnull Set<String> search(@Nonnull String fromHostsFile, @Nonnull MD5 forChunkHash) throws RemoteException {
        HashSet<String> result = new HashSet<>();
        
        Enumeration<String> clientHosts = chunkMapByHost.keys();
        while (clientHosts.hasMoreElements()) {
            String it = clientHosts.nextElement();
            Set<MD5> set = chunkMapByHost.get(it).get(fromHostsFile);
            if (set != null && set.contains(forChunkHash))
                result.add(it);
        }

        return result;
    }

    public static void main(String args[]) throws Exception {
        ChunkMapService chunkMapService = new TrackerMain();
        Naming.bind("ChunkMapService", chunkMapService);
        System.out.println("ChunkMapService bound");
    }
}
