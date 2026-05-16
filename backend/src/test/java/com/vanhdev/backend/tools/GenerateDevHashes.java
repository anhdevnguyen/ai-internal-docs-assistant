package com.vanhdev.backend.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Run this once to generate BCrypt hashes for seed SQL.
 * Copy the output into V3__seed_dev_data.sql before applying migration.
 */
public class GenerateDevHashes {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        System.out.println("Admin@123 => " + encoder.encode("Admin@123"));
        System.out.println("User@123  => " + encoder.encode("User@123"));
    }
}