package io.pyke.vitri.finorza.inference.api;

@FunctionalInterface
public interface Producer<T> {
	T produce();
}
