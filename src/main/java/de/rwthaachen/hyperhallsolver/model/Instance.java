package de.rwthaachen.hyperhallsolver.model;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;
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
   private Map<String, TimeConflict> strictTimeConflicts;
   private Map<String, TimeConflict> softTimeConflicts;
   private Vector<StableRoomGroup> stableRoomGroups;

//   private Map<Event, TimeConflict> conflictsOfEvent;
   public Instance(Map<String, Object> rawInstanceData) throws IOException {
      this.rawInstanceData = rawInstanceData;

      parseRawInstanceData();
   }

   public Instance(File problemFile) throws IOException {
      ObjectMapper mapper = new ObjectMapper();
      this.rawInstanceData = mapper.readValue(problemFile, Map.class);

      parseRawInstanceData();
   }

   private Instance() {
   }

   public String toJsonString() throws IOException {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.writeValueAsString(this.rawInstanceData);
   }

   public void save(File file) throws IOException {
      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(file, this.rawInstanceData);
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
      parsePossibleRooms();
      parseConflicts();
      parseStableRoomGroups();
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
         Event event = new Event((Map) rawEvent);
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
         Room room = new Room((Map) rawRoom);
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
         Timeslot timeslot = new Timeslot((Map) rawTimeslot);
         timeslots.put(timeslot.getId(), timeslot);
      }
   }

   private void parsePossibleTimeslots() throws IOException {
      for (Event event : getEvents()) {
         event.parsePossibleTimeslots(this);
      }
   }

   private void parsePossibleRooms() throws IOException {
      for (Event event : getEvents()) {
         event.parsePossibleRooms(this);
      }
   }

   private void parseConflicts() throws IOException {
      if (rawInstanceData.get("restrictions") == null) {
         return;  // no conflicts
      }
      if (!(rawInstanceData.get("restrictions") instanceof Collection)) {
         throw new IOException("Field 'restrictions' must be an Array!");
      }
      strictTimeConflicts = new HashMap();
      softTimeConflicts = new HashMap();
      for (Object rawConflict : (Collection) rawInstanceData.get("restrictions")) {
         // Check object correctness
         if (!(rawConflict instanceof Map)) {
            throw new IOException("'restrictions' array contains elements which aren't JSON objects!");
         }
         if (((Map) rawConflict).get("type") == null) {
            throw new IOException("Field 'type' of restrictions does not exist!");
         }
         if (!(((Map) rawConflict).get("type") instanceof String)) {
            throw new IOException("Field 'type' of restriction must be a String!");
         }
         String type = (String) ((Map) rawConflict).get("type");

         if (type.equals("time-conflict")) {
            TimeConflict conflict = new TimeConflict((Map) rawConflict, this);
            if (conflict.getWeight() == Double.POSITIVE_INFINITY) {
               strictTimeConflicts.put(conflict.getId(), conflict);
            } else {
               softTimeConflicts.put(conflict.getId(), conflict);
            }
         }
      }
   }

   private void parseStableRoomGroups() throws IOException {
      stableRoomGroups = new Vector();
      if (rawInstanceData.get("stableRoomGroups") == null) {
         return; // no stable room groups
      }
      if (!(rawInstanceData.get("stableRoomGroups") instanceof Collection)) {
         throw new IOException("Field 'stableRoomGroups' must be an Array!");
      }
      for (Object rawStableRoomGroup : (Collection) rawInstanceData.get("stableRoomGroups")) {
         if (!(rawStableRoomGroup instanceof Map)) {
            throw new IOException("'stableRoomGroups' array contains elements which aren't JSON objects!");
         }
         stableRoomGroups.add(new StableRoomGroup((Map) rawStableRoomGroup, this));
      }
   }

   public void assignSolution(Map<Event, TimeslotGroup> assignedTimeslots, Map<Event, RoomGroup> assignedRooms) {
      for (Event event : this.getEvents()) {
         event.assignSolution(assignedTimeslots.get(event), assignedRooms.get(event));
      }
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

   public Collection<TimeConflict> getStrictTimeConflicts() {
      return strictTimeConflicts.values();
   }

   public TimeConflict getStrictTimeConflict(String id) {
      return strictTimeConflicts.get(id);
   }

   public Collection<TimeConflict> getSoftTimeConflicts() {
      return softTimeConflicts.values();
   }

   public TimeConflict getSoftTimeConflict(String id) {
      return softTimeConflicts.get(id);
   }

   public Collection<TimeConflict> getTimeConflicts() {
      Collection<TimeConflict> result = new HashSet();
      result.addAll(getStrictTimeConflicts());
      result.addAll(getSoftTimeConflicts());
      return result;
   }

   public TimeConflict getTimeConflict(String id) {
      TimeConflict result = getStrictTimeConflict(id);
      if (result == null) {
         result = getSoftTimeConflict(id);
      }
      return result;
   }

   public Collection<StableRoomGroup> getStableRoomGroups() {
      return stableRoomGroups;
   }

   static public Instance createRandom() {
      Instance randomInstance = new Instance();



      return randomInstance;
   }
}
