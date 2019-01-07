import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BranchInitializer implements Runnable
{
	private Socket socket;
	private Branch branch;
	private long sendInterval;
	
	public BranchInitializer(Socket socket, Branch branch, long sendInterval) 
	{
		this.socket = socket;
		this.branch = branch;
		this.sendInterval = sendInterval;
	}
	
	@Override
	public void run()
	{
		try
		{
			Bank.BranchMessage branchMessage = Bank.BranchMessage.parseDelimitedFrom(socket.getInputStream());
			
			if(branchMessage.hasInitBranch())
			{
				//Setting InitBranch received
				branch.setInitBranchReceived(true);
				
				//Setting the branch balance
				branch.setBalance(branchMessage.getInitBranch().getBalance());
				
				List<Bank.InitBranch.Branch> branches = branchMessage.getInitBranch().getAllBranchesList();
				List<String> branchesStringList = new ArrayList<String>();
				
				//Setting up the half-duplex TCP connections with other branches
				HashMap<String, Socket> branchConnections = new HashMap<String, Socket>();
				
				Socket socket = null;
				for(int i = 0; i < branches.size(); i++)
				{
					if(!branches.get(i).getName().equalsIgnoreCase(branch.getBranchName()))
					{
						socket = new Socket(branches.get(i).getIp(), branches.get(i).getPort());
						
						//Adding the socket to the connections
						branchConnections.put(branches.get(i).getName(), socket);
						branchesStringList.add(branches.get(i).getName());
					}
				}
				
				//Setting the TCP connections object to branch object
				branch.setBranchConnections(branchConnections);
				branch.setBranchesList(branchesStringList);
				
				System.out.println("Branch " + branch.getBranchName() + " initialized with balance " + branch.getBalance());
				System.out.println("-----------------------------------------------------------------------------------------");

				//Starting the money sender thread
				System.out.println("Starting sender thread");
				
				try
				{
					Thread.currentThread().sleep(sendInterval);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				
				
				new Thread(new BranchSender(branch, sendInterval)).start();
			}
		}
		/*catch(InterruptedException e)
		{
			e.printStackTrace();
		}*/
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}