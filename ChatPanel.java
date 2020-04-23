import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Pannello per mostrare graficamente la chat dentro la finestra del client
 */

public class ChatPanel extends JPanel implements ActionListener {

    //Campo contenente il messaggio
    private JTextField msg;
    //Pulsante per inviare messaggio
    private JButton sendmsg;
    //Client
    private ClientImpl client;
    //Area dedicata alla visualizzazione dei messaggi in chat
    private TextArea chat;
    //Riga alla quale siamo a scrivere nel pannello
    private int row = 0;

    /**
     * Costruttore
     *
     * @param c l' oggetto che rappresenta il client
     */
    public ChatPanel(ClientImpl c) {

        //Uso il costruttore della classe superiore
        super();
        //Inizializzo componenti
        this.client = c;
        this.sendmsg = new JButton("Send Message");
        this.chat = new TextArea(5, 40);
        this.chat.setEditable(false);
        this.msg = new JTextField(32);
        //Aggiungo rilevatore di azione
        sendmsg.addActionListener(this);
        //Aggiungo componenti al pannello
        add(chat);
        add(msg);
        add(sendmsg);

    }

    /**
     * Metodo per gestire un evento
     *
     * @param e evento da gestire
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        String t = null;
        String tmp = null;
        String tx = null;
        //Recupero messaggio
        t = msg.getText();
        //Creo la stringa con il comando
        tmp = "turing send " + t;
        //Creo il comando
        Command cmd = client.createCommand(tmp);
        //Eseguo il comando
        client.executeAction(cmd);
        //Stampo di nuovo sul terminale carattere di attesa input
        System.out.printf("$ ");

    }


    /**
     * Metodo per mostrare un messaggio nell' area dedicata alla chat
     *
     * @param t il messaggio da mostrare
     */
    public void showText(String t) {

        String text = chat.getText();
        if (row != 0) text = text + "\n" + t;
        else text = t;
        //Vado alla riga sotto
        row++;
        chat.setText(text);

    }

    /**
     * Metodo per rimuovere il testo dall' area dedicata alla chat
     */
    public void removeText() {

        //Setto il testo da mostrare a null
        chat.setText(null);

    }

    /**
     * Metodo per rimuovere il messaggio dal campo dopo che è stato inviato
     */
    public void removeMsg() {

        //Setto il testo da mostrare a null
        msg.setText(null);

    }

}
