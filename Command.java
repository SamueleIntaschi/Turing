import java.util.ArrayList;
import java.util.StringTokenizer;
import org.json.simple.JSONObject;

/**
 * Classe che rappresenta un comando dato al client tramite finestra o linea di comando, che poi sarà mandato,
 * se corretto, al server
 */

public class Command {

    //Parole che compongono la richiesta
    private ArrayList<String> cmd;
    //Stringa contenente il comando
    private String command;
    //Stringa contenente l' utente che fa la richiesta
    private String sender;

    /**
     * Costruttore
     *
     * @param command il comando tutto insieme
     * @param sender l' utente che scrive il comando, se è connesso, altrimenti è null
     */
    public Command(String command, String sender) {

        this.command = command;
        this.sender = sender;
        StringTokenizer st = new StringTokenizer(command, " ");
        cmd = new ArrayList<String>(st.countTokens() + 1);
        //Spezzo il comando nelle singole parole
        int i = 0;
        while (st.hasMoreTokens()) {
            cmd.add(st.nextToken());
            i++;
        }
        if (sender == null) {
            this.sender = cmd.get(2);
        }

    }

    /**
     * Metodo per controllare se un comando è accettabile
     *
     * @return true se il comando è corretto, false altrimenti
     */
    public boolean isCorrect() {

        if (this.cmd.get(0).equals("exit") == true && this.cmd.size() == 1) return true;
        else if (this.cmd.size() > 4 && this.cmd.get(1).equals("send") == false) return false;
        else if((this.cmd.get(0).equals("turing")) == false ||
                (this.cmd.size() < 2) ||
                (this.cmd.get(0).equals("exit") == true && this.cmd.size() != 1)) return false;
        else if (this.cmd.size()== 3) {
            if (this.cmd.get(1).equals("show") == true ||
                    this.cmd.get(1).equals("send") == true ||
                    this.cmd.get(1).equals("--help") == true) return true;
            else return false;
        }
        else if (this.cmd.size() == 2) {
            if (this.cmd.get(1).equals("list") == true ||
                    this.cmd.get(1).equals("receive") == true ||
                    this.cmd.get(1).equals("logout") == true ||
                    this.cmd.get(1).equals("exit") == true) return true;
            else return false;
        }
        else return true;

    }

    /**
     * Metodo per recuperare l' operazione
     *
     * @return la stringa che contiene l' operazione
     */
    public String getOpt() {

        if (this.cmd.size() == 1) return "exit";
        else return this.cmd.get(1);

    }

    /**
     * Metodo per recuperare la terza parola che compone il documento
     *
     * @return la terza parola che compone il documento
     */
    public String get2() {

        if (this.cmd.size() < 3) return null;
        return this.cmd.get(2);

    }

    /**
     * Metodo per recuperare la quarta parola che compone il documento
     *
     * @return la quarta parola che compone il documento
     */    public String get3() {

        if (this.cmd.size() < 4) return null;
        return this.cmd.get(3);

    }

    /**
     * Metodo per recuperare dalla terza parola che compone il documento in poi finchè ce ne sono, usato in caso
     * di invio di un messaggio con operazione send
     *
     * @return la stringa contenente le parole che compongono il comando dalla terza in poi
     */
    public String getMsg() {

        if (this.cmd.size() < 3) return null;
        else if (getOpt().equals("send")){
            String tmp = new String();
            String t = new String();
            //Parto dalla terza parola
            int i = 2;
            //Prendo dalla terza parola in poi
            while (true) {
                try {
                    t = this.cmd.get(i);
                    if (i == 2) tmp = t;
                    else tmp = tmp + " " + t;
                    i++;
                    //Mi fermo quando vado fuori
                } catch (IndexOutOfBoundsException e) {
                    break;
                }
            }
            return tmp;
        } else return null;

    }

    /**
     * Metodo che cea e restituisce un oggetto JSON che rappresenta questo comando
     *
     * @return l' oggetto JSON rappresentante il comando
     */
    public JSONObject getJson() {

        JSONObject obj = new JSONObject();
        //Inserisco i campi nell' oggetto json
        obj.put("Command", this.getOpt());
        obj.put("Arg1", this.get2());
        obj.put("Arg2", this.get3());
        obj.put("Sender", this.sender);
        return obj;

    }

}
