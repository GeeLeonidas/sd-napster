package br.dev.gee.sdnapster;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Peer {

	public static class Address {
		public final String ip;
		public final int port;

		public Address(String ip, int port) {
			this.ip = ip;
			this.port = port;
		}
	}

	public static String DEFAULT_TCP_HOST = Servidor.DEFAULT_TRACKER_HOST;
	public static int DEFAULT_TCP_PORT = 31337;

	public static Path readPath(Scanner scanner) {
		while (true) {
			System.out.print("Insira diretório do peer: ");
			try {
				Path path = FileSystems.getDefault().getPath(scanner.nextLine());
				if (Files.isDirectory(path))
					return path;
			} catch (Exception ignored) {}
		}
	}

	public static String readOption(Scanner scanner) {
		while (true) {
			System.out.print("Insira opção (JOIN, SEARCH, DOWNLOAD, EXIT): ");
			final String optionString = scanner.nextLine().toUpperCase();
			switch (optionString) {
				case "JOIN":
				case "SEARCH":
				case "DOWNLOAD":
				case "EXIT":
					return optionString;
			}
		}
	}

	public static List<String> listAllFilenames(Path directory) {
		assert Files.isDirectory(directory);
		ArrayList<String> result = new ArrayList<>();
		File[] files = directory.toFile().listFiles();
		files = (files != null) ? files : new File[0];
		for (File file : files) {
			if (file.isFile())
				result.add(file.getName());
		}
		return result;
	}

	public static void main(String[] args) throws NotBoundException {
		final String trackerService = TrackerService.class.getCanonicalName();
		final Scanner scanner = new Scanner(System.in);
		
		final String trackerHost = Servidor.readHost(scanner);
		final int trackerPort = Servidor.readPort(scanner);
		
		try {
			final Registry registry = LocateRegistry.getRegistry(trackerHost, trackerPort);
			final TrackerService tracker = (TrackerService)
					registry.lookup(String.format("//%s:%d/%s", trackerHost, trackerPort, trackerService));
			String option;
			Path peerPath = null;
			List<Address> searchCache = null;
			while (!(option = Peer.readOption(scanner)).equals("EXIT")) {
				switch (option) {
					case "JOIN":
						final String tcpHost = Servidor.readHost(scanner, "TCP", Peer.DEFAULT_TCP_HOST);
						final int tcpPort = Servidor.readPort(scanner, "TCP", Peer.DEFAULT_TCP_PORT);
						peerPath = Peer.readPath(scanner);
						final List<String> filenames = Peer.listAllFilenames(peerPath);
						// TODO: Add TCP thread implementation
						assert tracker.join(filenames, new Address(tcpHost, tcpPort)).equals("JOIN_OK");
						break;
					case "SEARCH":
						if (peerPath == null) {
							System.out.println("Execute uma requisição JOIN antes!");
							continue;
						}
						while (true) {
							System.out.printf("Insira o nome do arquivo (em %s): ", peerPath.toAbsolutePath());
							final String filename = scanner.nextLine();
							final List<String> folder = Peer.listAllFilenames(peerPath);
							if (!folder.contains(filename))
								continue;
							searchCache = tracker.search(filename);
							break;
						}
						break;
					case "DOWNLOAD":
						if (searchCache == null) {
							System.out.println("Execute uma requisição SEARCH antes!");
							continue;
						}
						while (true) {
							final String downloadHost = Servidor.readHost(scanner, "Download", Peer.DEFAULT_TCP_HOST);
							final int downloadPort = Servidor.readPort(scanner, "Download", Peer.DEFAULT_TCP_PORT);
							final Address downloadAddress = new Address(downloadHost, downloadPort);
							if (!searchCache.contains(downloadAddress))
								continue;
							// TODO: Add TCP download implementation
							break;
						}
						break;
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		scanner.close();
	}

}
