package edu.gmu.swe622.lab5;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

public class LockServerWrapper {

	private ILockServer lockServer;
	public LockServerWrapper(ILockServer lockServer)
	{
		this.lockServer = lockServer;
	}
	
	private AtomicInteger clientIDCounter = new AtomicInteger();
	private HashMap<Integer, HashSet<String>> lockedPeopleByClients = new HashMap<Integer, HashSet<String>>();
	/**
	 * Contract:
	 * If clientStatus is non-zero: it might be a key into some map
	 * that will tell us if that client has a lock
	 * At the end, if we take out a new lock, generate new client ID
	 * return that new client ID
	 * @param name
	 * @param clientStatus
	 * @return
	 */
	public int getLockOnPersonIfDontHave(String name, int clientID)
	{
		try {
			//If clientID is 0: it's a new client
			if (clientID == 0) {
				clientID = clientIDCounter.incrementAndGet();
				synchronized (lockedPeopleByClients) {
					lockedPeopleByClients.put(clientID, new HashSet<String>());
				}
			}
			//I know clientID is valid
			synchronized (lockedPeopleByClients) {
				if(lockedPeopleByClients.containsKey(clientID)) //YES, we have a set of locks for that client
				{
					if(lockedPeopleByClients.get(clientID).contains(name))
					{
						//Already had lock
						return clientID;
					}
				}
				else
				{
					throw new IllegalArgumentException("Invalid ClientID Passed");
				}
			}
			//If we reach this part, we have guaranteed that the clientID is valid
			//and the client does not have the lock already.
			//First request is waiting here, Second request would wait here too
			lockServer.lockPerson(name);//This might block - might take a long time to return - fine.
			synchronized (lockedPeopleByClients) {
				lockedPeopleByClients.get(clientID).add(name);
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return clientID;
	}
	public void unlockPersonIfHave(String name, int clientID)
	{
		synchronized (lockedPeopleByClients) {
			if(lockedPeopleByClients.containsKey(clientID)) //YES, we have a set of locks for that client
			{
				if(lockedPeopleByClients.get(clientID).contains(name))
				{
					//Yes, you have the lock!
					try {
						lockServer.unlockPerson(name);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					lockedPeopleByClients.get(clientID).remove(name);
				}
				else
				{
					throw new IllegalStateException("You want to unlock but don't have lock");
				}
			}
			else
			{
				throw new IllegalArgumentException("Invalid ClientID Passed");
			}
		}
	}
}
