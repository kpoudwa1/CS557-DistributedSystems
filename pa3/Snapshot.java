import java.util.HashMap;

public class Snapshot
{
	private int snapshotId;
	private int balance;
	//private HashMap<String, Integer> channels = new HashMap<String, Integer>();
	
	public int getSnapshotId()
	{
		return snapshotId;
	}
	
	public void setSnapshotId(int snapshotId)
	{
		this.snapshotId = snapshotId;
	}
	
	public int getBalance()
	{
		return balance;
	}
	
	public void setBalance(int balance)
	{
		this.balance = balance;
	}

	/*public synchronized void updateChannels(String key, int amount)
	{
		if(channels.containsKey(key))
		{
			//-99 used for marking a channel as empty
			if(channels.get(key) != -99)
			{
				int amount1 = channels.get(key);
				channels.put(key, amount1 + amount);
			}
		}
		else
			channels.put(key, amount);
	}
	
	public HashMap<String, Integer> getChannels() {
		return channels;
	}*/
}