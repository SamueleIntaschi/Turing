import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Classe che rappresenta la finestra che appare all' avvio del client, usata per eseguire il login dell' utente
 * o la registrazione di un nuovo utente
 */

public class LoginPanel extends JPanel implements ActionListener {

    //Campo per il nome utente
    private JTextField name;
    //Client
    private ClientImpl client;
    //Campo per la password
    private JTextField psw;
    //Bottoni per login e registrazione
    private JButton log;
    private JButton reg;
    //Etichette varie
    private JLabel user;
    private JLabel pasw;
    private JLabel incorrect;
    private JLabel pswtooshort;
    private JLabel cmdnotcorrect;
    private JLabel sucreg;
    private JLabel userexists;
    private JLabel useralreadycon;


    /**
     * Costruttore
     *
     * @param c il client che usa il servizio
     */
    public LoginPanel(ClientImpl c) {

        //Uso il costruttore della classe superiore
        super();
        //Inizializzo i componenti
        this.client = c;
        this.log = new JButton("Login");
        this.reg = new JButton("Register");
        this.user = new JLabel("Username");
        this.pasw = new JLabel("Password");
        this.name = new JTextField(24);
        this.psw = new JTextField(24);
        //Etichette contenenti i possibili errori riscontrabili
        this.incorrect = new JLabel("Invalid Username or Password");
        this.pswtooshort = new JLabel("Password must contain at least 6 characters");
        this.cmdnotcorrect = new JLabel("Operation sot supported");
        this.userexists = new JLabel("User is already registered");
        this.useralreadycon = new JLabel("User is already connected");
        log.addActionListener(this);
        reg.addActionListener(this);
        //Aggiungo i componenti
        add(user);
        add(name);
        add(pasw);
        add(psw);
        add(log);
        add(reg);

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
        String tmp = new String("turing ");
        String t = new String();
        //In base a questo compio un' azione
        if (button == log) {
            //Inizio a creare la stringa
            tmp = tmp + "login ";
            //Recupero testo dal campo
            t = name.getText();
            //System.out.println("Azione: " + t);
            //Creo la stringa con il comando
            tmp = tmp + t;
            t = psw.getText();
            tmp = tmp + " " + t;
            //Creo il comando
            Command cmd = client.createCommand(tmp);
            //Eseguo il comando
            client.executeAction(cmd);
            //Stampo di nuovo sul terminale carattere di attesa input
            System.out.printf("$ ");
        }
        else if (button == reg){
            System.out.println("registrazione");
            tmp = tmp + "register ";
            t = name.getText();
            tmp = tmp + t;
            t = psw.getText();
            tmp = tmp + " " + t;
            Command cmd = client.createCommand(tmp);
            client.executeAction(cmd);
            System.out.printf("$ ");
        }

    }

    /**
     * Metodo per stampare sulla finestra gli errori
     *
     * @param e l' errore codificato come un intero
     */
    public void error(int e) {

        this.setVisible(false);
        //user o password invalidi
        if (e == 0) {
            removeNotification();
            add(incorrect);
        }
        //Password corta
        else if (e == 1) {
            removeNotification();
            add(pswtooshort);
        }
        //User esiste già
        else if (e == 3) {
            removeNotification();
            add(userexists);
        }
        //User già connesso
        else if (e == 4) {
            removeNotification();
            add(useralreadycon);
        }
        //Operazione non supportata
        else if (e == 5) {
            removeNotification();
            add(cmdnotcorrect);
        }

        this.setVisible(true);
    }

    /**
     * Metodo per rimuovere tutti i messaggi di errore dalla finestra
     */
    public void removeNotification() {

        //Nascondo finestra
        this.setVisible(false);
        //Rimuovo errori
        remove(pswtooshort);
        remove(incorrect);
        remove(userexists);
        remove(pswtooshort);
        remove(cmdnotcorrect);
        remove(useralreadycon);
        //Mostro nuovamente finestra
        this.setVisible(true);

    }

}
