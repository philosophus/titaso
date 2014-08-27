package de.rwthaachen.hyperhallsolver.model;

import java.util.Collection;
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

   public Vector<Event> events;

}
