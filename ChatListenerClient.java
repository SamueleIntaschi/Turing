import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Classe che rappresenta il thread che si occupa di ascoltare se arrivano messaggi sulla chat mentre si collabora
 * alla modifica di un documento
 */

public class ChatListenerClient extends Thread {

    //Socket multicast
    private MulticastSocket ms;
    //Porta usata
    private int port;
    //Indirizzo multicast del gruppo
    private InetAddress group;
    //Pannello inserito nella finestra del client per mostrare la chat
    private ChatPanel c;
    //Messaggi salvati
    private ArrayList<String> msgs;
    //Lock per l' accesso alla history dei messaggi
    private final Lock lockmsg;

    /**
     * Costruttore
     *
     * @param p porta su cui ascoltare
     * @param ind indirizzo di multicast da cui ricevere o inviare
     * @param c il pannello grafico per la chat
     */
    public ChatListenerClient(int p, String ind, ChatPanel c) {

        this.port = p;
        this.lockmsg = new ReentrantLock();
        try {
            group = InetAddress.getByName(ind);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.c = c;
        this.msgs = new ArrayList<String>();

    }

    /**
     * Metodo eseguito all' attivazione del thread
     */
    public void run() {

        //Stabilisco connessione legando il socket al gruppo di multicast
        DatagramPacket dp = null;
        try {
            //Creo il socket multicast sulla porta ricevuta
            ms = new MulticastSocket(port);
            //Setto un timeout per controllare ogni 2 secondi se il thread è stato interrotto
            ms.setSoTimeout(2000);
            //Setto il TTL a 1 per non far uscire i messaggi dalla rete locale
            ms.setOption(StandardSocketOptions.IP_MULTICAST_TTL,1);
            //Mi iscrivo al gruppo
            ms.joinGroup(group);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Ciclo finchè non è interrotto il thread poi chiudo
        while (this.isInterrupted() == false) {
            try {
                try {
                    //Attendo un messaggio, controllando ogni due secondi che il thread non sia stato interrotto ed eventualmente lo chiudo
                    while (true) {
                        //Creo un buffer
                        byte[] data = new byte[64];
                        //Creo il datagram packet
                        dp = new DatagramPacket(data, data.length);
                        ms.receive(dp);
                        //Finita l' attesa controllo se il thread è stato interrotto nel frattempo
                        if (this.isInterrupted() == false) {
                            String s = new String(dp.getData());
                            //Mostro il testo nella finestra
                            c.showText(s);

                            //Lo salvo in memoria, in mutua esclusione
                            lockmsg.lock();
                            msgs.add(s);
                            lockmsg.unlock();

                        } else {
                            break;
                        }
                    }
                } catch(SocketTimeoutException e) {
                    //Scade il timeout per ricevere il messaggio, non faccio niente
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //Lascio il gruppo
        try {
            ms.leaveGroup(group);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Chiudo il socket
        ms.close();

    }

    /**
     * Metodo che restituisce la lista dei messaggi ricevuti fino a quel momento
     *
     * @return la lista dei messaggi finora ricevuti
     */
    public List<String> getMsgs() {

        //Recupero e svuoto lista dei messaggi, in mutua esclusione perchè eseguito da un altro thread
        lockmsg.lock();
        int i = 0;
        List<String> tmp = new ArrayList<String>();
        int size = msgs.size();
        for (i = 0; i<size; i++) {
            //La remove decrementa l' indice agli elementi rimasti
            String t = msgs.remove(0);
            tmp.add(t);
        }
        lockmsg.unlock();
        return tmp;

    }

    /**
     * Metodo per inviare un messaggio in chat
     *
     * @param msg il messaggio da inviare
     */
    public void sendMessage(String msg) {

        try {
            //Genero buffer
            byte[] data = new byte[64];
            //Creo il pacchetto
            data = msg.getBytes();
            DatagramPacket dp = new DatagramPacket(data, data.length, group, port);
            //Lo invio
            ms.send(dp);
            //Rimuovo il messaggio inviato dal campo in grafica perchè ne venga scritto uno nuovo
            c.removeMsg();
        } catch (SocketException ex) {
            ex.printStackTrace();
            System.out.println("Send message error");
            //Quando il chatlistener scrive su terminale poi deve occuparsi di stampare di nuovo il carattere di immissione $
            System.out.printf("$ ");
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("Send message error");
            System.out.printf("$ ");
        }

    }

    /**
     * Metodo per cancellare i messaggi presenti in grafica
     */
    public void cancelText() {

        //Rimuovo il testo dall' area adibita
        c.removeText();

    }
}
