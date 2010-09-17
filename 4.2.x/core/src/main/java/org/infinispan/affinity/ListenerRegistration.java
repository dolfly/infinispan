package org.infinispan.affinity;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;

/**
* Used for registering various cache notifications.
*
* @author Mircea.Markus@jboss.com
* @since 4.1
*/
@Listener
public class ListenerRegistration {
   private final KeyAffinityServiceImpl keyAffinityService;

   public ListenerRegistration(KeyAffinityServiceImpl keyAffinityService) {
      this.keyAffinityService = keyAffinityService;
   }

   @ViewChanged
   public void handleViewChange(ViewChangedEvent vce) {
      keyAffinityService.handleViewChange(vce);
   }

   @CacheStopped
   public void handleCacheStopped(CacheStoppedEvent cse) {
      keyAffinityService.handleCacheStopped(cse);
   }
}
