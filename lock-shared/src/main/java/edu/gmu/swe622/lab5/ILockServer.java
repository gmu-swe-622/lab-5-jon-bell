package edu.gmu.swe622.lab5;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ILockServer extends Remote {
	public void lockList(boolean forWrite) throws RemoteException;
	public void lockPerson(String name) throws RemoteException;
	public void unLockList(boolean fromWrite) throws RemoteException;
	public void unlockPerson(String name) throws RemoteException;
	public static final String LOCK_SERVER_RMI_NAME = "Lab5LockServer";
}
