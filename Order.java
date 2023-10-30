package Food_Delivery_Service;

import java.io.Serializable;


@SuppressWarnings("serial")
public class Order extends Thread implements Serializable{
	private int time;
	private String rest;
	private String item;
	private transient Server S;
	private double lat;
	private double lon;
	
	
	public Order(int time, String rest, String item, Server S) {
		this.time = time;
		this.rest = rest;
		this.item = item;
		this.S = S;
	}
	
	public void run() {
		try {
			Thread.sleep(time * 1000);
			S.ready.add(this);
			S.count++;
		} catch (InterruptedException ie) {
            System.out.println("Order Interrupted");
        }
	}
	
	public String getRName() {
		return this.rest;
	}
	
	public double getLat() {
		return this.lat;
	}
	
	public double getLon() {
		return this.lon;
	}
	
	public String getItem() {
		return this.item;
	}
	
	public void print(){
		System.out.println("Time: " + this.time);
		System.out.println("Restaurant: " + this.rest);
		System.out.println("Item: " + this.item);
		System.out.println("Lat: " + this.lat);
		System.out.println("Lon: " + this.lon);
		System.out.println();
	}
	
	public void setCoords(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}
	
	public double calcDist(double lat, double lon) {
		double lat1 = Math.toRadians(lat);
		double lat2 = Math.toRadians(this.lat);
		double lon1 = Math.toRadians(lon);
		double lon2 = Math.toRadians(this.lat);
		double distance = 3963.0 * Math.acos((Math.sin(lat1) * Math.sin(lat2)) 
		                              + Math.cos(lat1) * Math.cos(lat2) 
		                              * Math.cos(lon2-lon1));
		return (Math.round(distance * 10.0) / 10.0);
	}
}
