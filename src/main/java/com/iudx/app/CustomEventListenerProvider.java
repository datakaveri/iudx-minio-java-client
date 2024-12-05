package com.iudx.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;


public class CustomEventListenerProvider implements EventListenerProvider {

  private static final Logger logger = LoggerFactory.getLogger(CustomEventListenerProvider.class);
  private final KeycloakSession session;

  private final MinioClient minioClient;

  public CustomEventListenerProvider(KeycloakSession session) {
    this.session = session;

    // ? Initialize Minio Client
    this.minioClient = MinioClient.builder()
          .endpoint(System.getenv("MINIO_API_URL"))
          .region("in")  // MinIO server address
          .credentials("myminioadmin", "minio-secret-key-change-me")  // Access key and secret key
          .build();
  }

  @Override
  public void onEvent(Event event) {
    // ? On user registration create a new bucket
    if (event.getType() == EventType.REGISTER) {
      handleNewUserRegistration(event);
    }
  }

  private void handleNewUserRegistration(Event event) {
    String userId = event.getUserId();
    logger.info("New user registered: {}", userId);

    UserModel user = getUserById(userId);
    if (user != null) {
        setUserPolicyAttribute(user);
//        createInitialBucketPolicy(user.getEmail());
    }
  }

  private UserModel getUserById(String userId) {
    return session.users().getUserById(session.getContext().getRealm(), userId);
  }

  private void setUserPolicyAttribute(UserModel user) {
    user.setSingleAttribute("policy", user.getEmail());
    logger.info("Policy attribute set for user: {}", user.getEmail());
  }

  private void createInitialBucketPolicy(String userEmail) {
    try {
        String policyJson = buildInitialPolicy("example-bucket");
        applyBucketPolicy("example-bucket", policyJson);
        logger.info("Initial bucket policy created for user: {}", userEmail);
    } catch (Exception e) {
        logger.error("Error creating initial bucket policy: ", e);
    }
  }

  private String buildInitialPolicy(String bucketName) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode policyJson = mapper.createObjectNode();

    policyJson.put("Version", "2012-10-17");

    ObjectNode statementJson = mapper.createObjectNode();
    statementJson.put("Effect", "Allow");
    statementJson.set("Principal", mapper.createObjectNode().put("AWS", "*"));

    ArrayNode actionsArray = statementJson.putArray("Action");
    actionsArray.add("s3:GetObject");

    ArrayNode resourceArray = statementJson.putArray("Resource");
    resourceArray.add("arn:aws:s3:::" + bucketName + "/*");

    ArrayNode statementArray = policyJson.putArray("Statement");
    statementArray.add(statementJson);

    try {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(policyJson);
    } catch (JsonProcessingException e) {
        logger.error("Error creating policy JSON: ", e);
        return "{}";
    }
  }

  private void applyBucketPolicy(String bucketName, String policyJson) {
    try {
        minioClient.setBucketPolicy(
            SetBucketPolicyArgs.builder()
                .bucket(bucketName)
                .config(policyJson)
                .build()
        );
        logger.info("Policy applied successfully to bucket: {}", bucketName);
    } catch (Exception e) {
        logger.error("Error applying bucket policy: ", e);
    }
  }

  @Override
  public void onEvent(AdminEvent adminEvent, boolean b) {
  }

  @Override
  public void close() {}
}
