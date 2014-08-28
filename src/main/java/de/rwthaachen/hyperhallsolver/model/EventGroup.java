package de.rwthaachen.hyperhallsolver.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class EventGroup {

   public EventGroup(Collection<Event> events) {
      this.events = new Vector(events);
   }

   public EventGroup(Event event) {
      this.events = new Vector(1);
      this.events.add(event);
   }

   public String getId() {
      StringBuilder sb = new StringBuilder();
      Iterator<Event> eventIter = events.iterator();
      while (eventIter.hasNext()) {
         Event event = eventIter.next();
         sb.append(event.getId());
         if (eventIter.hasNext()) {
            sb.append(", ");
         }
      }
      return sb.toString();
   }

   public Set<List<Room>> getPossibleRoomGroups() {
      assert (this.events != null);
      assert (this.events.size() > 0);

      Iterator<Event> iter = this.events.iterator();
      Set<List<Room>> result = getRooms(iter.next());
      while (iter.hasNext()) {
         result.retainAll(getRooms(iter.next()));
      }
      
      return result;
   }

   private Set<List<Room>> getRooms(Event event) {
      Set<List<Room>> result = new HashSet();
      for (RoomGroup rg : event.getPossibleRooms()) {
         result.add(new ArrayList(rg.getRooms()));
      }
      return result;
   }

   public Vector<Event> events;
   public List<EventsRoomsEdge> eventsRoomsEdges = new ArrayList();
}
