package org.infinispan.partitionhandling;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.marshall.core.Ids;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

/**
* @author Mircea Markus
* @author Dan Berindei
* @since 7.0
*/
public enum AvailabilityMode {
   /** Regular operation mode */
   AVAILABLE,
   /** Data can not be safely accessed because of a network split or successive nodes leaving. */
   DEGRADED_MODE,
   ;

   public AvailabilityMode min(AvailabilityMode other) {
      if (this == DEGRADED_MODE || other == DEGRADED_MODE)
         return DEGRADED_MODE;
      return AVAILABLE;
   }


   public static class Externalizer extends AbstractExternalizer<AvailabilityMode> {
      @Override
      public Integer getId() {
         return Ids.AVAILABILITY_MODE;
      }

      @Override
      public Set<Class<? extends AvailabilityMode>> getTypeClasses() {
         return Util.<Class<? extends AvailabilityMode>>asSet(AvailabilityMode.class);
      }

      @Override
      public void writeObject(ObjectOutput output, AvailabilityMode AvailabilityMode) throws IOException {
         output.writeByte(AvailabilityMode.ordinal());
      }

      @Override
      public AvailabilityMode readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         byte AvailabilityModeByte = input.readByte();
         try {
            // values() cached by Class.getEnumConstants()
            return AvailabilityMode.values()[AvailabilityModeByte];
         } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalStateException("Unknown AvailabilityMode index: " + AvailabilityModeByte);
         }
      }
   }
}
