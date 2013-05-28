package de.rwthaachen.hyperhallsolver.model;

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
}
