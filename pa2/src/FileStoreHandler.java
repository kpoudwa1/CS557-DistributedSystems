import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;


public class FileStoreHandler implements FileStore.Iface
{
	private List<RFile> files;
	private int port;
	private NodeID node; 
	private List<NodeID> nodes;
	
	public FileStoreHandler(int port) throws SystemException, UnknownHostException
	{
		//Init member variables
		files = new ArrayList<RFile>();
		this.port = port;
		node = new NodeID();
		
		try
		{
			node.setIp(InetAddress.getLocalHost().getHostAddress());
			node.setPort(port);
			node.setId(generateHash(node.getIp() + ":" + this.port));
			
			System.out.println("\nServer started on, " + node.getIp() + ":" + node.getPort());
			System.out.println("Node Id hash: " + node.getId());
		}
		catch(UnknownHostException e) 
		{
			e.printStackTrace();
			
			SystemException s = new SystemException();
			s.setMessage(e.getMessage());
			throw s;
		}
		catch(SystemException e)
		{
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	public void writeFile(RFile rFile) throws SystemException, TException
	{
		//Checking if the file object is not null
		if(rFile != null)
		{
			//Getting the hash of the file name
			String fileNameHash = generateHash(rFile.getMeta().getFilename().trim());
			System.out.println("File name hash: " + fileNameHash);
			
			//Find the owner for the file
			NodeID successor = findSucc(fileNameHash);
			
			//Check if the successor for the file and the current node are the same
			System.out.println("File's successor: " + successor.getId());
			if(successor.getId().compareTo(node.getId()) == 0)
			{
				System.out.println("The current server is the file's successor");
				
				//Checking if the file exists or not
				if(!checkIfFileExists(rFile.getMeta().getFilename().trim()))
				{
					System.out.println("The file is a new file");
					
					//Create file object
					RFile newFile = new RFile();
					
					//Set the file metadata
					RFileMetadata meta = new RFileMetadata();
					meta.setFilename(rFile.getMeta().getFilename().trim());
					meta.setVersion(0);
					
					//Set the file content
					newFile.setContent(rFile.getContent());
					
					//Set the content hash & meta
					meta.setContentHash(generateHash(rFile.getContent()));
					newFile.setMeta(meta);
					
					//Add the file to the list
					files.add(newFile);
					
					System.out.println("==========================================================");
					System.out.println("File Name: " + newFile.getMeta().getFilename());
					System.out.println("File Version: " + newFile.getMeta().getVersion());
					//System.out.println("File Content: " + newFile.getContent());
					System.out.println("==========================================================");
					
					System.out.println("File added successfully !!!!");
				}
				else//Update the file
				{
					System.out.println("File already exists");
					updateFile(rFile);
					System.out.println("File updated successfully");
				}
			}
			else
			{
				System.out.println("The server is not the file's successor !");
				
				SystemException s = new SystemException();
				s.setMessage("The server is not the file's successor");
				throw s;
			}
		}
		else
		{
			System.out.println("No file provided !");
			
			SystemException s = new SystemException();
			s.setMessage("No file provided");
			throw s;
		}
	}

	@Override
	public RFile readFile(String filename) throws SystemException, TException
	{
		//Checking if the file name passed is not null or empty string
		if(filename != null && filename.trim().length() > 0)
		{
			//Getting the hash of the file name
			String fileNameHash = generateHash(filename.trim());
			System.out.println("File name hash: " + fileNameHash);
			
			//Find the owner for the file
			NodeID successor = findSucc(fileNameHash);
			System.out.println("File's successor: " + successor.getId());
			
			//Check if the successor for the file and the current node are the same
			if(successor.getId().compareTo(node.getId()) == 0)
			{
				System.out.println("The current server is the file's successor");
				
				//Checking if the file exists or not
				if(checkIfFileExists(filename))
				{
					System.out.println("File exists");
					return getFile(filename);
				}
				else
				{
					System.out.println("File does not exists !");
					
					SystemException s = new SystemException();
					s.setMessage("File does not exists");
					throw s;
				}
			}
			else
			{
				System.out.println("The server is not the file's successor !");
				
				SystemException s = new SystemException();
				s.setMessage("The server is not the file's successor");
				throw s;
			}
		}
		else
		{
			System.out.println("File name shouldn't be blank !");
			
			SystemException s = new SystemException();
			s.setMessage("File name shouldn't be blank");
			throw s;
		}
	}

	@Override
	public void setFingertable(List<NodeID> node_list) throws TException 
	{
		//Setting the finger table
		nodes = new ArrayList<NodeID>(node_list);
		System.out.println("Finger table set");
	}

	@Override
	public NodeID findSucc(String key) throws SystemException, TException
	{
		NodeID successor = null;
		
		//If the node and the key to be searched are same
		if(node.getId().compareTo(key) == 0)
		{
			successor = node;
		}
		else
		{
			NodeID predecessor = findPred(key);
			
			//Check if the predecessor is the same as the current node
			if(predecessor.getId().compareTo(node.getId()) == 0)
				successor = getNodeSucc();
			else
			{
				//Getting the successor
				if(predecessor != null)
				{
					TTransport transport = null;
					try
					{
						transport = new TSocket(predecessor.getIp(), predecessor.getPort());
						transport.open();
						
						TProtocol protocol = new  TBinaryProtocol(transport);
						FileStore.Client client = new FileStore.Client(protocol);
						
						successor = client.getNodeSucc(); 
					}
					catch(TException e)
					{
						e.printStackTrace();
						
						SystemException s = new SystemException();
						s.setMessage(e.getMessage());
						throw s;
					}
					finally
					{
						//Closing transport
						if(transport != null)
							transport.close();
					}
					
				}
			}
		}
		return successor;
	}

	@Override
	public NodeID findPred(String key) throws SystemException, TException 
	{
		NodeID n1 = node;
		n1 = node;
		
		while(!isElementOf(key, n1))
		{
			n1 = closestPrecedingFinger(key, n1);
			
			//Code for making RPC call
			if(n1 != null)
			{
				TTransport transport = null;
				try
				{
					transport = new TSocket(n1.getIp(), n1.getPort());
					transport.open();
					
					TProtocol protocol = new  TBinaryProtocol(transport);
					FileStore.Client client = new FileStore.Client(protocol);
					
					n1 = client.findPred(key);
				}
				catch(TException e)
				{
					e.printStackTrace();
					
					SystemException s = new SystemException();
					s.setMessage(e.getMessage());
					throw s;
				}
				finally
				{
					//Closing transport
					if(transport != null)
						transport.close();
				}
			}
			
			//Check if the key is between the node and it's successor, then breaking the loop
			if(isElementOf(key, n1))
				break;
		}
		
		return n1;
	}

	@Override
	public NodeID getNodeSucc() throws SystemException, TException 
	{
		//Check if the Finger table has been initialized or not
		if(nodes != null && nodes.size() > 0)
			return nodes.get(0);
		else
		{
			System.out.println("No fingertable exists for the current node !");
			
			SystemException s = new SystemException();
			s.setMessage("No fingertable exists for the current node");
			throw s;
		}
	}
	
	//Function for checking if id is not in n1 and successor
	private boolean isElementOf(String key, NodeID n1) throws SystemException, TException
	{
		NodeID successor = null;
		
		try 
		{
			successor = rpcGetNodeSucc(n1);
		}
		catch(SystemException e)
		{
			e.printStackTrace();
			throw e;
		}
		catch (TException e)
		{
			SystemException s = new SystemException();
			s.setMessage(e.getMessage());
			throw s;
		}
		
		if(successor != null)
		{
			//Check if current node is less than the successor
			if(n1.getId().compareTo(successor.getId()) < 0)
			{
				//Check if the key is between the node and it's successor
				if((n1.getId().compareTo(key) < 0) && (successor.getId().compareTo(key) > 0))
				{
					return true;
				}
			}
			else//Check if current node is greater than the successor
			{
				//Check if the key is between the node and it's successor
				//In this case the key can be before the the chord loop (i.e.) value less than 256
				// or can be after the start of the chord loop
				if((n1.getId().compareTo(key) < 0) && (successor.getId().compareTo(key) < 0) || (n1.getId().compareTo(key) > 0) && (successor.getId().compareTo(key) > 0))
				{
					return true;
				}
			}
		} 
		return false;
	}

	//Function for making a RPC call for getting the Node Successor
	private NodeID rpcGetNodeSucc(NodeID n1) throws SystemException, TException
	{
		NodeID successor = null;
		TTransport transport = null;
		
		//If the current node is n1, then successor can be found by calling getNodeSucc() method
		if(n1.getId().compareTo(node.getId()) == 0)
		{
			try
			{
				return getNodeSucc();
			}
			catch (TException e)
			{
				e.printStackTrace();
				
				SystemException s = new SystemException();
				s.setMessage(e.getMessage());
				throw s;
			}
		}
		
		//If the current node is not n1, then successor can be found by calling getNodeSucc() method from RPC
		try
		{
			transport = new TSocket(n1.getIp(), n1.getPort());
			transport.open();
			
			TProtocol protocol = new  TBinaryProtocol(transport);
			FileStore.Client client = new FileStore.Client(protocol);
			
			successor = client.getNodeSucc();
		}
		catch(TException e)
		{
			e.printStackTrace();
			
			SystemException s = new SystemException();
			s.setMessage(e.getMessage());
			throw s;
		}
		finally
		{
			//Closing transport
			if(transport != null)
				transport.close();
		}
		return successor;
	}
	
	//Function for finding the closest preceding finger
	private NodeID closestPrecedingFinger(String key, NodeID node)
	{
		for(int i = nodes.size() - 1; i >= 0; i--)
		{
			if(node.getId().compareTo(key) < 0)
			{
				if((node.getId().compareTo(nodes.get(i).getId()) < 0) && (key.compareTo(nodes.get(i).getId()) > 0))
				{
					return nodes.get(i);
				}
			}
			else
			{
				if((node.getId().compareTo(nodes.get(i).getId()) < 0 && key.compareTo(nodes.get(i).getId()) < 0))
				{
					return nodes.get(i);
				}
				else if(node.getId().compareTo(nodes.get(i).getId()) > 0 && key.compareTo(nodes.get(i).getId()) > 0)
				{
					return nodes.get(i);
				}
			}
		}
		return node;
	}
	
	//Function for checking if a file exists or not
	private boolean checkIfFileExists(String name)
	{
		boolean fileExists = false;
		for(int i = 0; i < files.size(); i++)
		{
			if(files.get(i).getMeta().getFilename().equals(name))
			{
				fileExists = true;
				break;
			}
		}
		
		return fileExists;
	}
	
	//Function for updating the file contents
	private void updateFile(RFile rFile) throws SystemException
	{
		try
		{
			//Search the file and update the contents
			for(int i = 0; i < files.size(); i++)
			{
				if(files.get(i).getMeta().getFilename().equalsIgnoreCase(rFile.getMeta().getFilename().trim()))
				{
					//Set the file metadata
					int version = files.get(i).getMeta().getVersion();
					files.get(i).getMeta().setVersion(++version);
					
					//Set the file content
					files.get(i).setContent(rFile.getContent());
					
					//Set the content hash
					files.get(i).getMeta().setContentHash(generateHash(rFile.getContent()));
					
					System.out.println("==========================================================");
					System.out.println("File Name: " + files.get(i).getMeta().getFilename());
					System.out.println("File Version: " + files.get(i).getMeta().getVersion());
					//System.out.println("File Content: " + files.get(i).getContent());
					System.out.println("==========================================================");
					
					break;
				}
			}
		}
		catch(SystemException e)
		{
			e.printStackTrace();
			throw e;
		}
	}
	
	//Function for returning file
	private RFile getFile(String filename)
	{
		RFile file = null;
		for(int i = 0; i < files.size(); i++)
		{
			if(files.get(i).getMeta().getFilename().equalsIgnoreCase(filename))
			{
				file = files.get(i);
			}
		}
		return file;
	}
	
	//Function for generating hash
	private String generateHash(String data) throws SystemException
	{
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] endcodedData = digest.digest(data.getBytes("UTF-8"));
			StringBuffer hexData = new StringBuffer();
			
			for(int i = 0; i < endcodedData.length; i++)
			{
				String hexChar = Integer.toHexString(0xff & endcodedData[i]);
				
				if(hexChar.length() == 1)
					hexData.append('0');
				
				hexData.append(hexChar);
			}
			
			return hexData.toString();
		}
		catch(NoSuchAlgorithmException e)
		{
			e.printStackTrace();
			
			SystemException s = new SystemException();
			s.setMessage(e.getMessage());
			throw s;
		} catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			
			SystemException s = new SystemException();
			s.setMessage(e.getMessage());
			throw s;
		}
	}
}