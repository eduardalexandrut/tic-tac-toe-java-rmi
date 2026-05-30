package org.example;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameEngineImpl extends UnicastRemoteObject implements GameEngine {
    private final int COLS = 3;
    private final int ROWS = 3;

    Map<String, String[][]> boards = new ConcurrentHashMap<>();
    Map<String, List<String>> matchesPlayers = new ConcurrentHashMap<>();
    private final Map<String, List<GameClientListener>> clientListeners = new ConcurrentHashMap<>();

    protected GameEngineImpl() throws RemoteException {
        super();
    }

    private synchronized void initGame(String matchName) throws RemoteException {
        if (boards.containsKey(matchName)) {
            throw new RemoteException("Game already exists!");
        }

        //Initialize an empty board with dots
        String[][] emptyBoard = new String[3][3];
        for (int i = 0; i < this.COLS; i++) {
            for (int j = 0; j < this.ROWS; j++) {
                emptyBoard[i][j] = ".";
            }
        }
        boards.put(matchName, emptyBoard);
        matchesPlayers.put(matchName, new CopyOnWriteArrayList<>());
        clientListeners.put(matchName, new CopyOnWriteArrayList<>());
    }

    @Override
    public void joinGame(Message msg, GameClientListener gameClientListener) throws RemoteException {
        String match = msg.matchName();

        if (!matchesPlayers.containsKey(match)) {
            throw new RemoteException("Match doesn't exists!");
        }

        List<String> players = matchesPlayers.get(match);
        if (players.size() > 2) {
            throw new RemoteException("Game is full!");
        }

        players.add(msg.userName());
        clientListeners.get(match).add(gameClientListener);


        //If there are 2 players, start the game
        if (players.size() == 2) {
            for (GameClientListener client : clientListeners.get(match)) {
                client.notifyGameStarted();
            }
        }
    }

    @Override
    public void startGame() throws RemoteException {

    }

    @Override
    public void endGame() throws RemoteException {

    }

    @Override
    public void makeMove(String match, String player, int x, int y) throws RemoteException {
        String[][] board = boards.get(match);
        if (board == null) {
            throw new RemoteException("Game doesn't exist!");
        }

        synchronized (board) {
            List<String> players = matchesPlayers.get(match);
            if (players.size() < 2) {
                throw new RemoteException("Waiting for oponent!");
            }

            String symbol = matchesPlayers.get(match).indexOf(player) % 2 == 0 ? "X" : "O";
            String cell = board[y][x];

            if (Objects.equals(cell, "O") || Objects.equals(cell, "X")) {
                throw new RemoteException("Cell already occupied");
            }

            boards.get(match)[y][x] = symbol;

            for (GameClientListener client : clientListeners.get(match)) {
                client.notifyMoveMade(player, x, y);
            }

            //Check if player won
            if (checkWinner(symbol, match)) {
                for (GameClientListener client : clientListeners.get(match)) {
                    client.notifyGameOver(player);
                }
                cleanBoard(match);
                
            } else if (isBoardFull(board)) {
                for (GameClientListener client : clientListeners.get(match)) {
                    client.notifyGameOver("Draw");
                }
                cleanBoard(match);
            }
        }


    }

    private boolean isBoardFull(String[][] board) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (Objects.equals(board[i][j], ".")) return false;
            }
        }
        return true;
    }

    private boolean checkWinner(String symbol, String match) throws RemoteException {
        boolean isWinner = false;

        //Check cols
        for (int c = 0; c < 3; c++) {
            if (!Objects.equals(boards.get(match)[0][c], ".") &&
                    Objects.equals(boards.get(match)[0][c], boards.get(match)[1][c]) &&
                    Objects.equals(boards.get(match)[1][c], boards.get(match)[2][c]))
            {
                isWinner = true;
            }
        }


        //Check rows
        for (int r = 0; r < 3; r++) {
            if (!Objects.equals(boards.get(match)[r][0], ".") &&
                    Objects.equals(boards.get(match)[r][0], boards.get(match)[r][1]) &&
                    Objects.equals(boards.get(match)[r][1], boards.get(match)[r][2]))
            {
               isWinner = true;
            }
        }

        //Check diagonals
        if (!Objects.equals(boards.get(match)[0][0], ".") &&
                Objects.equals(boards.get(match)[0][0], boards.get(match)[1][1]) &&
                Objects.equals(boards.get(match)[1][1], boards.get(match)[2][2])) {
            isWinner = true;
        }

        if (!Objects.equals(boards.get(match)[0][2], ".") &&
                Objects.equals(boards.get(match)[0][2], boards.get(match)[1][1]) &&
                Objects.equals(boards.get(match)[1][1], boards.get(match)[2][0])) {
            isWinner = true;
        }

        return isWinner;
    }

    private void cleanBoard(String match) {
        this.matchesPlayers.remove(match);
        this.clientListeners.remove(match);
        this.boards.remove(match);
    }
}
