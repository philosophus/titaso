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
public class TimeslotGroup {

   Map<String, Object> rawTimeslotGroupData;
   Double weight;
   Set<Timeslot> timeslots;

   public TimeslotGroup(Map<String, Object> rawTimeslotGroupData, Instance instance) throws IOException {
      this.rawTimeslotGroupData = rawTimeslotGroupData;

      parseRawTimeslotGroupData(instance);
   }

   private void parseRawTimeslotGroupData(Instance instance) throws IOException {
      // Parse weight
      if (rawTimeslotGroupData.get("weight") == null) {
         weight = 0.0;  // Default to 0
      } else {
         if (!(rawTimeslotGroupData.get("weight") instanceof Number)) {
            throw new IOException("TimeslotGroup has 'weight' field which is not a Number!");
         }
         if (rawTimeslotGroupData.get("weight") instanceof Integer) {
            weight = ((Integer) rawTimeslotGroupData.get("weight")).doubleValue();
         } else {
            weight = (Double) rawTimeslotGroupData.get("weight");
         }
      }

      // Parse Timeslots
      timeslots = new HashSet();
      if (!(rawTimeslotGroupData.get("timeslots") instanceof Collection)) {
         throw new IOException("Field 'timeslots' of an element in 'possibleTimeslots' of an Event must be an Array!");
      }

      for (Object timeslotId : (Collection) rawTimeslotGroupData.get("timeslots")) {
         if (!(timeslotId instanceof String)) {
            throw new IOException("'possibleTimeslots' must be specified as Strings of the corresponding timeslots id!");
         }

         if (instance.getTimeslot((String) timeslotId) == null) {
            throw new IOException("No timeslot with id '" + (String) timeslotId + "' exists");
         }

         timeslots.add(instance.getTimeslot((String) timeslotId));
      }

   }

   public Double getWeight() {
      return weight;
   }

   public Set<Timeslot> getTimeslots() {
      return timeslots;
   }
}
