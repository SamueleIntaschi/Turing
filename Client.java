import java.io.*;

/**
 * Classe con metodo main, da cui parte il client
 */

public class Client {

    /**
     * Metodo main che non prende parametri
     *
     * @param args null
     */
    public static void main(String[] args) {

        /*----- CLIENT -----*/

        ClientImpl client = new ClientImpl();

        /*----- FINESTRA LOGIN -----*/

        client.createLoginFrame();

        /*----- CICLO PROGRAMMA -----*/

        boolean stop = false;

        while (stop == false) {
            //Stringa di supporto
            String str = new String();
            //Comando di supporto
            Command cmd;
            //Stampo un carattere che significa che va inserito un input
            System.out.printf("$ ");
            //Ricevo in input un comando
            InputStreamReader reader = new InputStreamReader(System.in);
            BufferedReader myInput = new BufferedReader(reader);
            try {
                str = new String(myInput.readLine());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            //Creo il comando
            cmd = client.createCommand(str);
            //Eseguo l' operazione specificata dal comando
            client.executeAction(cmd);
        }

    }

}