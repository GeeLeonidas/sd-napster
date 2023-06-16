package br.dev.gee.sdnapster;

import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class TrackerServiceImpl implements TrackerService {

	private HashMap<String, HashSet<String>> hostToFileMap;
	
	public TrackerServiceImpl() {
		this.hostToFileMap = new HashMap<>();
	}

	@Override
	public String join(List<String> filenames) throws RemoteException {
		try {
			final String clientHost = UnicastRemoteObject.getClientHost();
			synchronized (hostToFileMap) {
				hostToFileMap.put(clientHost, new HashSet<>(filenames));
			}
		} catch (ServerNotActiveException e) {
			e.printStackTrace();
			return "JOIN_ERR";
		}
		return "JOIN_OK";
	}

	@Override
	public List<String> search(String filename) throws RemoteException {
		ArrayList<String> result = new ArrayList<>();
		
		synchronized (hostToFileMap) {
			hostToFileMap.forEach((clientHost, fileSet) -> {
				if (fileSet.contains(filename))
					result.add(clientHost);
			});
		}
		
		return result;
	}

	@Override
	public String update(String filename) throws RemoteException {
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
