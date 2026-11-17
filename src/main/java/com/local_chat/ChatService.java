package com.local_chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service 
public class ChatService {
    private final Database db;
    private final ConcurrentHashMap<String, Boolean> chatList = new ConcurrentHashMap<String, Boolean>();
    private final ConcurrentHashMap<String, Set<WebSocketSession>> users_to_session 
        = new ConcurrentHashMap<>();

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public ChatService(Database db) {
        this.db = db;
        ArrayList<String> chats = this.db.getChatNamesAndPasswords();
        for (int i = 0; i < chats.size(); i += 2) {
            this.chatList.put(chats.get(i), !chats.get(i + 1).isEmpty());
        }
    }

    record ChatSuggestion(String chatName, boolean isPrivate) {}
    public List<ChatSuggestion> getSuggestions(String name_substr) {
        List<ChatSuggestion> suggestions = new ArrayList<>();

        if (name_substr.isEmpty()) return suggestions;
        for (var suggesstion : this.chatList.entrySet()) {
            if (suggesstion.getKey().contains(name_substr)) {
                suggestions.add(new ChatSuggestion(suggesstion.getKey(), suggesstion.getValue()));
            }
        }
        return suggestions;
    }
    
    public int joinChat(String username, String chat_name, String chat_password) {
        String password_hash = this.db.getChatPassowrd(chat_name);
        if (!this.db.userExists(username) || password_hash == null) return -1;
        if ( !password_hash.isEmpty() && 
             !this.encoder.matches(chat_password, password_hash)) {
            return -1; 
        } if (this.db.doesUserBelongToChat(username, chat_name)) {
            return 0;
        }
        return this.db.addUserToChat(username, chat_name);
    }


    public void leaveChat(String username, String chat_name) {
        this.db.leaveChat(username, chat_name);
    }

    public ArrayList<String> getUserChats(String username) {
        ArrayList<String> chats = this.db.getUserChats(username);
        if (chats == null) return new ArrayList<>();
        return chats;
    }

    public record Message(String content, String sender) {}
    public ArrayList<Message> getChatMessages(String chat_name, String username) {
        if (!this.db.doesUserBelongToChat(username, chat_name)) {
            return null;
        }
        ArrayList<String> db_msgs = this.db.getChatMessages(chat_name);
        ArrayList<Message> msgs = new ArrayList<Message>();
        for (int i = 0; i < db_msgs.size(); i += 2) {
            msgs.add(new Message(db_msgs.get(i), db_msgs.get(i + 1)));
        }

        return msgs; 
    }

    public void addConnection(String username, WebSocketSession socket) {
        Set<WebSocketSession> sessions = this.users_to_session.get(username);
        if (sessions == null) {
            sessions = new HashSet<>();
            sessions.add(socket);
            this.users_to_session.put(username, sessions);
        } else {
            sessions.add(socket);
        }
    }

    public void closeConnection(String username, WebSocketSession socket) {
        Set<WebSocketSession> set = this.users_to_session.get(username);
        if (set != null) {
            set.removeIf(s -> s.getId().equals(socket.getId()));
            if (set.isEmpty()) this.users_to_session.remove(username);
        }   
    }

    public void logout(String username) {
        Set<WebSocketSession> set = this.users_to_session.remove(username);
        if (set != null) {
            for (WebSocketSession s : set) {
                try { s.close(CloseStatus.POLICY_VIOLATION); } catch (IOException ignored) {}
            }
        }
    }

    public int sendMessage(String sender, String content, String chat_name) {
        if (!this.db.doesUserBelongToChat(sender, chat_name)) {
            return -1;
        }

        String message = 
            "{\"chatName\": \"" + chat_name + "\"," +
            "\"sender\": \"" + sender + "\"," +
            "\"message\": \"" + content + "\"}";

        System.out.println("Sending: " + message);

        ArrayList<String> memebers = this.db.getChatMemebers(chat_name);
        for (String member : memebers) {
            var socks = this.users_to_session.get(member);
            if (socks == null) continue;
            for (var socket : socks) {
                try {
                    socket.sendMessage(new TextMessage(message));
                } catch (Exception e) {break;}
            }
        }

        this.db.addMessage(content, chat_name, sender);
        return 0;
    }

    public enum CreateChatError {
        InvalidChatname,
        InvalidPassword,
        InvalidAdminUsername,
        ChatNameInUse,
        ServerError,
        Success
    }
    public CreateChatError createChat(String chat_name, String password, String creator_username) {
        if ((chat_name.length() > 40 || chat_name.length() == 0)) {
            return CreateChatError.InvalidChatname;
        } if (!password.isEmpty() && (!password.matches("[A-Za-z0-9]+") ||
           (password.length() > 50 || password.length() < 4))) {
            return CreateChatError.InvalidPassword;
        } if (!this.db.userExists(creator_username)) {
            return CreateChatError.InvalidAdminUsername;
        } if (this.chatList.containsKey(chat_name)) {
            return CreateChatError.ChatNameInUse;
        }
        
        // If password is empty chat is public
        String password_hash = "";
        if (!password.isEmpty()) {
            password_hash = this.encoder.encode(password);
        }

        int res = this.db.createChat(chat_name, password_hash, creator_username);
        if (res == -1) return CreateChatError.ServerError;
        res = this.db.addUserToChat(creator_username, chat_name);
        if (res == -1) return CreateChatError.ServerError;

        this.chatList.put(chat_name, password_hash != "");

        return CreateChatError.Success;
    }
}
