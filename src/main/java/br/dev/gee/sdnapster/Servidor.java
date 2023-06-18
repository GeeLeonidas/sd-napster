package br.dev.gee.sdnapster;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

public class Servidor {

	public static String DEFAULT_TRACKER_HOST = "127.0.0.1";
	public static int DEFAULT_TRACKER_PORT = 1099;

	public static String readHost(Scanner scanner) {
		return Servidor.readHost(scanner, "Tracker", Servidor.DEFAULT_TRACKER_HOST);
	}

	public static String readHost(Scanner scanner, String hostNickname, String defaultHost) {
		while (true) {
			System.out.printf("Insira host do %s (IPv4, padrão %s): ", hostNickname, Servidor.DEFAULT_TRACKER_HOST);
			String hostString = scanner.nextLine();
			if (hostString.isEmpty())
				return defaultHost;
			// Padrão IPv4 (Fonte: https://ihateregex.io/expr/ip/)
			if (hostString.matches("^(\\b25[0-5]|\\b2[0-4][0-9]|\\b[01]?[0-9][0-9]?)(\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}$"))
				return hostString;
		}
	}

	public static int readPort(Scanner scanner) {
		return Servidor.readPort(scanner, "Tracker", Servidor.DEFAULT_TRACKER_PORT);
	}

	public static int readPort(Scanner scanner, String hostNickname, int defaultPort) {
		while (true) {
			System.out.printf("Insira porta do %s (padrão %d): ", hostNickname, Servidor.DEFAULT_TRACKER_PORT);
			String portString = scanner.nextLine();
			if (portString.isEmpty())
				return defaultPort;
			// Portas válidas (Fonte: https://ihateregex.io/expr/port/)
			if (portString.matches("^((6553[0-5])|(655[0-2][0-9])|(65[0-4][0-9]{2})|(6[0-4][0-9]{3})|([1-5][0-9]{4})|([0-5]{0,5})|([0-9]{1,4}))$"))
				return Integer.parseInt(portString);
		}
	}

	public static void main(String[] args) {
		final String service = TrackerService.class.getCanonicalName();
		final Scanner scanner = new Scanner(System.in);
		
		final String host = Servidor.readHost(scanner);
		final int port = Servidor.readPort(scanner);
		
		final TrackerServiceImpl trackerImpl = new TrackerServiceImpl();
		try {
			final Registry registry = LocateRegistry.createRegistry(DEFAULT_TRACKER_PORT);
			final TrackerService stub =
					(TrackerService) UnicastRemoteObject.exportObject(trackerImpl, DEFAULT_TRACKER_PORT);
			registry.rebind(String.format("//%s:%d/%s", DEFAULT_TRACKER_HOST, DEFAULT_TRACKER_PORT, service), stub);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		scanner.close();
	}

}
