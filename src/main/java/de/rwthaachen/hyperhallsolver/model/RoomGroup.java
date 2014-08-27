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
public class RoomGroup {

   private Map<String, Object> rawRoomGroupData;
   private Double weight;
   private Set<Room> rooms;
   private Event event;

   public RoomGroup(Map<String, Object> rawRoomGroupData, Instance instance, Event event) throws IOException {
      this.rawRoomGroupData = rawRoomGroupData;
      this.event = event;

      parseRawRoomGroupData(instance);
   }

   public RoomGroup(Event event, Collection<Room> rooms) {
      this.rooms = new HashSet(rooms);
      this.event = event;
      this.weight = 0.0;
   }

   private void parseRawRoomGroupData(Instance instance) throws IOException {
      // Parse weight
      if (rawRoomGroupData.get("weight") == null) {
         weight = 0.0;  // Default to 0
      } else {
         if (!(rawRoomGroupData.get("weight") instanceof Number)) {
            throw new IOException("RoomGroup has 'weight' field which is not a Number!");
         }
         if (rawRoomGroupData.get("weight") instanceof Integer) {
            weight = ((Integer) rawRoomGroupData.get("weight")).doubleValue();
         } else {
            weight = (Double) rawRoomGroupData.get("weight");
         }
      }

      // Parse Timeslots
      rooms = new HashSet();
      if (!(rawRoomGroupData.get("rooms") instanceof Collection)) {
         throw new IOException("Field 'rooms' of an element in 'possibleRooms' of an Event must be an Array!");
      }

      for (Object roomId : (Collection) rawRoomGroupData.get("rooms")) {
         if (!(roomId instanceof String)) {
            throw new IOException("'possibleRooms' must be specified as Strings of the corresponding room id!");
         }

         if (instance.getRoom((String) roomId) == null) {
            throw new IOException("No room with id '" + (String) roomId + "' exists");
         }

         rooms.add(instance.getRoom((String) roomId));
      }
   }

   public Double getWeight() {
      return weight;
   }

   public Set<Room> getRooms() {
      return rooms;
   }

   public Event getEvent() {
      return event;
   }
}
