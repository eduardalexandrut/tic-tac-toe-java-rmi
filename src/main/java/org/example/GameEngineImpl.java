package org.example;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameEngineImpl extends UnicastRemoteObject implements GameEngine {
    private final Map<String, Match> matches = new ConcurrentHashMap<>();

    protected GameEngineImpl() throws RemoteException {
        super();
    }

    @Override
    public void createGame(Message msg, GameClientListener gameClientListener) throws RemoteException {
        String matchName = msg.getMatchName();
        Match match = new Match(matchName, msg.getUsername(), gameClientListener,
                () -> removeMatch(matchName));

        // putIfAbsent is atomic: if two clients create the same match at the
        // same time, exactly one of them succeeds.
        if (matches.putIfAbsent(matchName, match) != null) {
            throw new RemoteException("Game already exists!");
        }
        System.out.println("[Server] Match '" + matchName + "' created by " + msg.getUsername() + ". Waiting for opponent...");
    }

    @Override
    public void joinGame(Message msg, GameClientListener gameClientListener) throws RemoteException {
        Match match = matches.get(msg.getMatchName());
        if (match == null) {
            throw new RemoteException("Match doesn't exist!");
        }
        match.join(msg.getUsername(), gameClientListener);
    }

    @Override
    public void makeMove(String matchName, String player, int x, int y) throws RemoteException {
        Match match = matches.get(matchName);
        if (match == null) {
            throw new RemoteException("Game doesn't exist!");
        }
        match.makeMove(player, x, y);
    }

    private void removeMatch(String matchName) {
        matches.remove(matchName);
        System.out.println("[Server] Match '" + matchName + "' cleared from memory.");
    }
}
