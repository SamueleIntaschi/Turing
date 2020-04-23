import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Thread usato per ascoltare se arrivano inviti per l' utente per collaborare alla modifica di un documento
 */

public class NotificationListener extends Thread {

    private String user = null;
    private int port;
    private InetAddress ind;
    private ClientImpl client;

    /**
     * Costruttore
     *
     * @param client il client che utilizza il servizio
     * @param user l' utente che sta usando il client
     * @param ind l' indirizzo usato per ricevere le notifiche di invito
     * @param port la porta usata per ricevere le notifiche di invito
     */
    public NotificationListener(ClientImpl client, String user, InetAddress ind, int port) {

        this.user = user;
        this.ind = ind;
        this.port = port;
        this.client = client;

    }

    /**
     * Metodo eseguito all' attivazione del thread
     */
    public void run() {

        try {
            //Apro connessione con il server, che in questo caso è cliente del servizio,
            //inviando lui le richieste di notifica
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(ind, port));
            serverSocketChannel.configureBlocking(false);
            while(this.isInterrupted() == false) {
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel != null) {
                    //Ricevo dimensione del nome del documento
                    ByteBuffer input = ByteBuffer.allocate(4);
                    //Il Buffer sarà un array di interi con uno solo
                    IntBuffer view = input.asIntBuffer();
                    //Leggo la risposta
                    read(socketChannel, input, 4);
                    int size = view.get();
                    //Pulisco il buffer
                    input.clear();
                    //Ricevo il messaggio
                    input = ByteBuffer.allocate(size);
                    socketChannel.read(input);
                    String tmp = null;
                    try {
                        tmp = new String(input.array(), 0, size, "ASCII");
                    } catch (UnsupportedEncodingException ex) {
                        ex.printStackTrace();
                    }
                    client.createNotification(tmp);
                }
            }
            serverSocketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Metodo per leggere dal buffer con il server un numero specificato di bytes
     *
     * @param client il socket usato dal client
     * @param buf il buffer in cui scrivere i bytes letti
     * @param dim il numero di bytes da leggere
     */
    private static void read(SocketChannel client, ByteBuffer buf,int dim) {

        try {
            while (dim > 0) {
                int r = -1;
                r = client.read(buf);
                if (r != -1) dim = dim - r;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
