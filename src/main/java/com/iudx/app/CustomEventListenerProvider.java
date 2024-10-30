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
          .endpoint("http://172.19.0.1:9000")
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

    // ? Minio expects users to have policies. So we allocate newly registered
    // ? Keycloak users to a group
    // ? https://min.io/docs/minio/macos/operations/external-iam/configure-keycloak-identity-management.html#configure-minio-for-keycloak-authentication
    // ? Use the above link for reference
    addUserToReadWriteGroup(userId);
    createUserBucket(userId);
  }

  private void addUserToReadWriteGroup(String userId) {
    UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);
    GroupModel group = session.groups().getGroupByName(session.getContext().getRealm(), null, "readonly");

    if (group != null && user != null) {
      user.joinGroup(group);
      logger.info("User {} added to group {}", userId, group.getName());
    } else {
      logger.warn("Group or user not found for adding to group");
    }
  }

  private void createUserBucket(String userId) {
    try {
      UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);
      // ? Creating a unique bucket for a user
      String bucketName = user.getUsername() + "-bucket";
      boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());

      if (!bucketExists) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        logger.info("Bucket {} created successfully", bucketName);

        setBucketPolicy(bucketName);
      } else {
        logger.info("Bucket {} already exists", bucketName);
      }
    } catch (Exception e) {
      logger.error("Error creating user bucket: ", e);
    }
  }

  private void setBucketPolicy(String bucketName) throws Exception {
    String policy = createBucketPolicy(bucketName);
    minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
    logger.info("Bucket policy for {} added successfully", bucketName);
  }

  private String createBucketPolicy(String bucketName) throws JsonProcessingException {

    // ? Creating a bucket policy that gives access to single user
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode policyJson = mapper.createObjectNode();

    policyJson.put("Version", "2012-10-17");

    ObjectNode statementJson = mapper.createObjectNode();
    statementJson.put("Effect", "Allow");

    // Set the Principal to the specific user
    ObjectNode principalJson = mapper.createObjectNode();
    principalJson.put("AWS", "arn:aws:iam::*:user/" + bucketName.replace("-bucket", ""));
    statementJson.set("Principal", principalJson);

    List<String> actions = Arrays.asList(
      "s3:GetObject",
      "s3:DeleteObject",
      "s3:PutObject"
    );
    ArrayNode actionsArray = statementJson.putArray("Action");
    actions.forEach(actionsArray::add);

    ArrayNode resourceArray = statementJson.putArray("Resource");
    resourceArray.add("arn:aws:s3:::" + bucketName + "/*");

    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(policyJson);
  }

  @Override
  public void onEvent(AdminEvent adminEvent, boolean b) {
  }

  @Override
  public void close() {}
}
