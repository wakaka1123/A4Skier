import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

public class PostDAO implements Runnable {

  private DynamoDbClient client;
  private BlockingQueue<int[]> buffer;
  private static final int batchSize = 25;
  private static final int interval = 2000;

  public PostDAO(BlockingQueue<int[]> buffer) {
    this.buffer = buffer;
    this.client = DBClient.getDBClient();
  }


  public void insertLiftRide(List<int[]> batchData) {
    try {
      List<WriteRequest> writeRequests = batchData.stream().map(data -> createWriteRequest(data)).toList();

      BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder().requestItems(
          Map.of(Constants.DDB_TABLE, writeRequests)).build();
      client.batchWriteItem(batchWriteItemRequest);
    } catch (DynamoDbException e) {
      e.printStackTrace();
    }
  }

  private WriteRequest createWriteRequest(int[] data) {
    String itemId = UUID.randomUUID().toString();
    AttributeValue skierId = AttributeValue.builder().n(String.valueOf(data[5])).build();
    AttributeValue resortId = AttributeValue.builder().n(String.valueOf(data[2])).build();
    AttributeValue seasonId = AttributeValue.builder().n(String.valueOf(data[3])).build();
    AttributeValue dayId = AttributeValue.builder().n(String.valueOf(data[4])).build();
    AttributeValue time = AttributeValue.builder().n(String.valueOf(data[0])).build();
    AttributeValue liftId = AttributeValue.builder().n(String.valueOf(data[1])).build();

    Map<String, AttributeValue> item = Map.of(
        "uuid", AttributeValue.builder().s(itemId).build(),
        "skierId", skierId,
        "resortId", resortId,
        "seasonId", seasonId,
        "dayId", dayId,
        "time", time,
        "liftId", liftId
    );

    return WriteRequest.builder().putRequest(PutRequest.builder().item(item).build()).build();
  }


  @Override
  public void run() {

    long preTime = System.currentTimeMillis();
    List<int[]> batchData = new ArrayList<>();

    while (true) {
      try {
        if (!batchData.isEmpty() && (batchData.size() >= batchSize || System.currentTimeMillis() - preTime > interval)) {
          insertLiftRide(batchData);
          batchData.clear();
          preTime = System.currentTimeMillis();
        }
        int[] newLiftRide = buffer.poll(5, TimeUnit.SECONDS);
        if (newLiftRide != null) batchData.add(newLiftRide);
      } catch (Exception e) {
        e.printStackTrace();
      }

    }

  }
}
