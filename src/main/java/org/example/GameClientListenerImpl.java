package org.example;

import javafx.application.Platform;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class GameClientListenerImpl extends UnicastRemoteObject implements GameClientListener {
    private GameController controller;

    public GameClientListenerImpl(GameController controller) throws RemoteException {
        super();
        this.controller = controller;
    }


    @Override
    public void notifyMoveMade(String player, String symbol, int x, int y) throws RemoteException {
        Platform.runLater(() -> controller.updateBoardState(player, symbol, x, y));
    }

    @Override
    public void notifyGameOver(String winner) throws RemoteException {
        Platform.runLater(() -> controller.showGameOverState("winner: " + winner));
    }

    @Override
    public void notifyGameStarted() throws RemoteException {
        Platform.runLater(() -> controller.updateStatus("Game Started! Make your move."));
    }
}
