package de.rwthaachen.hyperhallsolver;

import java.io.IOException;
import java.util.Map;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class Event {
   private Map<String, Object> rawEventData;

   private String id;

   public Event(Map<String, Object> rawEventData) throws IOException {
      if (rawEventData == null) {
         throw new IOException("Event object is empty");
      }
      this.rawEventData = rawEventData;

      parseRawEventData();
   }

   private void parseRawEventData() throws IOException {
      if (rawEventData.get("id") == null) {
         throw new IOException("Event does not contain 'id' field!");
      }
      if (!(rawEventData.get("id") instanceof String)) {
         throw new IOException("Event has 'id' field which is not a String!");
      }
      id = (String)rawEventData.get("id");
   }

   public String getId() {
      return id;
   }
   
}
