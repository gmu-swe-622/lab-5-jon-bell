package edu.gmu.swe622.lab5;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.StampedLock;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.gmu.swe622.lab5.ILockServer;

public class LockServer implements ILockServer {
	public LockServer() {
	}

	private StampedLock listLock = new StampedLock();


	
	static HashMap<String, Lock> locksForPeople = new HashMap<String, Lock>();
	
	@Override
	public void lockPerson(String name) throws RemoteException {
		Lock lock = null;
		//Tries to lock "A": it can get it
		//Tries to lock "A": it waits

		synchronized (locksForPeople) {		
			lock = locksForPeople.get(name);
			if(lock == null)
			{
				lock = new StampedLock().asReadLock();
				locksForPeople.put(name, lock);
			}
		}
		lock.lock();//Client 2 is waiting here
	}

	@Override
	public void unlockPerson(String name) throws RemoteException {
		Lock lock = null;
		synchronized (locksForPeople) {		
			lock = locksForPeople.get(name);
			if(lock == null)
			{
				throw new IllegalStateException("Tried to unlock " + name + " but it's not locked!");
			}
		}
		lock.unlock();

	}
	@Override
	public void lockList(boolean forWrite) throws RemoteException {
		listLock.asReadLock().lock(); //TODO be a read-write lock
	}
	@Override
	public void unLockList(boolean fromWrite) throws RemoteException {
		listLock.asReadLock().unlock(); //TODO be a read-write lock		
	}

	public static Registry createAndBind(int port) throws Exception {
		ILockServer lockServer = new LockServer();
		ILockServer stub = (ILockServer) UnicastRemoteObject.exportObject(lockServer, 0);
		Registry registry = LocateRegistry.createRegistry(port);
		registry.rebind(ILockServer.LOCK_SERVER_RMI_NAME, stub);
		System.out.println("LockServer bound to port " + port);
		return registry;
	}

	public static void main(String[] args) {
		Options options = new Options();
		int lockPort = 9000;
		try {
			Option lockPortOpt = Option.builder("port").hasArg().desc("Lock server port (default 9000)").build();

			Option helpOpt = Option.builder("help").desc("Prints this message").build();
			options.addOption(lockPortOpt);
			options.addOption(helpOpt);

			CommandLine line = new DefaultParser().parse(options, args);
			if (line.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("Lab5LockServer", options);
				return;
			}
			if (line.hasOption("lockport"))
				lockPort = Integer.valueOf(line.getOptionValue("lockport"));
		} catch (ParseException | NumberFormatException ex) {
			ex.printStackTrace();
			return;
		}
		try {
			createAndBind(lockPort);
		} catch (Exception e) {
			System.err.println("Lock server unable to bind");
			e.printStackTrace();
		}
	}
}
