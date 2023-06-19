package br.dev.gee.sdnapster;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface TrackerService extends Remote {
	
	/**
 	 * Endereço do peer é inserido na rede junto de uma lista com o nome de seus arquivos
 	 * @param	filenames	Nome dos arquivos (sem o caminho) disponíveis na máquina do peer
	 * @param 	addressInfo	Informação do endereço onde os arquivos serão disponibilizados
	 * @return 				"JOIN_OK" ou "JOIN_ERR"
	 */
	String join(List<String> filenames, Peer.Address addressInfo) throws RemoteException;
	
	/**
	 * Procura na rede todos os peers que possuem dado nome do arquivo como parâmetro
	 * @param	filename	Nome do arquivo (sem o caminho) a ser procurado na rede
	 * @return				Lista com o endereço dos peers que possuem o arquivo
	 */
	List<Peer.Address> search(String filename) throws RemoteException;
	
	/**
	 * Dado o nome do arquivo baixado pelo peer, adiciona-o à lista dos disponíveis na rede
	 * @param	filename	Nome do arquivo (sem o caminho) a ser adicionado como disponível
	 * @param 	addressInfo	Informação do endereço onde os arquivos serão disponibilizados
	 * @return 				"UPDATE_OK" ou "UPDATE_ERR"
	 */
	String update(String filename, Peer.Address addressInfo) throws RemoteException;

}
