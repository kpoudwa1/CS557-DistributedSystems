import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.nio.file.*;

import javax.activation.MimetypesFileTypeMap;

public class Server implements Runnable
{
	//www folder path
	static String FOLDER_PATH = "./www";
	//Socket for serving the client
	private Socket socket;
	//Host name to be displayed
	private String hostName;
	//Hashmap for maintaining the access time of the file
	static HashMap<String, Integer> filesList;
	
	public Server(Socket socket, String hostName) 
	{
		this.socket = socket;
		this.hostName = hostName;
	}
	
	@Override
	public void run() 
	{
		try
		{
			//Getting the name of the resource
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String resourceName = in.readLine();
			resourceName = resourceName.split("\\s")[1].replace("/", "");

			//Preparing the response
			StringBuffer response = new StringBuffer("");
			
			//Getting the output stream for the socket
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());

			//Check if the file exists or not
			if(filesList.containsKey(resourceName))
			{
				//Setting the HTTP status code
				response.append("HTTP/1.0 200 OK\n");

				//Setting the date
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
				sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
				//response.append("Date: " + cal.getTime() + "\n");
				response.append("Date: " + sdf.format(new Date()) + "\n");
				
				//Setting the server name
				response.append("Server: " + hostName + "\n");
				
				//Setting the Last modified time
				File requestedResource = new File(FOLDER_PATH + System.getProperty("file.separator") + resourceName.toString());
				cal.setTimeInMillis(requestedResource.lastModified());
				//response.append("Last-Modified: " + cal.getTime() + "\n");
				response.append("Last-Modified: " + sdf.format(cal.getTime()) + "\n");
				
				//Setting the Content Type
				response.append("Content-Type: " + new MimetypesFileTypeMap().getContentType(resourceName) + "\n");
				
				//Setting response length
				response.append("Content-Length: " + requestedResource.length() + "\r\n\n");

				//Writing the headers to the socket
				out.writeBytes(response.toString());
				
				//Writing the contents of the file to the socket
				Files.copy(Paths.get(FOLDER_PATH + System.getProperty("file.separator") + resourceName.toString()), out);

				//Flushing the buffer
				out.flush();
				
				//Synchronized block for updating the access time of the file
				synchronized(Server.class) 
				{
					//Update the access time of the file
					filesList.put(resourceName, filesList.get(resourceName) + 1);
					
					//Display the access information
					StringBuffer resourceAccessDetails = new StringBuffer("");
					resourceAccessDetails.append("/" + resourceName + "|");
					resourceAccessDetails.append(socket.getRemoteSocketAddress().toString().split(":")[0].replaceAll("/", "") + "|");
					resourceAccessDetails.append(socket.getRemoteSocketAddress().toString().split(":")[1] + "|");
					resourceAccessDetails.append((filesList.get(resourceName) == null ? 0 : filesList.get(resourceName)));
					
					System.out.println(resourceAccessDetails);
				}
			}
			else
			{
				response.append("HTTP/1.0 404 Not Found\n");
				response.append("Content-Type: text/html\n");
				/*response.append("<html><head><title>Not found</title></head>"
						+ "<body><h2><center>The requested resource "
						+ "<i>" + resourceName + "</i>"
						+ " could not be found</center></h2></body>"
						+ "</html>");*/
				
				//Writing the headers to the socket
				out.writeBytes(response.toString());

				//Flushing the buffer
				out.flush();
			}
			
			//Closing connections
			in.close();
			out.close();
			socket.close();
		}
		catch(Exception e)
		{
			System.out.println("Error in serving the client");
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		//Creating a server socket 
		ServerSocket server = null;
		
		try
		{
			File parentDirectory = new File(FOLDER_PATH);
			
			//Check if the "www" folder exists
			if(!parentDirectory.isDirectory())
			{
				System.out.println("The directory \"www\" does not exists !");
				System.exit(1);
			}
			
			System.out.println("The directory \"www\" exists !");
			
			//Getting the list of files
			filesList = new HashMap<String, Integer>();
			String [] fileList = parentDirectory.list();
			
			//Initialize the access count of all files in the www directory to 0
			for(int i = 0; i < fileList.length; i++)
				filesList.put(fileList[i], 0);
			
			//Opening the socket
			server = new ServerSocket(0);
			System.out.println("Host Name: " + server.getInetAddress().getLocalHost().getHostName());
			System.out.println("Port Number: " + server.getLocalPort());
			System.out.println("Server started and waiting for client");
			
			//Waiting for client and running the server in infinite loop
			while(true)
			{
				//Accepting the client
				Socket serverSocket = server.accept();
				//System.out.println("\nConnected to a client");
				
				//Create a new instance of the class and serve the request
				new Thread(new Server(serverSocket, server.getInetAddress().getLocalHost().getHostName())).start();
			}
		}
		catch(Exception e)
		{
			System.out.println("Error in main");
			e.printStackTrace();
		}
		finally
		{
			try
			{
				//Closing connections
				if(server != null)
					server.close();
			}
			catch(Exception e)
			{
				System.out.println("Error in closing server connection");
				e.printStackTrace();
			}
		}
	}
}