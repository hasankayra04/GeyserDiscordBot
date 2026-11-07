/*
 * Copyright (c) 2020-2026 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.geysermc.discordbot.storage;

import com.zaxxer.hikari.HikariDataSource;
import org.geysermc.discordbot.util.PropertiesManager;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MySQLStorageManagerTest {
    @Test
    void reportsPreferenceWriteFailures() {
        MySQLStorageManager storage = new MySQLStorageManager();
        storage.dataSource = new HikariDataSource();
        storage.dataSource.close();

        assertThrows(IllegalStateException.class,
                () -> storage.setServerPreference(1, "forum-auto-reply-tag", "help"));
    }

    @Test
    void insertsAndUpdatesForumAutoReplyTag() throws Exception {
        String host = System.getenv("TEST_MYSQL_HOST");
        String database = System.getenv("TEST_MYSQL_DATABASE");
        String user = System.getenv("TEST_MYSQL_USER");
        String password = System.getenv("TEST_MYSQL_PASSWORD");
        assumeTrue(host != null && database != null && user != null && password != null,
                "MySQL integration test requires TEST_MYSQL_HOST, TEST_MYSQL_DATABASE, TEST_MYSQL_USER and TEST_MYSQL_PASSWORD");

        Properties properties = new Properties();
        properties.setProperty("db-host", host);
        properties.setProperty("db-database", database);
        properties.setProperty("db-user", user);
        properties.setProperty("db-pass", password);
        PropertiesManager.loadProperties(properties);

        MySQLStorageManager storage = new MySQLStorageManager();
        storage.setupStorage();
        long serverId = ThreadLocalRandom.current().nextLong(Long.MIN_VALUE, 0);
        try {
            storage.setServerPreference(serverId, "forum-auto-reply-tag", "help");
            assertEquals("help", storage.getServerPreference(serverId, "forum-auto-reply-tag"));

            storage.setServerPreference(serverId, "forum-auto-reply-tag", "updated 'help'");
            assertEquals("updated 'help'", storage.getServerPreference(serverId, "forum-auto-reply-tag"));
        } finally {
            try (Connection connection = storage.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM `preferences` WHERE `server` = ?")) {
                statement.setLong(1, serverId);
                statement.executeUpdate();
            } finally {
                storage.closeStorage();
            }
        }
    }
}
