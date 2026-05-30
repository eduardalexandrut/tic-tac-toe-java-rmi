package org.example;

import java.io.Serializable;

public class Message implements Serializable {
    private String matchName;
    private String username;

    public Message(String matchName, String username) {
        this.matchName = matchName;
        this.username = username;
    }

    public String getMatchName() {
        return matchName;
    }
    public void setMatchName(String matchName) {
        this.matchName = matchName;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
}
