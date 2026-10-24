package org.example;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One object per match. It owns everything about the match: board, players,
 * listeners and turn.
 *
 * Every method that reads or changes the state is synchronized on this object,
 * so each match is a monitor with its own lock: moves of the same match are
 * executed one at a time, and different matches never block each other.
 *
 * Callbacks to the clients are NOT made while holding the lock. They are queued
 * on a single thread owned by the match, so:
 *  - they are delivered in the same order as the events happened;
 *  - a slow or crashed client cannot block the match;
 *  - if a client cannot be reached, the match ends and the other player wins.
 */
public class Match {
    private final String name;
    private final String[][] board = new String[3][3];
    private final List<String> players = new ArrayList<>();
    private final List<GameClientListener> listeners = new ArrayList<>();
    private String currentTurn;
    private boolean over = false;

    // Single thread that delivers the callbacks of this match, in order.
    private final ExecutorService notifier = Executors.newSingleThreadExecutor();

    // Called once when the match ends, so the engine can forget it.
    private final Runnable onFinished;

    // A remote call to make on one listener.
    private interface Callback {
        void call(GameClientListener listener) throws RemoteException;
    }

    public Match(String name, String creator, GameClientListener creatorListener, Runnable onFinished) {
        this.name = name;
        this.onFinished = onFinished;

        // Initialize an empty board with dots
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                board[y][x] = ".";
            }
        }

        players.add(creator);
        listeners.add(creatorListener);
    }

    public synchronized void join(String username, GameClientListener listener) throws RemoteException {
        if (over) {
            throw new RemoteException("Match doesn't exist!");
        }
        if (players.size() >= 2) {
            throw new RemoteException("Game is full!");
        }
        if (players.contains(username)) {
            throw new RemoteException("Username already used in this match!");
        }

        players.add(username);
        listeners.add(listener);

        // Player 1 (the creator) always starts as 'X'
        currentTurn = players.get(0);
        System.out.println("[Server] Match '" + name + "' started! First turn: " + currentTurn);

        broadcast(l -> l.notifyGameStarted());
    }

    public synchronized void makeMove(String player, int x, int y) throws RemoteException {
        if (over) {
            throw new RemoteException("Game is over!");
        }
        if (players.size() < 2) {
            throw new RemoteException("Waiting for opponent!");
        }
        if (!Objects.equals(currentTurn, player)) {
            throw new RemoteException("It is not your turn!");
        }
        if (x < 0 || x > 2 || y < 0 || y > 2) {
            throw new RemoteException("Coordinates out of bounds.");
        }
        if (!Objects.equals(board[y][x], ".")) {
            throw new RemoteException("Cell is already occupied.");
        }

        // Determine Symbol (Player 1 = X, Player 2 = O)
        String symbol = players.indexOf(player) == 0 ? "X" : "O";
        board[y][x] = symbol;

        // The whole state change below happens before anybody is notified,
        // so a client failure can never leave the match half-updated.
        broadcast(l -> l.notifyMoveMade(player, symbol, x, y));

        if (checkWinner()) {
            finish(player);
            return;
        }
        if (isBoardFull()) {
            finish("Draw");
            return;
        }

        currentTurn = players.get(0).equals(player) ? players.get(1) : players.get(0);
    }

    // Called (with the lock held) when the game ends normally.
    private void finish(String winner) {
        over = true;
        broadcast(l -> l.notifyGameOver(winner));
        notifier.shutdown(); // callbacks already queued are still delivered
        System.out.println("[Server] Match '" + name + "' finished. Winner: " + winner);
        onFinished.run();
    }

    // Called by the notifier thread when a client cannot be reached.
    private synchronized void clientDisconnected(GameClientListener dead) {
        if (over) {
            return;
        }
        over = true;

        int index = listeners.indexOf(dead);
        System.out.println("[Server] A player left match '" + name + "'. Ending the match.");

        // If the opponent is still there, it wins the match.
        if (players.size() == 2) {
            String winner = players.get(1 - index);
            GameClientListener survivor = listeners.get(1 - index);
            notifier.execute(() -> {
                try {
                    survivor.notifyGameOver(winner);
                } catch (RemoteException e) {
                    System.out.println("[Server] Both players of '" + name + "' left.");
                }
            });
        }

        notifier.shutdown();
        onFinished.run();
    }

    // Queues a callback for all the listeners of this match.
    // The list is copied while the lock is held; the remote calls run later
    // on the notifier thread, without the lock.
    private void broadcast(Callback callback) {
        List<GameClientListener> targets = new ArrayList<>(listeners);
        notifier.execute(() -> {
            for (GameClientListener listener : targets) {
                try {
                    callback.call(listener);
                } catch (RemoteException e) {
                    clientDisconnected(listener);
                }
            }
        });
    }

    private boolean isBoardFull() {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (Objects.equals(board[y][x], ".")) return false;
            }
        }
        return true;
    }

    private boolean checkWinner() {
        // Check rows and columns
        for (int i = 0; i < 3; i++) {
            if (sameSymbol(board[i][0], board[i][1], board[i][2])) return true;
            if (sameSymbol(board[0][i], board[1][i], board[2][i])) return true;
        }
        // Check diagonals
        return sameSymbol(board[0][0], board[1][1], board[2][2])
                || sameSymbol(board[0][2], board[1][1], board[2][0]);
    }

    private boolean sameSymbol(String a, String b, String c) {
        return !Objects.equals(a, ".") && Objects.equals(a, b) && Objects.equals(b, c);
    }
}