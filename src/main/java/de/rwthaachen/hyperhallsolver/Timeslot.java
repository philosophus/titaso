package de.rwthaachen.hyperhallsolver;

import java.io.IOException;
import java.util.Map;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class Timeslot {
   private Map<String, Object> rawTimeslotData;

   private String id;

   public Timeslot(Map<String, Object> rawTimeslotData) throws IOException {
      if (rawTimeslotData == null) {
         throw new IOException("Room object is empty");
      }
      this.rawTimeslotData = rawTimeslotData;

      parseRawTimeslotData();
   }

   private void parseRawTimeslotData() throws IOException {
      if (rawTimeslotData.get("id") == null) {
         throw new IOException("Timeslot does not contain 'id' field!");
      }
      if (!(rawTimeslotData.get("id") instanceof String)) {
         throw new IOException("Timeslot has 'id' field which is not a String!");
      }
      id = (String)rawTimeslotData.get("id");
   }

   public String getId() {
      return id;
   }
}
