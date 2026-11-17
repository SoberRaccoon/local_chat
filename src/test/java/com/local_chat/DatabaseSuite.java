package com.local_chat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseSuite {
    @TempDir
    Path tempDir;

    private Database db;

    @BeforeEach
    void setUp() {
        db = new Database("jdbc:sqlite:" + tempDir.resolve("test.db"));
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    void AuthUserTest() {
        db.insertUser("bob", "abcd");

        assertFalse(db.userExists("steve"));
        assertTrue(db.userExists("bob"));
        assertEquals(db.getPassword("steve"), null);
        assertEquals(db.getPassword("bob"), "abcd");
    }
}
