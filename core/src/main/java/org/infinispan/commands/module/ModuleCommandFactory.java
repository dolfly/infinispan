package org.infinispan.commands.module;

import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.remote.CacheRpcCommand;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;

import java.util.Map;

/**
 * Modules which wish to implement their own commands and visitors must also provide an implementation of this interface
 * and declare it in their <tt>infinispan-module.properties</tt> file under key <tt>infinispan.module.command.factory</tt>.
 * <p />
 * Implementations <b>must</b> be public classes with a public, no-arg constructor for instantiation.
 * <p />
 * Note that this is a {@link Scopes#GLOBAL} component and as such cannot have {@link Inject} methods referring to
 * {@link Scopes#NAMED_CACHE} scoped components.  For such components, use a corresponding {@link Scopes#NAMED_CACHE}-scoped
 * {@link ModuleCommandInitializer}.
 * <p />
 * @author Manik Surtani
 * @since 5.0
 */
@Scope(Scopes.GLOBAL)
public interface ModuleCommandFactory {
   /**
    * Provides a map of command IDs to command types of all the commands handled by the command factory instance.
    * Unmarshalling requests for these command IDs will be dispatched to this implementation.
    *
    * @return map of command IDs to command types handled by this implementation.
    */
   Map<Byte, Class<? extends ReplicableCommand>> getModuleCommands();

   /**
    * Construct and initialize a {@link ReplicableCommand} based on the command
    * id and argument array passed in.
    *
    * @param commandId command id to construct
    * @param args array of arguments with which to initialize the ReplicableCommand
    * @return a ReplicableCommand
    */
   ReplicableCommand fromStream(byte commandId, Object[] args);

   /**
    * Construct and initialize a {@link CacheRpcCommand} based on the command
    * id and argument array passed in.
    *
    * @param commandId  command id to construct
    * @param args       array of arguments with which to initialize the {@link CacheRpcCommand}
    * @param cacheName  cache name at which command to be created is directed
    * @return           a {@link CacheRpcCommand}
    */
   CacheRpcCommand fromStream(byte commandId, Object[] args, String cacheName);
}
