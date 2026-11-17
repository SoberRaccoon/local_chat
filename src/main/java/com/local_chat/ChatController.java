package com.local_chat;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.local_chat.ChatService.CreateChatError;

import jakarta.validation.constraints.NotBlank;

@Controller
class ChatController {
    ChatService service;
    public ChatController(ChatService service) {
        this.service = service;
    }

    @GetMapping("/") 
    public String getMainPage() {
        return "chat";
    }

    @GetMapping("/search")
    public ResponseEntity<?> getSuggestions(@RequestParam String q) {
        var json = this.service.getSuggestions(q);
        return ResponseEntity.ok(json);
    }

    public record ChatInfo(
        @NotBlank String chatName, 
        String password) {}

    @PostMapping("/chat/join") 
    public ResponseEntity<?> joinChat(
        Principal principal,
        @RequestBody ChatInfo chat_info) {

        int res = this.service.joinChat(principal.getName(), chat_info.chatName, chat_info.password);
        if (res == -1) {
            return ResponseEntity.status(400).build();
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/chat/get") 
    public ResponseEntity<?> getChatMessages(
        Principal principal,
        @RequestParam String chatName) {
        var msgs = this.service.getChatMessages(chatName, principal.getName());
        if (msgs == null) {
            return ResponseEntity.status(400).build();
        }

        return ResponseEntity.ok(msgs);
    }


    @GetMapping("/chat_names") 
    public ResponseEntity<?> getChatMessages(
        Principal principal) {
        var chats = this.service.getUserChats(principal.getName());
        return ResponseEntity.ok(chats);
    }
 

    @GetMapping("/leave") 
    public ResponseEntity<?> leaveChat(
        Principal principal,
        @RequestParam String chat_name) {
        this.service.leaveChat(principal.getName(), chat_name);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/chat/create") 
    public ResponseEntity<?> createChat(
        Principal principal,
        @RequestBody ChatInfo chat_info
        ) {

        CreateChatError err = service.createChat(
                chat_info.chatName, 
                chat_info.password, 
                principal.getName());

        switch (err) {
            case CreateChatError.InvalidChatname:
                return ResponseEntity.status(400).
                    body("BAD_NAME");
            case CreateChatError.InvalidPassword:
                return ResponseEntity.status(400).
                    body("BAD_PASSWORD");
            case CreateChatError.InvalidAdminUsername:
                return ResponseEntity.status(400).
                    body("USERNAME_DOES_NOT_EXIST");
            case CreateChatError.ChatNameInUse:
                return ResponseEntity.status(400).
                    body("NAME_IN_USE");
            case CreateChatError.Success:
                return ResponseEntity.ok().build();
            default:
        }

        return ResponseEntity.status(500).build(); 
    }
}
