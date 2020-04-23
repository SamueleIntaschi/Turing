import java.io.IOException;
import java.net.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Set;

/**
 * Classe che implementa il thread listener del server, che distribuisce il lavoro tra i thread worker
 */

public class Server {

    /*
     * Indice porte usate dal programma
     * port 5000 : register RMI
     * port 5001 : richieste varie
     * port 5002 -> 5999 : richieste di invito
     * port 10000: chat multicast tra client (server non coinvolto)
     */

    /**
     * Metodo main
     *
     * @param args null
     */
    public static void main(String[] args) {

        //Creazione di un'istanza del server
        ServerImpl server = new ServerImpl();
        Task task = null;

        /*----- Preparazione server remoto per registrazione utenti ----- */

        try {
            //Esportazione dell'Oggetto
            ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(server, 0);
            int port = 5000;
            LocateRegistry.createRegistry(port);
            Registry r = LocateRegistry.getRegistry(port);
            //Pubblicazione dello stub nel registry
            r.rebind("SERVER", stub);
            System.out.println("Server pronto");
        } catch (RemoteException e) {
            System.out.println("Errore di comunicazione " + e.toString());
        }

        /*----- Creazione cartella documenti, se non già esistente -----*/

        Path path = Paths.get("Documents");
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /*----- Creazione Socket listen -----*/

        //Inizializzo selector
        Selector selector = null;
        ServerSocketChannel serverSocketChannel = null;
        ServerSocket ss = null;
        try {
            System.out.println("Inizio ricezione dati");
            //Apro il canale
            serverSocketChannel = ServerSocketChannel.open();
            //Prendo il socket listener
            ss = serverSocketChannel.socket();
            //Faccio la bind
            ss.bind(new InetSocketAddress(5001));
            serverSocketChannel.configureBlocking(false);
            //Apro il selettore
            selector = Selector.open();
            //Registro le chiavi con accept come operazione
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        /*----- Inizio ciclo server -----*/

        while (true) {

            //Faccio la select
            try {
                selector.select();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            //Insieme di chiavi pronte che rappresentano i canali da cui si ascolta
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            //Iteratore sulle chiavi
            Iterator<SelectionKey> iterator = readyKeys.iterator();
            //Scorro le chiavi
            while (iterator.hasNext()) {
                //Prendo una chiave
                SelectionKey key = iterator.next();
                //Rimuovo la chiave dal SelectedSet
                iterator.remove();
                try {
                    //Se la chiave è pronta per l' accept
                    if (key.isAcceptable()) {
                        //Prendo il canale rappresentato dalla chiave
                        ServerSocketChannel s = (ServerSocketChannel) key.channel();
                        //Accetto la connessione con il client
                        SocketChannel c = s.accept();
                        System.out.println("Accepted connection with " + c);
                        //Setto come non bloccante il socket ricevuto
                        c.configureBlocking(false);
                        //Registro la chiave
                        SelectionKey key2 = c.register(selector, SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        //Creo un task da affidare a un thread
                        task = new Task(key, server);
                        //Elimino la chiave dall' insieme
                        key.cancel();
                        //Faccio eseguire il task ad un thread
                        server.executeTask(task);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

        }
    }

}
