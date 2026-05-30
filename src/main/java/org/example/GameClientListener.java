package org.example;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameClientListener extends Remote {
    void notifyMoveMade(String player, String symbol, int x, int y) throws RemoteException;
    void notifyGameOver(String winner) throws RemoteException;
    void notifyGameStarted() throws RemoteException;
}
