package com.local_chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.local_chat.ChatService.ChatSuggestion;
import com.local_chat.ChatService.CreateChatError;
import com.local_chat.ChatService.Message;

public class ChatServiceSuite {
    @TempDir
    Path tempDir;

    private Database db;
    private ChatService service;

    @BeforeEach
    void setUp() {
        db = new Database("jdbc:sqlite:" + tempDir.resolve("test.db"));
        service = new ChatService(db);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    // ChatService uses a ConcurrentHashMap, so suggestion order is not guaranteed
    private static Set<ChatSuggestion> asSet(List<ChatSuggestion> list) {
        return Set.copyOf(list);
    }

    // ------------------------------------------------------------------
    // createChat
    // ------------------------------------------------------------------

    @Test
    public void CreatePublicChatTest() {
        db.insertUser("Bob", "1234");
        assertEquals(CreateChatError.Success, service.createChat("Fish", "", "Bob"));

        // creator is automatically a member
        assertNotNull(service.getChatMessages("Fish", "Bob"));
    }

    @Test
    public void CreatePrivateChatTest() {
        db.insertUser("Bob", "1234");
        assertEquals(CreateChatError.Success, service.createChat("Secret", "abcd", "Bob"));
        assertNotNull(service.getChatMessages("Secret", "Bob"));
    }

    @Test
    public void CreateChatInvalidAdminTest() {
        assertEquals(CreateChatError.InvalidAdminUsername, service.createChat("Fish", "", "Nobody"));
        // a failed creation must not register the chat name
        assertEquals(List.of(), service.getSuggestions("Fish"));
    }

    @Test
    public void CreateChatNameInUseTest() {
        db.insertUser("Bob", "1234");
        db.insertUser("Steve", "1234");

        assertEquals(CreateChatError.Success, service.createChat("Fish", "", "Bob"));
        assertEquals(CreateChatError.ChatNameInUse, service.createChat("Fish", "", "Bob"));
        assertEquals(CreateChatError.ChatNameInUse, service.createChat("Fish", "abcd", "Steve"));
    }

    @Test
    public void CreateChatValidationOrderTest() {
        db.insertUser("Bob", "1234");

        assertEquals(CreateChatError.InvalidChatname, service.createChat("", "x", "Bob"));
        assertEquals(CreateChatError.InvalidPassword, service.createChat("Fish", "x", "Nobody"));
        service.createChat("Fish", "", "Bob");
        assertEquals(CreateChatError.InvalidAdminUsername, service.createChat("Fish", "", "Nobody"));
    }

    // ------------------------------------------------------------------
    // getSuggestions
    // ------------------------------------------------------------------

    @Test
    public void SuggestionsTest() {
        // nothing exists yet
        assertEquals(List.of(), service.getSuggestions(""));
        assertEquals(List.of(), service.getSuggestions("F"));

        db.insertUser("Bob", "1234");
        db.insertUser("Steve", "1234");

        assertEquals(CreateChatError.Success, service.createChat("Fish", "", "Bob"));
        assertEquals(CreateChatError.InvalidAdminUsername, service.createChat("Fishy", "", "Ghost"));
        assertEquals(CreateChatError.ChatNameInUse, service.createChat("Fish", "", "Bob"));

        // empty search returns nothing, even when chats exist
        assertEquals(List.of(), service.getSuggestions(""));

        assertEquals(Set.of(new ChatSuggestion("Fish", false)), asSet(service.getSuggestions("F")));

        assertEquals(CreateChatError.Success, service.createChat("Fishing", "abcd", "Steve"));
        assertEquals(
            Set.of(new ChatSuggestion("Fish", false), new ChatSuggestion("Fishing", true)),
            asSet(service.getSuggestions("F")));

        // no chat name contains 'a'
        assertEquals(List.of(), service.getSuggestions("a"));
    }

    @Test
    public void SuggestionsMatchSubstringTest() {
        db.insertUser("Bob", "1234");
        service.createChat("Fishing", "", "Bob");

        assertEquals(Set.of(new ChatSuggestion("Fishing", false)), asSet(service.getSuggestions("shi")));
        assertEquals(Set.of(new ChatSuggestion("Fishing", false)), asSet(service.getSuggestions("ing")));
        assertEquals(Set.of(new ChatSuggestion("Fishing", false)), asSet(service.getSuggestions("Fishing")));
        assertEquals(List.of(), service.getSuggestions("Fishing!"));
    }

    @Test
    public void SuggestionsAreCaseSensitiveTest() {
        db.insertUser("Bob", "1234");
        service.createChat("Fishing", "", "Bob");

        assertEquals(List.of(), service.getSuggestions("fish"));
        assertEquals(Set.of(new ChatSuggestion("Fishing", false)), asSet(service.getSuggestions("Fish")));
    }

    @Test
    public void SuggestionsPrivacyFlagTest() {
        db.insertUser("Bob", "1234");
        service.createChat("PublicRoom", "", "Bob");
        service.createChat("PrivateRoom", "abcd", "Bob");

        assertEquals(
            Set.of(new ChatSuggestion("PublicRoom", false), new ChatSuggestion("PrivateRoom", true)),
            asSet(service.getSuggestions("Room")));
    }

    @Test
    public void SuggestionsSurviveServiceRestartTest() {
        db.insertUser("Bob", "1234");
        service.createChat("PublicRoom", "", "Bob");
        service.createChat("PrivateRoom", "abcd", "Bob");

        ChatService reloaded = new ChatService(db);
        assertEquals(
            Set.of(new ChatSuggestion("PublicRoom", false), new ChatSuggestion("PrivateRoom", true)),
            asSet(reloaded.getSuggestions("Room")));

        assertEquals(CreateChatError.ChatNameInUse, reloaded.createChat("PublicRoom", "", "Bob"));
    }

    // ------------------------------------------------------------------
    // joinChat
    // ------------------------------------------------------------------

    @Test
    public void JoinPublicChatTest() {
        db.insertUser("Greg", "1234");
        db.insertUser("Frog", "1234");
        service.createChat("Gregtopia", "", "Greg");

        assertNull(service.getChatMessages("Gregtopia", "Frog"));
        assertEquals(0, service.joinChat("Frog", "Gregtopia", ""));
        assertNotNull(service.getChatMessages("Gregtopia", "Frog"));
    }

    @Test
    public void JoinPrivateChatCorrectPasswordTest() {
        db.insertUser("Greg", "1234");
        db.insertUser("Frog", "1234");
        service.createChat("Secret", "abcd", "Greg");

        assertEquals(0, service.joinChat("Frog", "Secret", "abcd"));
        assertNotNull(service.getChatMessages("Secret", "Frog"));
    }

    @Test
    public void JoinPrivateChatWrongPasswordTest() {
        db.insertUser("Greg", "1234");
        db.insertUser("Frog", "1234");
        service.createChat("Secret", "abcd", "Greg");

        assertEquals(-1, service.joinChat("Frog", "Secret", "wrong"));
        assertEquals(-1, service.joinChat("Frog", "Secret", ""));
        assertEquals(-1, service.joinChat("Frog", "Secret", "ABCD"));
        assertNull(service.getChatMessages("Secret", "Frog"));
    }

    @Test
    public void JoinPublicChatIgnoresPasswordTest() {
        db.insertUser("Greg", "1234");
        db.insertUser("Frog", "1234");
        service.createChat("Gregtopia", "", "Greg");

        assertEquals(0, service.joinChat("Frog", "Gregtopia", "whatever"));
        assertNotNull(service.getChatMessages("Gregtopia", "Frog"));
    }

    @Test
    public void JoinNonExistentChatTest() {
        db.insertUser("Frog", "1234");
        assertEquals(-1, service.joinChat("Frog", "ghost_chat", ""));
    }

    @Test
    public void JoinNonExistentUserTest() {
        db.insertUser("Greg", "1234");
        service.createChat("Gregtopia", "", "Greg");
        assertEquals(-1, service.joinChat("Ghost", "Gregtopia", ""));
    }

    @Test
    public void JoinTwiceTest() {
        db.insertUser("Greg", "1234");
        db.insertUser("Frog", "1234");
        service.createChat("Gregtopia", "", "Greg");

        assertEquals(0, service.joinChat("Frog", "Gregtopia", ""));
        assertEquals(0, service.joinChat("Frog", "Gregtopia", ""));
    }

    // ------------------------------------------------------------------
    // getChatMessages / sendMessage
    // ------------------------------------------------------------------

    @Test
    public void MessageTest() {
        db.insertUser("Greg", "1234");
        db.insertUser("Frog", "1234");
        db.insertUser("Next", "1234");

        var chat_err = service.createChat("Gregtopia", "", "Greg");
        assertEquals(CreateChatError.Success, chat_err);

        ArrayList<Message> msg_err = service.getChatMessages("non_existent", "Greg");
        assertNull(msg_err);

        msg_err = service.getChatMessages("Gregtopia", "Greg");
        assertEquals(new ArrayList<Message>(), msg_err);

        msg_err = service.getChatMessages("Gregtopia", "Frog");
        assertNull(msg_err);

        service.joinChat("Frog", "Gregtopia", "");
        msg_err = service.getChatMessages("Gregtopia", "Greg");
        assertEquals(new ArrayList<Message>(), msg_err);

        Message msg1 = new Message("Greg!", "Greg");
        assertEquals(0, service.sendMessage("Greg", "Greg!", "Gregtopia"));
        Message msg2 = new Message("Greg Greg ?", "Frog");
        assertEquals(0, service.sendMessage("Frog", "Greg Greg ?", "Gregtopia"));

        msg_err = service.getChatMessages("Gregtopia", "Next");
        assertNull(msg_err);

        msg_err = service.getChatMessages("Gregtopia", "Greg");
        assertEquals(new ArrayList<Message>(List.of(msg1, msg2)), msg_err);
        msg_err = service.getChatMessages("Gregtopia", "Frog");
        assertEquals(new ArrayList<Message>(List.of(msg1, msg2)), msg_err);
    }

    @Test
    public void SendMessageNonMemberTest() {
        db.insertUser("Greg", "1234");
        db.insertUser("Frog", "1234");
        service.createChat("Gregtopia", "", "Greg");

        assertEquals(-1, service.sendMessage("Frog", "let me in", "Gregtopia"));
        // and the message must not have been stored
        assertEquals(new ArrayList<Message>(), service.getChatMessages("Gregtopia", "Greg"));
    }

    @Test
    public void SendMessageNonExistentChatTest() {
        db.insertUser("Greg", "1234");
        assertEquals(-1, service.sendMessage("Greg", "hello?", "ghost_chat"));
    }

    @Test
    public void SendMessageNonExistentUserTest() {
        db.insertUser("Greg", "1234");
        service.createChat("Gregtopia", "", "Greg");

        assertEquals(-1, service.sendMessage("Ghost", "boo", "Gregtopia"));
    }

    @Test
    public void MessagesKeepOrderTest() {
        db.insertUser("Greg", "1234");
        service.createChat("Gregtopia", "", "Greg");

        List<Message> sent = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Message m = new Message("msg " + i, "Greg");
            sent.add(m);
            assertEquals(0, service.sendMessage("Greg", "msg " + i, "Gregtopia"));
        }
        assertEquals(sent, service.getChatMessages("Gregtopia", "Greg"));
    }

    @Test
    public void MessagesAreIsolatedBetweenChatsTest() {
        db.insertUser("Greg", "1234");
        db.insertUser("Frog", "1234");
        service.createChat("ChatA", "", "Greg");
        service.createChat("ChatB", "", "Greg");
        service.joinChat("Frog", "ChatB", "");

        Message a = new Message("only in A", "Greg");
        Message b = new Message("only in B", "Frog");
        service.sendMessage("Greg", "only in A", "ChatA");
        service.sendMessage("Frog", "only in B", "ChatB");

        assertEquals(new ArrayList<Message>(List.of(a)), service.getChatMessages("ChatA", "Greg"));
        assertEquals(new ArrayList<Message>(List.of(b)), service.getChatMessages("ChatB", "Frog"));
        // Frog is not in ChatA so can't read it
        assertNull(service.getChatMessages("ChatA", "Frog"));
    }

    @Test
    public void PrivateChatMessagesTest() {
        db.insertUser("Greg", "1234");
        db.insertUser("Frog", "1234");
        service.createChat("Secret", "abcd", "Greg");

        Message m = new Message("classified", "Greg");
        assertEquals(0, service.sendMessage("Greg", "classified", "Secret"));

        // outsider can't read or write without joining
        assertNull(service.getChatMessages("Secret", "Frog"));
        assertEquals(-1, service.sendMessage("Frog", "hi", "Secret"));

        // after a wrong-password join they still can't
        assertEquals(-1, service.joinChat("Frog", "Secret", "nope"));
        assertNull(service.getChatMessages("Secret", "Frog"));

        // after a correct join they can read the history
        assertEquals(0, service.joinChat("Frog", "Secret", "abcd"));
        assertEquals(new ArrayList<Message>(List.of(m)), service.getChatMessages("Secret", "Frog"));
    }

    @Test
    public void MessageContentEdgeCasesTest() {
        db.insertUser("Greg", "1234");
        service.createChat("Gregtopia", "", "Greg");

        // quotes / SQL-looking text / unicode / long text must round-trip unchanged
        Message quote = new Message("it's \"quoted\"; DROP TABLE messages;--", "Greg");
        Message unicode = new Message("héllo 👋 世界", "Greg");
        Message longMsg = new Message("x".repeat(5000), "Greg");
        service.sendMessage("Greg", quote.content(), "Gregtopia");
        service.sendMessage("Greg", unicode.content(), "Gregtopia");
        service.sendMessage("Greg", longMsg.content(), "Gregtopia");

        assertEquals(
            new ArrayList<Message>(List.of(quote, unicode, longMsg)),
            service.getChatMessages("Gregtopia", "Greg"));
    }
}
