import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Implementa una lista di stringhe, mantenendone anche una su file di testo per essere recuperata in caso di
 * spegnimento e riavvio del server
 */

public class FileList {

    private ArrayList<String> list;
    Path path;

    /**
     * Metodo costruttore, che crea una lista, a meno che non esista già, i cui elementi sono stringhe e vengono scritte
     * anche su un file permanente. Se esista già ne recupera gli elementi sotto forma di ArrayList.
     *
     * @param p la stringa contenente il nome della lista che si vuole creare, in modo che possa essere recuperata se si
     *        trova già su un file
     *
     */
    public FileList(String p) {

        this.list = new ArrayList<String>();
        String pt = p + ".txt";
        this.path = Paths.get(pt);
        StringTokenizer st = null;
        String tmp = null;
        //Se la lista non esiste crea il nuovo file, altrimenti recupera i dati al suo interno
        try {
            Files.createFile(path);
        } catch (FileAlreadyExistsException e) {
            //Se la lista esista già la recupero
            try {
                BufferedReader reader = Files.newBufferedReader(this.path, Charset.forName("UTF-8"));
                String currentLine = null;
                //Leggo il file di testo linea per linea, ogni linea è un elemento
                while((currentLine = reader.readLine()) != null){
                    st = new StringTokenizer(currentLine, System.lineSeparator());
                    tmp = st.nextToken();
                    //Le stringhe contenute rappresentano oggetti JSON nel caso degli utenti, solo il nome nel caso di documenti
                    this.list.add(tmp);
                }
            }catch(IOException ex){
                ex.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Metodo per aggiungere una stringa alla lista
     *
     * @param s la stringa da inserire
     *
     */
    public void addToList(String s) {

        //Aggiungo alla lista
        list.add(s);
        //Creo una riga per il file
        String tmp = s;
        //Scrivo questa riga nel file
        try {
            OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.APPEND);
            outputStream.write((tmp + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    /**
     * Restituisce la lista di oggetti che contiene
     *
     * @return la lista contenuta nella struttura dati
     */
    public List<String> getList() {

        return this.list;

    }

    /**
     * Metodo per sapere se l' oggetto con questo nome è nella lista
     *
     * @param s la stringa da cercare
     *
     * @return true se la stringa è nella lista, false altrimenti
     */
    public boolean inList(String s) {

        int i = 0;
        for (i=0; i<this.list.size(); i++) {
            if (list.get(i).equals(s) == true) {
                return true;
            }
        }
        return false;

    }

    /**
     * Metodo per sapere se l' oggetto è nella lista decodificando gli oggetti JSON (Per i nomi utente, che sono salvati
     * nella lista come oggetti JSON)
     *
     * @param s la stringa, che dovrebbe rappresentare un oggetto JSON, da cercare
     *
     * @return true se l' elemento è nella lista, false altrimenti
     */
    public boolean inListJSON(String s) {

        int i = 0;
        for (i=0; i<this.list.size(); i++) {
            //Decodifico oggetto JSON rappresentante l' utente o il documento per ricavarne il nome
            String tmp = this.list.get(i);
            JSONParser parser = new JSONParser();
            Object obj = null;
            try {
                obj = parser.parse(tmp);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            JSONObject j = (JSONObject) obj;
            String name = null;
            name = new String((String) j.get("Name"));
            //System.out.println(name + " " + s);
            if (name.equals(s) == true) {
                return true;
            }
        }
        //Se arrivo qui ritorno falso
        return false;

    }

}
