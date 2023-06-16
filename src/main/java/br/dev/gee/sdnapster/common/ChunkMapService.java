package br.dev.gee.sdnapster.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ChunkMapService extends Remote {
    public void put(@Nonnull String fromFile, @Nonnull Set<MD5> chunkHashes) throws RemoteException;
    public @Nullable Set<MD5> get(@Nonnull String fromFile) throws RemoteException;
    public @Nonnull Set<String> search(@Nonnull String fromHostsFile, @Nonnull MD5 forChunkHash) throws RemoteException;
}
