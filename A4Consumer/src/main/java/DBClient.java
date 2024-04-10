import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DBClient {
  private static DynamoDbClient client;

  private DBClient() {
    try {
      AwsSessionCredentials creds = AwsSessionCredentials.create(Constants.awsKey, Constants.awsSecretKey, Constants.awsToken);
      client = DynamoDbClient.builder().region(Region.of(Constants.awsRegion)).credentialsProvider(
          StaticCredentialsProvider.create(creds)).build();
    } catch (Exception e) {
      throw new RuntimeException("Error connecting to DynamoDB");
    }
  }

  public static synchronized DynamoDbClient getDBClient() {
    if (client == null) {
      new DBClient();
    }
    return client;
  }

}
