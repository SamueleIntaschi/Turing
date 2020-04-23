import java.rmi.Remote;
import java.rmi.RemoteException;


public interface ServerInterface extends Remote {

    int register(String user, String password) throws RemoteException;

}
