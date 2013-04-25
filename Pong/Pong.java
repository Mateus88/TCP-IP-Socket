import java.io.*;
import java.net.*;
import java.util.*;

public class Pong {

	DatagramSocket clientSocket;
	int serverPort;
	long sendTime; // used as minTime, after loop
	long receiveTime; // used as maxTime, after loop
	long [] timeArray;
	static String hostname;
	Pong(){};
	
	void run(String hostname)
	{
		serverPort = 1024;
		try 
		{
			// Create socket: clientSocket = socket(AF_INET,SOCK_DGRAM)
			clientSocket = new DatagramSocket(); // port will be selected randomly 
			try 
			{
				InetAddress IPAddress = InetAddress.getByName(hostname);
				System.out.println();
				System.out.println("Ponging "+hostname+" ["+IPAddress.getHostAddress()+"] with 32 bytes of data:");
				byte[] sendData;
				byte[] receiveData;
				timeArray = new long[10];
				int lostPack = 0;
				for (int i=0;i<10;i++) 
				{
					sendData = new byte[32];
					receiveData = new byte[32];
					
					// Create datagram with serverIP and port=x; send datagram via clientSocket
					sendTime = sendMessage(sendData, IPAddress, serverPort);
					
					// Read datagram from clientSocket
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					try{
						clientSocket.setSoTimeout(4000);
						clientSocket.receive(receivePacket);
					}
					catch(SocketTimeoutException ste) {
						System.out.println("Request timed out.");
						timeArray[i] = -1;
						lostPack++;
						continue;
					}
					System.out.print("Reply form "+IPAddress.getHostAddress()+": bytes=32 time=");
					receiveTime = System.nanoTime();
					timeArray[i] = receiveTime-sendTime;
					long milliSec=timeArray[i]/1000000;
					long microSec=(timeArray[i]%1000000)/1000;
					System.out.println(milliSec+" ms." + microSec);
					try {
						Thread.currentThread().sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				// Statistics after loop
				showStatics(timeArray, lostPack, IPAddress.getHostAddress());
			}
			catch(UnknownHostException uhe) {
				System.err.println(hostname+": Name or service not known");
				System.exit(1);
			}
			
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		finally {
			clientSocket.close();
		}
	}
	
	long sendMessage(byte[] sendData, InetAddress IPAddress, int port)
	{
		long startTime = 0;
		try {
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
			startTime = System.nanoTime();
			clientSocket.send(sendPacket);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return startTime;
	}
	
	void showStatics(long[] timeArray, int lostPack, String ip )
	{
		sendTime = timeArray[0]; // minTime
		receiveTime = timeArray[0]; // maxTime
		int receivedPack = 10 - lostPack;
		if(receivedPack>0)
		{
			for(int j=1;j<10;j++) {
				if(timeArray[j]!=-1) {
					timeArray[0]+=timeArray[j]; // compute summary, exclude lost packets;
					if(sendTime > timeArray[j]) {
						sendTime = timeArray[j];
					}
					if(receiveTime < timeArray[j]) {
						receiveTime = timeArray[j];
					}
				}
			}
			timeArray[1] = (timeArray[0]/receivedPack)/1000000; // average milli-seconds
			timeArray[2] = (timeArray[0]/receivedPack)/1000; // average micro-seconds
			System.out.println();
			System.out.println("Pong statistics for "+ ip +":");
			System.out.print("    Packets: Sent = 10,");
			System.out.print(" Received = " + receivedPack +"," );
			System.out.print(" Lost = " + lostPack);
			System.out.println(" (" + (lostPack*10) +"% loss),");
			System.out.println("Approximate round trip times in milli-seconds.micro-seconds:");
			System.out.print("    Minimum = " + sendTime/1000000 + " ms." + sendTime/1000 + "," );
			System.out.print(" Maximum = " + receiveTime/1000000 + " ms." + receiveTime/1000 + "," );
			System.out.print(" Average = " + timeArray[1] + " ms." + timeArray[2] );
			System.out.println();
		}
	}
	
	public static void main(String[] args) {
		Pong client = new Pong();
		if (args.length == 1) {
		    try {
		        hostname = String.valueOf(args[0]);
		        client.run(hostname);
		    } catch (NumberFormatException e) {
		        System.err.println("Argument" + " must be an IP address");
		        System.exit(1);
		    }
		} else {
			System.out.println("Bad command fomulation, please use: java Pong www.google.com.");
			System.exit(0);
		}
		
//		// debug
//		client.hostname = "localhost";
//		client.run(hostname);
	}

}
