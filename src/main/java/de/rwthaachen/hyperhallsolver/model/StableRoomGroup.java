package de.rwthaachen.hyperhallsolver.model;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Vector;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class StableRoomGroup {

   Vector<Event> events;
   Map<String, Object> rawData;
   Instance instance;

   public StableRoomGroup(Map<String, Object> rawData, Instance instance) throws IOException {
      this.rawData = rawData;
      this.instance = instance;
      parseRawData();
   }

   public Vector<Event> getEvents() {
      return events;
   }


   private void parseRawData() throws IOException {
      if (rawData.get("events") == null) {
         throw new IOException("StableRoomGroup does not contain 'events' field!");
      }
      if (!(rawData.get("events") instanceof Collection)) {
         throw new IOException("Field 'events' in StableRoomGroup must be an Array!");
      }
      Collection eventIds = (Collection) rawData.get("events");
      events = new Vector(eventIds.size());
      for (Object eventId : eventIds) {
         if (!(eventId instanceof String)) {
            throw new IOException("events Array in StableRoomGroup must contain Strings!");
         }
         Event event = instance.getEvent((String) eventId);
         if (event == null) {
            throw new IOException("No event with ID " + (String) eventId + " found!");
         }
         events.add(event);
      }
   }
}
