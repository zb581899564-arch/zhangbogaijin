package org.uma.jmetal.util.pseudorandom.impl;

import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * An {@link AuditableRandomGenerator} is a {@link PseudoRandomGenerator} which can be audited
 * to know when a random generation method is called.
 * 
 * @author Matthieu Vergne <matthieu.vergne@gmail.com>
 *
 */
@SuppressWarnings("serial")
public class AuditableRandomGenerator implements PseudoRandomGenerator {

	public static enum RandomMethod {
		BOUNDED_INT, BOUNDED_DOUBLE, DOUBLE
	}

	public static class Bounds {
		final Number lower;
		final Number upper;

		public Bounds(Number lower, Number upper) {
			this.lower = Objects.requireNonNull(lower, "No lower bound provided");
			this.upper = Objects.requireNonNull(upper, "No upper bound provided");
		}

		public Number getLowerBound() {
			return lower;
		}

		public Number getUpperBound() {
			return upper;
		}
	}

	private final PseudoRandomGenerator generator;
	private final String randomStreamId;
	private long drawOrdinal;
	private final Set<Consumer<Audit>> listeners = new HashSet<Consumer<Audit>>();

	public AuditableRandomGenerator(PseudoRandomGenerator generator) {
		this(generator, generator == null ? "UNSPECIFIED" : generator.getName());
	}

	public AuditableRandomGenerator(PseudoRandomGenerator generator, String randomStreamId) {
		this.generator = Objects.requireNonNull(generator, "No generator provided");
		this.randomStreamId = Objects.requireNonNull(randomStreamId, "No random stream id provided");
	}

	public PseudoRandomGenerator getDelegate() {
		return generator;
	}

	public String getRandomStreamId() {
		return randomStreamId;
	}

	public long getDrawOrdinal() {
		return drawOrdinal;
	}

	public static class Audit {
		private final long ordinal;
		private final String randomStreamId;
		private final RandomMethod method;
		private final Optional<Bounds> bounds;
		private final Number result;

		public Audit(RandomMethod method, Bounds bounds, Number result) {
			this(-1L, "UNSPECIFIED", method, bounds, result);
		}

		public Audit(long ordinal, String randomStreamId, RandomMethod method, Bounds bounds, Number result) {
			this.ordinal = ordinal;
			this.randomStreamId = Objects.requireNonNull(randomStreamId, "No random stream id provided");
			this.method = Objects.requireNonNull(method, "No method provided");
			this.bounds = Optional.ofNullable(bounds);
			this.result = Objects.requireNonNull(result, "No result provided");
		}

		public long getOrdinal() {
			return ordinal;
		}

		public String getRandomStreamId() {
			return randomStreamId;
		}

		public RandomMethod getMethod() {
			return method;
		}

		public Optional<Bounds> getBounds() {
			return bounds;
		}

		public Number getResult() {
			return result;
		}
	}

	public void addListener(Consumer<Audit> listener) {
		listeners.add(listener);
	}

	public void removeListener(Consumer<Audit> listener) {
		listeners.remove(listener);
	}

	private void notifies(Audit audit) {
		for (Consumer<Audit> listener : listeners) {
			listener.accept(audit);
		}
	}

	private Audit audit(RandomMethod method, Bounds bounds, Number result) {
		return new Audit(++drawOrdinal, randomStreamId, method, bounds, result);
	}

	@Override
	public int nextInt(int lowerBound, int upperBound) {
		int result = generator.nextInt(lowerBound, upperBound);
		notifies(audit(RandomMethod.BOUNDED_INT, new Bounds(lowerBound, upperBound), result));
		return result;
	}

	@Override
	public double nextDouble(double lowerBound, double upperBound) {
		double result = generator.nextDouble(lowerBound, upperBound);
		notifies(audit(RandomMethod.BOUNDED_DOUBLE, new Bounds(lowerBound, upperBound), result));
		return result;
	}

	@Override
	public double nextDouble() {
		double result = generator.nextDouble();
		notifies(audit(RandomMethod.DOUBLE, null, result));
		return result;
	}

	@Override
	public void setSeed(long seed) {
		generator.setSeed(seed);
	}

	@Override
	public long getSeed() {
		return generator.getSeed();
	}

	@Override
	public String getName() {
		return generator.getName();
	}

}
