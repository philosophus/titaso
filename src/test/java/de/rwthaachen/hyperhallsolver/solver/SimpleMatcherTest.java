package de.rwthaachen.hyperhallsolver.solver;

import de.rwthaachen.hyperhallsolver.model.Event;
import de.rwthaachen.hyperhallsolver.model.Instance;
import de.rwthaachen.hyperhallsolver.model.TimeslotGroup;
import gurobi.GRBEnv;
import gurobi.GRBException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class SimpleMatcherTest {

   private Instance instance;
   private Map<TimeslotGroup, Event> assignedTimeslots;


   public SimpleMatcherTest() {
   }

   @BeforeClass
   public static void setUpClass() {
   }

   @AfterClass
   public static void tearDownClass() {
   }

   @Before
   public void setUp() throws IOException {
      instance = new Instance(new File("instances/xxs"));

      assignedTimeslots = new HashMap();
      for (TimeslotGroup possibleTimeslot: instance.getEvent("e1").getPossibleTimeslots()) {
         if (possibleTimeslot.getWeight() == 1.0) {
            assignedTimeslots.put(possibleTimeslot, instance.getEvent("e1"));
         }
      }
      for (TimeslotGroup possibleTimeslot: instance.getEvent("e2").getPossibleTimeslots()) {
         if (possibleTimeslot.getWeight() == 1.0) {
            assignedTimeslots.put(possibleTimeslot, instance.getEvent("e2"));
         }
      }
      for (TimeslotGroup possibleTimeslot: instance.getEvent("e3").getPossibleTimeslots()) {
         if (possibleTimeslot.getWeight() == 3.0) {
            assignedTimeslots.put(possibleTimeslot, instance.getEvent("e3"));
         }
      }
      for (TimeslotGroup possibleTimeslot: instance.getEvent("e4").getPossibleTimeslots()) {
         if (possibleTimeslot.getWeight() == 2.0) {
            assignedTimeslots.put(possibleTimeslot, instance.getEvent("e4"));
         }
      }
   }

   @After
   public void tearDown() {
   }

   @Test
   public void testSimpleMatcher() throws GRBException {
      SimpleMatcher matcher = new SimpleMatcher(instance, assignedTimeslots);

      matcher.setUp(new GRBEnv());

      matcher.createVariables();

      matcher.createShallBeMatchedConstraintsAndVariables();

      matcher.createRoomConflictConstraints();

      matcher.setObjective();

      matcher.solve();
   }
}
