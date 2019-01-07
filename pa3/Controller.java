import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

public class Controller
{
	private static int snapshotId = 0;
	//TODO Revert value
	//private static long sleepDelay = 10000;
	private static long sleepDelay = 30000;
	
	//Function for reading the branches.txt file
	public List<String> readBranchesInfo(String filePath) throws IOException
	{
		List<String> fileContent = null; 
		try
		{
			fileContent = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
		}
		catch(IOException e)
		{
			throw e;
		}
		return fileContent;
	}
	
	public void sendInitBranch(int amount, List<String> branches) throws UnknownHostException, IOException
	{
		Bank.InitBranch.Builder initBranch = Bank.InitBranch.newBuilder();
		
		//Splitting the amount
		int individualAmount = amount / branches.size();
		initBranch.setBalance(individualAmount);
		
		//Looping through the branches
		for(int i = 0; i < branches.size(); i++)
		{
			String [] lineContents = branches.get(i).split("\\s");
			
			Bank.InitBranch.Branch.Builder branch = Bank.InitBranch.Branch.newBuilder(); 
			branch.setName(lineContents[0]);
			branch.setIp(lineContents[1]);
			branch.setPort(Integer.parseInt(lineContents[2]));
			
			initBranch.addAllBranches(branch);
		}
		
		Bank.BranchMessage.Builder branch = Bank.BranchMessage.newBuilder();
		branch.setInitBranch(initBranch);
		
		Socket socket = null;
		for(int i = 0; i < branches.size(); i++)
		{
			try
			{
				socket = new Socket(initBranch.getAllBranches(i).getIp(), initBranch.getAllBranches(i).getPort());
				branch.build().writeDelimitedTo(socket.getOutputStream());
			}
			catch(UnknownHostException e)
			{
				e.printStackTrace();
			}
			catch(IOException e) 
			{
				e.printStackTrace();
			}
			finally
			{
					try
					{
						if(socket != null)
							socket.close();
					} 
					catch(IOException e)
					{
						e.printStackTrace();
					}
			}
		}
	}
	
	public static void main(String[] args)
	{
		try
		{
			if (args.length != 2)
			{
				System.err.println("Error: Incorrect number of arguments. Controller program accepts 2 arguments.");
				System.exit(0);
			}
			
			//Creating controller object
			Controller controller = new Controller();
			
			//Reading the input file
			List<String> branches = controller.readBranchesInfo(args[1]);
			System.out.println("File read successfully");
			
			//Sending the InitBranch message
			controller.sendInitBranch(Integer.parseInt(args[0]), branches);
			System.out.println("InitBranch request sent");
			
			while(true)
			{
				controller.sendInitSnapshot(branches);
				controller.retrieveSnapshot(branches);
			}
		}
		catch(UnknownHostException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	//Function for sending InitSnapshot message
	private void sendInitSnapshot(List<String> branches) throws InterruptedException, IOException
	{
		//Putting the Controller thread to sleep for 10 seconds
		try
		{
			//Thread.currentThread().sleep(10000);
			Thread.currentThread().sleep(sleepDelay);
		}
		catch(InterruptedException e)
		{
			throw e;
		}
		
		System.out.println("Sending InitSnapshot");
		//Code for sending InitSnapshot
		Bank.InitSnapshot.Builder initSnapshot = Bank.InitSnapshot.newBuilder();
		initSnapshot.setSnapshotId(++snapshotId);
		
		//Getting random bank
		Random random = new Random();
		int index = random.nextInt(branches.size());
		
		String [] lineContents = null;
		if(index != 0)
			lineContents = branches.get(index - 1).split("\\s");
		else
			lineContents = branches.get(index).split("\\s");
		
		System.out.println("Sending InitSnapshot to " + lineContents[0]);
		
		Socket socket = null;
		try
		{
			socket = new Socket(lineContents[1], Integer.parseInt(lineContents[2]));
			Bank.BranchMessage.Builder messageBuilder = Bank.BranchMessage.newBuilder();
			messageBuilder.setInitSnapshot(initSnapshot);
			
			messageBuilder.build().writeDelimitedTo(socket.getOutputStream());
			
		}
		catch(NumberFormatException e)
		{
			throw e;
		}
		catch(UnknownHostException e)
		{
			throw e;
		}
		catch(IOException e)
		{
			throw e;
		}
		finally
		{
			try
			{
				if(socket != null)
					socket.close();
			}
			catch(IOException e)
			{
				throw e;
			}
		}
	}
	
	//Function for sending RetrieveSnapshot message
	private void retrieveSnapshot(List<String> branches) throws InterruptedException, UnknownHostException, IOException
	{
		try
		{
			//Thread.currentThread().sleep(10000);
			Thread.currentThread().sleep(sleepDelay);
		}
		catch(InterruptedException e)
		{
			throw e;
		}
		
		System.out.println("Sending RetrieveSnapshot");
		Socket socket = null;
		try
		{
			System.out.println("snapshot_id: " + snapshotId);
			for(int i = 0; i < branches.size(); i++)
			{
				String [] lineContents = branches.get(i).split("\\s");
				/*System.out.println("IP: " + lineContents[1]);
				System.out.println("Port: " + lineContents[2]);*/
				
				Bank.RetrieveSnapshot.Builder retrieveSnapshot = Bank.RetrieveSnapshot.newBuilder();
				retrieveSnapshot.setSnapshotId(snapshotId);
				Bank.BranchMessage.Builder branch = Bank.BranchMessage.newBuilder();
				branch.setRetrieveSnapshot(retrieveSnapshot);
				
				//Sending Retrieve Snapshot to each branch
				socket = new Socket(lineContents[1], Integer.parseInt(lineContents[2]));
				branch.build().writeDelimitedTo(socket.getOutputStream());
				
				Bank.BranchMessage branchR = Bank.BranchMessage.parseDelimitedFrom(socket.getInputStream());
				if(branchR.hasReturnSnapshot())
				{
					//System.out.println("SNAPSHOT RETRIEVED: " + branchR);
					System.out.println(lineContents[0] + " : " + branchR.getReturnSnapshot().getLocalSnapshot().getBalance() + ",");
				}
			}
		}
		catch(NumberFormatException e)
		{
			throw e;
		}
		catch(UnknownHostException e)
		{
			throw e;
		}
		catch(IOException e)
		{
			throw e;
		}
		finally
		{
			try
			{
				if(socket != null)
					socket.close();
			}
			catch(IOException e)
			{
					throw e;
			}
		}
	}
}