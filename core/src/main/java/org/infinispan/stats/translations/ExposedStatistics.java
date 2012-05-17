package org.infinispan.stats.translations;

/**
 * Date: 28/12/11
 * Time: 15:38
 * @author Diego Didona <didona@gsd.inesc-id.pt>
 * @author Pedro Ruivo
 * @since 5.2
 */

public class ExposedStatistics {
   public static final int NUM_STATS = IspnStats.values().length;
   public static final int NUM_TX_CLASSES = TransactionalClasses.values().length;

   public static enum TransactionalClasses {
      DEFAULT_CLASS
   }

   public static enum IspnStats {
      LOCK_WAITING_TIME(true, true),         // C
      LOCK_HOLD_TIME(true, true),            // C
      NUM_HELD_LOCKS(true, true),            // C
      NUM_HELD_LOCKS_SUCCESS_TX(true, false),   // L
      ROLLBACK_EXECUTION_TIME(true, true),   // C
      NUM_ROLLBACKS(true, true),             // C
      WR_TX_LOCAL_EXECUTION_TIME(true, false),  // L      
      REPLAY_TIME(false, true),        // R
      NUM_REPLAYED_TXS(false, true),   // R
      NUM_COMMITTED_RO_TX(true, true), // C
      NUM_COMMITTED_WR_TX(true, true), // C
      NUM_ABORTED_WR_TX(true, true),   // C
      NUM_ABORTED_RO_TX(true, true),   // C      
      NUM_PREPARES(true, false), // L
      NUM_PUTS(true, true),               // C
      COMMIT_EXECUTION_TIME(true, true), // C
      LOCAL_EXEC_NO_CONT(false, false),            // ONLY FOR QUERY, derived on the fly
      LOCAL_CONTENTION_PROBABILITY(false, false),  // ONLY FOR QUERY, derived on the fly
      LOCK_CONTENTION_TO_LOCAL(true, true),  // C
      LOCK_CONTENTION_TO_REMOTE(true, true), // C
      NUM_SUCCESSFUL_PUTS(true, false),   // L, this includes also repeated puts over the same item
      PUTS_PER_LOCAL_TX(false, false), // ONLY FOR QUERY, derived on the fly
      NUM_WAITED_FOR_LOCKS(true, true),   // C      
      NUM_REMOTE_GET(true, true),         // C
      REMOTE_GET_EXECUTION(true, true),   // C
      REMOTE_PUT_EXECUTION(true, true),   // C
      NUM_REMOTE_PUT(true, true),         // C
      ARRIVAL_RATE(false, false),                  // ONLY FOR QUERY, derived on the fly
      TX_WRITE_PERCENTAGE(false, false),           // ONLY FOR QUERY, derived on the fly
      SUCCESSFUL_WRITE_PERCENTAGE(false, false),   // ONLY FOR QUERY, derived on the fly
      WR_TX_ABORTED_EXECUTION_TIME(true, true),    //C
      WR_TX_SUCCESSFUL_EXECUTION_TIME(true, true), //C
      RO_TX_SUCCESSFUL_EXECUTION_TIME(true, true), //C
      RO_TX_ABORTED_EXECUTION_TIME(true, true),    //C
      NUM_COMMIT_COMMAND(true, true),              //C
      APPLICATION_CONTENTION_FACTOR(false, false), // ONLY FOR QUERY

      //Lock querying
      NUM_LOCK_PER_LOCAL_TX(false, false),         // ONLY FOR QUERY, derived on the fly
      NUM_LOCK_PER_REMOTE_TX(false, false),        // ONLY FOR QUERY, derived on the fly
      NUM_LOCK_PER_SUCCESS_LOCAL_TX(false, false), // ONLY FOR QUERY, derived on the fly

      //commands size
      PREPARE_COMMAND_SIZE(true, false),        // L
      COMMIT_COMMAND_SIZE(true, false),         // L
      CLUSTERED_GET_COMMAND_SIZE(true, false),  // L

      //Lock failed stuff
      NUM_LOCK_FAILED_TIMEOUT(true, false),  //L
      NUM_LOCK_FAILED_DEADLOCK(true, false), //L

      //RTT STUFF: everything is local && synchronous communication
      NUM_RTTS_PREPARE(true, false),   // L
      RTT_PREPARE(true, false),        // L      
      NUM_RTTS_COMMIT(true, false),    // L
      RTT_COMMIT(true, false),         // L
      NUM_RTTS_ROLLBACK(true, false),  // L
      RTT_ROLLBACK(true, false),       // L
      NUM_RTTS_GET(true, false),       // L
      RTT_GET(true, false),            // L

      //SEND STUFF: everything is local && asynchronous communication
      ASYNC_PREPARE(true, false),               // L
      NUM_ASYNC_PREPARE(true, false),           // L
      ASYNC_COMMIT(true, false),                // L
      NUM_ASYNC_COMMIT(true, false),            // L
      ASYNC_ROLLBACK(true, false),              // L
      NUM_ASYNC_ROLLBACK(true, false),          // L
      ASYNC_COMPLETE_NOTIFY(true, false),       // L
      NUM_ASYNC_COMPLETE_NOTIFY(true, false),   // L            

      //Number of nodes involved stuff
      NUM_NODES_PREPARE(true, false),           //L
      NUM_NODES_COMMIT(true, false),            //L
      NUM_NODES_ROLLBACK(true, false),          //L
      NUM_NODES_COMPLETE_NOTIFY(true, false),   //L
      NUM_NODES_GET(true, false);               //L

      private boolean local;
      private boolean remote;

      IspnStats(boolean local, boolean remote) {
         this.local = local;
         this.remote = remote;
      }

      public boolean isLocal() {
         return local;
      }

      public boolean isRemote() {
         return remote;
      }
   }
}
