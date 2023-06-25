package br.dev.gee.sdnapster;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

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

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Address))
				return false;
			return this.toString().equals(obj.toString());
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
					// Requisição JOIN
					case "JOIN":
						if (peerPath != null) {
							System.out.println("Requisição JOIN já executada!");
							continue;
						}
						// Entrada dos dados
						final InetAddress tcpHost = Servidor.readHost(scanner, "TCP", Peer.DEFAULT_TCP_HOST);
						final int tcpPort = Servidor.readPort(scanner, "TCP", Peer.DEFAULT_TCP_PORT);
						final Path finalPeerPath = Peer.readPath(scanner);
						final List<String> filenames = Peer.listAllFilenames(finalPeerPath);
						// Início da Thread de upload via TCP
						tcpThread = new Thread(() -> {
							try (final ServerSocket socket = new ServerSocket(tcpPort, 50, tcpHost)) {
								while (true) {
									final Socket clientSocket = socket.accept();
									new Thread(() -> {
										try (
												BufferedInputStream in = new BufferedInputStream(clientSocket.getInputStream());
												BufferedOutputStream out = new BufferedOutputStream(clientSocket.getOutputStream())
										) {
											// Buffer de 4 KiB para ler a stream
											byte[] buffer = new byte[4096];
											int bytesRead = in.read(buffer);
											if (bytesRead == -1)
												return;
											// Interpretação do buffer como um array de Strings (especificação do Charset obrigatória para evitar bugs)
											final String[] message = new String(buffer, StandardCharsets.UTF_8).split("\n");
											if (message.length < 2 || !message[0].equals("DOWNLOAD")) // 1º elemento é um header
												return;
											// Uso do conteúdo da mensagem de fato
											Path filePath = FileSystems.getDefault().getPath(finalPeerPath + "/" + message[1]);
											if (!Files.isRegularFile(filePath)) {
												// Retorno em caso de erro
												out.write("DOWNLOAD_ERR\n".getBytes(StandardCharsets.UTF_8));
												return;
											}
											// Retorno em caso de sucesso
											out.write("DOWNLOAD_OK\n".getBytes(StandardCharsets.UTF_8));
											out.flush();
											try (InputStream fileStream = Files.newInputStream(filePath)) {
												while (!clientSocket.isClosed()) {
													bytesRead = fileStream.read(buffer);
													if (bytesRead == -1)
														break;
													// Uso direto do buffer *DEVE* ser acompanhado de `bytesRead` para evitar a leitura de lixo
													out.write(buffer, 0, bytesRead);
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
						// Chamada RMI `TrackerService::join`
						// Se o valor de retorno não for "JOIN_OK", volte ao menu interativo
						if (!tracker.join(filenames, new Address(tcpHost, tcpPort)).equals("JOIN_OK")) {
							tcpThread.interrupt();
							continue;
						}
						peerPath = finalPeerPath;
						tcpAddress = new Address(tcpHost, tcpPort);
						System.out.printf("Sou peer %s:%d com arquivos %s\n", tcpHost.getHostAddress(), tcpPort, String.join(" ", filenames));
						break;
					// Requisição SEARCH
					case "SEARCH":
						if (peerPath == null) {
							System.out.println("Execute uma requisição JOIN antes!");
							continue;
						}
						// Entrada dos dados
						System.out.print("Insira o nome do arquivo a ser procurado na rede: ");
						final String filename = scanner.nextLine();
						// Chamada RMI `TrackerService::search`
						searchCache = tracker.search(filename);
						searchedFile = filename;
						final ArrayList<String> stringSearchCache = new ArrayList<>();
						for (Address address : searchCache)
							stringSearchCache.add(address.toString());
						System.out.printf("peers com arquivo solicitado: %s\n", String.join(" ", stringSearchCache));
						break;
					// Requisição DOWNLOAD
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
							// Entrada dos dados
							final InetAddress downloadHost = Servidor.readHost(scanner, "Download", Peer.DEFAULT_TCP_HOST);
							final int downloadPort = Servidor.readPort(scanner, "Download", Peer.DEFAULT_TCP_PORT);
							final Address downloadAddress = new Address(downloadHost, downloadPort);
							if (!searchCache.contains(downloadAddress))
								continue;
							final String finalSearchedFile = searchedFile;
							final Path finalDownloadFolder = peerPath;
							final Path downloadPath = FileSystems.getDefault().getPath(finalDownloadFolder + "/" + finalSearchedFile);
							final Address finalTcpAddress = tcpAddress;
							// Início da Thread de download via TCP
							new Thread(() -> {
								try (
										Socket serverSocket = new Socket(downloadHost, downloadPort);
										BufferedInputStream in = new BufferedInputStream(serverSocket.getInputStream());
										BufferedOutputStream out = new BufferedOutputStream(serverSocket.getOutputStream())
								) {
									// Envio formatado dos dados da requisição
									out.write(String.format("DOWNLOAD\n%s\n", finalSearchedFile).getBytes(StandardCharsets.UTF_8));
									out.flush();
									byte[] buffer = new byte[4096];
									int bytesRead = in.read(buffer);
									if (bytesRead == -1)
										return;
									// Interpretação do buffer como um array de Strings (especificação do Charset obrigatória para evitar bugs)
									final String[] message = new String(buffer, StandardCharsets.UTF_8).split("\n");
									if (message.length < 1 || !message[0].equals("DOWNLOAD_OK")) // Uso apenas do header
										return;
									try (OutputStream fileStream = Files.newOutputStream(downloadPath)) {
										while (!serverSocket.isClosed()) {
											bytesRead = in.read(buffer);
											if (bytesRead == -1)
												break;
											// Uso direto do buffer *DEVE* ser acompanhado de `bytesRead` para evitar a leitura de lixo
											fileStream.write(buffer, 0, bytesRead);
										}
									}
									if (!serverSocket.isClosed()) {
										// Chamada RMI `TrackerService::update`
										// Apenas mostre a mensagem em caso de sucesso (retorno igual a "UPDATE_OK")
										if (tracker.update(finalSearchedFile, finalTcpAddress).equals("UPDATE_OK"))
											System.out.printf("\nArquivo %s baixado com sucesso na pasta %s\n", finalSearchedFile, finalDownloadFolder);
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
		System.exit(0);
	}

}
