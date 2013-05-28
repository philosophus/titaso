package de.rwthaachen.hyperhallsolver;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class Event {
   private Map<String, Object> rawEventData;

   private String id;

   private Set<TimeslotGroup> possibleTimeslots;

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

   public void parsePossibleTimeslots(Instance instance) throws IOException {
      possibleTimeslots = new HashSet();

      if (rawEventData.get("possibleTimeslots") == null) {
         return; // if no timeslot is possible the event simply cannot get assigned one
      }
      if (!(rawEventData.get("possibleTimeslots") instanceof Collection)) {
         throw new IOException("Field 'possibleTimeslots' of an Event must be an Array!");
      }

      for (Object rawTimeslotGroupData : (Collection) rawEventData.get("possibleTimeslots")) {
         if (!(rawTimeslotGroupData instanceof Map)) {
            throw new IOException("'possibleTimeslots' array of Event contains elemts which aren't JSON objects!");
         }

         possibleTimeslots.add(new TimeslotGroup((Map)rawTimeslotGroupData, instance));
      }
   }

   public String getId() {
      return id;
   }

   public Set<TimeslotGroup> getPossibleTimeslots() {
      return possibleTimeslots;
   }
   
}
