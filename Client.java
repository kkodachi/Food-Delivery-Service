package Food_Delivery_Service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.Vector;

public class Client extends Thread{
	Vector<Order> deliveries = new Vector<>();
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private boolean sample;
	private long starttime;
	private long now;
	private double startlat;
	private double startlon;
	private double lat;
	private double lon;
	String hostname;
	int port;
	Socket s;

	public Client(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
		this.s = null;
		this.ois = null;
		this.sample = true;
		this.oos = null;
		try {
			// cite
			this.s = new Socket(this.hostname, this.port);
			this.oos = new ObjectOutputStream(this.s.getOutputStream());
			this.ois = new ObjectInputStream(this.s.getInputStream());
			this.start();
		} catch(IOException ioe) {
			System.out.println("ioe in ChatClient constructor: " + ioe.getMessage());
		}
	}
	
	public static void main(String[] args) {
		String hostname = "";
		int port = 0;
		try {
			Scanner in  = new Scanner(System.in);
			System.out.println("Welcome to Food Delivery Service!");
			System.out.print("Enter the server hostname: ");
			hostname = in.nextLine();
			System.out.println();
			System.out.print("Enter the server port: ");
			port = Integer.parseInt(in.nextLine());
			System.out.println();
		} catch (InputMismatchException ime) {
			System.out.print("Port need to be of type int. \n");
		} catch (NullPointerException npe) {
			System.out.print("Port need to be of type int. \n");
		} catch (NumberFormatException nfe) {
			System.out.print("Port need to be of type int. \n");
		}
		Client c = new Client(hostname, port);
	}
	

	public void run() {
		try {
			// cite
			String cmd = "";
			while (true) {
				cmd = ois.readUTF();
				if (cmd.equals("0")) {
					cmd = ois.readUTF();
					this.startlat = Double.parseDouble(cmd);
					this.lat = this.startlat;
					cmd = ois.readUTF();
					this.startlon = Double.parseDouble(cmd);
					this.lon = this.startlon;
					System.out.println("All drivers have arrived. Starting Service.");
					System.out.println();
					break;
				}
				System.out.println(this.waitDriver(cmd));
				System.out.println();
			}
			while (true) {
				cmd = ois.readUTF();
				if (cmd.equals("0")) {
					break;
				}
				else if (cmd.equals("1")){
					// "Java ObjectInputStream/ObjectOutPutStream documentation" documentation site (referenced) 29 Oct. 2023
					// https://docs.oracle.com/javase/8/docs/api/java/io/ObjectInputStream.html
					this.deliveries = (Vector<Order>) ois.readObject();
					if (this.sample) {
						this.starttime = System.currentTimeMillis();
						this.sample = false;
					}
					this.startDeliveries();
					while (this.deliveries.size() != 0) {
						this.sortDist();
						this.deliver();
						this.printOrders();
						this.ordersLeft();
					}
					this.now = System.currentTimeMillis();
					System.out.println(this.returnHQ());
					oos.writeUTF("3");
					oos.flush();
					try {
						Thread.sleep((int) (this.calc(this.lat,this.lon,this.startlat,this.startlon) * 1000)); // return to HQ
					} catch(InterruptedException ie) {
						System.out.println("Return to HQ interrupted");
					}
					this.lat = this.startlat;
					this.lon = this.startlon;
					this.now = System.currentTimeMillis();
					System.out.println(this.atHQ());
					System.out.println();
				}
			}
			oos.writeUTF("4");
			oos.flush();
			this.now = System.currentTimeMillis();
			oos.writeLong(this.now - this.starttime);
			oos.flush();
			long t = Long.parseLong(ois.readUTF());
			System.out.println(this.ordersDone(t));
			// catch
		} catch (IOException ioe) {
			System.out.println("ioe in Client.run(): " + ioe.getMessage());
			ioe.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Exception ClassNotFound: " );
			e.printStackTrace();
		}
	}
	
	public void startDeliveries() {
		for (Order o: this.deliveries) {
			System.out.println(this.startOrder(o));
		}
	}
	
	public void ordersLeft() {
		for (Order o: this.deliveries) {
			System.out.println(this.contOrders(o));
		}
	}
	
	public void sortDist() {
		// "java: Arrays.sort() with lambda expression" prompt (referenced, nothing copied) 29 Oct. 2023
		// https://stackoverflow.com/questions/21970719/java-arrays-sort-with-lambda-expression
		Collections.sort(this.deliveries, (o1,o2) -> Double.compare(this.calc(this.lat,this.lon,o1.getLat(),o1.getLon()),this.calc(this.lat,this.lon,o2.getLat(),o2.getLon())));
	}
	
	public void deliver() {
		if (this.deliveries == null) {
			return;
		}
		if (this.deliveries.size() != 0) {
			try {
				double templat = this.deliveries.get(0).getLat();
				double templon = this.deliveries.get(0).getLon();
				double sleep = this.calc(this.lat,this.lon,templat,templon);
				Thread.sleep((int)(sleep * 1000));
				this.now = System.currentTimeMillis();
				this.lat = templat;
				this.lon = templon;
			} catch(InterruptedException ie) {
				System.out.println("Order Interrupted");
			}
			
		}
	}
	
	public void printOrders() {
		Vector<Order> temp = new Vector<>();
		String name = this.deliveries.get(0).getRName();
		for (Order o: this.deliveries) {
			if (name.equals(o.getRName())) {
				System.out.println(this.finishOrder(o));
			}
			else {
				temp.add(o);
			}
		}
		this.deliveries = temp;
	}
	
	public String contOrders(Order o) {
		return this.getTime(this.now - this.starttime) + " Continuing delivery to " + o.getRName() + ".";
	}
	
	public String startOrder(Order o) {
		return this.getTime(System.currentTimeMillis() - this.starttime) + " Starting delivery of " + o.getItem() + " to " + o.getRName() + ".";
	}
	
	public String finishOrder(Order o) {
		return this.getTime(this.now - this.starttime) + " Finished delivery of " + o.getItem() + " to " + o.getRName() + ".";
	}
	
	public String returnHQ() {
		return this.getTime(this.now - this.starttime) + " Finished all deliveries, returning back to HQ.";
	}
	
	public String atHQ() {
		return this.getTime(this.now - this.starttime) + " Returned to HQ.";
	}
	
	public String ordersDone(long ends) {
		return this.getTime(ends) + " All orders completed.";
	}
	
	public String waitDriver(String more) {
		return more + " driver(s) needed before the service can begin. Waiting...";
	}
	
	public void printInitial() {
		System.out.println("Orders: ");
		for (Order o: this.deliveries) {
			o.print();
		}
	}
	
	public String getTime(long ms) {
		// "convert time given in ms to format HH:mm:ss.SSS prompt (9 lines) ChatGPT 3 Aug. version, OpenAI, 25 Sep. 2023, chat.openai.com/chat.
		long hours = ms / 3600000;
	    long minutes = (ms % 3600000) / 60000;
	    long seconds = ((ms % 3600000) % 60000) / 1000;
	    long milliseconds = ms % 1000;
	    if (hours > 0) {
	        return String.format("[%02d:%02d:%02d.%03d]", hours, minutes, seconds, milliseconds);
	    } else {
	        return String.format("[00:%02d:%02d.%03d]", minutes, seconds, milliseconds);
	    }
	}
	
	public double calc(double LA1, double LO1, double LA2, double LO2) {
		double lat1 = Math.toRadians(LA1);
		double lat2 = Math.toRadians(LA2);
		double lon1 = Math.toRadians(LO1);
		double lon2 = Math.toRadians(LO2);
		double distance = 3963.0 * Math.acos((Math.sin(lat1) * Math.sin(lat2)) 
		                              + Math.cos(lat1) * Math.cos(lat2) 
		                              * Math.cos(lon2-lon1));
		return (Math.round(distance * 10.0) / 10.0);
	}
}
