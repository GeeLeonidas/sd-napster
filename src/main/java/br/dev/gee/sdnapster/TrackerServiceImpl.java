package br.dev.gee.sdnapster;

import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class TrackerServiceImpl implements TrackerService {

	private final HashMap<String, HashSet<String>> hostToFileMap;
	private final HashMap<String, Peer.Address> hostToTcpAddress;
	
	public TrackerServiceImpl() {
		this.hostToFileMap = new HashMap<>();
		this.hostToTcpAddress = new HashMap<>();
	}

	@Override
	public String join(List<String> filenames, Peer.Address addressInfo) throws RemoteException {
		try {
			final String clientHost = UnicastRemoteObject.getClientHost();
			synchronized (hostToFileMap) {
				hostToFileMap.put(clientHost, new HashSet<>(filenames));
				synchronized (hostToTcpAddress) {
					hostToTcpAddress.put(clientHost, addressInfo);
				}
			}
		} catch (ServerNotActiveException e) {
			e.printStackTrace();
			return "JOIN_ERR";
		}

		System.out.printf("Peer %s adicionado com arquivos %s\n", addressInfo, String.join(" ", filenames));
		return "JOIN_OK";
	}

	@Override
	public List<Peer.Address> search(String filename) throws RemoteException {
		ArrayList<Peer.Address> result = new ArrayList<>();
		
		synchronized (hostToFileMap) {
			hostToFileMap.forEach((clientHost, fileSet) -> {
				if (fileSet.contains(filename)) {
					synchronized (hostToTcpAddress) {
						result.add(hostToTcpAddress.get(clientHost));
					}
				}
			});
		}

		try {
			final String clientHost = UnicastRemoteObject.getClientHost();
			synchronized (hostToTcpAddress) {
				System.out.printf("Peer %s solicitou arquivo %s\n", hostToTcpAddress.get(clientHost), filename);
			}
		} catch (ServerNotActiveException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
		return result;
	}

	@Override
	public String update(String filename, Peer.Address addressInfo) throws RemoteException {
		try {
			final String clientHost = UnicastRemoteObject.getClientHost();
			synchronized (hostToFileMap) {
				if (!hostToFileMap.containsKey(clientHost))
					return "UPDATE_ERR";
				HashSet<String> newFileSet =
						hostToFileMap.getOrDefault(clientHost, new HashSet<>());
				newFileSet.add(filename);
				hostToFileMap.put(clientHost, newFileSet);
			}
		} catch (ServerNotActiveException e) {
			e.printStackTrace();
			return "UPDATE_ERR";
		}
		return "UPDATE_OK";
	}

}
