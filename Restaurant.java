package Food_Delivery_Service;

public class Restaurant{
	private String name;
	private double lat;
	private double lon;
	
	public Restaurant(String name, double lat, double lon) {
		this.name = name;
		this.lat = lat;
		this.lon = lon;
	}
	
	public Restaurant() {
		this.name = "undefined";
		this.lat = 0;
		this.lon = 0;
	}
	
	public String getName(){
		return this.name;
	}
	
	public double getLat(){
		return this.lat;
	}
	
	public double getLon(){
		return this.lon;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setLat(double lat) {
		this.lat = lat;
	}
	
	public void setLon(double lon) {
		this.lon = lon;
	}
	
	public void print(){
		System.out.println("Name: " + this.name);
		System.out.println("Lat: " + this.lat);
		System.out.println("Long: " + this.lon);
		System.out.println();
	}
}
