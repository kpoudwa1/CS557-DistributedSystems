import java.io.IOException;
import java.util.Random;

public class BranchSender implements Runnable
{
	private Branch branch;
	private long sendInterval;
	
	public BranchSender(Branch branch, long sendInterval)
	{
		this.branch = branch;
		this.sendInterval = sendInterval;
	}
	
	@Override
	public void run()
	{
		try
		{
			while(true)
			{
				//Check for disabling sending when markers are to be sent
				if(branch.isSendingEnabled())
				{
					//Putting the sender thread to sleep before sending
					Thread.currentThread().sleep(sendInterval);
				
					//Getting index for random branch
					Random random = new Random();
					
					//Getting index for random branch
					int index = random.nextInt(branch.getBranchConnections().size());
					
					//Getting the amount
					int min = (int) (branch.getBalance() * 0.01);
					int max = (int) (branch.getBalance() * 0.05);
					
					int transferAmount = random.nextInt((max - min) + 1) + min;
					branch.removeMoney(transferAmount);
					
					Bank.Transfer.Builder transfer = Bank.Transfer.newBuilder();
					transfer.setMoney(transferAmount);
					transfer.setSrcBranch(branch.getBranchName());
					
					Bank.BranchMessage.Builder branchMessage  = Bank.BranchMessage.newBuilder();
					branchMessage.setTransfer(transfer);
					
					//For handling of index
					String receiverBranch;
					if(index != 0)
						receiverBranch = branch.getBranchesList().get(index - 1);
					else
						receiverBranch = branch.getBranchesList().get(index);
					
					branchMessage.build().writeDelimitedTo(branch.getBranchConnections().get(receiverBranch).getOutputStream());
					System.out.println("MoneyTransfer: " + branch.getBranchName() + " -> (" + transferAmount + ") -> " + receiverBranch);
				}
			}
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}