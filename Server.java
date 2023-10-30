package Food_Delivery_Service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



public class Server{
	double lat;
	double lon;
	int drivers;
	int connections;
	volatile int count;
	volatile long t;
	volatile int timed;
	String endtime;
	Vector<Order> ready = new Vector<>();
	Map<String, Restaurant> search = new ConcurrentHashMap<>();
	Vector<Order> orders = new Vector<>();
	Vector<ServerThread> threads = new Vector<>();
	ConcurrentLinkedDeque<ServerThread> q = new ConcurrentLinkedDeque<>();
	
	public void printOrders() {
		System.out.println("start");
		for (Order o: this.orders) {
			o.print();
		}
	}
	
	public void printReady() {
		System.out.println("start");
		for (Order o: this.ready) {
			o.print();
		}
	}
	
	public void printMap() {
		for (Restaurant r: this.search.values()) {
			r.print();
		}
	}
	
	public void broadcast(String n) {
		for (ServerThread temp: this.threads) {
			temp.sendMessage(n);
		}
	}
	
	public void broadcastq(String n) {
		for (ServerThread temp: this.q) {
			temp.sendMessage(n);
		}
	}
	
	public void validate() {
		for (Order o: this.orders) {
			o.setCoords(this.search.get(o.getRName()).getLat(),this.search.get(o.getRName()).getLon());
		}
	}
	
	public boolean driversFinished() {
		for (int i=0;i<this.threads.size();i++) {
			if (!this.threads.get(i).isAvailable()) {
				return false;
			}
		}
		return true;
	}
	
	public static void main(String [] args) {
		Server S = new Server();
		boolean valid = false;
		Scanner in = new Scanner(System.in);
		String fn = "";
		Restaurant holder = new Restaurant("temp",34.021160,-118.287132);
		while(!valid) {
			System.out.print("What is the name of the file containing the schedule information? ");
			System.out.println();
			fn = in.nextLine();
			try {
				FileReader fr = new FileReader(fn);
				BufferedReader br = new BufferedReader(fr);
				String ln = "";
				// "How can I read a large text file line by line using Java?" prompt (2 lines), StackOverflow 25 Sep. 2023
				// https://stackoverflow.com/questions/5868369/how-can-i-read-a-large-text-file-line-by-line-using-java
				while ((ln = br.readLine()) != null) {
					String[] line = ln.split(",");
					Order temp = new Order(Integer.parseInt(line[0]),line[1].trim(),line[2].trim(),S);
					S.orders.add(temp);
					S.search.put(line[1].trim(), holder);
				}
				fr.close();
				br.close();
				valid = true;
			} catch (FileNotFoundException fnfe) {
				System.out.println("The file " + fn + " could not be found.");
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		while (true) {
			try {
				System.out.print("What is your latitude? ");
				S.lat = Double.parseDouble(in.nextLine());
				break;
			} catch (InputMismatchException ime) {
				System.out.print("Latitude needs to be of type double. \n");
			} catch (NullPointerException npe) {
				System.out.print("Latitude needs to be of type double. \n");
			} catch (NumberFormatException nfe) {
				System.out.print("Latitude needs to be of type double. \n");
			}
		}
		while (true) {
			try {
				System.out.println("");
				System.out.print("What is your longitude? ");
				S.lon = Double.parseDouble(in.nextLine());
				break;
			} catch (InputMismatchException ime) {
				System.out.print("Longitude needs to be of type double. \n");
			}catch (NullPointerException npe) {
				System.out.print("Longitude needs to be of type double. \n");
			} catch (NumberFormatException nfe) {
				System.out.print("Longitude needs to be of type double. \n");
			}
		}
        String apikey = "";
        System.out.println("You will need your own API key for Food Delivery Service.");
        System.out.println("Enter you API key: ");
        apikey = in.nextLine();
		// for testing
//		S.lat = 34.021160;
//		S.lon = -118.287132;
		for (String key: S.search.keySet()) {
			// "replace spaces with other char" prompt (5 lines) ChatGPT 3 Aug. version, OpenAI, 25 Oct. 2023, chat.openai.com/chat.
			String[] split = key.trim().split("\\s+");
			String n = "";
			for(int i=0; i<split.length -1; i++) {
				n += split[i] + "%20";
			}
			n += split[split.length-1];
			try {
				// "Yelp Developer Documentation" prompt (7 lines) 25 Oct. 2023
				// https://docs.developer.yelp.com/reference/v3_business_search
				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create("https://api.yelp.com/v3/businesses/search?latitude="+Double.toString(S.lat)+"&longitude="+Double.toString(S.lon)+"&term="+n+"&categories=&sort_by=best_match&limit=1"))
						.header("accept", "application/json")
						.header("Authorization", "Bearer " + apikey)
						.method("GET", HttpRequest.BodyPublishers.noBody())
						.build();
				HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
				String file = response.body();
				Restaurant rest = new Restaurant();
				// "json-simple documentation (used as reference, nothing directly copied) 24 Oct. 2023
				// http://alex-public-doc.s3.amazonaws.com/json_simple-1.1/index.html
				// "How to create correct JSONArray in Java using JSONObject" prompt (5 lines), StackOverflow 24 Oct. 2023
				// https://stackoverflow.com/questions/18983185/how-to-create-correct-jsonarray-in-java-using-jsonobject
				JSONParser parser = new JSONParser();
		        JSONObject obj = ((JSONObject)parser.parse(file));
		        JSONArray businesses = (JSONArray)obj.get("businesses");
		        JSONObject hit = (JSONObject) businesses.get(0);
	            JSONObject coords = (JSONObject) hit.get("coordinates");
		        rest.setName((String) hit.get("name"));
		        rest.setLat((Double) coords.get("latitude"));
		        rest.setLon((Double) coords.get("longitude"));
	            S.search.put(key, rest);
			} catch(InterruptedException ie) {
				ie.printStackTrace();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} catch (ParseException pe) {
				pe.printStackTrace();
			}
		}
		S.validate();
		while (true) {
			try {
				System.out.println("");
				System.out.print("How many drivers will be in service today? ");
				S.drivers = Integer.parseInt(in.nextLine());
				System.out.println();
				break;
			} catch (InputMismatchException ime) {
				System.out.print("Drivers need to be of type int. \n");
			} catch (NullPointerException npe) {
				System.out.print("Drivers need to be of type int. \n");
			} catch (NumberFormatException nfe) {
				System.out.print("Drivers need to be of type int. \n");
			}
		}
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(3456);
			System.out.println("Listening of port 3456. Waiting for drivers...");
			System.out.println();
			while (S.drivers - S.connections > 0) {
				Socket s = ss.accept();
				S.connections++;
				ServerThread st = new ServerThread(s,S);
				S.threads.add(st);
				S.q.add(st);
				System.out.println("Connection from " + s.getInetAddress());
				if (S.drivers - S.connections == 0) {
					System.out.println("Starting service.");
					System.out.println();
					S.broadcast("0");
					S.broadcast(Double.toString(S.lat));
					S.broadcast(Double.toString(S.lon));
					break;
				}
				System.out.println("Waiting for " + (S.drivers - S.connections) + " more driver(s)");
				System.out.println();
				S.broadcast(String.valueOf(S.drivers - S.connections));
			}
			int size = S.q.size();
			S.count = 0;
			S.timed = 0;
			ExecutorService executors = Executors.newCachedThreadPool();
			for (int i=0;i<S.orders.size();i++) {
				executors.execute(S.orders.get(i));
			}
			executors.shutdown();
			S.t = 0;
			while (true) {
				if (S.ready.isEmpty()) {
					if (S.count == S.orders.size() && S.driversFinished()) {
						break;
					}
					Thread.yield();
				}
				else {
					Thread.sleep(100);
					if (!S.q.isEmpty()) {
						ServerThread temp = S.q.pollFirst();
						temp.assignOrders(S.ready);
						temp.waitInput();
						S.ready = new Vector<>();
					}
				}
			}
			S.broadcast("0");
			while (S.timed != size) {
				Thread.yield();
			}
			System.out.println("All orders completed");
			S.broadcast(Long.toString(S.t));
			ServerThread.endDriver();
		} catch (IOException ioe) {
			System.out.println("ioe in Server: " + ioe.getMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			// "Correct way to close ServerSocket" prompt (referenced, nothing copied) 29 Oct. 2023
			// https://stackoverflow.com/questions/41200798/correct-way-to-close-serversocket
			if (ss != null && !ss.isClosed()) {
                try {
                    ss.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
		}
	}
}
