package it.unisa.studenti.bruno.auction.utilities;

import java.io.Serializable;

public class Pair<K extends Serializable, V extends Serializable> implements Serializable {

	private final K element0;
	private final V element1;

	public Pair(K element0, V element1) {
		this.element0 = element0;
		this.element1 = element1;
	}

	public K element0() {
		return element0;
	}

	public V element1() {
		return element1;
	}
	
	public boolean isEmpty() {
		return element0 == null && element1 == null;
	}

}