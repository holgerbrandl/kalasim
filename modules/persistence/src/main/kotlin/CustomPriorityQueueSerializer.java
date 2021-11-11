import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;

public class CustomPriorityQueueSerializer extends CollectionSerializer<PriorityQueue> {
		protected void writeHeader (Kryo kryo, Output output, PriorityQueue queue) {
			kryo.writeClassAndObject(output, queue.comparator());
		}

		protected PriorityQueue create (Kryo kryo, Input input, Class<? extends PriorityQueue> type, int size) {
			return createPriorityQueue(type, size, (Comparator)kryo.readClassAndObject(input));
		}

		protected PriorityQueue createCopy (Kryo kryo, PriorityQueue original) {
			return createPriorityQueue(original.getClass(), original.size(), original.comparator());
		}

		private PriorityQueue createPriorityQueue (Class<? extends Collection> type, int size, Comparator comparator) {
			if (type == PriorityQueue.class || type == null) return new PriorityQueue(size, comparator);
			// Use reflection for subclasses.
			try {
				Constructor constructor = type.getConstructor(int.class, Comparator.class);
				if (!constructor.isAccessible()) {
					try {
						constructor.setAccessible(true);
					} catch (SecurityException ignored) {
					}
				}
				return (PriorityQueue)constructor.newInstance(comparator);
			} catch (Exception ex) {
				throw new KryoException(ex);
			}
		}
	}