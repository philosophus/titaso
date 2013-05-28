package de.rwthaachen.hyperhallsolver;

import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;

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
      assertEquals(4, instance.getEvents().size());
      assertEquals("e1", instance.getEvent("e1").getId());
   }
}
