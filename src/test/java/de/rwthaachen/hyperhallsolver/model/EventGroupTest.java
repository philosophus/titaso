package de.rwthaachen.hyperhallsolver.model;

import java.util.Collection;
import java.util.Vector;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class EventGroupTest {

   public EventGroupTest() {
   }

   @BeforeClass
   public static void setUpClass() {
   }

   @AfterClass
   public static void tearDownClass() {
   }

   @Before
   public void setUp() {
   }

   @After
   public void tearDown() {
   }

   /**
    * Test of getPossibleRoomGroups method, of class EventGroup.
    */
   @Test
   public void testGetPossibleRoomGroups() {
      Room r1 = new Room("R1");
      Room r2 = new Room("R2");
      Vector<Room> rooms1 = new Vector();
      rooms1.add(r2);
      Vector<Room> rooms2 = new Vector();
      rooms2.add(r1);
      rooms2.add(r2);
      Vector<Vector<Room>> rrooms1 = new Vector();
      rrooms1.add(rooms1);
      rrooms1.add(rooms2);
      Vector<Vector<Room>> rrooms2 = new Vector();
      rrooms2.add(rooms2);

      Event e1 = new Event("E1", new Vector(), rrooms1);
      Event e2 = new Event("E2", new Vector(), rrooms2);
      Vector<Event> events = new Vector();
      events.add(e1);
      events.add(e2);

      EventGroup eg = new EventGroup(events);
      assertThat(eg.getPossibleRoomGroups().size(), is(1));
      assertThat(eg.getPossibleRoomGroups().iterator().next().size(), is(2));
   }
}
