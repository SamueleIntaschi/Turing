import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.StringTokenizer;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

/**
 * Classe che implementa il thread worker che eleabora la richiesta di un client
 */

public class Task implements Runnable {

    private SelectionKey key;
    private ServerImpl server;

    /**
     * Costruttore
     *
     * @param k la chiave data dal thread listener del server
     * @param server l' oggetto rappresentante il server
     */
    public Task(SelectionKey k, ServerImpl server) {

        this.server = server;
        this.key = k;

    }

    /**
     * Metodo eseguito dal thread all' attivazione
     */
    @Override
    public void run() {

        SocketChannel client = (SocketChannel) key.channel();
        try {
            client.configureBlocking(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("There is a new request from " + client);
        String request = null;
        ByteBuffer output = null;
        ByteBuffer input = null;
        String user = null;
        String sender = null;
        String psw = null;
        Command cmd = null;
        String docname = null;
        Section sect = null;
        JSONParser parser = new JSONParser();
        int sec = 0;
        boolean res;
        try {

            /* --- RICEZIONE RICHIESTA --- */

            //Ricevo lunghezza della richiesta
            input = ByteBuffer.allocate(4);
            //Vedo il buffer come un array di interi
            IntBuffer view = input.asIntBuffer();
            //Provo a leggere dal canale la dimensione della stringa che rappresenta la richiesta
            read(client, input, 4);
            int dim = view.get();
            input = ByteBuffer.allocate(dim);
            read(client, input, dim);
            try {
                request = new String(input.array(), 0, dim, "ASCII");
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }
            //Pulisco il buffer dopo aver preso l' informazione
            input.clear();
            //Alloco il buffer che conterrà la risposta
            output = ByteBuffer.allocate(4);
            //Elimino gli spazi in fondo dalla stringa ricevuta
            request = stringCleaner(request);
            //Ricavo oggetto Json dalla stringa
            Object obj = parser.parse(request);
            JSONObject jcmd = (JSONObject) obj;
            //Ricavo operazione da eseguire
            String opt = (String) jcmd.get("Command");
            sender = (String) jcmd.get("Sender");
            //A questo punto elaboro le richieste


            /* --- ELABORAZIONE RICHIESTA --- */

            if (opt.equals("login") == true) {

                user = (String) jcmd.get("Arg1");
                psw = (String) jcmd.get("Arg2");
                System.out.println(user + ": Login");
                int r = server.login(user, psw);
                if (r == 1) {
                    System.out.println(sender + ": Login succesfully");
                    //Metto la risposta, cioè la conferma che è andato tutto a buon fine, nel buffer
                    response(output, client, 1);
                    //Reperisco numero di porta
                    int port = server.getPort(user);
                    //Invio numero di porta da usare per ricevere messaggi
                    response(output, client, port);
                }
                else if (r == 2) {
                    System.out.println(user + ": Login succesfully and there are notifications");
                    response(output, client, 2);
                    //Recupero lista dei documenti per i quali l' utente ha ricevuto un invito
                    String notifylist = server.getNList(user);
                    //Ricavo la dimensione del file
                    int size = notifylist.length();
                    //Metto la dimensione in un buffer e alloco il buffer che conterrà la dimensione della lista
                    output = ByteBuffer.allocate(4);
                    output.putInt(size);
                    output.flip();
                    write(client, output, 4);
                    output.clear();
                    //Invio lista
                    output = ByteBuffer.wrap(notifylist.getBytes());
                    write(client, output, notifylist.getBytes().length);
                    output.flip();
                    output.clear();
                    int port = server.getPort(user);
                    //Invio numero di porta da usare per ricevere messaggi
                    response(output, client, port);
                }
                else if (r == -1) {
                    System.out.println(user + ": User not exists");
                    response(output, client, -1);
                }
                else if (r == -2) {
                    System.out.println(user + ": User already connected");
                    response(output, client, -2);
                }

            }

            else if (opt.equals("logout") == true) {

                System.out.println(sender + ": Logout");
                res = server.logout(sender);
                if (res == true) {
                    System.out.println(sender + ": Logout succesfully");
                    //Metto la risposta, cioè la conferma che è andato tutto a buon fine, nel buffer
                    response(output, client, 1);
                }
                else {
                    System.out.println(sender + ": User not connected");
                    response(output, client, -1);
                }

            }

            else if (opt.equals("create") == true) {

                System.out.println(sender + ": Document creation");
                docname = (String) jcmd.get("Arg1");
                sec = Integer.parseInt((String) jcmd.get("Arg2"));
                sender = (String) jcmd.get("Sender");
                //Creo il documento
                int r = server.documentCreation(sender, docname, sec);
                if (r == 1) {
                    System.out.println(sender + ": Document created succesfully");
                    //Metto la risposta, cioè la conferma che è andato tutto a buon fine, nel buffer
                    response(output, client, 1);
                }
                //Utente non connesso
                else if (r == -1){
                    System.out.println(sender + ": User not connected");
                    response(output, client, -1);
                }
                //Documento già esistente
                else if (r == -2) {
                    System.out.println(sender + ": Document already exists");
                    response(output, client, -2);
                }
                else if (r == -3) {
                    System.out.println(sender + ": Document was not saved correctly");
                    response(output, client, -3);
                }

            }

            else if (opt.equals("share") == true) {

                docname = (String) jcmd.get("Arg1");
                user = (String) jcmd.get("Arg2");
                sender = (String) jcmd.get("Sender");
                System.out.println(sender + ": Document invitation to " + user);
                //Il sender aggiunge l' utente add al documento docname
                int r = server.addUserToDoc(docname, sender, user);
                if (r == 1) {
                    //Metto la risposta, cioè la conferma che è andato tutto a buon fine, nel buffer
                    System.out.println(sender + ": User " + user + " added");
                    response(output, client, 1);
                    //Invio la notifica al thread che si occupa di ricevere gli inviti se l' utente è online
                    if (server.isConnected(user) == true) {
                        //L' utente è online
                        this.sendNotification(docname, user);
                    } else {
                        //Salvo notifica se non è connesso
                        server.addNotification(docname, user);
                    }
                } else if (r == -1){
                    System.out.println(sender + ": User not connected");
                    response(output, client, -1);
                } else if (r == -2) {
                    System.out.println(sender + ": Access denied to document");
                    response(output, client, -2);
                } else if (r == -3) {
                    System.out.println(sender + ": Document not exists");
                    response(output, client, -3);
                } else if (r == -4) {
                    System.out.println(sender + ": User with whom you want to share the document does not exist");
                    response(output, client, -4);
                } else if (r == -5) {
                    System.out.println(sender + ": Document was not saved correctly");
                    response(output, client, -5);
                }

            }

            else if (opt.equals("show") == true) {

                docname = (String) jcmd.get("Arg1");
                sender = (String) jcmd.get("Sender");
                System.out.println(sender + ": Document " + docname + " viewing");
                Document doc = server.getDocument(docname, sender);
                //Se tutto va bene
                if (doc != null) {
                    String filename = "Documents/" + docname + ".json";
                    Path path = Paths.get(filename);
                    //Apro un canale per prendere i dati dal file
                    FileChannel inChannel = (FileChannel) Files.newByteChannel(path);
                    //Alloco il buffer che conterrà la dimensione del file
                    output = ByteBuffer.allocate(4);
                    //Ricavo la dimensione del file
                    int size = (int) Files.size(path);
                    response(output, client, size);
                    //Trasferisco dal canale del file a quello con il client
                    inChannel.transferTo(0, size, client);
                    //Chiudo il canale del file
                    inChannel.close();
                    //Recupero lista sequenza in corso modifica
                    List<Integer> tmp = server.getSecModList(docname);
                    if (tmp == null) {
                        //La lista non contiene elementi perchè non ci sono sezioni in modifica
                        response(output, client, 0);
                    }
                    else {
                        //Creo stringa contenente gli indici delle sezioni in modifica
                        size = tmp.size();
                        String sections = new String();
                        for (int i = 0; i < size; i++) {
                            if (i == 0) sections = sections + tmp.get(i);
                            else sections = sections + " " + tmp.get(i);
                        }
                        //Invio dimensione di questa lista come stringa di caratteri
                        size = sections.length();
                        output.putInt((int) size);
                        output.flip();
                        client.write(output);
                        output.clear();
                        //Invio lista
                        output = ByteBuffer.wrap(sections.getBytes());
                        write(client, output, sections.getBytes().length);
                        output.flip();
                        output.clear();
                    }
                    System.out.println(sender + ": Document send succesfully");
                }
                //Se il documento non viene recuperato significa che l' accesso era negato all' utente o non esiste il documento
                else {
                    //Se il documento esiste allora l' accesso è negato
                    if (server.documentExists(docname) == true) {
                        System.out.println(sender + ": Access denied to document");
                        response(output, client, -1);
                    }
                    //Il documento non esiste
                    else {
                        System.out.println(sender + ": Document not exists");
                        response(output, client, -2);
                    }
                }

            }

            else if (opt.equals("list") == true) {

                System.out.println(sender + ": List");
                //Creo una stringa che contiene tutti i nomi dei documenti presenti
                String docs = this.server.getDocList(sender);
                if (docs != null) {
                    //Ricavo la dimensione del file
                    int size = docs.getBytes().length;
                    response(output, client, size);
                    //Invio lista
                    output = ByteBuffer.wrap(docs.getBytes());
                    write(client, output, docs.getBytes().length);
                    output.flip();
                    output.clear();
                    System.out.println(sender + ": List send succesfully");
                }
                //L' utente non è connesso
                else {
                    System.out.println(sender + ": User not connected");
                    response(output, client, -1);
                }

            }

            else if (opt.equals("edit") == true) {

                docname = (String) jcmd.get("Arg1");
                System.out.println(sender + ": Edit document " + docname);
                int index = Integer.parseInt((String) jcmd.get("Arg2"));
                //Recupero il documento
                Document doc = server.getDocument(docname, sender);
                //Se non riesco a recuperare il documento o sto già modificando questa sezione
                if (doc == null || server.secInMod(docname, index) == true) {
                    //Il documento è null se non esiste o se l' accesso è negato all' utente
                    if (doc == null) {
                        if (server.documentExists(docname) == true) {
                            System.out.println(sender + ": Access denied to document");
                            response(output, client, -1);
                        }
                        else {
                            System.out.println(sender + ": Document not exists");
                            response(output, client, -2);
                        }
                    }
                    else {
                        System.out.println(sender + ": Section already in modification");
                        response(output, client, -3);
                    }
                }
                //Se il documento non contiene questa sezione
                else if (doc.containsSec(index) == false) {
                    System.out.println(sender + ": Document not contains this section");
                    response(output, client, -4);
                }
                //Altrimenti invio la sezione al client e segno che la sezione x è in modifica
                else {
                    //Invio dimensione indirizzo di multicast
                    String ind = server.getDocInd(doc.getName());
                    //Rispondo con dimensione indirizzo di multicast
                    response(output, client, ind.getBytes().length);
                    output = ByteBuffer.allocate(ind.getBytes().length);
                    output = ByteBuffer.wrap(ind.getBytes());
                    //Invio indirizzo di multicast
                    write(client, output, ind.getBytes().length);
                    output.flip();
                    output.clear();
                    //Segno che la sezione è in modifica al server e da quale utente
                    server.secMod(sender, docname, index);
                    sect = doc.getSection(index);
                    JSONObject s = sect.getJSON();
                    //Invio dimensione della sezione, vista come oggetto JSON
                    response(output, client, s.toJSONString().getBytes().length);
                    //Invio la sezione stessa, vista come oggetto JSON
                    output = ByteBuffer.wrap(s.toJSONString().getBytes());
                    //Invio sequenza
                    write(client, output, s.toJSONString().getBytes().length);
                    output.flip();
                    output.clear();
                    System.out.println(sender + ": Edit succesfully");
                }

            }

            else if (opt.equals("end-edit") == true) {

                //Ricavo informazioni utili per l' operazione
                docname = (String) jcmd.get("Arg1");
                System.out.println(sender + ": End-edit for document " + docname);
                int index = Integer.parseInt((String) jcmd.get("Arg2"));
                //Rispondo al client con successo
                response(output, client, 1);
                //Ricevo dimensione del testo
                input = ByteBuffer.allocate(4);
                view = input.asIntBuffer();
                read(client, input, 4);
                input.flip();
                int size = 0;
                size = view.get();
                view.rewind();
                //Rispondo al client con la dimensione ricevuta, in modo che lui controlli se va bene
                response(output, client, size);
                String t = null;
                String tmp = new String();
                //Buffer usato per leggere il testo
                input = ByteBuffer.allocate(size);
                //A questo punto dovrei ricevere il testo
                read(client, input, size);
                //Decodifico la stringa ricevuta
                try {
                    t = new String(input.array(), 0, size, "ASCII");
                    tmp = tmp + t;
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                }
                input.clear();
                //Aggiorno e salvo il documento
                if (server.docSave(docname, sender, tmp, index) == true) {
                    System.out.println(sender + ": End-edit succesfully");
                    response(output, client, 1);
                }
                else {
                    System.out.println(sender + ": Document was not saved correctly");
                    response(output, client, -1);
                }

            }

            else {

                System.out.println("Operation not supported");
                response(output, client, -1);

            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        try {
            client.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Metodo che restituisce la prima parola di una stringa con spazi eliminando questi ultimi e quello che segue
     *
     * @param s la stringa iniziale
     * @return la stringa contenente solo la prima parola della stringa passata
     */
    private String stringCleaner(String s) {

        StringTokenizer st = new StringTokenizer(s, "\0");
        return st.nextToken();

    }

    /**
     * Metodo per rispondere al client con un intero che codifica l' esito della richiesta
     *
     * @param output il buffer da cui prelevare l' intero
     * @param client il socket usato dal client
     * @param n l' intero da scrivere come risposta
     */
    private void response(ByteBuffer output, SocketChannel client, int n) {

        output = ByteBuffer.allocate(4);
        output.putInt(n);
        output.flip();
        write(client, output, 4);
        output.clear();

    }

    /**
     * Metodo per inviare notifiche di invito ricevute in tempo reale
     *
     * @param docname il nome del documento per il quale inviare un invito
     * @param user l' utente che si desidera invitare
     */
    private void sendNotification(String docname, String user) {

        try {
            SocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), server.getPort(user));
            //Apro connessione con il thread del client che si occupa di ascoltare gli inviti, che in questo caso
            //fa da server
            SocketChannel server;
            server = SocketChannel.open();
            server.connect(address);
            //Invio lunghezza nome del documento
            int size = docname.length();
            ByteBuffer output = ByteBuffer.allocate(4);
            response(output, server, size);
            //Invio messaggio, nome del documento
            output = ByteBuffer.wrap(docname.getBytes());
            write(server, output, size);
            output.flip();
            output.clear();
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

    /**
     * Metodo per scrivere nel buffer con il client un numero specificato di bytes
     *
     * @param client il socket usato dal client
     * @param buf il buffer da cui prelevare i bytes da scrivere
     * @param dim il numero di bytes da scrivere
     */
    private static void write(SocketChannel client, ByteBuffer buf, int dim) {

        try {
            while (dim > 0) {
                int w = -1;
                w = client.write(buf);
                if (w != -1) dim = dim - w;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
