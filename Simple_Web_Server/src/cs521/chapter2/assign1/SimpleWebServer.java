package cs521.chapter2.assign1;

import java.net.*;
import java.util.*;
import java.io.*;

/*
 * Renjie Weng
 * rweng@stevens.edu
 * 2/27/2013
 */
public class SimpleWebServer {
	ServerSocket serverSocket;
	Socket connectionSocket = null;
	HTTPrequest httpRequest;
	int port = 8080;
	
	/*
	 * Question:
	 * 1. How to accept multi-requestes?
	 * 2. How to create thread to accept connections?
	 */
	void run() 
	{
		try {
			serverSocket = new ServerSocket(port,10);
			httpRequest = new HTTPrequest();
		// Accept the comming connection request
			InetAddress thisIp =InetAddress.getLocalHost();
			System.out.println("Waiting for connection :"+port+"...");
			connectionSocket = serverSocket.accept();
		
		// Get the coming request
			httpRequest = getRequest(connectionSocket);
		
		// Response sending
			implementMethod(httpRequest);
		}
		catch(IOException io) {
			io.printStackTrace();
		}
		finally {
			//4. closing connection
			try {
				serverSocket.close();
			}
			catch(IOException io) {
				io.printStackTrace();
			}
		}
	}

	/**
	 * Read an HTTP request into a continuous String
	 * @param client a connected client stream socket
	 * @return a populated HTTPrequest instance
	 * @exception ProtocolException If not a valid HTTP header
	 * @exception IOException
	 */
	public HTTPrequest getRequest(Socket client) 
			throws IOException, ProtocolException
	{
		DataInputStream inbound = null;
		HTTPrequest request = null;
		try {
			// Acquire an input stream for the socket
			inbound = new DataInputStream(client.getInputStream());
			// Read the header into a String
			String reqHdr = readHeader(inbound);
			// Parse the string into an HTTPrequest instance
			request = parseRequestHeader(reqHdr);
			// Add the client socket and inbound stream
			request.setClientSocket(client);
			request.inbound = inbound;
		}
		catch (ProtocolException pe) {
			if (inbound != null) {
				inbound.close();
			}
			throw pe;
		}
		catch(IOException ioe) {
			if(inbound != null) {
				inbound.close();
			}
			throw ioe;
		}
		return request;
	}
	
	/**
     * Assemble an HTTP request header String
     * from the passed DataInputStream.
     * @param is the input stream to use
     * @return a continuous String representing the header
     * @exception ProtocolException If a pre HTTP/1.0 request
     * @exception IOException
     */
    private String readHeader(DataInputStream is) 
    		throws IOException, ProtocolException
    {
    	String command;
    	String line;
    	// Get the first request line
    	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
    	if((command = bufferedReader.readLine()) == null) {
    		command = "";
    	}
    	command +="\n";
    	// Check for HTTP/1.0 signature
    	if(command.indexOf("HTTP/") != -1) {
    		// Retrieve any additional lines
    		while( (line = bufferedReader.readLine()) != null && !line.equals("") ) {
    			command += line +"\n";
    		}
    	}
    	else {
    		throw new ProtocolException("Pre HTTP/1.0 request");
    	}
    	return command;
    }
	
    /**
     * Parsed the passed request String and populate an HTTPrequest.
     * @param reqhdr the HTTP request as a continous String
     * @return a populated HTTPrequest instance
     * @exception ProtocolException If name,value pairs have no ':'
     * @exception IOException
     */
    private HTTPrequest parseRequestHeader(String reqHdr)
    		throws IOException, ProtocolException
    {
    	HTTPrequest req = new HTTPrequest();
    	// Break the request into lines
    	StringTokenizer lines = new StringTokenizer(reqHdr, "\r\n");
    	String currentLine = lines.nextToken();
    	
    	// Process the initial request line into method, file, version Strings
    	StringTokenizer members = new StringTokenizer(currentLine, " \t");
        req.setMethod(members.nextToken());
        req.setFile(members.nextToken());
        if (req.getFile().equals("/")) {
        	req.setFile("/index.html");
        }
        req.setVersion(members.nextToken());
    	
        // Process additional lines into name/value pairs
        while ( lines.hasMoreTokens() )
        {
            String line = lines.nextToken();
            // Search for separating character
            int slice = line.indexOf(':');
            // Error if no separating character
            if ( slice == -1 )
            {
                throw new ProtocolException("Invalid HTTP header: " + line);
            }
            else
            {
                // Separate at the slice character into name, value
                String name = line.substring(0,slice).trim();
                String value = line.substring(slice + 1).trim();
                req.addNameValue(name, value);
            }
        }
    	return req;
    }
    
    /**
     * Respond to an HTTP request
     * @param request the HTTP request to respond to.
     * @exception ProtocolException If unimplemented request method
     */
    private void implementMethod(HTTPrequest request)
    		throws ProtocolException
    {
        try
        {
            if ( (request.getMethod().equals("GET") ) ||
                 (request.getMethod().equals("HEAD")) )
                serviceGetRequest(request);
            else
            {
                throw new ProtocolException("Unimplemented method: " + request.getMethod());
            }
        }
        catch (ProtocolException pe)
        {
            sendNegativeResponse(request);
            throw pe;
        }
    }
    
    /**
     * Send a response header for the file and the file itself.
     * Handles GET and HEAD request methods.
     * @param request the HTTP request to respond to
     */
    private void serviceGetRequest(HTTPrequest request)
    		throws ProtocolException
    {
        try
        {
            if (request.getFile().indexOf("..") != -1) {
                throw new ProtocolException("Relative paths not supported");
            }
            String currentDir = new File(".").getCanonicalPath();
            String fileToGet = currentDir + request.getFile();
            System.out.println("Requesting file: " + fileToGet +" .");
            FileInputStream inFile = new FileInputStream(fileToGet);
            sendFile(request, inFile);
            inFile.close();
        }
        catch (FileNotFoundException fnf)
        {
            sendNegativeResponse(request);
        }
        catch (ProtocolException pe)
        {
            throw pe;
        }
        catch (IOException ioe)
        {
            System.out.println("IOException: Unknown file length: " + ioe);
            sendNegativeResponse(request);
        }
    }

    /**
     * Send a negative (404 NOT FOUND) response
     * @param request the HTTP request to respond to.
     */
    private void sendNegativeResponse(HTTPrequest request)
    {
        DataOutputStream outbound = null;

        try
        {
            // Acquire the output stream
            outbound = new DataOutputStream(request.getClientSocket().getOutputStream());

            // Write the negative response header
            outbound.writeBytes("HTTP/1.0 ");
            outbound.writeBytes("404 NOT_FOUND\r\n");
            outbound.writeBytes("\r\n");
            outbound.writeBytes("<body>");            
            outbound.writeBytes("<center><div style=\"width:500pt;height:100pt;display:table-cell;vertical-align:middle;font-family:Verdana;color:red;\">");
            outbound.writeBytes("<a>CANNOT FIND YOUR REQUESTED FILE</a>");
            outbound.writeBytes("</div></center>");
            outbound.writeBytes("</body>\r\n");

            // Clean up
            outbound.close();
            request.inbound.close();
        }
        catch (IOException ioe)
        {
            System.out.println("IOException while sending -rsp: " + ioe);
        }
    }
    
    /**
     * Send the passed file
     * @param request the HTTP request instance
     * @param inFile the opened input file stream to send\
     */
    private void sendFile(HTTPrequest request, FileInputStream inFile)
    {
        DataOutputStream outbound = null;

        try
        {
            // Acquire an output stream
            outbound = new DataOutputStream(
                request.getClientSocket().getOutputStream());

            // Send the response header
            outbound.writeBytes("HTTP/1.0 200 OK\r\n");
            outbound.writeBytes("Content-type: text/html\r\n");
            outbound.writeBytes("Content-Length: " + inFile.available() + "\r\n");
            outbound.writeBytes("\r\n");

            // Added to allow Netscape to process header properly
            // This is needed because the close is not recognized
            try {
				Thread.currentThread().sleep(500);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}

            // If not a HEAD request, send the file body.
            // HEAD requests solicit only a header response.
            if (!request.getMethod().equals("HEAD"))
            {
                byte dataBody[] = new byte[1024];
                int cnt;
                while ((cnt = inFile.read(dataBody)) != -1)
                    outbound.write(dataBody, 0, cnt);
            }

            // Clean up
            outbound.flush();
            outbound.close();
            request.inbound.close();
        }
        catch (IOException ioe)
        {
            System.out.println("IOException while sending file: " + ioe);
        }
    }
	
    /**
     * How to run:
     * ~/workspace/Simple_Web_Server$ 
     * java -classpath ./bin  cs521.chapter2.assign1.SimpleWebServer
     */
	public static void main(String[] args) {
		SimpleWebServer server = new SimpleWebServer();
		while(true) {
			server.run();
		}
	}

}






















