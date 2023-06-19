package br.dev.gee.sdnapster;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

public class Servidor {

	public static InetAddress DEFAULT_TRACKER_HOST;
	static {
		try {
			DEFAULT_TRACKER_HOST = InetAddress.getByName("localhost");
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	public static int DEFAULT_TRACKER_PORT = 1099;

	public static InetAddress readHost(Scanner scanner) {
		return Servidor.readHost(scanner, "Tracker", Servidor.DEFAULT_TRACKER_HOST);
	}

	public static InetAddress readHost(Scanner scanner, String hostNickname, InetAddress defaultHost) {
		while (true) {
			System.out.printf("Insira host do %s (padrão %s): ", hostNickname, Servidor.DEFAULT_TRACKER_HOST);
			String hostString = scanner.nextLine();
			if (hostString.isEmpty())
				return defaultHost;
			try {
				return InetAddress.getByName(hostString);
			} catch (UnknownHostException ignored) {}
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
		
		final InetAddress host = Servidor.readHost(scanner);
		final int port = Servidor.readPort(scanner);
		
		final TrackerServiceImpl trackerImpl = new TrackerServiceImpl();
		try {
			final Registry registry = LocateRegistry.createRegistry(port);
			final TrackerService stub =
					(TrackerService) UnicastRemoteObject.exportObject(trackerImpl, port);
			registry.rebind(String.format("//%s:%d/%s", host.getHostAddress(), port, service), stub);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		scanner.close();
	}

}
