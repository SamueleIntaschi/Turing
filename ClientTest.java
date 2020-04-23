import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Client usato per il testing
 */

public class ClientTest {

    public static void main(String[] args) {

        //Do il tempo al server di partire aspettando 3 secondi
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Genero un array di thread
        ArrayList<ThreadTest> ts = new ArrayList<ThreadTest>();
        int i = 0;
        //Mando tutti i thread in esecuzione
        for (i=0; i<32; i++) {
            ThreadTest t = new ThreadTest(i);
            ts.add(t);
        }
        for (i=0; i<ts.size(); i++) {
            ts.get(i).start();
        }


        /*--- TERMINAZIONE ---*/

        //Attendo che tutti i thread terminino
        for (i=0; i<ts.size(); i++) {
            try {
                ts.get(i).join();
            }
            catch (InterruptedException e) {
                System.out.println("Thread interrotto sulla join");
            }
        }

        //Attendo 3 secondi e cancello tutti i file residui
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        /*--- ELIMINAZIONE FILE RESIDUI ---*/


        boolean success = true;
        //Elimino le liste
        success = (new File("users.txt").delete());
        success = (new File("documents.txt").delete());
        String Dir = "Documents";
        File directory = new File(Dir);
        //Elimino tutti i file all' interno della directory
        String[] contenuto = directory.list();
        for (i=0; i<contenuto.length; i++) {
            File f = new File(directory, contenuto[i]);
            try {
                Files.delete(Paths.get(f.getPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (success == false) {
                System.out.println("Impossibile eliminare file");
            }
        }
        //Dopo averlo fatto elimino la directory vuota che conteneva i documenti
        success = directory.delete();
        if (success) {
            System.out.println("Ho cancellato la cartella " + Dir);
        }
        else {
            System.out.println("Impossibile cancellare la cartella: " + Dir);
        }
        //A questo punto è tutto finito
        System.out.println("Programma terminato");

        //Chiudo il programma
        System.exit(1);

    }

}
