package com.twocall.chat.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${app.firebase.credentials-path:./config/firebase-service-account.json}")
    private String credentialsPath;

    @PostConstruct
    public void initFirebase() {
        try {
            File file = new File(credentialsPath);
            if (file.exists() && file.isFile()) {
                try (InputStream serviceAccount = new FileInputStream(file)) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();

                    if (FirebaseApp.getApps().isEmpty()) {
                        FirebaseApp.initializeApp(options);
                        log.info("Firebase Cloud Messaging initialized successfully from {}", credentialsPath);
                    }
                }
            } else {
                log.warn("Firebase credentials not found at {}. Push notifications will run in mock/log mode for development.", credentialsPath);
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage());
        }
    }
}
