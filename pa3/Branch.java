import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

public class Branch 
{
	private String branchName;
	private int port;
	private boolean initBranchReceived;
	private int balance;
	private HashMap<String, Socket> branchConnections;
	private List<String> branchesList;
	private Snapshot currentSnapshot;
	private boolean initSnapshotReceived;
	private boolean markerReceived;
	private boolean sendingEnabled;
	private boolean startRecording;

	public Branch(String branchName, int port)
	{
		this.branchName = branchName;
		this.port = port;
		sendingEnabled = true;
	}
	
	public static void main(String[] args) 
	{
		if (args.length != 3)
		{
			System.err.println("Error: Incorrect number of arguments. Branch program accepts 3 arguments.");
			System.exit(0);
		}
		
		//Create branch object
		Branch branch = new Branch(args[0], Integer.parseInt(args[1]));
		ServerSocket server = null;
		try
		{
			server = new ServerSocket(Integer.parseInt(args[1]));
			System.out.println("Branch: " + branch.branchName + " started on port: " + branch.port);
			System.out.println("Branch HostName: " + InetAddress.getLocalHost().getHostAddress());
			
			Socket serverSocket = null;
			while(true)
			{
				serverSocket = server.accept();
				
				//Check if InitBranch message has been received
				if(!branch.initBranchReceived)
				{
					new Thread(new BranchInitializer(serverSocket, branch, Long.parseLong(args[2]))).start();
				}
				else//Receiver threads
				{
					new Thread(new BranchReceiver(serverSocket, branch)).start();
				}
			}
		}
		catch(NumberFormatException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			System.out.println("BRANCH: AFTER WHILE");
				try
				{
					if(server != null)
						server.close();
				} 
				catch(IOException e)
				{
					e.printStackTrace();
				}
		}
	}

	public String getBranchName()
	{
		return branchName;
	}

	public void setBranchName(String branchName)
	{
		this.branchName = branchName;
	}

	public boolean isInitBranchReceived()
	{
		return initBranchReceived;
	}

	public void setInitBranchReceived(boolean initBranchReceived)
	{
		this.initBranchReceived = initBranchReceived;
	}

	public int getBalance()
	{
		return balance;
	}

	public void setBalance(int balance)
	{
		this.balance = balance;
	}

	public HashMap<String, Socket> getBranchConnections()
	{
		return branchConnections;
	}

	public void setBranchConnections(HashMap<String, Socket> branchConnections)
	{
		this.branchConnections = branchConnections;
	}

	public List<String> getBranchesList()
	{
		return branchesList;
	}

	public void setBranchesList(List<String> branchesList)
	{
		this.branchesList = branchesList;
	}
	
	public synchronized void putMoney(int amount)
	{
		balance += amount;
	}

	public synchronized void removeMoney(int amount)
	{
		balance -= amount;
	}

	public Snapshot getCurrentSnapshot()
	{
		return currentSnapshot;
	}

	public void setCurrentSnapshot(Snapshot currentSnapshot)
	{
		this.currentSnapshot = currentSnapshot;
	}

	public boolean isInitSnapshotReceived()
	{
		return initSnapshotReceived;
	}

	public void setInitSnapshotReceived(boolean initSnapshotReceived)
	{
		this.initSnapshotReceived = initSnapshotReceived;
	}

	public boolean isMarkerReceived()
	{
		return markerReceived;
	}

	public void setMarkerReceived(boolean markerReceived)
	{
		this.markerReceived = markerReceived;
	}

	public boolean isSendingEnabled()
	{
		return sendingEnabled;
	}

	public synchronized void setSendingEnabled(boolean sendingEnabled)
	{
		this.sendingEnabled = sendingEnabled;
	}

	public boolean isStartRecording()
	{
		return startRecording;
	}

	public void setStartRecording(boolean startRecording)
	{
		this.startRecording = startRecording;
	}
	
}