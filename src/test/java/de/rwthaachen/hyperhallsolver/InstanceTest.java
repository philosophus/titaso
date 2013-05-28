package de.rwthaachen.hyperhallsolver;

import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class InstanceTest extends TestCase {

   public InstanceTest(String testName) {
      super(testName);
   }

   @Override
   protected void setUp() throws Exception {
      super.setUp();
   }

   @Override
   protected void tearDown() throws Exception {
      super.tearDown();
   }

   public void testJsonParsing() throws IOException {
      Instance instance = new Instance(new File("instances/xxs"));

      // Check if events are parsed correctly
      assertThat(instance.getEvents().size(), is(4));
      assertThat(instance.getEvent("e1").getId(), is("e1"));
      assertThat(instance.getEvent("e2").getId(), is(not("e1")));

      // Check if rooms are parsed correctly
      assertThat(instance.getRooms().size(), is(2));
      assertThat(instance.getRoom("r1").getId(), is("r1"));

      // Check if timeslots are parsed correctly
      assertThat(instance.getTimeslots().size(), is(3));
      assertThat(instance.getTimeslot("t2").getId(), is("t2"));

      // Check if possible timeslots are parsed correctly
      assertThat(instance.getEvent("e1").getPossibleTimeslots().size(), is(2));
      assertThat(instance.getEvent("e1").getPossibleTimeslots().iterator().next().getTimeslots(), hasItem(instance.getTimeslot("t2")));
   }
}
