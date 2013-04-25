import java.io.*;
import java.net.*;

public class PongServer {

	DatagramSocket serverSocket;
	int serverPort;

	public PongServer() {};
	
	void run() {
		try {
			//1. createing a server socket
			serverSocket = new DatagramSocket(1024); //serverPort, unless server, otherwise no need to bind a port
			//2. wait for information
			byte[] receiveData;
            byte[] sendData;
			System.out.println("Waiting for request...");
			
			do {
				try {
					receiveData = new byte[32];
		            sendData = new byte[32];
					//3. READ UDP segment from serverSocket
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					serverSocket.receive(receivePacket);
	                InetAddress IPAddress = receivePacket.getAddress();
	                serverPort = receivePacket.getPort();
	                // Write reply to serverSocket, specifying client address, port number
					responseMessage(sendData, IPAddress, serverPort);
					System.out.println("pong from "+IPAddress.getHostAddress()+"["+serverPort+"]");
				}
				catch(IOException e) {
					e.printStackTrace();
					break;
				}
			} while(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			System.out.println("Closing server.");
			serverSocket.close();
		}
	}
	
	void responseMessage(byte[] sendData, InetAddress IPAddress, int port) {
		try {
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
            serverSocket.send(sendPacket);
		}
		catch (IOException io) {
			io.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		PongServer server = new PongServer();
		server.run();
	}

}
