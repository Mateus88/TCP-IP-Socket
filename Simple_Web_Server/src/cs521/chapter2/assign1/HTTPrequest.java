package cs521.chapter2.assign1;

import java.io.*;
import java.util.*;
import java.net.*;

/**
* This class maintains all the information from an HTTP request
*/
public class HTTPrequest 
{
	private String version;
	private String method;
	private String file;
	private Socket clientSocket;
	private NameValue headerpairs[];
	public DataInputStream inbound;
	
	/**
     * Create an instance of this class
     */
	public HTTPrequest() 
	{
		version = null;
		method = null;
		file = null;
		clientSocket = null;
		inbound = null;
		headerpairs = new NameValue[0];
	}
	
	/**
	 * Add a name/value pair to the internal array
	 */
	public void addNameValue(String name, String value) 
	{
		try {
			NameValue temp[] = new NameValue[headerpairs.length+1];
			System.arraycopy(headerpairs, 0, temp, 0, headerpairs.length);
			temp[headerpairs.length] = new NameValue(name, value);
			headerpairs = temp;
		}
		catch (NullPointerException npe) {
			System.err.println("NullPointerException while adding name-value: " + npe);
		}
	}
	
	/**
     * Renders the contents of the class in String format
     */
	public String toString()
	{
		String s  = method + " " + file + " " + version + "\n";
		for(int i=0;i<headerpairs.length;i++) {
			s+=headerpairs[i]+"\n";
		}
		return s;
	}
	
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public Socket getClientSocket() {
		return clientSocket;
	}

	public void setClientSocket(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	public NameValue[] getHeaderpairs() {
		return headerpairs;
	}

	public void setHeaderpairs(NameValue[] headerpairs) {
		this.headerpairs = headerpairs;
	}
}
