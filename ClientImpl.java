import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Implementazione dei servizi offerti dal client all' utente
 */

public class ClientImpl {

    //Utente connesso al client
    private String conUser = null;
    //Interfaccia remota del server per RMI
    private ServerInterface serverObject;
    //Oggetto remoto rappresentante il server
    private Remote RemoteObject;
    //Finestra principale del client una volta che un utente è connesso
    private JFrame f;
    //Finestra di login, mostrata finchè nessun utente è connesso
    private JFrame logf;
    //Finestra di notifica, mostrata al ricevimento di un invito mentre un utente è online
    private JFrame notf;
    //Notifiche ricevute mentre l' utente è online, memorizzate per poi chiuderle
    private ArrayList<JFrame> notifications;
    //Pannello contenuto nella finestra principale
    private GraphicPanel ins;
    //Pannello all' interno della finestra di login
    private LoginPanel l;
    //Pannello chat, gestito dal chatlistenerclient
    private ChatPanel chat;
    //Variabile che indica se l' utente sta modificando un documento o no
    private int editing = -1;
    //Variabile per sapere se un utente è loggato o no
    private boolean logged = false;
    //Variabile usata per i vari indirizzi
    private String ind = null;
    //Ports per chat listener, casuale
    private int port = 10000;
    //Thread che gestisce chat
    private ChatListenerClient clc = null;
    //Thread che gestisce inviti
    private NotificationListener nl = null;

    /**
     * Costruttore che non fa niente
     */
    public ClientImpl() { }

    /**
     * Metodo per creare graficamente la finestra usata dal client con tutti i componenti che ne fanno parte
     */
    public void createFrame() {

        //Modo alternativo alla linea di comando per gestire il programma
        this.f = new JFrame("TuringClient");
        //Imposto azione da fare alla chiusura della finestra (logout dell' utente connesso)
        this.f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                //Se un utente è connesso chiedo il logout
                if (conUser != null) {
                    //Creo il comando e lo eseguo
                    Command cmd = new Command("turing logout", conUser);
                    executeAction(cmd);
                }
                //Chiudo il programma
                System.exit(1);
            }});
        //Stabilisco misure e aggiungo componenti
        this.notifications = new ArrayList<JFrame>();
        this.ins = new GraphicPanel(this);
        this.chat = new ChatPanel(this);
        this.f.add(ins);
        this.f.add(chat);
        this.f.setSize(1000, 600);
        this.f.setLayout(new BorderLayout());
        this.f.add(ins, BorderLayout.CENTER);
        this.f.add(chat, BorderLayout.SOUTH);
        //Stabilisco che la finestra non possa essere ridimensionata
        this.f.setResizable(false);
        //La rendo visibile
        this.f.setVisible(true);

    }

    /**
     * Metodo per creare graficamente la finestra di login con tutti i componenti che ne fanno parte
     */
    public void createLoginFrame() {

        //Inizializzo misure e componenti
        logf = new JFrame("Login");
        logf.setSize(300, 200);
        Container logc = logf.getContentPane();
        logc.add(new JLabel("Login"));
        logf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        l = new LoginPanel(this);
        logc.add(l);
        //Stabilisco che la finestra non possa essere ridimensionata
        logf.setResizable(false);
        //La rendo visibile
        logf.setVisible(true);

    }

    /**
     * Metodo per creare graficamente la finestra che comunica che l' utente è stato invitato a lavorare su un documento
     *
     * @param docname il nome del documento per il quale si riceve l' invito
     */
    public void createNotification(String docname) {

        //Inizializzo misure e componenti
        notf = new JFrame("Notification");
        notf.setSize(450, 150);
        Container notc = notf.getContentPane();
        notc.add(new JLabel("You have been invited to collaborate on the document " + docname));
        notf.setVisible(true);
        //Aggiungo la notifica all' array di notifiche
        notifications.add(notf);

    }

    /**
     * Metodo per creare una richiesta da inviare al server partendo da una stringa
     *
     * @param cmd la stringa testuale rappresentante il comando
     * @return l' oggetto rappresentante il comando
     */
    public Command createCommand(String cmd) {

        return (new Command(cmd, conUser));

    }

    /**
     * Metodo per eseguire l' azione specificata dal comando dato al client e da inviare al server
     *
     * @param cmd l' oggetto rappresentante il comando
     */
    public void executeAction(Command cmd) {

        //Variabili per creare una richiesta da inviare al server
        String user = null;
        String psw = null;
        String sender = null;
        String docname = null;
        ByteBuffer input = null;
        //Operazione che andrà eseguita
        String opt = cmd.getOpt();
        int sec = -1;

        //Se il comando è corretto lo eseguo
        if (cmd.isCorrect()) {

            //Ricavo l' utente che invia la richiesta
            sender = conUser;

            //Se non è connesso nessun utente posso fare solo operazioni di login e registrazione
            if (logged == false && editing == -1) {

                /*----- REGISTRAZIONE -----*/

                if (opt.equals("register") == true) {

                    //Ricavo campi della richiesta
                    user = cmd.get2();
                    psw = cmd.get3();
                    System.out.println("Register " + user);
                    try {
                        //Cerco oggetto server remoto
                        Registry r = LocateRegistry.getRegistry(5000);
                        RemoteObject = r.lookup("SERVER");
                        serverObject = (ServerInterface) RemoteObject;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (user != null && psw != null) {
                        try {
                            //Provo a registrare l' utente
                            int res = serverObject.register(user, psw);
                            //Controllo se ci sono stati errori e stampo l' errore
                            if (res == 0) {
                                System.out.println("Password or username not specified");
                                //Scrivo nel pannello di login che c' è stato un errore
                                this.l.error(0);
                            } else if (res == -1) {
                                System.out.println("User is already registered");
                                //Scrivo nel pannello di login che c' è stato un errore
                                this.l.error(3);
                            } else if (res == -2) {
                                System.out.println("Password must contain at least 6 characters");
                                //Scrivo nel pannello di login che c' è stato un errore
                                this.l.error(1);
                            } else {
                                System.out.println("User registered succesfully");
                                //Scrivo nel pannello di login che c' è stato un errore
                                this.l.removeNotification();
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("Password or username not specified");
                        //Scrivo nel pannello di login che c' è stato un errore
                        this.l.error(0);
                    }
                }

                /*----- LOGIN -----*/

                else if (opt.equals("login") == true) {

                    //Recupero informazioni utili
                    user = cmd.get2();
                    System.out.println("Login " + user);
                    SocketAddress addr = null;
                    SocketChannel server = null;

                    //Connessione con il server
                    try {
                        addr = new InetSocketAddress(InetAddress.getLocalHost(), 5001);
                        server = SocketChannel.open(addr);
                        server.configureBlocking(true);
                    } catch (UnknownHostException ex) {
                        ex.printStackTrace();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    //Ottengo oggetto JSON del comando
                    JSONObject command = cmd.getJson();
                    //Metto nel buffer la richiesta sotto forma di oggetto JSON
                    String json = command.toJSONString();

                    //Invio dimensione della stringa contenente la richiesta
                    ByteBuffer out = ByteBuffer.allocate(4);
                    out.putInt(json.getBytes().length);
                    out.flip();
                    write(server, out, 4);
                    out.clear();

                    //Invio la richiesta
                    out = ByteBuffer.wrap(json.getBytes());
                    write(server, out, json.getBytes().length);
                    //Pulisco il buffer
                    out.clear();
                    //Ricevo la risposta
                    int res = ack(input, server);

                    //In base alla risposta del server vedo cosa fare
                    if (res == 2) {
                        System.out.println("Login successfully and there are notifications");
                        //Rimuovo errori dalla finestra
                        this.l.removeNotification();
                        user = cmd.get2();
                        conUser = new String(user);
                        this.logged = true;
                        //Presento finestra del programma
                        this.createFrame();
                        //Nascondo finestra di login
                        this.logf.setVisible(false);
                        //Ricevo dimensione della lista con i messaggi di invito
                        int size = ack(input, server);
                        //Se la dimensione è maggiore di 0 ricevo la lista
                        if (size > 0) {
                            //Ricevo lista
                            input = ByteBuffer.allocate(size);
                            read(server, input, size);
                            //Decodifico stringa ricevuta
                            String tmp = null;
                            try {
                                //Decodifico stringa
                                tmp = new String(input.array(), 0, size, "ASCII");
                            } catch (UnsupportedEncodingException ex) {
                                ex.printStackTrace();
                            }
                            //Spezzo la stringa ricevuta, che contiene i nomi dei documenti
                            StringTokenizer st = new StringTokenizer(tmp, " ");
                            //Per ogni parola mostro una notifica
                            while (st.hasMoreTokens()) {
                                String s = st.nextToken();
                                //La mostro sul terminale e anche in grafica
                                createNotification(s);
                                System.out.println(s);
                            }
                            //Recupero porta da usare per ricevere inviti
                            int prt = ack(input, server);
                            //Creo e avvio thread per ricevere le notifiche
                            try {
                                this.nl = new NotificationListener(this, conUser, InetAddress.getLocalHost(), prt);
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }
                            nl.start();
                        }
                    } else if (res == -1) {
                        System.out.println("User does not exist");
                        //Scrivo nel pannello di login che c' è stato un errore
                        this.l.error(0);
                    } else if (res == -2) {
                        System.out.println("User is already connected");
                        this.l.error(4);
                    } else {
                        //Cancello errori presenti in grafica se ce ne sono
                        this.l.removeNotification();
                        //Recupero informazioni utili
                        user = cmd.get2();
                        conUser = new String(user);
                        this.logged = true;
                        //Presento finestra principale
                        this.createFrame();
                        //Nascondo finestra di login
                        this.logf.setVisible(false);
                        //Ricevo porta su cui riceverò eventuali inviti
                        int prt = ack(input, server);
                        //Creo e avvio thread per ricevere le notifiche
                        try {
                            this.nl = new NotificationListener(this, conUser, InetAddress.getLocalHost(), prt);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                        nl.start();
                        System.out.println("Login succesfully");
                    }
                }

                else if (opt.equals("exit") == true) {
                    //All' uscita eseguo il logout
                    System.out.println("Closing client...");
                    //Se è connesso faccio il logour
                    if (conUser != null) {
                        cmd = new Command("turing logout", conUser);
                        executeAction(cmd);
                    }
                    //Chiudo il programma
                    System.exit(1);
                }

                else {
                    System.out.println("Operation not supported");
                    ins.error(13);
                }

            }

            //Se c'è un utente connesso posso fare solo queste cose
            else if (logged == true) {

                /*----- LOGOUT -----*/

                if (opt.equals("logout") == true) {

                    System.out.println("Logout " + sender);
                    //Recupero informazioni utili
                    SocketAddress addr = null;
                    SocketChannel server = null;
                    //Apro connessione con il server
                    try {
                        addr = new InetSocketAddress(InetAddress.getLocalHost(), 5001);
                        server = SocketChannel.open(addr);
                        server.configureBlocking(true);
                    } catch (UnknownHostException ex) {
                        ex.printStackTrace();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    //Ottengo oggetto JSON del comando
                    JSONObject command = cmd.getJson();
                    //Metto nel buffer la richiesta sotto forma di oggetto JSON
                    String json = command.toJSONString();

                    //Invio dimensione della stringa contenente la richiesta
                    ByteBuffer out = ByteBuffer.allocate(4);
                    out.putInt(json.getBytes().length);
                    out.flip();
                    write(server, out, 4);
                    out.clear();

                    //Invio la richiesta
                    out = ByteBuffer.wrap(json.getBytes());
                    write(server, out, json.getBytes().length);
                    //Pulisco il buffer
                    out.clear();
                    int res = ack(input, server);

                    //A seconda della risposta ricevuta vedo cosa fare
                    if (res != 1) {
                        System.out.println("Utente non connesso");
                        ins.error(1);
                    } else {
                        //Comunico che non sono più connesso
                        logged = false;
                        //Comunico che non sto più editando un documento, in caso lo stessi facendo prima
                        editing = -1;
                        //Esco dalla chat per il documento, chiudendo anche il thread
                        if (clc != null) {
                            clc.interrupt();
                            //Attendo che il thread listener sia terminato
                            try {
                                clc.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            //Cancello i messaggi nella finestra
                            clc.cancelText();
                        }
                        //Cancello eventuali messaggi di errore nella finestra
                        ins.removeNotification();
                        //Rendo testo non modificabile
                        ins.makeTextNotEditable();
                        //Cancello il testo dalla finestra di modifica
                        ins.showText(null);
                        //Nascondo finestra principale
                        this.f.setVisible(false);
                        //Mostro di nuovo finestra di login
                        this.logf.setVisible(true);
                        //Cancello anche tutte le notifiche di invito dalla grafica, se presenti
                        for (int i=0; i<notifications.size(); i++) {
                            if (notifications.get(i).isVisible()) notifications.get(i).setVisible(false);
                        }
                        //Chiudo il thread listener delle notifiche
                        nl.interrupt();
                        //Aspetto che sia terminato
                        try {
                            nl.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Logout successfully");
                    }

                }

                //Se non sto editando un documento
                else if (editing == -1) {

                    /*----- DOCUMENT CREATION -----*/

                    if (opt.equals("create") == true) {

                        //Recupero informazioni utili
                        docname = cmd.get2();
                        try {
                            sec = Integer.parseInt(cmd.get3());
                        } catch (NumberFormatException e) {
                            System.out.println("Number is required for index of section");
                            ins.error(9);
                        }
                        System.out.println("Document " + docname +" creation");
                        SocketAddress addr = null;
                        SocketChannel server = null;

                        //Apro connessione con il server
                        try {
                            addr = new InetSocketAddress(InetAddress.getLocalHost(), 5001);
                            server = SocketChannel.open(addr);
                            server.configureBlocking(true);
                        } catch (UnknownHostException ex) {
                            ex.printStackTrace();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }

                        //Ottengo oggetto JSON del comando
                        JSONObject command = cmd.getJson();
                        //Metto nel buffer la richiesta sotto forma di oggetto JSON
                        String json = command.toJSONString();

                        //Invio dimensione della stringa contenente la richiesta
                        ByteBuffer out = ByteBuffer.allocate(4);
                        out.putInt(json.getBytes().length);
                        out.flip();
                        write(server, out, 4);
                        out.clear();

                        //Invio richiesta
                        out = ByteBuffer.wrap(json.getBytes());
                        //Invio il JSON del comando
                        write(server, out, json.getBytes().length);
                        //Pulisco il buffer
                        out.clear();
                        int res = ack(input, server);

                        //A seconda della risposta ricevuta vedo cosa fare
                        if (res == 1) {
                            //Elimino tutte le precedenti notifiche di errore dalla grafica
                            ins.removeNotification();
                            System.out.println("Document created succesfully");
                        }
                        else if (res == -1) {
                            System.out.println("User not connected");
                            ins.error(1);
                        }
                        else if (res == -2) {
                            System.out.println("Document already exists");
                            ins.error(2);
                        }
                        else if (res == -3) {
                            System.out.println("Document was not saved correctly by the server");
                            ins.error(6);
                        }

                    }

                    /*----- DOCUMENT SHARING -----*/

                    else if (opt.equals("share") == true) {

                        //Recupero informazioni utili
                        docname = cmd.get2();
                        user = cmd.get3();
                        System.out.println("Document " + docname + " invitation to " + user);
                        SocketAddress addr = null;
                        SocketChannel server = null;

                        //Apro connessione con il server
                        try {
                            addr = new InetSocketAddress(InetAddress.getLocalHost(), 5001);
                            server = SocketChannel.open(addr);
                            server.configureBlocking(true);
                        } catch (UnknownHostException ex) {
                            ex.printStackTrace();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        //Invio richiesta
                        //Ottengo oggetto JSON del comando
                        JSONObject command = cmd.getJson();
                        //Metto nel buffer la richiesta sotto forma di oggetto JSON
                        String json = command.toJSONString();

                        //Invio dimensione della stringa contenente la richiesta
                        ByteBuffer out = ByteBuffer.allocate(4);
                        out.putInt(json.getBytes().length);
                        out.flip();
                        write(server, out, 4);
                        out.clear();

                        //Invio lista
                        out = ByteBuffer.wrap(json.getBytes());
                        //Invio il JSON del comando
                        write(server, out, json.getBytes().length);
                        //pulisco il buffer
                        out.clear();
                        int res = ack(input, server);

                        //A seconda della risposta ricevuta faccio qualcosa
                        if (res == -1) {
                            System.out.println("User not connected");
                            ins.error(1);
                        } else if (res == -2) {
                            System.out.println("Access denied to document");
                            ins.error(4);
                        } else if (res == -3) {
                            System.out.println("Document not exists");
                            ins.error(8);
                        } else if (res == -4) {
                            System.out.println("User with whom you want to share the document does not exist");
                            ins.error(7);
                        } else if (res == -5) {
                            System.out.println("Il documento non è stato correttamente salvato per problemi al server");
                            ins.error(6);
                        } else {
                            ins.removeNotification();
                            System.out.println("User invited successfully");
                        }

                    }

                    /*----- SHOW -----*/

                    else if (opt.equals("show") == true) {

                        //Recupero informazioni utili
                        docname = cmd.get2();
                        System.out.println("Show Document " + docname);
                        SocketAddress addr = null;
                        SocketChannel server = null;

                        //Apro connessione con il server
                        try {
                            addr = new InetSocketAddress(InetAddress.getLocalHost(), 5001);
                            server = SocketChannel.open(addr);
                            server.configureBlocking(true);
                        } catch (UnknownHostException ex) {
                            ex.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //Ottengo oggetto JSON del comando
                        JSONObject command = cmd.getJson();
                        //Metto nel buffer la richiesta sotto forma di oggetto JSON
                        String json = command.toJSONString();

                        //Invio dimensione della stringa contenente la richiesta
                        ByteBuffer out = ByteBuffer.allocate(4);
                        out.putInt(json.length());
                        out.flip();
                        write(server, out, 4);
                        out.clear();
                        out = ByteBuffer.wrap(json.getBytes());

                        //Invio il JSON del comando
                        write(server, out, json.getBytes().length);
                        //pulisco il buffer
                        out.clear();

                        //A seconda della risposta vedo che fare
                        int res = ack(input, server);
                        //Accesso al documento negato all' utente
                        if (res == -1) {
                            System.out.println("Access denied to document");
                            ins.error(4);
                        }
                        //Documento inesistente
                        else if (res == -2) {
                            System.out.println("Document not exists");
                            ins.error(8);
                        } else {
                            //Rimuovo messaggi di errore presenti in grafica
                            ins.removeNotification();
                            //Se la richiesta va a buon fine ricevo prima la dimensione del file
                            int filesize = res;
                            //Alloco il buffer che conterrà il file json
                            ByteBuffer buf = ByteBuffer.allocate(filesize);
                            String fin = new String();
                            String decode = null;
                            //Ricevo file JSON cha rappresenta il documento
                            read(server, buf, filesize);
                            try {
                                //Decodifico stringa
                                decode = new String(buf.array(), 0, filesize, "ASCII");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            fin = fin + decode;
                            //Parsing del JSON
                            JSONParser parser = new JSONParser();
                            Object obj = null;
                            try {
                                obj = parser.parse(fin);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            JSONObject docj = (JSONObject) obj;
                            //Creo il documento usando il file JSON
                            Document doc = new Document(docj);
                            //Ricevo dimensione della lista delle sequenze in corso di modifica
                            int size = ack(input, server);
                            if (size == 0) {
                                //Non ci sono sezioni in modifica
                                String text = doc.getText();
                                System.out.println(text);
                                ins.showText(text);
                            } else {
                                //Ricevo lista delle sezioni in corso di modifica
                                input = ByteBuffer.allocate(size);
                                read(server, input, size);
                                //Decodifico stringa ricevuta
                                String tmp = null;
                                try {
                                    tmp = new String(input.array(), 0, size, "ASCII");
                                } catch (UnsupportedEncodingException ex) {
                                    ex.printStackTrace();
                                }
                                //Mostro l' intero documento con la lista di sequenza in modifica
                                String text = doc.getText();
                                System.out.println(text + "\n" + "\n" + "Sections in modification: " + tmp);
                                ins.showText(text + "\n" + "\n" + "Sections in modification: " + tmp);
                            }
                        }

                    }

                    /*----- LIST -----*/

                    else if (opt.equals("list") == true) {

                        System.out.println("List");
                        SocketAddress addr = null;
                        SocketChannel server = null;

                        //Apro connessione con il server
                        try {
                            addr = new InetSocketAddress(InetAddress.getLocalHost(), 5001);
                            server = SocketChannel.open(addr);
                            server.configureBlocking(true);
                        } catch (UnknownHostException ex) {
                            ex.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //Ottengo oggetto JSON del comando
                        JSONObject command = cmd.getJson();
                        //Metto nel buffer la richiesta sotto forma di oggetto JSON
                        String json = command.toJSONString();

                        //Invio dimensione della stringa contenente la richiesta
                        ByteBuffer out = ByteBuffer.allocate(4);
                        out.putInt(json.getBytes().length);
                        out.flip();
                        write(server, out, 4);
                        out.clear();

                        //Invio richiesta
                        out = ByteBuffer.wrap(json.getBytes());
                        //Invio il JSON del comando
                        write(server, out, json.getBytes().length);
                        //pulisco il buffer
                        out.clear();
                        //Ricevo in risposta dimensione della lista
                        int size = ack(input, server);
                        if (size == -1) {
                            System.out.println("User not connected");
                            ins.error(1);
                        } else {
                            ins.removeNotification();
                        }
                        //Ricevo la lista stessa
                        input = ByteBuffer.allocate(size);
                        read(server, input, size);
                        //Decodifico stringa ricevuta
                        String tmp = null;
                        try {
                            tmp = new String(input.array(), 0, size, "ASCII");
                        } catch (UnsupportedEncodingException ex) {
                            ex.printStackTrace();
                        }
                        //Lo mostro sul terminale e anche in grafica
                        System.out.printf(tmp);
                        ins.showText(tmp);

                    }

                    /*----- EDIT -----*/

                    else if (opt.equals("edit") == true) {

                        docname = cmd.get2();
                        System.out.println("Edit " + docname);
                        //Variabile ausiliaria
                        String tmp = null;
                        //Se non viene immesso un numero il server risponde con un errore
                        int index = -1;
                        try {
                            index = Integer.parseInt(cmd.get3());
                        } catch (NumberFormatException e) {
                            System.out.println("Number is required for index of section");
                            ins.error(9);
                        }

                        //Apro connessione con il server
                        SocketAddress addr = null;
                        SocketChannel server = null;
                        try {
                            addr = new InetSocketAddress(InetAddress.getLocalHost(), 5001);
                            server = SocketChannel.open(addr);
                            server.configureBlocking(true);
                        } catch (UnknownHostException ex) {
                            ex.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //Ottengo oggetto JSON del comando
                        JSONObject command = cmd.getJson();
                        //Metto nel buffer la richiesta sotto forma di oggetto JSON
                        String json = command.toJSONString();

                        //Invio dimensione della stringa contenente la richiesta
                        ByteBuffer out = ByteBuffer.allocate(4);
                        out.putInt(json.getBytes().length);
                        out.flip();
                        write(server, out, 4);
                        out.clear();

                        //Invio richiesta
                        out = ByteBuffer.wrap(json.getBytes());
                        //Invio il JSON del comando
                        write(server, out, json.getBytes().length);
                        //pulisco il buffer
                        out.clear();
                        //Ricevo la dimensione dell' indirizzo di multicast dal server oppure un errore
                        int d = ack(input, server);
                        //Se ricevo risposta di errore non faccio niente
                        if (d == -1) {
                            System.out.println("Access denied to document");
                            ins.error(4);
                        }
                        else if (d == -2) {
                            System.out.println("Document not exist");
                            ins.error(8);
                        }
                        else if (d == -3) {
                            System.out.println("Section already in modification");
                            ins.error(10);
                        }
                        else if (d == -4) {
                            System.out.println("Document not contains this section");
                            ins.error(11);
                        }
                        //Se ricevo dimensione
                        else {
                            ins.removeNotification();
                            //Ricevo indirizzo vero e proprio
                            input = ByteBuffer.allocate(d);
                            read(server, input, d);
                            //Decodifico stringa ricevuta
                            tmp = null;
                            try {
                                tmp = new String(input.array(), 0, d, "ASCII");
                            } catch (UnsupportedEncodingException ex) {
                                ex.printStackTrace();
                            }
                            ind = tmp;
                            input.clear();
                            //Creo un thread che si occupa della chat mentre sto editando il documento
                            clc = new ChatListenerClient(port, this.ind, this.chat);
                            clc.start();
                            //Ricevo dimensione sequenza e alloco un buffer per la ricezione della dimensione del file
                            int secsize = ack(input, server);
                            //Alloco un buffer di questa dimensione
                            input = ByteBuffer.allocate(secsize);
                            read(server, input, secsize);
                            try {
                                tmp = new String(input.array(), 0, secsize, "ASCII");
                            } catch (UnsupportedEncodingException ex) {
                                ex.printStackTrace();
                            }
                            JSONParser parser = new JSONParser();
                            //Ricavo oggetto Json dalla stringa
                            Object obj = null;
                            try {
                                obj = parser.parse(tmp);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            JSONObject secj = (JSONObject) obj;
                            String text = (String) secj.get("Text");
                            //Rimuovo precedenti notifiche di errore dalla grafica
                            ins.removeNotification();
                            //Salvo la sezione che sto modificando e il nome del documento
                            editing = index;
                            //Da qui in poi il testo nella text area è modificabile
                            ins.makeTextEditable();
                            //Lo mostro in modo che possa essere modificato
                            ins.showText(text);
                        }

                    }

                    else if (opt.equals("exit") == true) {
                        System.out.println("Chiusura del client");
                        //Se è connesso faccio il logout
                        if (conUser != null) {
                            cmd = new Command("turing logout", conUser);
                            executeAction(cmd);
                        }
                        //Chiudo il programma
                        System.exit(1);
                    }

                    else {
                        System.out.println("Operation not supported");
                        ins.error(13);
                    }

                }

                //Se è in corso la modifica di un documento posso fare solo end-edit oppure uscire o inviare messaggi
                else if (editing > -1) {

                    /*----- END-EDIT -----*/

                    if (opt.equals("end-edit") == true) {

                        System.out.println("End-edit operation");
                        docname = cmd.get2();
                        int index = -1;
                        try {
                            index = Integer.parseInt(cmd.get3());
                        } catch (NumberFormatException e) {
                            System.out.println("Number is required for index of section");
                            ins.error(9);
                        }
                        //Se sto richiedendo la modifica per la sezione giusta procedo, altrimenti stampo un errore
                        if (index == editing) {

                            //Apro connessione con il server
                            SocketAddress addr = null;
                            SocketChannel server = null;
                            try {
                                addr = new InetSocketAddress(InetAddress.getLocalHost(), 5001);
                                server = SocketChannel.open(addr);
                                server.configureBlocking(true);
                            } catch (UnknownHostException ex) {
                                ex.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            //Ottengo oggetto JSON del comando
                            JSONObject command = cmd.getJson();
                            //Metto nel buffer la richiesta sotto forma di oggetto JSON
                            String json = command.toJSONString();

                            //Invio dimensione della stringa contenente la richiesta
                            ByteBuffer out = ByteBuffer.allocate(4);
                            out.putInt(json.getBytes().length);
                            out.flip();
                            write(server, out, 4);
                            out.clear();

                            //Invio richiesta
                            out = ByteBuffer.wrap(json.getBytes());
                            //Invio il JSON del comando
                            write(server, out, json.getBytes().length);
                            //pulisco il buffer
                            out.clear();
                            out.compact();
                            //Ricevo risposta ma non importa controllare in questo caso
                            int r = ack(input, server);
                            //Recupero testo nella finestra grafica
                            String t = ins.takeText();
                            //Ricavo dimensione del testo da inviare
                            int s = t.getBytes().length;
                            out = ByteBuffer.allocate(4);
                            out.putInt(s);
                            out.flip();
                            //Invio dimensione del testo
                            write(server, out, 4);
                            out.clear();
                            out.compact();
                            //Attendo risposta del server
                            input = ByteBuffer.allocate(4);
                            IntBuffer view = input.asIntBuffer();
                            read(server, input, 4);
                            int res = view.get();
                            //La risposta deve corrispondere alla dimensione che io gli ho inviato oppure c'è un errore,
                            //ma questo non dovrebbe poter accadere
                            if (res != s) {
                                System.out.println("Incorrect answer from the server");
                            }
                            //Creo nuovo buffer per scrivere il testo aggiornato
                            ByteBuffer txt = ByteBuffer.wrap(t.getBytes());
                            //Scrivo il testo
                            write(server, txt, t.getBytes().length);
                            //Pulisco buffer
                            txt.clear();
                            //Rendo area di testo in grafica non più modificabile
                            ins.makeTextNotEditable();
                            //Cancello il testo dalla finestra di modifica
                            ins.showText(null);
                            //Cancello gli eventuali errori presenti in finestra
                            ins.error(0);
                            //Termino l' editing del documento, e con esso il thread che si occupa della chat,
                            //disconnettendomi da essa ed eliminando il testo
                            clc.interrupt();
                            //Attendo che il thread listener sia terminato
                            try {
                                clc.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            //Cancello messaggi visualizzati
                            clc.cancelText();
                            //Segno che non sto più modificando nulla
                            editing = -1;
                            clc = null;
                            //Attendo risposta dal server con esito del salvataggio
                            res = ack(input, server);
                            if (res == -1) {
                                System.out.println("Document was not saved correctly by the server");
                                ins.error(6);
                            }
                            else {
                                ins.removeNotification();
                                System.out.println("Modifica sezione terminata");
                            }
                        }
                        else {
                            System.out.println("Modification request for wrong section");
                            ins.error(12);
                        }

                    }


                    /*----- SEND -----*/

                    else if (opt.equals("send") == true) {

                        System.out.println("Send message");
                        //Creo messaggio
                        String msg = this.conUser + ": " + cmd.getMsg();
                        //Invio messaggio
                        clc.sendMessage(msg);
                        System.out.println("Message send succesfully");

                    }

                    /*----- RECEIVE -----*/

                    //Chiamabile solo da terminale
                    else if (opt.equals("receive") == true) {

                        System.out.println("Receive messages");
                        //Con un iteratore scorro i messaggi e li stampo, è il metodo chiamato per avere la lista a rimuoverli
                        List<String> msgs = clc.getMsgs();
                        Iterator<String> it = msgs.iterator();
                        while (it.hasNext()) {
                            System.out.println(it.next());
                        }

                    }

                    //Stampo errore sulla finestra
                    else {

                        System.out.println("A document is being edited");
                        //Scrivo nel pannello di login che c' è stato un errore
                        this.ins.error(3);

                    }

                }


            }

            /*----- USCITA DAL CLIENT -----*/

            //Dal client posso uscire in qualsiasi momento
            else if (opt.equals("exit") == true) {

                System.out.println("Closing client...");
                //Se è connesso faccio il logout
                if (conUser != null) {
                    cmd = new Command("turing logout", conUser);
                    executeAction(cmd);
                }
                //Chiudo il programma
                System.exit(1);

            }

        }

        //Se il comando non è corretto vuol dire che è troppo lungo, ma potrebbe essere una richiesta di invio messaggio
        else {
            if (logged == false) {

                //Scrivo nel pannello di login che c' è stato un errore
                this.l.error(0);
                System.out.println("Operation not supported");
                l.error(5);

            } else if (logged == true && editing > -1) {

                if (opt.equals("send") == false) {
                    System.out.println("Operation not supported");
                    ins.error(13);
                } else {
                    //Se è una send possono esserci più opzioni quindi la svolgo normalmente
                    System.out.println("Send message " + cmd.getMsg());
                    //Recupero l' intero messaggio, non solo la terza parola del comando
                    String msg = this.conUser + ": " + cmd.getMsg();
                    clc.sendMessage(msg);
                    System.out.println("Message send succesfully");
                }

            } else {
                System.out.println("Operation not supported");
                ins.error(13);
            }
        }

    }

    /**
     * Metodo per ricevere un intero dal server
     *
     * @param input il buffer in cui mettere l' intero letto
     * @param server il socket usato dal server
     * @return l' intero letto
     */
    private int ack(ByteBuffer input, SocketChannel server) {

        //Il Buffer sarà un array di interi con uno solo
        input = ByteBuffer.allocate(4);
        IntBuffer view = input.asIntBuffer();
        int res = 0;
        //Leggo la risposta
        read(server, input, 4);
        res = view.get();
        //Pulisco il buffer
        input.clear();
        return res;

    }

    /**
     * Metodo per leggere dal buffer con il server un numero specificato di bytes
     *
     * @param server il socket usato dal server
     * @param buf il buffer su cui scrivere le informazioni lette
     * @param dim il numero di bytes da leggere
     */
    private static void read(SocketChannel server, ByteBuffer buf, int dim) {

        try {
            while (dim > 0) {
                int r = -1;
                r = server.read(buf);
                if (r != -1) dim = dim - r;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Metodo per scrivere nel buffer con il server un numero specificato di bytes
     *
     * @param server il socket usato dal server
     * @param buf il buffer da cui prelevare i bytes da scrivere
     * @param dim il numero di bytes da scrivere
     */
    private static void write(SocketChannel server, ByteBuffer buf, int dim) {

        try {
            while (dim > 0) {
                int w = -1;
                w = server.write(buf);
                if (w != -1) dim = dim - w;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
