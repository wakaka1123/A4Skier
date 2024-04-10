import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import constants.Constants;
import constants.Message;
import constants.Payload;
import constants.ResponseMsg;
import io.swagger.client.model.LiftRide;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

@WebServlet(name = "SkierServlet", value = "/SkierServlet")
public class SkierServlet extends HttpServlet {
  private GenericObjectPool<Channel> channelPool;
  private final Gson gson = new Gson();
  private final ResponseMsg message = new ResponseMsg();

  @Override
  public void init() throws ServletException {
    super.init();
    try {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost("44.230.124.84");
      factory.setUsername("guest");
      factory.setPassword("guest");

      Connection connection = factory.newConnection();
      GenericObjectPoolConfig<Channel> poolConfig = new GenericObjectPoolConfig<>();
      poolConfig.setMaxTotal(10);
      channelPool = new GenericObjectPool<>(new RMQChannelFactory(connection), poolConfig);
      System.out.println("Channel pool initialized successfully");
    } catch (Exception e) {
      throw new ServletException("Failed to initialize channel pool", e);
    }
  }

  @Override
  public void destroy() {
    super.destroy();
    try {
      if (channelPool != null) {
        channelPool.close();
        System.out.println("Channel pool closed");
      }
    } catch (Exception e) {
      System.err.println("Failed to close channel pool" + e.getMessage());
      throw new RuntimeException("Failed to close channel pool", e);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    //TODO: Handle the query params later, which might leads to different queries to dynamodb
//    String[] resortArr = req.getParameterValues("resort");
//    String[] seasonArr = req.getParameterValues("season");

    res.setContentType(Constants.CONTENT_JSON);
    String urlPath = req.getPathInfo();

    //TODO: check request param also here and report missing param if some are absent
    if (urlPath == null || urlPath.isEmpty()) {
      buildResponse(res, gson, message, HttpServletResponse.SC_NOT_FOUND, Constants.MISSING_PARAM);
      return;
    }

    String[] urlParts = urlPath.split(Constants.SPLIT);

    if (!isUrlValid(urlParts)) {
      buildResponse(res, gson, message, HttpServletResponse.SC_BAD_REQUEST, Constants.INVALID_INPUTS);
    } else {
      if (urlParts.length == 3) {
        buildResponse(res, gson, message, HttpServletResponse.SC_OK, Constants.VALID);
      } else buildResponse(res, gson, message, HttpServletResponse.SC_OK, Constants.DEFAULT);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    res.setContentType(Constants.CONTENT_JSON);
    String urlPath = req.getPathInfo();
    String[] urlParts = urlPath.split(Constants.SPLIT);
    //check url
    if (urlParts.length != 8 || !isUrlValid(urlParts)) {
      buildResponse(res, gson, message, HttpServletResponse.SC_BAD_REQUEST, Constants.INVALID_INPUTS);
      return;
    }

    //check req body
    StringBuilder sb = new StringBuilder();
    String s;
    while ((s = req.getReader().readLine()) != null) {
      sb.append(s);
    }
    Payload payload = gson.fromJson(sb.toString(), Payload.class);
    if (payload.getTime() == null || payload.getLiftID() == null) {
      buildResponse(res, gson, message, HttpServletResponse.SC_BAD_REQUEST, Constants.MISSING_PARAM);
      return;
    }


    sendToConsumer(urlParts, payload);
    buildResponse(res, gson, message, HttpServletResponse.SC_CREATED, Constants.WRITE_SUCCESS);
  }

  private boolean isUrlValid(String[] urlParts) {
      // urlPath  = "/1/vertical"
      // urlParts = [, 1, vertical]
    if (urlParts.length == 3) {
      return Constants.isSkierIDValid(Integer.parseInt(urlParts[1])) && Constants.VERTICAL.equals(urlParts[2]);
    } else if (urlParts.length == 8) {
      // urlPath  = "/1/seasons/2019/days/1/skiers/123"
      // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
      return Constants.isResortIDValid(Integer.parseInt(urlParts[1])) && Constants.SEASONS.equals(urlParts[2]) &&
          Constants.isSeasonIDValid(Integer.parseInt(urlParts[3])) && Constants.DAYS.equals(urlParts[4]) &&
          Constants.isDayIDValid(Integer.parseInt(urlParts[5])) && Constants.SKIERS.equals(urlParts[6]) &&
          Constants.isSkierIDValid(Integer.parseInt(urlParts[7]));
    }
    return false;
  }

  private void buildResponse(HttpServletResponse res, Gson gson, ResponseMsg message, int statusCode,
      String content) throws IOException {
    res.setStatus(statusCode);
    message.setMsg(content);
    res.getOutputStream().print(gson.toJson(message));
    res.getOutputStream().flush();
  }

  private void sendToConsumer(String[] urlParts, Payload liftRide)
      throws ServletException, IOException {
    try {
      //    this.time = time;
      //    this.liftID = liftID;
      //    this.resortID = resortID;
      //    this.seasonID = seasonID;
      //    this.dayID = dayID;
      //    this.skierID = skierID;
      String msg = new Message(liftRide.getTime(), liftRide.getLiftID(), Integer.parseInt(urlParts[1]),
          Integer.parseInt(urlParts[3]), Integer.parseInt(urlParts[5]),
          Integer.parseInt(urlParts[7])).toString();
      System.out.println("Message to be sent: " + msg);

      Channel channel = channelPool.borrowObject();
      channel.exchangeDeclare(Constants.EXCHANGE_NAME, BuiltinExchangeType.FANOUT, false, false, false, null);

      byte[] message = msg.getBytes();
      channel.basicPublish(Constants.EXCHANGE_NAME, Constants.ROUTE_KEY, null, message);
      channelPool.returnObject(channel);

    } catch (Exception e) {
      Logger.getLogger(SkierServlet.class.getName()).log(Level.INFO, null, e);
      throw new ServletException("Failed to send message to consumer", e);
    }


  }


}