import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class ConsumerHandlerRunnable implements Runnable {

  private final Connection connection;
  private final String queueName;
  private final String exchangeName;
  private BlockingQueue<int[]> buffer;

  public ConsumerHandlerRunnable(Connection connection, String exchangeName, String queueName,
      BlockingQueue<int[]> buffer) {
    this.connection = connection;
    this.exchangeName = exchangeName;
    this.queueName = queueName;
    this.buffer = buffer;
  }

  @Override
  public void run() {
    try {
      Channel channel = connection.createChannel();
      channel.exchangeDeclare(exchangeName, "fanout");
      channel.queueDeclare(queueName, false, false, false, null);
      channel.queueBind(queueName, exchangeName, "");
      System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

      channel.basicQos(1);

      //fair dispatch
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String msg = new String(delivery.getBody(), "UTF-8");
        System.out.println(" [x] Received '" + msg + "'");
        try {
          //add to the blocking queue first
          String[] parts = msg.split(",");
          int[] liftRideMsg = {Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
              Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]),
              Integer.parseInt(parts[5])};
          buffer.put(liftRideMsg);

          System.out.println(" [x] Done");
          channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
          //latch.countDown();
        } catch (Exception e) {
          e.printStackTrace();
        }
      };
      channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

//  private void updateMap(String msg) {
//    //[217, 10, 1, 2024, 1, 123]
//    //[time, liftID, resortID, seasonID, dayID, skierID]
//    String[] parts = msg.split(",");
//    int[] arr = {Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
//        Integer.parseInt(parts[2]),
//        Integer.parseInt(parts[3]), Integer.parseInt(parts[4])};
//    synchronized (mp) {
//      mp.put(parts[5], arr);
//    }
//  }
}
