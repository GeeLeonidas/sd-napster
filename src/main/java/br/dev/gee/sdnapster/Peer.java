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

	public static class Address implements Serializable {
		public final InetAddress ip;
		public final int port;

		public Address(InetAddress ip, int port) {
			this.ip = ip;
			this.port = port;
		}

		@Override
		public String toString() {
			return String.format("%s:%d", this.ip.getHostAddress(), this.port);
		}

		@Override
		public int hashCode() {
			return this.toString().hashCode();
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
		if (!Files.isDirectory(directory))
			throw new RuntimeException(String.format("%s is not a directory!", directory));
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
						tracker.join(filenames, new Address(tcpHost, tcpPort)); // TODO: Handle return code
						peerPath = finalPeerPath;
						tcpAddress = new Address(tcpHost, tcpPort);
						System.out.printf("Sou peer %s:%d com arquivos %s\n", tcpHost.getHostAddress(), tcpPort, String.join(" ", filenames));
						break;
					case "SEARCH":
						if (peerPath == null) {
							System.out.println("Execute uma requisição JOIN antes!");
							continue;
						}
						System.out.print("Insira o nome do arquivo a ser procurado na rede: ");
						final String filename = scanner.nextLine();
						searchCache = tracker.search(filename);
						searchedFile = filename;
						final ArrayList<String> stringSearchCache = new ArrayList<>();
						for (Address address : searchCache)
							stringSearchCache.add(address.toString());
						System.out.printf("peers com arquivo solicitado: %s\n", String.join(" ", stringSearchCache));
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
							final Path finalDownloadFolder = peerPath;
							final Path downloadPath = FileSystems.getDefault().getPath(finalDownloadFolder + finalSearchedFile);
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
									if (!serverSocket.isClosed()) {
										tracker.update(finalSearchedFile, finalTcpAddress); // TODO: Handle return code
										System.out.printf("Arquivo %s baixado com sucesso na pasta %s\n", finalSearchedFile, finalDownloadFolder);
									}
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
