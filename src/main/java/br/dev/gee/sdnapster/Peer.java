package br.dev.gee.sdnapster;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
		public final InetAddress ip;
		public final int port;

		public Address(InetAddress ip, int port) {
			this.ip = ip;
			this.port = port;
		}
	}

	public static InetAddress DEFAULT_TCP_HOST = Servidor.DEFAULT_TRACKER_HOST;
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
		
		final InetAddress trackerHost = Servidor.readHost(scanner);
		final int trackerPort = Servidor.readPort(scanner);
		
		try {
			final Registry registry = LocateRegistry.getRegistry(trackerHost.getHostAddress(), trackerPort);
			final TrackerService tracker = (TrackerService)
					registry.lookup(String.format("//%s:%d/%s", trackerHost.getHostAddress(), trackerPort, trackerService));
			String option;
			Path peerPath = null;
			List<Address> searchCache = null;
			String searchedFile = null;
			Thread tcpThread;
			Address tcpAddress = null;
			while (!(option = Peer.readOption(scanner)).equals("EXIT")) {
				switch (option) {
					case "JOIN":
						if (peerPath != null) {
							System.out.println("Requisição JOIN já executada!");
							continue;
						}
						final InetAddress tcpHost = Servidor.readHost(scanner, "TCP", Peer.DEFAULT_TCP_HOST);
						final int tcpPort = Servidor.readPort(scanner, "TCP", Peer.DEFAULT_TCP_PORT);
						final Path finalPeerPath = Peer.readPath(scanner);
						final List<String> filenames = Peer.listAllFilenames(finalPeerPath);
						tcpThread = new Thread(() -> {
							try (final ServerSocket socket = new ServerSocket(tcpPort, 50, tcpHost)) {
								while (true) {
									final Socket clientSocket = socket.accept();
									new Thread(() -> {
										try (
												BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
												BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
										) {
											String input = in.readLine();
											if (!input.equals("DOWNLOAD"))
												return;
											input = in.readLine();
											Path filePath = FileSystems.getDefault().getPath(finalPeerPath + input);
											if (!Files.isRegularFile(filePath)) {
												out.write("DOWNLOAD_ERR\n");
												return;
											}
											out.write("DOWNLOAD_OK\n");
											out.flush();
											try (
													BufferedReader fileReader = Files.newBufferedReader(filePath)
											) {
												while (!clientSocket.isClosed()) {
													char[] cbuf = new char[4096];
													int charNum = fileReader.read(cbuf);
													if (charNum == -1)
														break;
													out.write(cbuf);
													if (charNum < 4096)
														break;
												}
											}
										} catch (IOException e) {
											throw new RuntimeException(e);
										}
									}).start();
								}
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						});
						tcpThread.start();
						assert tracker.join(filenames, new Address(tcpHost, tcpPort)).equals("JOIN_OK");
						peerPath = finalPeerPath;
						tcpAddress = new Address(tcpHost, tcpPort);
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
							searchedFile = filename;
							break;
						}
						break;
					case "DOWNLOAD":
						if (searchCache == null) {
							System.out.println("Execute uma requisição SEARCH antes!");
							continue;
						}
						if (searchCache.isEmpty()) {
							System.out.printf("Nenhum endereço disponível para `%s`!\n", searchedFile);
							continue;
						}
						while (true) {
							final InetAddress downloadHost = Servidor.readHost(scanner, "Download", Peer.DEFAULT_TCP_HOST);
							final int downloadPort = Servidor.readPort(scanner, "Download", Peer.DEFAULT_TCP_PORT);
							final Address downloadAddress = new Address(downloadHost, downloadPort);
							if (!searchCache.contains(downloadAddress))
								continue;
							final String finalSearchedFile = searchedFile;
							final Path downloadPath = FileSystems.getDefault().getPath(peerPath + finalSearchedFile);
							final Address finalTcpAddress = tcpAddress;
							new Thread(() -> {
								try (
										Socket serverSocket = new Socket(downloadHost, downloadPort);
										BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
										BufferedWriter out = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream()))
								) {
									out.write(String.format("DOWNLOAD\n%s\n", finalSearchedFile));
									out.flush();
									String input = in.readLine();
									if (!input.equals("DOWNLOAD_OK"))
										return;
									try (
											BufferedWriter fileWriter = Files.newBufferedWriter(downloadPath)
									) {
										while (!serverSocket.isClosed()) {
											char[] cbuf = new char[4096];
											int charNum = in.read(cbuf);
											if (charNum == -1)
												break;
											fileWriter.write(cbuf);
											if (charNum < 4096)
												break;
										}
									}
									if (!serverSocket.isClosed())
										tracker.update(finalSearchedFile, finalTcpAddress);
								} catch (IOException e) {
									throw new RuntimeException(e);
								}
							}).start();
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
