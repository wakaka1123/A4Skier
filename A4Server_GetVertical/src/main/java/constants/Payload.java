package constants;

public class Payload {
  private Integer time;
  private Integer liftID;

  public Integer getTime() {
    return time;
  }

  public Integer getLiftID() {
    return liftID;
  }

  public Payload() {
  }

  public Payload(Integer time, Integer liftID) {
    this.time = time;
    this.liftID = liftID;
  }
}
