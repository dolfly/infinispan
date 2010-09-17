package org.infinispan.interceptors;

import org.infinispan.commands.VisitableCommand;
import org.infinispan.commands.control.LockControlCommand;
import org.infinispan.commands.tx.PrepareCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.RemoveCommand;
import org.infinispan.commands.write.ReplaceCommand;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.TxInvocationContext;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.transaction.xa.DldGlobalTransaction;
import org.infinispan.transaction.xa.GlobalTransaction;
import org.infinispan.transaction.xa.RemoteTransaction;
import org.infinispan.transaction.xa.TransactionTable;

import java.util.Collections;
import java.util.Set;

/**
 * This interceptor populates the {@link org.infinispan.transaction.xa.DldGlobalTransaction} with
 * appropriate information needed in order to accomplish deadlock detection. It MUST process populate data before the
 * replication takes place, so it will do all the tasks before calling {@link org.infinispan.interceptors.base.CommandInterceptor#invokeNextInterceptor(org.infinispan.context.InvocationContext,
 * org.infinispan.commands.VisitableCommand)}.
 * <p/>
 * Note: for local caches, deadlock detection dos NOT work for aggregate operations (clear, putAll).
 *
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
public class DeadlockDetectingInterceptor extends CommandInterceptor {

   /**
    * Only does a sanity check.
    */
   @Start
   public void start() {
      if (!configuration.isEnableDeadlockDetection()) {
         throw new IllegalStateException("This interceptor should not be present in the chain as deadlock detection is not used!");
      }
   }

   @Override
   public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {
      return handleDataCommand(ctx, command);
   }

   @Override
   public Object visitRemoveCommand(InvocationContext ctx, RemoveCommand command) throws Throwable {
      return handleDataCommand(ctx, command);
   }

   @Override
   public Object visitReplaceCommand(InvocationContext ctx, ReplaceCommand command) throws Throwable {
      return handleDataCommand(ctx, command);
   }

   @Override
   public Object visitLockControlCommand(TxInvocationContext ctx, LockControlCommand command) throws Throwable {
      DldGlobalTransaction globalTransaction = (DldGlobalTransaction) ctx.getGlobalTransaction();
      if (ctx.isOriginLocal()) {
         globalTransaction.setRemoteLockIntention(command.getKeys());
         //in the case of DIST we need to propagate the list of keys. In all other situations in can be determined
         // based on the actual command
         if (configuration.getCacheMode().isDistributed()) {
            ((DldGlobalTransaction) ctx.getGlobalTransaction()).setLocksHeldAtOrigin(ctx.getLockedKeys());
         }
      }
      return handleDataCommand(ctx, command);
   }

   @Override
   public Object visitPrepareCommand(TxInvocationContext ctx, PrepareCommand command) throws Throwable {
      DldGlobalTransaction globalTransaction = (DldGlobalTransaction) ctx.getGlobalTransaction();
      if (ctx.isOriginLocal()) {
         globalTransaction.setRemoteLockIntention(command.getAffectedKeys());
      }
      Object result = invokeNextInterceptor(ctx, command);
      if (ctx.isOriginLocal()) {
         globalTransaction.setRemoteLockIntention(Collections.EMPTY_SET);
      }
      return result;
   }


   private Object handleDataCommand(InvocationContext ctx, VisitableCommand command) throws Throwable {
      return invokeNextInterceptor(ctx, command);
   }
}
