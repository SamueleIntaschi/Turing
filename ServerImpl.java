import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementazione dei metodi usati dal server per soddisfare le richieste dei client
 */

public class ServerImpl implements ServerInterface {

    //Utenti iscritti
    private FileList usrs;
    //Documenti presenti
    private FileList documents;
    //Utenti connessi
    private HashMap<String, Integer> connUsers;
    //Hashtable che collega nomi di documenti a indirizzi di multicast, usati per comunicare in chat
    private HashMap<String, String> addrs;
    //Hashmap per ricordarsi per ogni documento quali sezioni sono in modifica e da quale utente
    private HashMap<String, HashMap<Integer, String>> secmod;
    //Hashmap per ricordarsi di inviare notifiche di link di invito: <username, inviti>
    private HashMap<String, ArrayList<String>> notify;
    //Hashmap per chiavi da usare sui documenti
    private final HashMap<String, Lock> doclocks;
    //Esecutore dei thread
    private ExecutorService executor;
    //Variabile usata per creare indirizzi di multicast diversi
    private int ind = 0;
    //Stringa contenente l' indirizzo per comunicare in chat per il docuemnto specifico
    private String docind = null;
    //Array di lock per mutua esclusione
    private final ArrayList<Lock> locks;
    /*
     * Locks nell' array usate:
     * - 0 per usrs, nomi utenti registrati
     * - 1 per connUsers, nomi utenti connessi
     * - 2 per documents, nomi documenti salvati
     * - 3 per addrs, indirizzi usati per il multicast
     * - 4 per secmod, sezioni che sono in corsi di modifica
     * - 5 per notify, inviti pendenti
     */
    //Prima porta usata per comunicare gli inviti
    private int port = 5002;

    /**
     * Costruttore che inizializza lock e strutture dati
     */
    public ServerImpl() {
        //Lista utenti registrati, visti come oggetto JSON
        this.usrs = new FileList("users");
        //Lista documenti salvati
        this.documents = new FileList("documents");
        //Lista utenti connessi
        this.connUsers = new HashMap<String, Integer>();
        //Indirizzi multicast chat
        this.addrs = new HashMap<String, String>();
        //Sezioni in corso di modifica e a quale documento appartengono
        this.secmod = new HashMap<String, HashMap<Integer, String>>();
        //Inviti per utente
        this.notify = new HashMap<String, ArrayList<String>>();
        //Genero pool di thread
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(24);
        this.locks = new ArrayList<Lock>();
        for (int i=0; i<6; i++) {
            Lock tmp = new ReentrantLock();
            locks.add(tmp);
        }
        this.doclocks = new HashMap<String, Lock>();
        List<String> d = documents.getList();
        for (int i=0; i<d.size(); i++) {
            Lock tmp = new ReentrantLock();
            doclocks.put(d.get(i), tmp);
        }
    }

    /**
     * Metodo esportato chiamato via RMI per registrare nuovi utenti
     *
     * @param user il nome dell' utente da registrare
     * @param psw la password dell' utente
     * @return un intero che codifica l' esito dell' operazione
     * @throws RemoteException
     */
    @Override
    public int register(String user, String psw) throws RemoteException {

        System.out.println(user + ": Registration");
        //Se la password è troppo corta
        if (psw.length() < 6) {
            System.out.println("Password too short");
            return -2;
        }
        locks.get(0).lock();
        //Se l' user è già presente
        if (this.usrs.inListJSON(user) == true){
            locks.get(0).unlock();
            System.out.println(user + ": User already exists");

            return -1;
        }
        else {
            //Aggiungo utente a quelli registrati
            User u = new User(user, psw);
            JSONObject usr = u.getJSON();
            this.usrs.addToList(usr.toJSONString());
            locks.get(0).unlock();
            System.out.println(user + ": Registration succesfully");

            return 1;
        }

    }

    /**
     * Procedura che manda in esecuzione il task, attivando un thread worker
     *
     * @param task il compito da eseguire
     */
    public void executeTask(Task task) {

        boolean b = false;
        while (b == false) {
            //Provo a eseguire il task
            try {
                //Invio richiesta al pool
                executor.submit(task);
                b = true;
            }
            catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Procedura per autenticazione
     *
     * @param user il nome dell' utente che vuole connettersi
     * @param psw la password dell' utente
     * @return un intero che codifica l' esito dell' operazione
     */
    public int login(String user, String psw) {
        User u = new User(user, psw);
        //Se l' utente è registrato, la password corretta e l' utente non ancora connesso
        //L' oggetto JSON contiene anche la password quindi se è presente nella lista allora le credenziali sono giuste
        String usr = u.getJSON().toJSONString();

        locks.get(0).lock();
        //Se l' utente è registrato lo inserisco in quelli connessi
        if (this.usrs.inList(usr) == true) {
            locks.get(0).unlock();

            locks.get(1).lock();
            if (connUsers.containsKey(user) == false) {
                //Aggiungo l' utente alla lista di quelli connessi, assegnandogli una porta su cui comunicherò gli inviti
                port++;
                connUsers.put(user, port);
                locks.get(1).unlock();

                locks.get(5).lock();
                //Se non ci sono inviti ritorno 1, altrimenti 2 e invio la lista dal task
                if (notify.containsKey(user) == false) {
                    locks.get(5).unlock();

                    return 1;
                } else {
                    locks.get(5).unlock();

                    return 2;
                }
            } else {
                //Utente già connesso
                locks.get(1).unlock();

                return -2;
            }
        } else {
            //Utente non esistente
            locks.get(0).unlock();

            return -1;
        }
    }

    //Procedura per logout
    public boolean logout(String user) {

        locks.get(1).lock();
        //Se l' utente è tra quelli connessi lo disconnetto
        if (connUsers.containsKey(user)) {
            connUsers.remove(user);
            locks.get(1).unlock();

            locks.get(4).lock();
            //Se l' utente stava modificando sezioni comunico che non lo sta più facendo
            Set<String> users = secmod.keySet();
            Iterator it = users.iterator();
            boolean stop = false;
            int sec = -1;
            String doc = new String();
            //Scorro su tutti i documenti in corso di modifica
            while(it.hasNext() == true && stop == false) {
                //Nome del documento
                String docname = (String) it.next();
                //Tabella che mappa indice della sezione con utente che la sta modificando
                HashMap<Integer, String> h = secmod.get(docname);;
                //Se la tabella contiene il nome utente da disconnettere rimuovo la sezione che sta modificando
                if (h.containsValue(user)) {
                    //Indici delle sezioni del documento che sono in modifica
                    Set<Integer> inds = h.keySet();
                    Iterator it2 = inds.iterator();
                    while(it2.hasNext()) {
                        try {
                            Integer ind = (Integer) it2.next();
                            int i = (int) ind;
                            if (h.get(i).equals(user) == true) {
                                //Mi segno sezione e documento per eliminarli alla fine del ciclo
                                sec = i;
                                doc = docname;
                                stop = true;
                            }
                        } catch (NoSuchElementException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (sec != -1 && doc != null) secmod.get(doc).remove(sec);
            locks.get(4).unlock();

            return true;
        }
        else {
            locks.get(1).unlock();

            return false;
        }
    }

    /**
     * Procedura per creare documento e salvarlo in memoria
     *
     * @param user l' utente che fa la richiesta
     * @param name il nome del documento da creare
     * @param sec il numero di sezioni
     * @return un intero che codifica l' esito dell' operazione
     */
    public int documentCreation(String user, String name, int sec) {

        //Entro in mutua esclusione sugli utenti connessi
        locks.get(1).lock();
        //Controllo se l' utente è connesso, e quindi registrato, e che il documento non esista già
        if (connUsers.containsKey(user) == true) {
            locks.get(1).unlock();

            locks.get(2).lock();
            if (documents.inList(name) == false) {
                locks.get(2).unlock();

                //Genero un indirizzo multicast per la chat di quel documento
                ind = (ind + 1) % 255;
                if (ind == 1) ind = 10;
                docind = "224.0.0." + ind;

                //Creo documento
                Document doc = new Document(user, name, sec);

                locks.get(3).lock();
                //Inserisco nella tabella l' indirizzo di multicast
                addrs.put(name, docind);
                locks.get(3).unlock();

                locks.get(2).lock();
                //Aggiunta alla lista
                documents.addToList(doc.getName());
                locks.get(2).unlock();

                //Genero una nuova lock per il documento e la aggiungo alla struttura dati
                Lock tmp = new ReentrantLock();
                doclocks.put(doc.getName(), tmp);

                //Salvo il documento in memoria, in mutua esclusione su di esso
                doclocks.get(doc.getName()).lock();
                boolean b = doc.save();
                doclocks.get(doc.getName()).unlock();

                if (b) return 1;
                else return -3;
            } else {
                //Documento già esistente
                locks.get(2).unlock();

                return -2;
            }
        } else {
            //Utente non connesso
            locks.get(1).unlock();

            return -1;
        }

    }

    /**
     * Procedura per recuperare un documento, precedentemente creato, dalla memoria
     *
     * @param name il nome del documento
     * @param user il nome dell' utente che fa la richiesta
     * @return il documento recuperato
     */
    public Document getDocument(String name, String user) {

        //Controllo se l' utente è connesso
        locks.get(1).lock();
        if (connUsers.containsKey(user) == true) {
            locks.get(1).unlock();

            //Controllo se il documento è presente
            locks.get(2).lock();
            if (documents.inList(name) == true) {
                locks.get(2).unlock();

                Document doc = null;
                try {

                    //Entro in mutua esclusione sul documento
                    doclocks.get(name).lock();
                    //Recupero il file dalla cartella Documents
                    //Preparo il path del file
                    Path path = Paths.get("Documents/" + name + ".json");
                    //Deserializzo il file contenente il documento
                    JSONParser parser = new JSONParser();
                    //Alloco il buffer che conterrà il file json
                    ByteBuffer buf = ByteBuffer.allocate(4096);
                    //Genero il canale di input
                    FileChannel inChannel = (FileChannel) Files.newByteChannel(path);
                    //Buffer temporaneo da usare per la parte restante del buffer
                    //che non rappresenta un oggetto json completo ma solo parziale
                    String buftemp = new String();
                    //Numero di bytes letti
                    int letti = 0;
                    //Stringa temporanea
                    String tmp;
                    //Stringa finale
                    String finale = new String();
                    //Posizione all' interno del file da leggere
                    int pos = (int) inChannel.position();
                    //Leggo le prime informazioni dal buffer (4096 bytes)
                    letti = inChannel.read(buf, pos);
                    int i = 0;
                    //Ciclo finchè non ho letto tutto
                    while (letti > 0) {
                        //Concatenazione stringa
                        tmp = new String(buf.array(), 0, letti, "ASCII");
                        finale = finale + tmp;
                        //Aggiornamento posizione all' interno del file
                        pos = pos + letti;
                        //Pulizia del buffer
                        buf.clear();
                        //Leggo altri bytes
                        letti = inChannel.read(buf, pos);
                    }
                    //Ricavo oggetto Json dalla stringa
                    Object obj = parser.parse(finale);
                    JSONObject docj = (JSONObject) obj;
                    doc = new Document(docj);
                    //Ho finito di recuperare il documento, rilascio la chiave
                    doclocks.get(name).unlock();

                    locks.get(3).lock();
                    //Se il documento non è stato creato in questa sessione del server ma in una precedente,
                    //ne genero per questa sessione un indirizzo di multicast
                    if (addrs.containsKey(doc.getName()) == false) {
                        ind = (ind + 1) % 255;
                        if (ind == 1) ind = 10;
                        docind = "224.0.0." + ind;
                        addrs.put(doc.getName(), docind);
                    }
                    locks.get(3).unlock();

                    //Una volta recuperato controllo che l' utente sia ammesso a lavorare sul documento
                    if (doc.containsUser(user) == true) {
                        //Utente ammesso a lavorare sul documento
                        return doc;
                    } else {
                        //Utente non ammesso a lavorare sul documento
                        return null;
                    }
                } catch (IOException | ParseException e) {
                    //Se ottengo un' eccezione il file non è presente nella cartella, quindi il documento non esiste
                    e.printStackTrace();
                    //Documento non esistente
                    return null;
                }
            } else {
                //Documento non esistente
                locks.get(2).unlock();

                return null;
            }
        } else {
            //Utente sbagliato
            locks.get(1).unlock();

            return null;
        }

    }

    /**
     * Metodo per invitare un user su un documento
     *
     * @param name il nome del documento per il quale si invia l' invito
     * @param user il nome dell' utente che fa la richiesta
     * @param add il nome dell' utente da invitare
     * @return un intero che codifica l' esito dell' operazione
     */
    public int addUserToDoc(String name, String user, String add) {

        //Controllo se l' utente è connesso
        locks.get(1).lock();
        if (connUsers.containsKey(user) == true) {
            locks.get(1).unlock();

            Document doc = this.getDocument(name, user);
            if (doc == null) {

                locks.get(2).lock();
                if (this.documents.inList(name)) {
                    //Accesso negato all' utente
                    locks.get(2).unlock();

                    return -2;
                } else {
                    //Documento inesistente
                    locks.get(2).unlock();

                    return -3;
                }
            }

            locks.get(0).lock();
            if (this.usrs.inListJSON(add)) {
                locks.get(0).unlock();

                //L' utente da invitare esiste, lo invito
                boolean b = doc.addUser(user, add);
                if (b == true) {
                    //Salvo il documento con l' utente autorizzato aggiunto
                    doclocks.get(doc.getName()).lock();
                    boolean bool = doc.save();
                    doclocks.get(doc.getName()).unlock();

                    //Utente invitato con successo
                    if (bool) return 1;
                    else return -5;
                } else {
                    //Accesso negato perchè l' invitante non è il creatore del documento
                    return -2;
                }
            } else {
                //L' utente da invitare non esiste
                locks.get(0).unlock();

                return -4;
            }
        } else {
            //Utente non connesso
            locks.get(1).unlock();

            return -1;
        }
    }

    /**
     * Metodo per aggiungere una notifica di invito per un utente, quando questo non è connesso, per poi inviargliela
     * al momento della connessione
     *
     * @param docname il documento per il quale è ricevuto l' invito
     * @param add l' utente invitato
     */
    public void addNotification(String docname, String add) {

        locks.get(5).lock();
        //Se l' utente non ha ancora inviti aggiungo una voce alla tabella con l' invito
        if (notify.containsKey(add) == false) {
            ArrayList<String> tmp = new ArrayList<String>();
            tmp.add(docname);
            notify.put(add, tmp);
        } else {
            //Aggiungo invito per l' utente
            notify.get(add).add(docname);
        }
        locks.get(5).unlock();

    }

    /**
     * Metodo per ricevere la lista dei documenti a cui un utente è autorizzato ad accedere
     *
     * @param user l' utente che fa la richiesta
     * @return la lista dei documenti creati o per i quali l' utente ha ricevuto un invito
     */
    public String getDocList(String user) {

        //Conotrollo se l' utente è connesso
        locks.get(1).lock();
        if (connUsers.containsKey(user) == true) {
            locks.get(1).unlock();

            String docs = new String();
            int i = 0;
            Document doc = null;

            //Recupero lista in mutua esclusione
            locks.get(2).lock();
            List<String> d = this.documents.getList();
            locks.get(2).unlock();

            for (i = 0; i < d.size(); i++) {
                doc = getDocument(d.get(i), user);
                //Se non ottengo null vuol dire che l' utente è ammesso al documento, quindi lo aggiungo alla lista
                if (doc != null) {
                    docs = docs + d.get(i) + "\n";
                }
            }
            return docs;
        }
        else {
            locks.get(1).unlock();

            return null;
        }
    }

    /**
     * Metodo per recuperare l' indirizzo di multicast usato per la chat sul documento in questa sessione del server
     *
     * @param doc il nome del documento
     * @return la stringa contenente l' indirizzo
     */
    public String getDocInd(String doc) {

        //Controllo se il documento è presente
        locks.get(3).lock();
        if (this.addrs.containsKey(doc)) {
            String add = this.addrs.get(doc);
            locks.get(3).unlock();

            return add;
        }
        else {
            //Documento non esiste
            locks.get(3).unlock();

            return null;
        }

    }

    /**
     * Metodo per aggiornare sezione e salvare
     *
     * @param name nome del documento da aggiornare e salvare
     * @param user utente che fa la richiesta
     * @param sec testo aggiornato della sezione
     * @param index indice della sezione da aggiornare
     * @return true se va a buon fine, false altrimenti
     */
    public boolean docSave(String name, String user, String sec, int index) {

        //Recupero documento richiesto
        Document doc = this.getDocument(name, user);

        //Entro in mutua esclusione sul documento
        doclocks.get(doc.getName()).lock();
        //Aggiorno sezione
        doc.updateSection(sec, index);
        //Salvo il documento
        boolean b = doc.save();
        doclocks.get(doc.getName()).unlock();

        //Se il documento non è stato serializzato con successo
        if (b == false) return false;
        else {
            //Rimuovo la sezione da quelle in corso di modifica
            this.removeSec(name, index);
            return true;
        }

    }

    /**
     * Metodo per comunicare al server che una sezione è in modifica
     *
     * @param user utente che fa la richiesta
     * @param name nome del documento
     * @param index indice della sezione
     * @return true se qualcuno non la sta già modificando, false altrimenti
     */
    public boolean secMod(String user, String name, int index) {

        locks.get(4).lock();
        //Se il documento è già nella mappa
        if (secmod.containsKey(name)) {
            //Se la sezione index del doc è già in modifica
            if (secmod.get(name).containsKey(index)) {
                locks.get(4).unlock();

                return false;
            }
            //Altrimenti la segno
            else {
                secmod.get(name).put(index, user);
                locks.get(4).unlock();

                return true;
            }
        }
        //Lo aggiungo ora e segno la sezione e chi la sta modificando
        else {
            HashMap<Integer, String> secs = new HashMap<Integer, String>();
            secs.put(index, user);
            secmod.put(name, secs);
            locks.get(4).unlock();

            return true;
        }

    }

    /**
     * Metodo per rimuovere sezione in modifica da mappa
     *
     * @param name nome del documento
     * @param index indice della sezione
     * @return true se il documento esiste e quindi posso rimuovere la sezione da quelle in modifica, false altrimenti
     */
    public boolean removeSec(String name, int index) {

        //Conotrollo se il documento è nella tabella, quindi se qualche sezione è in modifica
        locks.get(4).lock();
        if (secmod.containsKey(name)) {
            Integer tmp = index;
            secmod.get(name).remove(tmp);
            locks.get(4).unlock();

            return true;
        }
        else {
            //Documento senza sezioni in corso di modifica
            locks.get(4).unlock();

            return false;
        }
    }

    /**
     * Metodo per controllare se sezione è in modifica
     *
     * @param name nome del documento
     * @param index indice della sezione
     * @return true se la sezione è in modifica, false altrimenti
     */
    public boolean secInMod(String name, int index) {

        locks.get(4).lock();
        //Controllo se il documento è nella tabella
        if (secmod.containsKey(name)) {
            //Controllo se la sezione è in modifica
            if (secmod.get(name).containsKey(index)) {
                locks.get(4).unlock();

                return true;
            }
            else {
                //Sezione non in modifica
                locks.get(4).unlock();

                return false;
            }
        }
        else {
            //Documento senza sezioni in corso di modifica
            locks.get(4).unlock();

            return false;
        }

    }

    /**
     * Metodo per avere lista delle sezioni attualmente in modifica di un documento
     *
     * @param docname il nome del documento
     * @return la lista di sezioni del documento in modifica o null se il documento non ne ha
     */
    public List getSecModList(String docname) {

        List<Integer> tmp = new ArrayList<Integer>();

        //Conrollo se il documento è in tabella, cioè se ha sezioni in modifica
        locks.get(4).lock();
        HashMap<Integer, String> h = secmod.get(docname);
        if (h == null) {
            //Non ha sezioni in modifica
            locks.get(4).unlock();

            return null;
        }
        //Ha sezioni in modifica
        else {
            //Insieme di indici di sezione
            Set<Integer> set = h.keySet();
            Iterator it = set.iterator();
            while (it.hasNext()) {
                //Aggiungo sezione alla stringa
                tmp.add((Integer) it.next());
            }
            locks.get(4).unlock();

            return tmp;
        }

    }

    /**
     * Metodo per sapere se un documento esiste sapendone soltanto il nome
     *
     * @param name il nome del documento
     * @return true se esiste, false altrimenti
     */
    public boolean documentExists(String name) {

        locks.get(2).lock();
        boolean b = this.documents.inList(name);
        locks.get(2).unlock();

        return b;
    }

    /**
     * Metodo per recuperare lista di inviti ricevuti da un utente mentre era offline
     *
     * @param user l' utente per il quale è richiesta la lista
     * @return una stringa contenente, concatenati, i nomi dei documenti per i quali l' utente ha ricevuto
     * un invito mentre era offline
     */
    public String getNList(String user) {

        int i = 0;
        String tmp = new String();

        //Recuoero lista di nomi dei documenti
        locks.get(5).lock();
        ArrayList<String> doc = notify.get(user);
        locks.get(5).unlock();

        //Li concateno in una stringa
        for (i=0; i < doc.size(); i++) {
            if (i == 0) tmp = doc.get(i);
            else tmp = tmp + " " + doc.get(i);
            //Rimuovo il documento una volta inserito nella stringa
            doc.remove(doc.get(i));
        }
        return tmp;

    }

    /**
     * Metodo per sapere se un utente è connesso dall' esterno
     *
     * @param user l' utente per il quale è richiesto il controllo
     * @return treu se è connesso, false altrimenti
     */
    public boolean isConnected(String user) {

        locks.get(1).lock();
        if (connUsers.containsKey(user) == true) {
            locks.get(1).unlock();

            return true;
        } else {
            locks.get(1).unlock();

            return false;
        }

    }

    /**
     * Metodo per recuperare porta su cui inviare un invito ad un utente online
     *
     * @param user l' utente per il quale si richiede la porta
     * @return un intero che indica la porta
     */
    public int getPort(String user) {

        locks.get(1).lock();
        int port = this.connUsers.get(user);
        locks.get(1).unlock();

        return port;

    }

}
