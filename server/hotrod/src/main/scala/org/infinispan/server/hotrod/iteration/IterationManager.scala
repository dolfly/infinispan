package org.infinispan.server.hotrod.iteration

import java.util
import java.util.stream.Collectors
import java.util.{BitSet => JavaBitSet, UUID}

import org.infinispan.CacheStream
import org.infinispan.commons.marshall.Marshaller
import org.infinispan.commons.util.{CollectionFactory, InfinispanCollections}
import org.infinispan.configuration.cache.CompatibilityModeConfiguration
import org.infinispan.container.entries.CacheEntry
import org.infinispan.filter.CacheFilters.filterAndConvert
import org.infinispan.filter.{KeyValueFilterConverter, KeyValueFilterConverterFactory, ParamKeyValueFilterConverterFactory}
import org.infinispan.manager.EmbeddedCacheManager
import org.infinispan.server.hotrod.OperationStatus.OperationStatus
import org.infinispan.server.hotrod._
import org.infinispan.server.hotrod.logging.Log
import org.infinispan.util.concurrent.ConcurrentHashSet

/**
 * @author gustavonalle
 * @since 8.0
 */
trait IterationManager {
   type IterationId = String
   def start(cacheName: String, segments: Option[JavaBitSet], filterConverterFactory: NamedFactory, batch: Integer): IterationId
   def next(cacheName: String, iterationId: IterationId): IterableIterationResult
   def close(cacheName: String, iterationId: IterationId): Boolean
   def addKeyValueFilterConverterFactory[K, V, C](name: String, factory: KeyValueFilterConverterFactory[K, V, C]): Unit
   def removeKeyValueFilterConverterFactory(name: String): Unit
   def setMarshaller(maybeMarshaller: Option[Marshaller]): Unit
   def activeIterations: Int
}

class IterationSegmentsListener extends CacheStream.SegmentCompletionListener {
   var finished: util.Set[Integer] = new ConcurrentHashSet[Integer]()

   def clear() = finished.clear()

   override def segmentCompleted(segments: util.Set[Integer]): Unit = finished addAll segments
}

class IterationState(val listener: IterationSegmentsListener, val iterator: java.util.Iterator[CacheEntry[AnyRef, AnyRef]], val stream: CacheStream[CacheEntry[AnyRef, AnyRef]], val batch: Integer, val compatInfo: CompatInfo)

class IterableIterationResult(finishedSegments: util.Set[Integer], val statusCode: OperationStatus, entries: List[CacheEntry[AnyRef, AnyRef]], compatInfo: CompatInfo) {
   def segmentsToBytes = {
      val bs = new util.BitSet
      finishedSegments.stream().forEach((i: Integer) => bs.set(i))
      bs.toByteArray
   }

   def entrySeq: Seq[(AnyRef, AnyRef)] = {
      entries.map { entry =>
         val key = entry.getKey
         val value = entry.getValue
         if (compatInfo.enabled && compatInfo.hotRodTypeConverter.isDefined) {
            unbox(key, value)
         } else (key, value)
      }
   }

   private def unbox(key: AnyRef, value: AnyRef) = {
      val converter = compatInfo.hotRodTypeConverter.get
      (converter.unboxKey(key), converter.unboxValue(value))
   }
}

class CompatInfo(val enabled: Boolean, val hotRodTypeConverter: Option[HotRodTypeConverter])

object CompatInfo {
   def apply(config: CompatibilityModeConfiguration) =
      new CompatInfo(config.enabled(), Option(config.marshaller()).map(HotRodTypeConverter(_)))
}

class DefaultIterationManager(val cacheManager: EmbeddedCacheManager) extends IterationManager with Log {
   @volatile var marshaller: Option[_ <: Marshaller] = None

   private val iterationStateMap = CollectionFactory.makeConcurrentMap[String, IterationState]()
   private val filterConverterFactoryMap = CollectionFactory.makeConcurrentMap[String, KeyValueFilterConverterFactory[_, _, _]]()

   override def start(cacheName: String, segments: Option[JavaBitSet], namedFactory: NamedFactory, batch: Integer): IterationId = {
      val iterationId = UUID.randomUUID().toString
      val stream = cacheManager.getCache(cacheName).getAdvancedCache.cacheEntrySet.stream().asInstanceOf[CacheStream[CacheEntry[AnyRef, AnyRef]]]
      segments.map(bitSet => {
         val segments = bitSet.stream().boxed().collect(Collectors.toSet[Integer])
         stream.filterKeySegments(segments)
      })

      val segmentListener = new IterationSegmentsListener
      val compatInfo = CompatInfo(cacheManager.getCacheConfiguration(cacheName).compatibility())

      val filteredStream = for {
         (name, params) <- namedFactory
         factory <- getFactory(name)
         (filter, isBinary) <- buildFilter(factory, params.toArray)
         iterationFilter <- Some(new IterationFilter(compatInfo.enabled, Some(filter), marshaller, isBinary))
         stream <- Some(filterAndConvert(stream.asInstanceOf[util.stream.Stream[CacheEntry[Any, Any]]], iterationFilter.asInstanceOf[KeyValueFilterConverter[Any, Any, Any]]))
      } yield stream

      val iterator = filteredStream.getOrElse(stream).iterator()

      val iterationState = new IterationState(segmentListener, iterator.asInstanceOf[java.util.Iterator[CacheEntry[AnyRef, AnyRef]]], stream, batch, compatInfo)

      iterationStateMap.put(iterationId, iterationState)
      iterationId
   }

   private def getFactory(name: String) =
      Option(filterConverterFactoryMap.get(name)).orElse(throw log.missingKeyValueFilterConverterFactory(name))

   private def buildFilter(factory: KeyValueFilterConverterFactory[_, _, _], params: Array[Bytes]) = {
      factory match {
         case f: ParamKeyValueFilterConverterFactory[_, _, _] =>
            val parameters: Array[AnyRef] = if (f.binaryParam()) params.toArray else unmarshallParams(params.toArray, f)
            Some(f.getFilterConverter(parameters), f.binaryParam())
         case f: KeyValueFilterConverterFactory[_, _, _] => Some(f.getFilterConverter, false)
      }
   }

   private def unmarshallParams(params: Array[Bytes], factory: AnyRef): Array[AnyRef] = {
      val m = marshaller.getOrElse(MarshallerBuilder.genericFromInstance(Some(factory)))
      params.map(m.objectFromByteBuffer)
   }

   override def next(cacheName: String, iterationId: IterationId): IterableIterationResult = {
      val iterationState = Option(iterationStateMap.get(iterationId))
      iterationState.map { state =>
         val iterator = state.iterator
         val listener = state.listener
         val batch = state.batch
         val entries = for (i <- 0 to batch - 1; if iterator.hasNext) yield iterator.next
         new IterableIterationResult(listener.finished, OperationStatus.Success, entries.toList, state.compatInfo)
      }.getOrElse(new IterableIterationResult(InfinispanCollections.emptySet(), OperationStatus.InvalidIteration, List.empty, null))
   }

   override def close(cacheName: String, iterationId: IterationId): Boolean = {
      val iterationState = Option(iterationStateMap.get(iterationId))
      val removed = iterationState.map { state =>
         state.stream.close()
         iterationStateMap.remove(iterationId)
      }
      Option(removed).isDefined
   }

   override def addKeyValueFilterConverterFactory[K, V, C](name: String, factory: KeyValueFilterConverterFactory[K, V, C]): Unit = filterConverterFactoryMap.put(name, factory)

   override def removeKeyValueFilterConverterFactory(name: String) = filterConverterFactoryMap.remove(name)

   override def activeIterations: Int = iterationStateMap.size()

   override def setMarshaller(maybeMarshaller: Option[Marshaller]): Unit = this.marshaller = maybeMarshaller
}
