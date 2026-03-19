package com.examscheduler.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.stereotype.Repository;

import com.examscheduler.security.AppUserPrincipal;
import com.mongodb.client.MongoCollection;

@Repository
public class AppUserDAO extends BaseDAO {

    public AppUserPrincipal findByUsername(String username) throws SQLException {
        try {
            Document user = collection("app_user").find(new Document("username", username)).first();
            if (user == null) {
                return null;
            }

            String hash = getString(user, "passwordHash");
            if (hash == null) {
                hash = getString(user, "password_hash");
            }

            Boolean enabled = user.getBoolean("enabled");
            if (enabled == null) {
                enabled = Boolean.TRUE;
            }

            List<String> roles = user.getList("roles", String.class);
            if (roles == null) {
                roles = new ArrayList<>();
            }

            return new AppUserPrincipal(username, hash, enabled, roles);
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to load user from MongoDB", ex);
        }
    }

    public int countUsers() throws SQLException {
        try {
            return Math.toIntExact(collection("app_user").countDocuments());
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to count users from MongoDB", ex);
        }
    }

    public void upsertRole(String roleName) throws SQLException {
        try {
            MongoCollection<Document> roles = collection("role");
            Document existing = roles.find(new Document("roleName", roleName)).first();
            if (existing == null) {
                roles.insertOne(new Document("roleName", roleName));
            }
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to upsert role in MongoDB", ex);
        }
    }

    public void createUserWithRole(String username, String passwordHash, String roleName) throws SQLException {
        try {
            upsertRole(roleName);

            MongoCollection<Document> users = collection("app_user");
            Document existing = users.find(new Document("username", username)).first();
            if (existing == null) {
                long id = nextSequence("app_user");
                List<String> roles = new ArrayList<>();
                roles.add(roleName);

                users.insertOne(new Document()
                    .append("id", id)
                    .append("username", username)
                    .append("passwordHash", passwordHash)
                    .append("enabled", true)
                    .append("roles", roles));
            } else {
                List<String> roles = existing.getList("roles", String.class);
                if (roles == null) {
                    roles = new ArrayList<>();
                }
                if (!roles.contains(roleName)) {
                    roles.add(roleName);
                    users.updateOne(new Document("username", username), new Document("$set", new Document("roles", roles)));
                }
            }
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to create user/role in MongoDB", ex);
        }
    }
}
