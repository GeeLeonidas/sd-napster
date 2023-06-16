package br.dev.gee.sdnapster;

import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Peer {

	public static Path readPath(Scanner scanner) {
		while (true) {
			System.out.print("Insira diretório do peer: ");
			try {
				Path path = Path.of(scanner.nextLine());
				if (Files.isDirectory(path))
					return path;
			} catch (Exception e) {
				continue;
			}
		}
	}
	
	public static void main(String args[]) throws NotBoundException {
		final String trackerService = TrackerService.class.getCanonicalName();
		final Scanner scanner = new Scanner(System.in);
		
		final String trackerHost = Servidor.readHost(scanner);
		final int trackerPort = Servidor.readPort(scanner);
		final Path peerPath = Peer.readPath(scanner);
		
		try {
			final Registry registry = LocateRegistry.getRegistry(trackerHost, trackerPort);
			final TrackerService tracker = (TrackerService)
					registry.lookup("//%s:%d/%s".formatted(trackerHost, trackerPort, trackerService));
			
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		scanner.close();
	}

}
