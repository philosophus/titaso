package de.rwthaachen.hyperhallsolver.model;

import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class EventsRoomsEdge {

   private EventGroup eventGroup;
   private List<Room> rooms;

   public EventsRoomsEdge(EventGroup eventGroup, List<Room> rooms) {
      this.eventGroup = eventGroup;
      this.rooms = rooms;
   }

   public EventGroup getEventGroup() {
      return eventGroup;
   }

   public List<Room> getRooms() {
      return rooms;
   }

   public String getId() {
      StringBuilder sb = new StringBuilder();
      sb.append(eventGroup.getId());
      sb.append(" in rooms ");
      Iterator<Room> roomIter = rooms.iterator();
      while (roomIter.hasNext()) {
         Room room = roomIter.next();
         sb.append(room.getId());
         if (roomIter.hasNext()) {
            sb.append(", ");
         }
      }
      return sb.toString();
   }

}
