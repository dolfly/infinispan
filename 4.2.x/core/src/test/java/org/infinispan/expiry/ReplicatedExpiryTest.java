package org.infinispan.expiry;

import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.MortalCacheEntry;
import org.infinispan.container.entries.TransientCacheEntry;
import org.infinispan.container.entries.TransientMortalCacheEntry;
import org.infinispan.test.MultipleCacheManagersTest;
import org.testng.annotations.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Test(groups = "functional", testName = "expiry.ReplicatedExpiryTest")
public class ReplicatedExpiryTest extends MultipleCacheManagersTest {

   protected void createCacheManagers() throws Throwable {
      Configuration cfg = getDefaultClusteredConfig(Configuration.CacheMode.REPL_SYNC);
      createClusteredCaches(2, "cache", cfg);      
   }

   public void testLifespanExpiryReplicates() throws InterruptedException {
      Cache c1 = cache(0,"cache");
      Cache c2 = cache(1,"cache");
      long lifespan = 3000;
      c1.put("k", "v", lifespan, MILLISECONDS);
      InternalCacheEntry ice = c2.getAdvancedCache().getDataContainer().get("k");

      assert ice instanceof MortalCacheEntry;
      assert ice.getLifespan() == lifespan;
      assert ice.getMaxIdle() == -1;
   }

   public void testIdleExpiryReplicates() throws InterruptedException {
      Cache c1 = cache(0,"cache");
      Cache c2 = cache(1,"cache");
      long idle = 3000;
      c1.put("k", "v", -1, MILLISECONDS, idle, MILLISECONDS);
      InternalCacheEntry ice = c2.getAdvancedCache().getDataContainer().get("k");

      assert ice instanceof TransientCacheEntry;
      assert ice.getMaxIdle() == idle;
      assert ice.getLifespan() == -1;
   }

   public void testBothExpiryReplicates() throws InterruptedException {
      Cache c1 = cache(0,"cache");
      Cache c2 = cache(1,"cache");
      long lifespan = 10000;
      long idle = 3000;
      c1.put("k", "v", lifespan, MILLISECONDS, idle, MILLISECONDS);
      InternalCacheEntry ice = c2.getAdvancedCache().getDataContainer().get("k");
      assert ice instanceof TransientMortalCacheEntry;
      assert ice.getLifespan() == lifespan : "Expected " + lifespan + " but was " + ice.getLifespan();
      assert ice.getMaxIdle() == idle : "Expected " + idle + " but was " + ice.getMaxIdle();
   }
}
