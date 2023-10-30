package Food_Delivery_Service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Vector;

public class ServerThread extends Thread{
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private boolean available;
	private static boolean done;
	private boolean waitingForInput;
	Socket s;
	Server S;
	Vector<Order> deliveries = new Vector<>();
	
	public ServerThread(Socket s, Server S) {
		try {
			this.available = true;
			this.s = s;
			this.S = S;
			ServerThread.done = false;
			this.waitingForInput = false;
			// "Java ObjectInputStream/ObjectOutPutStream documentation" documentation site (referenced) 29 Oct. 2023
			// https://docs.oracle.com/javase/8/docs/api/java/io/ObjectInputStream.html
			this.oos = new ObjectOutputStream(this.s.getOutputStream());
			this.ois = new ObjectInputStream(this.s.getInputStream());
			this.start();
		} catch(IOException ioe) {
			System.out.println("ioe in serverthread constructor: " + ioe.getMessage());
		}
	}
	
	public static void endDriver() {
		ServerThread.done = true;
	}
	
	public static boolean isDone() {
		return ServerThread.done;
	}
	
	public void sendMessage(String message) {
		try {
			this.oos.writeUTF(message);
			this.oos.flush();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public void assignOrders(Vector<Order> d) {
		this.deliveries = new Vector<>(d);
		try {
			// "Java ObjectInputStream/ObjectOutPutStream documentation" documentation site (referenced) 29 Oct. 2023
			// https://docs.oracle.com/javase/8/docs/api/java/io/ObjectInputStream.html
			oos.writeUTF("1");
			oos.flush();
			oos.writeObject(d);
			oos.flush();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public boolean isAvailable() {
		return this.available;
	}
	
	public void setAv(boolean a) {
		this.available = a;
	}
	
	public void waitInput() {
		this.waitingForInput = true;
	}
	
	public void run() {	
		try {
			while (!ServerThread.done) {
				if (this.waitingForInput) {
					String cmd = ois.readUTF();
					S.q.add(this);
					if (cmd.equals("3")) {
						this.available = true;
					}
					else if (cmd.equals("4")){
						long temp = ois.readLong();
						if (temp > S.t) {
							S.t = temp;
						}
						S.timed++;
						this.waitingForInput = false;
					}
				}
				else {
					Thread.yield();
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
