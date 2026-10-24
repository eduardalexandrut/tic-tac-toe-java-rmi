package org.example;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameEngine extends Remote {
    // Creates a new match with the given name and waits for an opponent.
    // Fails if a match with that name already exists.
    void createGame(Message msg, GameClientListener gameClientListener) throws RemoteException;

    // Joins an existing match, given its name.
    // Fails if the match does not exist or is already full.
    void joinGame(Message msg, GameClientListener gameClientListener) throws RemoteException;

    void makeMove(String match, String player, int x, int y) throws RemoteException;
}
