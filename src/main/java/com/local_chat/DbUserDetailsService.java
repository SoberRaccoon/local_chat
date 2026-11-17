package com.local_chat;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DbUserDetailsService implements UserDetailsService {

    private final Database db;

    public DbUserDetailsService(Database db) {
        this.db = db;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        if (!this.db.userExists(username)) {
            throw new UsernameNotFoundException(username);
        }

        return User.withUsername(username)
                .password(this.db.getPassword(username))
                .roles("USER")
                .build();
    }
}
