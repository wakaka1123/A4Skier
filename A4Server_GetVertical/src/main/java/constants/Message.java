package constants;

public class Message {
  private int time;
  private int liftID;
  private int resortID;
  private int seasonID;
  private int dayID;
  private int skierID;

  public Message(){}

  public Message(int time, int liftID, int resortID, int seasonID, int dayID, int skierID) {
    this.time = time;
    this.liftID = liftID;
    this.resortID = resortID;
    this.seasonID = seasonID;
    this.dayID = dayID;
    this.skierID = skierID;
  }

  @Override
  public String toString() {
    return time +
        "," + liftID +
        "," + resortID +
        "," + seasonID +
        "," + dayID +
        "," + skierID;
  }

  public int getTime() {
    return time;
  }

  public void setTime(int time) {
    this.time = time;
  }

  public int getLiftID() {
    return liftID;
  }

  public void setLiftID(int liftID) {
    this.liftID = liftID;
  }

  public int getResortID() {
    return resortID;
  }

  public void setResortID(int resortID) {
    this.resortID = resortID;
  }

  public int getSeasonID() {
    return seasonID;
  }

  public void setSeasonID(int seasonID) {
    this.seasonID = seasonID;
  }

  public int getDayID() {
    return dayID;
  }

  public void setDayID(int dayID) {
    this.dayID = dayID;
  }

  public int getSkierID() {
    return skierID;
  }

  public void setSkierID(int skierID) {
    this.skierID = skierID;
  }
}
