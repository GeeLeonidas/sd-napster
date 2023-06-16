package br.dev.gee.sdnapster;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface TrackerService extends Remote {
	
	/**
 	 * Endereço do peer é inserido na rede junto de uma lista com o nome de seus arquivos
 	 * @param	filenames	Nome dos arquivos (sem o caminho) disponíveis na máquina do peer
	 * @return 					"JOIN_OK" ou "JOIN_ERR"
	 */
	public String join(List<String> filenames) throws RemoteException;
	
	/**
	 * Procura na rede todos os peers que possuem dado nome do arquivo como parâmetro
	 * @param	filename	Nome do arquivo (sem o caminho) a ser procurado na rede
	 * @return				Lista com o endereço dos peers que possuem o arquivo
	 */
	public List<String> search(String filename) throws RemoteException;
	
	/**
	 * Dado o nome do arquivo baixado pelo peer, adiciona-o à lista dos disponíveis na rede
	 * @param	filename	Nome do arquivo (sem o caminho) a ser adicionado como disponível
	 * @return 				"UPDATE_OK" ou "UPDATE_ERR"
	 */
	public String update(String filename) throws RemoteException;

}
