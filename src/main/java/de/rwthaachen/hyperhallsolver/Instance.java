package de.rwthaachen.hyperhallsolver;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.codehaus.jackson.map.ObjectMapper;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class Instance {

   /**
    * Contains the entire raw JSON object data.
    */
   private Map<String, Object> rawInstanceData;

   private Map<String, Event> events;

   private Map<String, Room> rooms;

   private Map<String, Timeslot> timeslots;

   public Instance(Map<String, Object> rawInstanceData) throws IOException {
      this.rawInstanceData = rawInstanceData;

      parseRawInstanceData();
   }

   public Instance(File problemFile) throws IOException {
      ObjectMapper mapper = new ObjectMapper();
      this.rawInstanceData = mapper.readValue(problemFile, Map.class);

      parseRawInstanceData();
   }

   public String toJsonString() throws IOException {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.writeValueAsString(this.rawInstanceData);
   }

   /**
    * Turns the necessary parts of the raw instance data into the object model.
    */
   private void parseRawInstanceData() throws IOException {
      if (rawInstanceData == null) {
         throw new IOException("Instance data appears to be empty!");
      }
      parseRawEvents();
      parseRawRooms();
      parseRawTimeslots();
      parsePossibleTimeslots();
   }

   private void parseRawEvents() throws IOException {
      if (rawInstanceData.get("events") == null) {
         throw new IOException("No events are specified!");
      }
      if (!(rawInstanceData.get("events") instanceof Collection)) {
         throw new IOException("Field 'events' must be an Array!");
      }

      events = new HashMap();
      for (Object rawEvent : (Collection) rawInstanceData.get("events")) {
         if (!(rawEvent instanceof Map)) {
            throw new IOException("'events' array contains elemts which aren't JSON objects!");
         }
         Event event = new Event((Map)rawEvent);
         events.put(event.getId(), event);
      }
   }

   private void parseRawRooms() throws IOException {
      if (rawInstanceData.get("rooms") == null) {
         throw new IOException("No rooms are specified!");
      }
      if (!(rawInstanceData.get("rooms") instanceof Collection)) {
         throw new IOException("Field 'rooms' must be an Array!");
      }

      rooms = new HashMap();
      for (Object rawRoom : (Collection) rawInstanceData.get("rooms")) {
         if (!(rawRoom instanceof Map)) {
            throw new IOException("'rooms' array contains elemts which aren't JSON objects!");
         }
         Room room = new Room((Map)rawRoom);
         rooms.put(room.getId(), room);
      }
   }

   private void parseRawTimeslots() throws IOException {
      if (rawInstanceData.get("timeslots") == null) {
         throw new IOException("No timeslots are specified!");
      }
      if (!(rawInstanceData.get("timeslots") instanceof Collection)) {
         throw new IOException("Field 'timeslots' must be an Array!");
      }

      timeslots = new HashMap();
      for (Object rawTimeslot : (Collection) rawInstanceData.get("timeslots")) {
         if (!(rawTimeslot instanceof Map)) {
            throw new IOException("'timeslots' array contains elemts which aren't JSON objects!");
         }
         Timeslot timeslot = new Timeslot((Map)rawTimeslot);
         timeslots.put(timeslot.getId(), timeslot);
      }
   }

   private void parsePossibleTimeslots() {
      
   }

   public Collection<Event> getEvents() {
      return events.values();
   }

   public Event getEvent(String id) {
      return events.get(id);
   }

   public Collection<Room> getRooms() {
      return rooms.values();
   }

   public Room getRoom(String id) {
      return rooms.get(id);
   }

   public Collection<Timeslot> getTimeslots() {
      return timeslots.values();
   }

   public Timeslot getTimeslot(String id) {
      return timeslots.get(id);
   }
}
