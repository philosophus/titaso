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
   }

   private void parseRawEvents() throws IOException {
      if (rawInstanceData.get("events") == null) {
         throw new IOException("No events are specified!");
      }
      if (!(rawInstanceData.get("events") instanceof Collection)) {
         throw new IOException("Field 'events' must be an Array!");
      }

      events = new HashMap();
      for (Object event : (Collection) rawInstanceData.get("events")) {
         if (!(event instanceof Map)) {
            throw new IOException("'events' array contains elemts which aren't JSON objects!");
         }
         Event eventObject = new Event((Map)event);
         events.put(eventObject.getId(), eventObject);
      }
   }

   public Collection<Event> getEvents() {
      return events.values();
   }

   public Event getEvent(String id) {
      return events.get(id);
   }
}
