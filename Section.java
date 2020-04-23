import org.json.simple.JSONObject;

/**
 * Classe che rappresenta una sezione di un documento
 */

public class Section {

    private String text;
    private int index;

    /**
     * Costruttore che crea una sezione a partire dall' indice
     *
     * @param index l' indice della sezione da creare
     */
    public Section(int index) {

        this.text = new String("<empty>");
        this.index = index;

    }

    /**
     * Costruttore che prende come parametro un oggetto JSON e ne recupera l' oggetto rappresentante la sezione
     *
     * @param obj l' oggetto JSON che rappresenta la sezione codificata
     */
    public Section(JSONObject obj) {

        this.text = (String) obj.get("Text");
        Long i = (Long) obj.get("Number");
        this.index = i.intValue();

    }

    /**
     * Metodo per avere la sezione sotto forma di oggetto JSON
     *
     * @return l' oggetto JSON che rappresenta la sezione
     */
    public JSONObject getJSON() {

        JSONObject sec = new JSONObject();
        sec.put("Number", this.index);
        sec.put("Text", this.text);
        return sec;

    }

    /**
     * Metodo per modificare il testo della sezione
     *
     * @param t la stringa contenente il nuovo testo
     */
    public void update(String t) {
        this.text = t;
    }

    /**
     * Metodo per recuperare il testo della sezione
     *
     * @return il testo della sezione
     */
    public String getText() {
        return this.text;
    }

    /**
     * Metodo per recuperare l' indice della sezione
     *
     * @return l' indice della sezione
     */
    public int getIndex() {
        return this.index;
    }

}
