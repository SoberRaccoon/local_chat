package com.local_chat;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.*;

@Repository
public class Database {
    SQLiteWrapper sql_wrapper;

    public Database(@Value("${database.path}") String database_path) {
        this.sql_wrapper = new SQLiteWrapper(database_path);
    }

    private boolean valueExists(String table, String value_name, String value) {
        ArrayList<String> res = this.sql_wrapper.executeQuerry(
                "SELECT (" + value_name + ") FROM " + table +
                " WHERE " + value_name + "=?;",
                new ArrayList<String>(List.of(value)),
                1
            );
        if (res == null) return false;
        return !res.isEmpty();
    }

    public boolean userExists(String username) {
        return this.valueExists("users", "username", username);
    }

    public boolean chatExists(String chatName) {
        return this.valueExists("chats", "chat_name", chatName);
    }

    public int insertUser(String username, String password_hash) {
        return this.sql_wrapper.executeUpdate(
            "INSERT INTO users (username, password) " + 
            "Values(?, ?);",
            new ArrayList<String>(List.of(username, password_hash)));
    }

    public String getPassword(String username) {
        ArrayList<String> res = this.sql_wrapper.executeQuerry(
                "SELECT password FROM users " +
                "WHERE username=?;",
                new ArrayList<String>(List.of(username)),
                1
            );
        if (res.isEmpty()) return null;
        return res.getFirst();
    }

    public ArrayList<String> getChatNamesAndPasswords() {
        return this.sql_wrapper.executeQuerry(
            "SELECT chat_name, password FROM chats;",
            new ArrayList<String>(), 2);
    }

    public int addUserToChat(String username, String chat_name) {
        return this.sql_wrapper.executeUpdate(
            "INSERT INTO chat_users (username, chat_name) " +
            "Values(?, ?);",
            new ArrayList<String>(List.of(username, chat_name)));
    }

    public int createChat(String chat_name, String password_hash, String creator_username) {
        return this.sql_wrapper.executeUpdate(
            "INSERT INTO chats (chat_name, password, admin) " +
            "Values(?, ?, ?);",
            new ArrayList<String>(List.of(chat_name, password_hash, creator_username)));
    } 
    
    public String getChatPassowrd(String chat_name) {
        var res = this.sql_wrapper.executeQuerry(
            "SELECT password FROM chats " +
            "WHERE chat_name=?;",
            new ArrayList<String>(List.of(chat_name)), 1);

        if (res == null || res.isEmpty()) return null;
        return res.getFirst();
    } 

    public boolean doesUserBelongToChat(String username, String chat_name) {
        var res = this.sql_wrapper.executeQuerry(
            "SELECT chat_name FROM chat_users " +
            "WHERE chat_name=? AND username=?;",
            new ArrayList<String>(List.of(chat_name, username)), 1);

        if (res == null) return false;
        return !res.isEmpty();
    }

    
    public void leaveChat(String username, String chat_name) {
        this.sql_wrapper.executeUpdate(
            "DELETE FROM chat_users " +
            "WHERE username = ? AND chat_name = ?;",
            new ArrayList<String>(List.of(username, chat_name)));
    }

    public ArrayList<String> getUserChats(String username) {
        return this.sql_wrapper.executeQuerry(
                "SELECT chat_name FROM chat_users " + 
                "WHERE username = ?;",
            new ArrayList<String>(List.of(username)), 1);
    }

    public ArrayList<String> getChatMessages(String chat_name) {
        return this.sql_wrapper.executeQuerry(
            "SELECT message, sender " +
            "FROM messages " +
            "WHERE chat_name = ? " +
            "ORDER BY message_id ASC;",
            new ArrayList<String>(List.of(chat_name)), 2);
    }

    public ArrayList<String> getChatMemebers(String chat_name) {
        return this.sql_wrapper.executeQuerry(
                "SELECT username FROM chat_users " + 
                "WHERE chat_name = ?;",
            new ArrayList<String>(List.of(chat_name)), 1);
    }

    public int addMessage(String message, String chat_name, String sender) {
        return this.sql_wrapper.executeUpdate(
            "INSERT INTO messages (message, chat_name, sender) " +
            "Values(?, ?, ?);",
            new ArrayList<String>(List.of(message, chat_name, sender)));
    }
    
    public void close() { this.sql_wrapper.close(); }
}
