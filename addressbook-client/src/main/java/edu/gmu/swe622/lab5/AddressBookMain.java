package edu.gmu.swe622.lab5;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class AddressBookMain {

	private static Jedis jedis;
	private static ILockServer lockServer;
	private static LockServerWrapper lockServerWrapper;
	
	public static void main(String[] args) {
		JedisPool pool = null;
		pool = new JedisPool("localhost", 6379);
		jedis = pool.getResource();
		try {
			System.out.println("Connected.");
			Registry registry = LocateRegistry.getRegistry("localhost", 9000);
			lockServer = (ILockServer) registry.lookup(ILockServer.LOCK_SERVER_RMI_NAME);
			lockServerWrapper = new LockServerWrapper(lockServer);
		} catch (Exception e) {
			System.err.println("Client exception connecting to lock server: " + e.toString());

		}
		Scanner s = new Scanner(System.in);
		System.out.println("Options: 1 (quit) 2 (list) 3 (append note field) 4 (add)");
		scannerLoop: while (s.hasNextLine()) {

			int opt = 0;
			try {
				opt = Integer.valueOf(s.nextLine());
			} catch (Throwable t) {
				System.out.println("Invalid input.");
			}
			switch (opt) {
			case 1:
				break scannerLoop;
			case 2:
				listPeople();
				break;
			case 3:
				System.out.println("Enter name that you want to update: ");
				String name = s.nextLine();
				System.out.println("Enter new note:");
				String note = s.nextLine();
				editPerson(name, note);
				break;
			case 4:
				System.out.println("Enter new name: ");
				name = s.nextLine();
				System.out.println("Enter email: ");
				String email = s.nextLine();
				System.out.println("Enter note: ");
				note = s.nextLine();
				addPerson(name, email, note);
				break;
			}
			System.out.println("Options: 1 (quit) 2 (list) 3 (append note field) 4 (add)");
		}
		s.close();
		jedis.close();
		pool.close();
	}

	static void addPerson(String name, String email, String notes) {
		try {
			lockServer.lockList(true);
			try {
				if (jedis.exists("/contacts/" + name))
					throw new IllegalArgumentException("Contact already exists!");
				HashMap<String, String> newPerson = new HashMap<String, String>();
				newPerson.put("name", name);
				newPerson.put("email", email);
				newPerson.put("notes", notes);

				jedis.hmset("/contacts/" + name, newPerson);
				jedis.sadd("/contacts", new String[] { name });
			} finally {
				lockServer.unLockList(true);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	static void listPeople() {
		Set<String> people = jedis.smembers("/contacts");
		for (String key : people) {
			List<String> person = jedis.hmget("/contacts/" + key, new String[] { "name", "email", "notes" });
			System.out.println("KEY: " + key + ": " +person.get(0) + ": " + person.get(1) + "\n\t" + person.get(2));
		}
	}


	static void editPerson(String name, String newNote) {
//		try {
//			lockServer.lockPerson(name);
			
			int id = lockServerWrapper.getLockOnPersonIfDontHave(name, 0);
			System.out.println("Got ID" + id);
			try {
				// Update user's notes by appending a new note to the string
				String appendedNote = jedis.hget("/contacts/"+name, "notes")
						+"\n"+newNote;
				jedis.hset("/contacts/"+name, "notes", appendedNote);
			} finally {
//				lockServer.unlockPerson(name);
				lockServerWrapper.unlockPersonIfHave(name, id);
			}
//		} catch (RemoteException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
}
