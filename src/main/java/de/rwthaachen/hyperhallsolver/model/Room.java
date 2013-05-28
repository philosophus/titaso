package de.rwthaachen.hyperhallsolver.model;

import java.io.IOException;
import java.util.Map;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class Room {

   private Map<String, Object> rawRoomData;

   private String id;

   public Room(Map<String, Object> rawRoomData) throws IOException {
      if (rawRoomData == null) {
         throw new IOException("Room object is empty");
      }
      this.rawRoomData = rawRoomData;

      parseRawRoomData();
   }

   private void parseRawRoomData() throws IOException {
      if (rawRoomData.get("id") == null) {
         throw new IOException("Room does not contain 'id' field!");
      }
      if (!(rawRoomData.get("id") instanceof String)) {
         throw new IOException("Room has 'id' field which is not a String!");
      }
      id = (String)rawRoomData.get("id");
   }

   public String getId() {
      return id;
   }
}
