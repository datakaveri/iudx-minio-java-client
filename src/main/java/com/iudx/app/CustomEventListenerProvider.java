package com.iudx.app;


import io.minio.MinioClient;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class CustomEventListenerProvider implements EventListenerProvider {

  private static final Logger logger = LoggerFactory.getLogger(CustomEventListenerProvider.class);
  private final KeycloakSession session;

  private final MinioClient minioClient;

  public CustomEventListenerProvider(KeycloakSession session) {
    this.session = session;
    try {
      this.minioClient = MinioClient.builder()
            .endpoint(System.getenv("MINIO_API_URL"))
            .region("in")  // MinIO server address
            .credentials(System.getenv("MINIO_ROOT_USER"), System.getenv("MINIO_ROOT_PASSWORD"))  // Access key and secret key
            .build();
    } catch (IllegalArgumentException e) {
      logger.error("Failed to initialize MinioClient: ", e);
      throw new RuntimeException("MinioClient initialization failed", e);
    }
  }

  @Override
  public void onEvent(Event event) {
    // ? On user registration create a new bucket
    if (event.getType() == EventType.REGISTER) {
      handleNewUserRegistration(event);
    } else if (event.getType() == EventType.LOGIN) {
      handleOldUserLogins(event);
    }
  }

  private void handleNewUserRegistration(Event event) {
    try {
      String userId = event.getUserId();
      logger.info("New user registered: {}", userId);

      UserModel user = getUserById(userId);
      if (user != null) {
        setUserPolicyAttribute(user);
        createUserPolicy(user.getId());
      } else {
        logger.error("User not found for ID: {}", userId);
      }
    } catch (Exception e) {
      logger.error("Error handling new user registration: ", e);
    }
  }

  private void handleOldUserLogins(Event event) {
    try {
      String userId = event.getUserId();

      UserModel user = getUserById(userId);

      logger.info("Logged in user: " + user.getEmail());

      if(!user.getAttributes().containsKey("policy") || !user.getAttributes().get("policy").contains(user.getId())) {

        if(!user.getAttributes().containsKey("policy")) {
          logger.info("No existing user policies");
          logger.info("Setting policy attribute for existing user " + user.getEmail());
          setUserPolicyAttribute(user);
        }

        logger.info("Creating named policy for " + user.getEmail());
        createUserPolicy(user.getId());
      }

    } catch (Exception e) {
      logger.error("Error handling old login: ", e);
    }
  }

  private UserModel getUserById(String userId) {
    return session.users().getUserById(session.getContext().getRealm(), userId);
  }

  private void setUserPolicyAttribute(UserModel user) {
    user.setSingleAttribute("policy", user.getId());
    logger.info("User ID for user {} is {}", user.getEmail(), user.getId());
    logger.info("Policy attribute set for user: {}", user.getEmail());
  }

  private void createUserPolicy(String userId) {
    try {
      // URL for the POST request
      URL url = new URL(System.getenv("MINIO_POLICY_MIDDLEWARE_URL") + "/create-user-policy");

      // Open a connection
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();

      // Set the request method to POST
      conn.setRequestMethod("POST");

      // Set headers
      conn.setRequestProperty("Authorization", System.getenv("MINIO_POLICY_MIDDLEWARE_AUTHORIZATION_KEY"));
      conn.setRequestProperty("Content-Type", "application/json");

      // Enable output for the request body
      conn.setDoOutput(true);

      // JSON payload
      String jsonPayload = "{\n" +
        "\t\"email\": \"" + userId + "\"\n" +
        "}";

      // Write the JSON payload to the output stream
      try (OutputStream os = conn.getOutputStream()) {
        byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      } catch (Exception e) {
        logger.info("Error " + e);
      }

      // Check the response code
      int responseCode = conn.getResponseCode();
      logger.info("Response Code: " + responseCode);

      logger.info("Creating named policy for " + userId);

      // Close the connection
      conn.disconnect();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  @Override
  public void onEvent(AdminEvent adminEvent, boolean b) {
  }

  @Override
  public void close() {}
}
