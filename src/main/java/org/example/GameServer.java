package org.example;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class GameServer {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.createRegistry(1099);
            System.out.println("[Server] RMI Registry started on port 1099.");

            GameEngine engine = new GameEngineImpl();

            registry.rebind("GameEngine", engine);
            System.out.println("[Server] GameEngine bound successfully. Ready for players!");

        } catch (Exception e) {
            System.err.println("[Server] Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
