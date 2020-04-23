/**
 * Classe che implementa un thread usato per il test
 */

public class ThreadTest extends Thread {

    private int n;

    public ThreadTest(int n) {
        this.n = n;
    }

    public void run () {

        //Genero un client
        ClientImpl client = new ClientImpl();
        //Creo finestra di login
        client.createLoginFrame();
        //Stringa di supporto
        String str = new String();
        //Comando di supporto
        Command cmd;

        try {

            //Registro utente
            str = "turing register c" + n + " pasw12";
            cmd = client.createCommand(str);
            client.executeAction(cmd);
            Thread.sleep(500);

            //Parte solo lo 0 che crea documento e invita i primi 16
            if (n != 0) Thread.sleep(3000);

            //Eseguo login utente
            str = "turing login c" + n + " pasw12";
            cmd = client.createCommand(str);
            client.executeAction(cmd);
            Thread.sleep(1000);

            //Creo documento
            str = "turing create documento" + n + " 10";
            cmd = client.createCommand(str);
            client.executeAction(cmd);
            Thread.sleep(500);

            //Se non sono il thread 0 faccio qualcosa
            if (n != 0) {
                //Edito il mio documento
                str = "turing edit documento" + n + " 1";
                cmd = client.createCommand(str);
                client.executeAction(cmd);
                //Faccio end-edit
                str = "turing end-edit documento" + n + " 1";
                cmd = client.createCommand(str);
                client.executeAction(cmd);
            }

            //Se sono il thread 0 invito tutti a collaborare al mio documento
            else {
                //Invito solo i primi 16, gli altri riceveranno accesso negato
                for (int i=1; i<16; i++) {
                    str = "turing share documento0 c" + i;
                    cmd = client.createCommand(str);
                    client.executeAction(cmd);
                }
            }

            //Se sono il thread 0 testo qualche errore
            if (n == 0) {

                //Provo edit documento che non esiste
                str = "turing edit documento 1";
                cmd = client.createCommand(str);
                client.executeAction(cmd);

                //Provo end-edit documento che non esiste
                str = "turing end-edit documento 1";
                cmd = client.createCommand(str);
                client.executeAction(cmd);

                //Provo show documento che non esiste
                str = "turing show documento 1";
                cmd = client.createCommand(str);
                client.executeAction(cmd);

                //Esco dal client con un logout
                str = "turing logout";
                cmd = client.createCommand(str);
                client.executeAction(cmd);

                //Eseguo login con utente sbagliato
                str = "turing login pippo pasw12";
                cmd = client.createCommand(str);
                client.executeAction(cmd);

            }
            else {
                //Se non sono il thread 0

                //Inizio editing documento0
                str = "turing edit documento0 " + n;
                cmd = client.createCommand(str);
                client.executeAction(cmd);
                //Attendo che tutti lo facciano, quelli a cui è permesso, perchè gli arrivino i messaggi
                Thread.sleep(2000);

                //Invio messaggio in chat agli altri che lo fanno
                str = "turing send ciao a tutti";
                cmd = client.createCommand(str);
                client.executeAction(cmd);

                //Ricevo messaggi
                str = "turing receive";
                cmd = client.createCommand(str);
                client.executeAction(cmd);

                //Concludo editing documento0
                str = "turing end-edit documento0 " + n;
                cmd = client.createCommand(str);
                client.executeAction(cmd);

                //Vedo il documento
                str = "turing show documento0";
                cmd = client.createCommand(str);
                client.executeAction(cmd);
                //Do il tempo di mostrarlo in grafica
                Thread.sleep(2000);

                //Chiedo lista documenti
                str = "turing list";
                cmd = client.createCommand(str);
                client.executeAction(cmd);
                //Do il tempo di mostrarla in grafica
                Thread.sleep(2000);

                //Esco dal client con un logout
                str = "turing logout";
                cmd = client.createCommand(str);
                client.executeAction(cmd);

            }
        } catch (InterruptedException e) {
            //Per le sleep
            e.printStackTrace();
        }
    }

}
