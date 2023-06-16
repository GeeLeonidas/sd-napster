package br.dev.gee.sdnapster;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

public class Servidor {

	public static String DEFAULT_HOST = "127.0.0.1";
	public static int DEFAULT_PORT = 1099;
	
	public static String readHost(Scanner scanner) {
		while (true) {
			System.out.print("Insira host do tracker (IPv4, padrão %s): ".formatted(Servidor.DEFAULT_HOST));
			String hostString = scanner.nextLine();
			if (hostString.isEmpty())
				return Servidor.DEFAULT_HOST;
			// Padrão IPv4 (Fonte: https://ihateregex.io/expr/ip/)
			if (hostString.matches("^(\\b25[0-5]|\\b2[0-4][0-9]|\\b[01]?[0-9][0-9]?)(\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}$"))
				return hostString;
		}
	}
	
	public static int readPort(Scanner scanner) {
		while (true) {
			System.out.print("Insira porta do tracker (padrão %d): ".formatted(Servidor.DEFAULT_PORT));
			String portString = scanner.nextLine();
			if (portString.isEmpty())
				return Servidor.DEFAULT_PORT;
			// Portas válidas (Fonte: https://ihateregex.io/expr/port/)
			if (portString.matches("^((6553[0-5])|(655[0-2][0-9])|(65[0-4][0-9]{2})|(6[0-4][0-9]{3})|([1-5][0-9]{4})|([0-5]{0,5})|([0-9]{1,4}))$"))
				return Integer.parseInt(portString);
		}
	}
	
	private static Registry registry;
	
	public static void main(String args[]) {
		final String service = TrackerService.class.getCanonicalName();
		final Scanner scanner = new Scanner(System.in);
		
		final String host = Servidor.readHost(scanner);
		final int port = Servidor.readPort(scanner);
		
		final TrackerServiceImpl trackerImpl = new TrackerServiceImpl();
		try {
			registry = LocateRegistry.createRegistry(DEFAULT_PORT);
			final TrackerService stub =
					(TrackerService) UnicastRemoteObject.exportObject(trackerImpl, DEFAULT_PORT);
			registry.rebind("//%s:%d/%s".formatted(DEFAULT_HOST, DEFAULT_PORT, service), stub);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		scanner.close();
	}

}
