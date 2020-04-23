import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Pannello che mostra in generale tutti i servizi offerti dal client
 */

public class GraphicPanel extends JPanel implements ActionListener {

    //Componenti finestra
    //Campi testo
    private JTextField doc;
    private JTextField sec;
    //Client
    private ClientImpl client;
    //Area di testo principale
    private JTextArea text;
    //Pannello con scrolling
    private JScrollPane scroll;
    //Pulsanti per operazioni
    private JButton create;
    private JButton share;
    private JButton edit;
    private JButton end_edit;
    private JButton list;
    private JButton show;
    private JButton logout;
    //Possibili errori
    private JLabel edtng;
    private JLabel docex;
    private JLabel usrnotcon;
    private JLabel usralreadycon;
    private JLabel denied;
    private JLabel docnotsvd;
    private JLabel usrinvitnotex;
    private JLabel docnotex;
    private JLabel numreq;
    private JLabel secmod;
    private JLabel docnotsec;
    private JLabel wrongsec;
    private JLabel opnotsup;

    /**
     * Costruttore
     *
     * @param c l' oggetto rappresentante l' implementazione del client
     */
    public GraphicPanel(ClientImpl c) {

        //Uso costruttore classe superiore
        super();
        this.client = c;
        //Inizializzo componenti con relative dimensioni dove servono
        this.doc = new JTextField(12);
        this.sec = new JTextField(12);
        this.create = new JButton("Create");
        this.share = new JButton("Share");
        this.edit = new JButton("Edit");
        this.end_edit = new JButton("End-Edit");
        this.list = new JButton("List");
        this.show = new JButton("Show");
        this.logout = new JButton("Logout");
        this.text = new JTextArea(24, 96);
        this.text.setEditable(false);
        this.text.setWrapStyleWord(true);
        this.text.setLineWrap(true);
        this.scroll = new JScrollPane(text);
        JLabel d = new JLabel("Document");
        JLabel s = new JLabel("Section/User");
        //Aggiungo listener (questo stesso oggetto)
        create.addActionListener(this);
        share.addActionListener(this);
        edit.addActionListener(this);
        end_edit.addActionListener(this);
        list.addActionListener(this);
        show.addActionListener(this);
        logout.addActionListener(this);
        //Aggiungo i componenti
        add(d);
        add(doc);
        add(s);
        add(sec);
        add(create);
        add(share);
        add(edit);
        add(end_edit);
        add(list);
        add(show);
        add(logout);
        add(scroll);
        //Possibili errori
        edtng = new JLabel("A document is being edited");
        docex = new JLabel("Document already exists");
        docnotex = new JLabel("Document not exists");
        usrnotcon = new JLabel("User is not connected");
        usralreadycon = new JLabel("User is already connected");
        denied = new JLabel("Access denied to document");
        docnotsvd = new JLabel("Document was not saved correctly by the server");
        usrinvitnotex = new JLabel("User with whom you want to share the document does not exist");
        numreq = new JLabel("Number is required for index of section");
        secmod = new JLabel("Sections already in modification");
        docnotsec = new JLabel("Document not contains this section");
        wrongsec = new JLabel("Modification request for wrong section");
        opnotsup = new JLabel("Operation not supported");

    }

    /**
     * Metodo per gestire un evento (ActionListener)
     *
     * @param e l' evento da gestire
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        //Vedo chi ha generato la richiesta
        Object button = e.getSource();
        String t = new String();
        String tmp = new String();
        //In base a questo compio un' azione
        if (button == create) {
            //Recupero nome del documento
            t = doc.getText();
            //Creo la stringa con il comando
            tmp = "turing create " + t;
            //Aggiungo numero di sequenza
            t = sec.getText();
            tmp = tmp + " " + t;
            //Creo il comando
            Command cmd = client.createCommand(tmp);
            //Eseguo il comando
            client.executeAction(cmd);
            //Stampo di nuovo sul terminale carattere di attesa input
            System.out.printf("$ ");
        }
        else if (button == share) {
            //Recupero nome del documento
            t = doc.getText();
            //Creo la stringa con il comando
            tmp = "turing share " + t;
            //Aggiungo nome dell' utente con cui condividere il documento
            t = sec.getText();
            tmp = tmp + " " + t;
            //Creo il comando
            Command cmd = client.createCommand(tmp);
            //Eseguo il comando
            client.executeAction(cmd);
            //Stampo di nuovo sul terminale carattere di attesa input
            System.out.printf("$ ");
        }
        else if (button == list) {
            //Recupero nome del documento
            t = doc.getText();
            //Creo la stringa con il comando
            tmp = "turing list";
            //Creo il comando
            Command cmd = client.createCommand(tmp);
            //Eseguo il comando
            client.executeAction(cmd);
            //Stampo di nuovo sul terminale carattere di attesa input
            System.out.printf("$ ");
        }
        else if (button == show) {
            //Recupero nome del documento
            t = doc.getText();
            //Creo la stringa con il comando
            tmp = "turing show " + t;
            //Aggiungo numero di sequenza
            t = sec.getText();
            tmp = tmp + " " + t;
            //Creo il comando
            Command cmd = client.createCommand(tmp);
            //Eseguo il comando
            client.executeAction(cmd);
            //Stampo di nuovo sul terminale carattere di attesa input
            System.out.printf("$ ");
        }
        else if (button == logout) {
            //Recupero nome del documento
            t = doc.getText();
            //Creo la stringa con il comando
            tmp = "turing logout";
            //Creo il comando
            Command cmd = client.createCommand(tmp);
            //Eseguo il comando
            client.executeAction(cmd);
            //Stampo di nuovo sul terminale carattere di attesa input
            System.out.printf("$ ");
        }
        else if (button == edit) {
            //Recupero nome del documento
            t = doc.getText();
            //Creo la stringa con il comando
            tmp = "turing edit " + t;
            //Aggiungo numero di sequenza
            t = sec.getText();
            tmp = tmp + " " + t;
            //Creo il comando
            Command cmd = client.createCommand(tmp);
            //Eseguo il comando
            client.executeAction(cmd);
            //Stampo di nuovo sul terminale carattere di attesa input
            System.out.printf("$ ");
        }
        else if (button == end_edit) {
            //Recupero nome del documento
            t = doc.getText();
            //Creo la stringa con il comando
            tmp = "turing end-edit " + t;
            //Aggiungo numero di sequenza
            t = sec.getText();
            tmp = tmp + " " + t;
            //Creo il comando
            Command cmd = client.createCommand(tmp);
            //Eseguo il comando
            client.executeAction(cmd);
            //Stampo di nuovo sul terminale carattere di attesa input
            System.out.printf("$ ");
        }
        else {
            System.out.println("Nothing");
            System.out.printf("$ ");
        }

    }

    /**
     * Metodo per scrivere nella finestra i possibili errori riscontrabili
     *
     * @param e un intero che codifica un errore
     */
    public void error(int e) {

        this.setVisible(false);
        //Quando faccio comparire un errore devo nascondere gli altri
        //Modifica in corso
        if (e == 3) {
            removeNotification();
            add(edtng);
        }
        //Documento già esistente
        else if (e == 2) {
            removeNotification();
            add(docex);
        }
        //User non connesso
        else if (e == 1) {
            removeNotification();
            add(usrnotcon);
        }
        //Accesso negato al documento
        else if (e == 4) {
            removeNotification();
            add(denied);
        }
        //User già connesso
        else if (e == 5) {
            removeNotification();
            add(usralreadycon);
        }
        //Documento non salvato correttamente
        else if (e == 6) {
            removeNotification();
            add(docnotsvd);
        }
        //Utente invitato non esiste
        else if (e == 7) {
            removeNotification();
            add(usrinvitnotex);
        }
        //Documento non esiste
        else if (e == 8) {
            removeNotification();
            add(docnotex);
        }
        //E' richiesto un numero intero
        else if (e == 9) {
            removeNotification();
            add(numreq);
        }
        //C' è una sezione in modifica
        else if (e == 10) {
            removeNotification();
            add(secmod);
        }
        //Il documento non contiene questa sezione
        else if (e == 11) {
            removeNotification();
            add(docnotsec);
        }
        //Richiesta modifica di sezione sbagliata
        else if (e == 12) {
            removeNotification();
            add(wrongsec);
        }
        //Operazione non supportata
        else if (e == 13) {
            removeNotification();
            add(opnotsup);
        }
        //Se ricevo 0 rimuovo tutti gli errori
        else if (e == 0) {
            removeNotification();
        }
        this.setVisible(true);

    }

    /**
     * Metodo per rimuovere dalla grafica tutte le notifiche di errore
     */
    public void removeNotification() {

        //Nascondo finestra
        this.setVisible(false);
        //Elimino errori
        remove(docex);
        remove(usrnotcon);
        remove(edtng);
        remove(usralreadycon);
        remove(docnotsvd);
        remove(usrinvitnotex);
        remove(docnotex);
        remove(numreq);
        remove(secmod);
        remove(docnotsec);
        remove(wrongsec);
        remove(opnotsup);
        //Mostro nuovamente
        this.setVisible(true);

    }

    /**
     * Metodo per mostrare nell' area di testo adibita alla modifica di una sezione un testo
     *
     * @param t il testo da mostrare
     */
    public void showText(String t) {

        text.setText(t);

    }

    /**
     * Metodo per reperire il testo dall' area di testo dedicata alla modifica di una sezione
     *
     * @return il testo contenuto nell' area di testo
     */
    public String takeText() {

        return this.text.getText();

    }

    /**
     * Metodo per rendere modificabile l' area di testo
     */
    public void makeTextEditable() {

        this.text.setEditable(true);

    }

    /**
     * Metodo per rendere non modificabile l' area di testo
     */
    public void makeTextNotEditable() {

        this.text.setEditable(false);

    }

}
