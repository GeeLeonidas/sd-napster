package br.dev.gee.sdnapster;

import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class TrackerServiceImpl implements TrackerService {

	private final HashMap<Peer.Address, HashSet<String>> hostToFileMap;
	
	public TrackerServiceImpl() {
		this.hostToFileMap = new HashMap<>();
	}

	@Override
	public String join(List<String> filenames, Peer.Address addressInfo) throws RemoteException {
		try {
			final String senderHost = UnicastRemoteObject.getClientHost();
			if (!addressInfo.ip.getHostAddress().equals(senderHost))
				return "JOIN_ERR";
		} catch (ServerNotActiveException e) {
			e.printStackTrace();
			return "JOIN_ERR";
		}
		synchronized (hostToFileMap) {
			hostToFileMap.put(addressInfo, new HashSet<>(filenames));
		}
		System.out.printf("Peer %s adicionado com arquivos %s\n", addressInfo, String.join(" ", filenames));

		return "JOIN_OK";
	}

	@Override
	public List<Peer.Address> search(String filename) throws RemoteException {
		ArrayList<Peer.Address> result = new ArrayList<>();

		try {
			final String senderHost = UnicastRemoteObject.getClientHost();
			AtomicReference<Peer.Address> senderAddress = new AtomicReference<>(); // Guesswork
			synchronized (hostToFileMap) {
				hostToFileMap.forEach((clientAddress, fileSet) -> {
					if (fileSet.contains(filename))
						result.add(clientAddress);
					if (clientAddress.ip.getHostAddress().equals(senderHost))
						senderAddress.set(clientAddress);
				});
			}
			System.out.printf("Peer %s solicitou arquivo %s\n", senderAddress.get(), filename);
		} catch (ServerNotActiveException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}

		return result;
	}

	@Override
	public String update(String filename, Peer.Address addressInfo) throws RemoteException {
		try {
			final String senderHost = UnicastRemoteObject.getClientHost();
			if (!addressInfo.ip.getHostAddress().equals(senderHost))
				return "UPDATE_ERR";
		} catch (ServerNotActiveException e) {
			e.printStackTrace();
			return "UPDATE_ERR";
		}
		synchronized (hostToFileMap) {
			if (!hostToFileMap.containsKey(addressInfo))
				return "UPDATE_ERR";
			HashSet<String> newFileSet =
					hostToFileMap.getOrDefault(addressInfo, new HashSet<>());
			newFileSet.add(filename);
			hostToFileMap.put(addressInfo, newFileSet);
		}

		return "UPDATE_OK";
	}

}
