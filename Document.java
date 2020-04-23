import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

/**
 * Classe che rappresenta un documento con i metodi per recuperarlo e salvarlo in memoria.
 * Il documento quando deve essere modificato o acceduto per il recupero di un' informazione viene deserializzato come
 * oggetto JSON, decodificato e poi di nuovo serializzato come JSON.
 */

public class Document {

    //Nome del documento
    private String name;
    //Nome del creatore
    private String admin;
    //Lista di utenti ammessi a lavorare sul documento
    private ArrayList<String> users;
    //Sezioni
    private ArrayList<Section> sections;
    //Numero di sezioni
    private int sec;

    /**
     * Metodo costruttore che prende in input il nome dell' utente che richiede la creazione del documento,
     * il nome del documento e il numero di sezioni, che sono indicizzate a partire da 0
     */
    public Document(String user, String name, int sec) {

        this.users = new ArrayList<String>();
        this.name = name;
        this.users.add(user);
        this.admin = user;
        this.sections = new ArrayList<Section>();
        this.sec = sec;
        //Creo sezioni per il documento
        for (int i=0; i<sec; i++) {
            sections.add(new Section(i));
        }

    }

    /**
     * Metodo costruttore che prende in input un oggetto JSON, lo decodifica e recupera il documento
     */
    public Document(JSONObject obj) {

        //Recupero nome documento
        this.name = (String) obj.get("Name");
        //Recupero nome amministratore documento
        this.admin = (String) obj.get("Creator");
        this.sections= new ArrayList<Section>();
        this.users = new ArrayList<String>();
        //Recupero numero di sezioni
        Long x = (Long) obj.get("NoSecs");
        this.sec = x.intValue();
        //Recupero sezioni
        JSONArray secs = (JSONArray) obj.get("Sections");
        //Recupero utenti autorizzati a lavorare sul documento
        JSONArray usrs = (JSONArray) obj.get("Users");
        int i = 0;
        for (i=0; i<sec; i++) {
            //Decodifico sezione e la aggiungo all' array
            sections.add(new Section((JSONObject) secs.get(i)));
        }
        //Decodifico array contenente gli utenti
        this.users = (ArrayList<String>)obj.get("Users");

    }


    /**
     * Metodo per salvare documento in memoria
     *
     * @return true se il salvataggio va a buon fine, false altrimenti
     */

    public boolean save() {

        //Creazione percorso
        String tmp = "Documents/" +  this.name + ".json";
        //Acquisizione path
        Path path = Paths.get(tmp);
        //Acquisizione JSON, questo avviene in mutua esclusione
        JSONObject docj = this.getJson();
        try {
            //Creo il file se non esiste
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            //Creo il buffer che conterrà il file
            ByteBuffer buffer = null;
            //Creo il canale
            SeekableByteChannel outChannel = Files.newByteChannel(path, StandardOpenOption.WRITE);
            buffer = ByteBuffer.wrap(docj.toJSONString().getBytes());
            //Scrivo la stringa rappresentante il documento
            while (buffer.hasRemaining()) {
                outChannel.write(buffer);
            }
            //Chiudo il canale
            outChannel.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Metodo per ottenere il nome del documento
     *
     * @return una stringa contenente il nome del documento
     */
    public String getName() {

        return this.name;

    }

    /**
     * Metodo per recuperare una sezione del documento
     *
     * @param index indice della sezione che si vuole recuperare
     *
     * @return l' oggetto sezione rappresentante la sezione di indice index
     */
    public Section getSection(int index) {

        return this.sections.get(index);

    }

    /**
     * Metodo che restituisce il testo intero del documento, concatenando quello delle sezioni
     */
    public String getText() {

        int i = 0;
        String tmp = this.name + "\n";
        Section s;
        for (i=0; i<this.sec; i++) {
            s = this.sections.get(i);
            tmp = tmp + "\n" + "Section " + s.getIndex();
            tmp = tmp + "\n" + s.getText();
        }
        return tmp;

    }

    /**
     * Metodo per consentire ad un utente di lavorare sul documento
     *
     * @param user il nome dell' utente che fa la richiesta
     * @param add il nome dell' utente da aggiungere
     *
     * @return true se l' aggiunta va a buon fine, false altrimenti
     */
    public boolean addUser(String user, String add) {

        if (user.equals(this.admin) == true) {
            this.users.add(add);
            return true;
        } else {
            return false;
        }

    }

    /** Metodo che aggiorna il testo di una sezione
     *
     *
     * @param t il testo da inserire nella sezione
     * @param ind l' indice della sezione
     */
    public void updateSection(String t, int ind) {

        Section s = sections.get(ind);
        s.update(t);

    }

    /**
     * Metodo che crea e restituisce un oggetto JSON che rappresenta questo documento
     */
    public JSONObject getJson() {

        JSONObject obj = new JSONObject();
        //Inserisco il nome nell' oggetto json
        obj.put("Name", this.getName());
        //Inserisco il nome dell' admin dell' oggetto JSON
        obj.put("Creator", this.admin);
        JSONArray usrs = (JSONArray) new JSONArray();
        for (int i=0; i<this.users.size(); i++) {
            usrs.add(users.get(i));
        }
        obj.put("Users", usrs);
        obj.put("NoSecs", this.sec);
        //Creo un array json con le sezioni
        JSONArray secs = (JSONArray) new JSONArray();
        for (int i=0; i<this.sec; i++) {
            secs.add(this.sections.get(i).getJSON());
        }
        //Inserisco l' array di sezioni nell' oggetto json che
        //rappresenterà il documento
        obj.put("Sections", secs);
        return obj;

    }


    /**
     * Metodo per sapere se un utente è autorizzato a lavorare su un documento
     *
     * @param name il nome dell' utente
     * @return true se l' utente è autorizzato, false altrimenti
     */
    public boolean containsUser(String name) {

        return (this.users.contains(name));

    }

    /**
     * Metodo per sapere se il documento contiene una sezione
     *
     * @param index l' indice della sezione
     * @return true se contiene la sezione, false altrimenti
     */
    public boolean containsSec(int index) {

        for (int i=0; i<sections.size(); i++) {
            if (sections.get(i).getIndex() == index) return true;
        }
        return false;

    }

}
