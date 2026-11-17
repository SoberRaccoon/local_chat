package com.local_chat;

import java.sql.*;
import java.util.ArrayList;

public class SQLiteWrapper {
    Connection connection = null;

    void print_exception(Exception e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
        e.printStackTrace();
    } 

    public SQLiteWrapper(String database_path) {
        try {
            this.connection = DriverManager.getConnection(database_path);
            Statement stmt = this.connection.createStatement();
            
            String[] commands = new String[] {
                "CREATE TABLE IF NOT EXISTS users (" +
                    "username VARCHAR(64) PRIMARY KEY, " +
                    "password VARCHAR(64));",
            
                "CREATE TABLE IF NOT EXISTS persistent_logins ( " +
                    "username  VARCHAR(64) NOT NULL, " +
                    "series    VARCHAR(64) PRIMARY KEY, " +
                    "token     VARCHAR(64) NOT NULL, " +
                    "last_used TIMESTAMP   NOT NULL);",

                "CREATE TABLE IF NOT EXISTS chats (" +
                    "chat_name VARCHAR(64) PRIMARY KEY, " +
                    "admin VARCHAR(64), " +
                    "password VARCHAR(64), " +
                    "FOREIGN KEY (admin) REFERENCES users(username));",

                "CREATE TABLE IF NOT EXISTS chat_users (" +
                    "username VARCHAR(64), " +
                    "chat_name VARCHAR(64), " +
                    "PRIMARY KEY (username, chat_name), " + 
                    "FOREIGN KEY (username) REFERENCES users(username), " +
                    "FOREIGN KEY (chat_name) REFERENCES chats(chat_name));",

                "CREATE TABLE IF NOT EXISTS messages (" +
                    "message_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "message TEXT, " +
                    "chat_name VARCHAR(64), " +
                    "sender VARCHAR(64), " +
                    "FOREIGN KEY (sender) REFERENCES users(username), " +
                    "FOREIGN KEY (chat_name) REFERENCES chats(chat_name));",
            };

            for (String cmd : commands) {
                stmt.executeUpdate(cmd);
            }
            
            stmt.close();
        } catch ( Exception e ) {
            System.out.println("SQL_Wrapper Constructor");
            this.print_exception(e);
        }
    } 

    public int executeUpdate(String querry, ArrayList<String> args) {
        try (PreparedStatement stmt = this.connection.prepareStatement(querry)) {
            for (int i = 0; i < args.size(); i++) {
                stmt.setString(i + 1, args.get(i));
            }

            stmt.executeUpdate();
        } catch ( Exception e ) {
            this.print_exception(e);
            return -1;
        }
        return 0;
    }

    public ArrayList<String> executeQuerry(String querry, ArrayList<String> args, int result_columns) {
        ArrayList<String> result = new ArrayList<>();

        try (PreparedStatement stmt = this.connection.prepareStatement(querry)) {
            for (int i = 0; i < args.size(); i++) {
                stmt.setString(i + 1, args.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                for (int i = 1; i <= result_columns; i++) {
                    result.add(rs.getString(i));
                }
            }
        } catch ( Exception e ) {
            this.print_exception(e);
            return null;
        }
        return result;
    }

    public void close() {
        try {
            this.connection.close();
        } catch ( Exception e ) {
            this.print_exception(e);
        }
    }
}
