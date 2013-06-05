package de.rwthaachen.hyperhallsolver.model;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class TimeConflict {

   Map<String, Object> rawConflictData;
   String id;
   Double weight;
   Set<Event> events;

   TimeConflict(Map<String, Object> rawConflictData, Instance instance) throws IOException {
      if (rawConflictData == null) {
         throw new IOException("Conflict object is empty");
      }
      this.rawConflictData = rawConflictData;

      parseRawConflictData(instance);
   }

   private void parseRawConflictData(Instance instance) throws IOException {
      if (rawConflictData.get("id") == null) {
         throw new IOException("Conflict does not contain 'id' field!");
      }
      if (!(rawConflictData.get("id") instanceof String)) {
         throw new IOException("Conflict has 'id' field which is not a String!");
      }
      id = (String)rawConflictData.get("id");

      if (rawConflictData.get("weight") == null) {
         weight = Double.POSITIVE_INFINITY;
      } else {
         if (!(rawConflictData.get("weight") instanceof Number)) {
            throw new IOException("Conflict " + id + " has a weight field that is not a Number!");
         }
         if ((rawConflictData.get("weight") instanceof Integer)) {
            weight = ((Integer)rawConflictData.get("weight")).doubleValue();
         } else {
            weight = (Double) rawConflictData.get("weight");
         }
      }

      if (rawConflictData.get("events") == null) {
         throw new IOException("Conflict " + id + " has no 'events' field!");
      }

      if (!(rawConflictData.get("events") instanceof Collection)) {
         throw new IOException("Conflict " + id + " has 'events' field that is not an array!");
      }

      events = new HashSet();
      for (Object eventId : (Collection) rawConflictData.get("events")) {
         if (!(eventId instanceof String)) {
            throw new IOException("'events' must be specified as Strings of the corresponding event id!");
         }

         if (instance.getEvent((String) eventId) == null) {
            throw new IOException("No event with id '" + (String) eventId + "' exists");
         }

         events.add(instance.getEvent((String) eventId));
      }
   }

   public String getId() {
      return id;
   }

   public Double getWeight() {
      return weight;
   }

   public Set<Event> getEvents() {
      return events;
   }


}
