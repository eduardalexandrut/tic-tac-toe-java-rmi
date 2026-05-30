package org.example;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameEngine extends Remote {
    void joinGame(Message msg, GameClientListener gameClientListener) throws RemoteException;
    void startGame() throws RemoteException;
    void endGame() throws RemoteException;
    void makeMove(String match, String player, int x, int y) throws RemoteException;
}
