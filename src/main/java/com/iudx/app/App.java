package com.iudx.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;

import java.util.Arrays;
import java.util.List;


/**
 * Hello world!
 */
public class App {

//  static MinioClient minioClient = MinioClient.builder()
//      .endpoint("http://172.19.0.1:9000")
//      .region("in")  // MinIO server address
//      .credentials("myminioadmin", "minio-secret-key-change-me")  // Access key and secret key
//      .build();
//
//    public static void setBucketPolicy () throws Exception {
//      String bucketName = "new-bucket";
//      String policy = createBucketPolicy(bucketName);
//      minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
//      System.out.println("Bucket policy  added successfully");
//    }
//
//    public static String createBucketPolicy (String bucketName) throws JsonProcessingException {
//      ObjectMapper mapper = new ObjectMapper();
//      ObjectNode policyJson = mapper.createObjectNode();
//
//      policyJson.put("Version", "2012-10-17");
//
//      ObjectNode statementJson = mapper.createObjectNode();
//      statementJson.put("Effect", "Allow");
//
//      // Set the Principal to the specific user
//      ObjectNode principalJson = mapper.createObjectNode();
//      principalJson.put("AWS", "arn:aws:iam::*:user/" + "amarthya_iudx");
//      statementJson.set("Principal", principalJson);
//
//      List<String> actions = Arrays.asList(
//        "s3:GetObject",
//        "s3:DeleteObject",
//        "s3:PutObject"
//      );
//      ArrayNode actionsArray = statementJson.putArray("Action");
//      actions.forEach(actionsArray::add);
//
//      ArrayNode resourceArray = statementJson.putArray("Resource");
//      resourceArray.add("arn:aws:s3:::" + bucketName + "/*");
//
//
//
//      ObjectNode statementJson2 = mapper.createObjectNode();
//      statementJson2.put("Effect", "Deny");
//
//      // Set the Principal to the specific user
//      ObjectNode principalJson2 = mapper.createObjectNode();
//      principalJson2.put("AWS", "arn:aws:iam::*:user/*");
//      statementJson2.set("Principal", principalJson2);
//
//      List<String> actions2 = Arrays.asList(
//        "s3:GetObject",
//        "s3:DeleteObject",
//        "s3:PutObject"
//      );
//      ArrayNode actionsArray2 = statementJson2.putArray("Action");
//      actions2.forEach(actionsArray2::add);
//
//      ArrayNode resourceArray2 = statementJson2.putArray("Resource");
//      resourceArray2.add("arn:aws:s3:::" + bucketName + "/*");
//
//      ArrayNode statementArray = policyJson.putArray("Statement");
//      statementArray.add(statementJson);
//      statementArray.add(statementJson2);
//
//      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(policyJson);
//
//    }

    public static void main(String[] args) {
      System.out.println("Hello World!");
//      try {
//        setBucketPolicy();
//
//      } catch (Exception e) {
//        System.out.println(e);
//      }
    }
}
