import org.json.simple.JSONObject;

/**
 * Classe che rappresenta un utente
 */

public class User {

    //Username
    private String name;
    //Password
    private String psw;

    /**
     * Costruttore
     *
     * @param name il nome dell' utente
     * @param password la password usata dall' utente
     */
    public User(String name, String password) {
        this.name = name;
        this.psw = password;
    }

    /**
     * Metodo per avere un oggetto JSON che rappresenta l' utente
     *
     * @return l' oggetto JSON che rappresenta l' utente
     */
    public JSONObject getJSON() {
        JSONObject user = new JSONObject();
        user.put("Psw", this.psw);
        user.put("Name", this.name);
        return user;
    }

}
