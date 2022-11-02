package MainPackage;


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IProcess extends Remote
{
	
	public boolean IsProcessAlive() throws RemoteException;

	public void InitiateElection (String nodeId) throws RemoteException;

	public void ReplyOk(String where, String to) throws RemoteException;

	public void DeclareNewLeader(String node) throws RemoteException;
	
	
}
