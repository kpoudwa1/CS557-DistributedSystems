import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class BranchReceiver implements Runnable
{
	private Socket socket;
	private Branch branch;
	
	public BranchReceiver(Socket socket, Branch branch)
	{
		this.socket = socket;
		this.branch = branch;
	}
	
	@Override
	public void run()
	{
		try
		{
			while(true)
			{
				if(branch.isInitBranchReceived())
				{
					Bank.BranchMessage branchMessage = Bank.BranchMessage.parseDelimitedFrom(socket.getInputStream());
					//System.out.println("RECEIVER: MESSAGE OUTSIDE CONDITION: #" + branchMessage + "#");
					if(branchMessage != null)
					{
						//Check if the message is Transfer message
						if(branchMessage.hasTransfer())
						{
							int receivedAmount = branchMessage.getTransfer().getMoney();
							System.out.println("MoneyTransfer: " + branch.getBranchName() + " <- (" + receivedAmount + ") <- " + branchMessage.getTransfer().getSrcBranch());
							branch.putMoney(branchMessage.getTransfer().getMoney());
							
							//For handling if marker is received
							/*if(branch.isStartRecording())
							{
								branch.getCurrentSnapshot().updateChannels(branchMessage.getTransfer().getSrcBranch(), branchMessage.getTransfer().getMoney());
							}*/
						}//Check if the message is InitSnapshot message
						else if(branchMessage.hasInitSnapshot())
						{
							//STORE THE LOCAL SNAPSHOT
							//SEND MARKER branchmessages
							System.out.println("Branch " + branch.getBranchName() + " received InitSnapshot");
							
							//Stopping the sender thread
							branch.setSendingEnabled(false);
							
							//Saving the local state
							Snapshot s = new Snapshot();
							s.setSnapshotId(branchMessage.getInitSnapshot().getSnapshotId());
							s.setBalance(branch.getBalance());
							branch.setCurrentSnapshot(s);
							branch.setInitSnapshotReceived(true);
							
							Bank.Marker.Builder marker = Bank.Marker.newBuilder();
							marker.setSnapshotId(branchMessage.getInitSnapshot().getSnapshotId());
							marker.setSrcBranch(branch.getBranchName());
							Bank.BranchMessage.Builder branchMessage1 = Bank.BranchMessage.newBuilder();
							branchMessage1.setMarker(marker);
							
							//Sending maker messages to all branches
							for(int i = 0; i < branch.getBranchesList().size(); i++)
							{
								String key = branch.getBranchesList().get(i);
								branchMessage1.build().writeDelimitedTo(branch.getBranchConnections().get(key).getOutputStream());
							}
							
							//Starting the sender thread
							branch.setSendingEnabled(true);
						}//Check if the message is Marker message
						else if(branchMessage.hasMarker())
						{
							//Check if the current branch is the one which received InitSnapshot
							if(branch.isInitSnapshotReceived())
							{
								//TODO Add code for saving channel state
								branch.setStartRecording(true);
							}//Check if the marker received is the first marker received
							else if(!branch.isMarkerReceived())
							{
								//TODO Mark this channel state as empty
								//TODO Send markers to others
								
								//Stopping the sender thread
								branch.setSendingEnabled(false);
								
								//Saving the local state
								Snapshot s = new Snapshot();
								s.setSnapshotId(branchMessage.getMarker().getSnapshotId());
								s.setBalance(branch.getBalance());
								branch.setCurrentSnapshot(s);
								
								//Setting the channel state as empty
								//branch.getCurrentSnapshot().updateChannels(branchMessage.getMarker().getSrcBranch(), -99);
								
								branch.setMarkerReceived(true);
								branch.setStartRecording(true);
								
								Bank.Marker.Builder marker = Bank.Marker.newBuilder();
								marker.setSnapshotId(branchMessage.getMarker().getSnapshotId());
								marker.setSrcBranch(branch.getBranchName());
								Bank.BranchMessage.Builder branchMessage1 = Bank.BranchMessage.newBuilder();
								branchMessage1.setMarker(marker);
								
								//Sending maker messages to all branches
								for(int i = 0; i < branch.getBranchesList().size(); i++)
								{
									String key = branch.getBranchesList().get(i);
									branchMessage1.build().writeDelimitedTo(branch.getBranchConnections().get(key).getOutputStream());
								}
								
								//Starting the sender thread
								branch.setSendingEnabled(true);
							}//Multiple markers received
							else if(branch.isMarkerReceived())
							{
								//TODO Add code for saving the channel state
								System.out.println("RECEIVER: MARKER: MULTIPLE MARKERS RECEIVED AND WILL ONLY SAVE THE CHANNER STATE");
							}
						}//Check if the message is RetrieveSnapshot message
						else if(branchMessage.hasRetrieveSnapshot())
						{
							//TODO RETURN THE LOCAL STATE
							//TODO RETURN CHANNEL STATE
							
							System.out.println("Branch " + branch.getBranchName() + " received RetrieveSnapshot received");
							
							Bank.ReturnSnapshot.LocalSnapshot.Builder localSnapshot = Bank.ReturnSnapshot.LocalSnapshot.newBuilder();
							localSnapshot.setSnapshotId(branch.getCurrentSnapshot().getSnapshotId());
							localSnapshot.setBalance(branch.getCurrentSnapshot().getBalance());
							
							//TODO add code for Channel states
							/*List<Integer> channelList = new ArrayList<Integer>();
							for(int i = 0; i < branch.getBranchesList().size(); i++)
							{
								System.out.println("#####################BRANCH NAME: " + branch.getBranchesList().get(i));
								String branchId = branch.getBranchesList().get(i);
								
								if(branch.getCurrentSnapshot().getChannels().get(branchId) != null)
									channelList.add(branch.getCurrentSnapshot().getChannels().get(branchId));
								else
									channelList.add(0);
							}
							//Setting the channel state
							localSnapshot.addAllChannelState(channelList);
							
							System.out.println("RECEIVER: THE CHANNEL STATE IS AS FOLLOWS: " + branch.getCurrentSnapshot().getChannels());*/
							branch.setStartRecording(false);
							
							Bank.ReturnSnapshot.Builder returnSnapshot = Bank.ReturnSnapshot.newBuilder();
							returnSnapshot.setLocalSnapshot(localSnapshot);
							
							Bank.BranchMessage.Builder branchMessage1 = Bank.BranchMessage.newBuilder();
							branchMessage1.setReturnSnapshot(returnSnapshot);
							
							branchMessage1.build().writeDelimitedTo(socket.getOutputStream());
						}
					}
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}