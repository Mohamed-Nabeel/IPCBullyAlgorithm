/**
 * 
 */
package MainPackage;


import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * @author mnabil
 *
 */
public class Process implements IProcess 
{
	private static String processID;
	private static Integer registryPort;
	private static Boolean electionState;
	private static String leader;
	private static Random rand;
	private static Integer commPortNumber;
	private static Registry appRegistry;
	private Boolean processAliveStatue;
	private static IProcess globalStub;
	private static Timer runner;
	
	public Process (String PID  , Integer RegPort )
	{
		processID = new String(PID);		//thisNode
		registryPort = RegPort;
		electionState = false;
		rand = new Random();
		commPortNumber = rand.nextInt((2000 - 1000) + 1) + 1000;
		processAliveStatue = true;
		runner = new Timer();
	}
	
	private static void outputLine (String line)
	{
		
		System.out.println(LocalTime.now() + "     " + line + "\n");		//Add timestamp
	}

	
	@Override
	public boolean IsProcessAlive() throws RemoteException 
	{
		if (this.processAliveStatue) 
		{
			//Uncomment the following line to see process status each time it replies
			//outputLine("You, Process : " + processID + " replying status alive.");  
			return true;
		}
		else {return false;}
	}

	@Override
	public void InitiateElection(String InitiatorID) throws RemoteException 
	{
		Boolean higherReplied = false;
		electionState = true;
		
		if (InitiatorID.equals(processID)) 
		{
			outputLine("You, Process : "+ processID + " Initiated this election.");
			for (String processName : appRegistry.list()) 
			{
				if (!processName.equals(processID) && Integer.parseInt(processName) > Integer.parseInt(processID)) 
				{
					try
					{
						IProcess localStub = (IProcess) appRegistry.lookup(processName);
						outputLine ("Sending election request to " + processName);
						localStub.InitiateElection(InitiatorID);
						higherReplied = true;

					} 
					catch (NotBoundException e)
					{
						e.printStackTrace();
					}
				}
			}
			if (!higherReplied) 
			{
				DeclareNewLeader(processID);
			}
			
			
		}
		else
		{
			outputLine("Election Request Received from Process " + InitiatorID);
			ReplyOk(processID,InitiatorID);
		}
		
		

	}
	
	@Override
	public void ReplyOk(String source, String dest) throws RemoteException 
	{
		
		if (!processID.equals(dest))
		{
			try 
			{
				IProcess localStub = (IProcess) appRegistry.lookup(dest);
				outputLine("Sending OK to " + dest);
				localStub.ReplyOk(source, dest);

				// Initiate an election after the reply
				InitiateElection(processID);
			} 
			catch (NotBoundException e) 
			{
				e.printStackTrace();
			}
		} 
		else 
		{
			// an OK reply received
			outputLine(source + " Replied with Ok..");
		}
	}
	
	
	@Override
	public void DeclareNewLeader(String WinnerProcess) throws RemoteException 
	{
		leader = WinnerProcess;
		electionState = false;
		if (WinnerProcess.equals(processID))
		{
			outputLine ("You, Process "+ processID +" are elected as the new leader");
			
			for (String RegisteredProcessID : appRegistry.list()) 
			{
				if (!RegisteredProcessID.equals(processID)) 
				{
					try 
					{
						IProcess localStub = (IProcess) appRegistry.lookup(RegisteredProcessID);
						localStub.DeclareNewLeader(WinnerProcess);

					} 
					catch (NotBoundException e)
					{
						e.printStackTrace();
					}
				}
			}

		} 
		else 
		{
			outputLine("Process " + WinnerProcess + " is elected as the new leader.");
		}

	}
	
	private static void start4Processes () throws IOException, InterruptedException
	{
		Runtime.getRuntime().exec(new String[]{"cmd.exe","/c","start java -classpath bin -Djava.security.policy=java.security.AllPermission -Djava.rmi.server.codebase=file:classDir/ MainPackage.Process 1 1919"});
		
		//Uncomment the following line for a delayed processes starting ( better view for starting flow ).
		//TimeUnit.SECONDS.sleep(1); 
		Runtime.getRuntime().exec(new String[]{"cmd.exe","/c","start java -classpath bin -Djava.security.policy=java.security.AllPermission -Djava.rmi.server.codebase=file:classDir/ MainPackage.Process 2 1919"});
		
		//Uncomment the following line for a delayed processes starting ( better view for starting flow ).
		//TimeUnit.SECONDS.sleep(1); 
		Runtime.getRuntime().exec(new String[]{"cmd.exe","/c","start java -classpath bin -Djava.security.policy=java.security.AllPermission -Djava.rmi.server.codebase=file:classDir/ MainPackage.Process 3 1919"});
		
		//Uncomment the following line for a delayed processes starting ( better view for starting flow ).
		//TimeUnit.SECONDS.sleep(1); 
		Runtime.getRuntime().exec(new String[]{"cmd.exe","/c","start java -classpath bin -Djava.security.policy=java.security.AllPermission -Djava.rmi.server.codebase=file:classDir/ MainPackage.Process 4 1919"});
	}

	
	public static void main(String[] args) 
	{	
		if (args.length == 0)
		{
			try 
			{
				start4Processes();
			} 
			catch (IOException | InterruptedException e)
			{e.printStackTrace();}	
		} 
		else
		{
		
			Process objectProcess = new Process(args[0],Integer.parseInt(args[1]) );
			try
			{
				globalStub = (IProcess) UnicastRemoteObject.exportObject(objectProcess, commPortNumber);
			
				try
				{
					LocateRegistry.createRegistry(registryPort); 
					outputLine("Registry started running on port " + registryPort );
				}
				catch (RemoteException e)
				{outputLine("Registry Already running on port " + registryPort); }
			
				appRegistry = LocateRegistry.getRegistry(1919);
				appRegistry.bind(processID, globalStub);
			
				globalStub.InitiateElection(processID);
			
			}
			catch (RemoteException e) 
			{e.printStackTrace(); } 
			catch (AlreadyBoundException e)
			{e.printStackTrace();}
		
			Runtime.getRuntime().addShutdownHook(new unBindRegistry());
			runner.schedule(new talkToLeaderTask(),500,500);
		}
	}
	
	static class talkToLeaderTask extends TimerTask
	{
		@Override
		public void run() 
		{
			if (!processID.equals(leader) && !electionState) 
			{
				try 
				{
					Registry localRegistry = LocateRegistry.getRegistry(registryPort);
					IProcess localStub = (IProcess) localRegistry.lookup(leader);
					
					//Uncomment the following line to see fully detailed requests from current process to leader
					//System.out.println(LocalTime.now() + "     Process : " + processID + " asking for leader status.");
					
					localStub.IsProcessAlive();
					
					//Uncomment the following line to see fully detailed responses from leader to current process
					//System.out.println(LocalTime.now() + "     leader Process : " + leader + " Replied status Alive.");
				} 
				catch (RemoteException e) 
				{
					e.printStackTrace();
				} 
				catch (NotBoundException e) 
				{
					System.out.println(LocalTime.now() + "     Leader process has been terminated, Initiating a new election");
					try 
					{
						globalStub.InitiateElection(processID);
					} 
					catch (RemoteException ex) 
					{ex.printStackTrace();}
				}
			}
		}
	}
	
	
	
	static class unBindRegistry extends Thread 
	{
		@Override
		public void run() 
		{
			super.run();
			try 
			{
				LocateRegistry.getRegistry(registryPort).unbind(processID);
				System.out.println(LocalTime.now() + "     Process " + processID + " Terminated.");		//Timestamp
			} 
			catch (AccessException e) 
			{e.printStackTrace();} 
			catch (RemoteException e) 
			{e.printStackTrace();} 
			catch (NotBoundException e) 
			{e.printStackTrace();}
		}

	}
	
	
}
