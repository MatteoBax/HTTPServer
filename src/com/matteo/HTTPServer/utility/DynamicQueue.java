package com.matteo.HTTPServer.utility;

public class DynamicQueue <T>{
	private Object[] coda;
	private int incremento = 0;
	private int initialCapacity = 10;
	private int testa = 0; // posizione elemento da estrarre
	private int codaDellaCoda = 0; // posizione successiva all'ultimo elemento inserito
	private int elementiInCoda = 0;
	
	private boolean dynamicStructure = false;
	
	/**
	 * Costruttore di default dell'oggetto coda (la dimensione iniziale sarà di 10, la capienza raddoppia e la struttura è statica)
	 */
	public DynamicQueue() {
		this(false);
	}

	/**
	 * Costruttore di default dell'oggetto coda (la dimensione iniziale sarà di 10, la capienza raddoppia)
	 * 
	 * @param isDynamic indica se la struttura è dinamica o meno
	 */
	public DynamicQueue(boolean isDynamic) {
		coda = new Object[10];
		this.dynamicStructure = isDynamic;
	}
	
	/**
	 * Costruttore dell'oggetto Coda con dimensione iniziale specificata (se piena la dimensione raddoppia)
	 * La struttura è statica
	 * 
	 * @param initialCapacity la dimensione iniziale dell'array
	 * @throws IllegalArgumentException se initialCapacity  < 0
	 */
	public DynamicQueue(int initialCapacity) {
		this(initialCapacity, false);
	}

	/**
	 * Costruttore dell'oggetto Coda con dimensione iniziale specificata (se piena la dimensione raddoppia)
	 * 
	 * @param initialCapacity la dimensione iniziale dell'array
	 * @param isDynamic indica se la struttura è dinamica o meno
	 * @throws IllegalArgumentException se initialCapacity  < 0
	 */
	public DynamicQueue(int initialCapacity, boolean isDynamic) {
		if(initialCapacity > 0) {
			this.initialCapacity = initialCapacity;
			coda = new Object[initialCapacity]; 
		} else {
			throw new IllegalArgumentException("Initial capacity: " + initialCapacity + " is not > 0");
		}
		this.dynamicStructure = isDynamic;
	}
	
	/**
	 * Costruttore dell'oggetto Coda con dimensione iniziale e incremento della dimensione dell'array specificati
	 * La struttura è statica
	 * 
	 * @param initialCapacity la dimensione iniziale dell'array
	 * @param increment l'incremento della dimensione dell'array
	 * @throws IllegalArgumentException se initialCapacity  < 0
	 */
	public DynamicQueue(int initialCapacity, int increment) {
		this(initialCapacity, increment, false);
	}

	/**
	 * Costruttore dell'oggetto Coda con dimensione iniziale e incremento della dimensione dell'array specificati
	 * 
	 * @param initialCapacity la dimensione iniziale dell'array
	 * @param increment l'incremento della dimensione dell'array
	 * @param isDynamic indica se la struttura è dinamica o meno
	 * @throws IllegalArgumentException se initialCapacity  < 0
	 */
	public DynamicQueue(int initialCapacity, int increment, boolean isDynamic) {
		if(initialCapacity > 0) {
			this.initialCapacity = initialCapacity;
			coda = new Object[initialCapacity]; 
		} else {
			throw new IllegalArgumentException("Initial capacity: " + initialCapacity + " is not > 0");
		}
		
		
		if(increment > 0) {
			this.incremento = increment;
		}
		this.dynamicStructure = isDynamic;
	}
	
	public void changeStructureToDynamic() {
		this.dynamicStructure = true;
	}
	
	public void changeStructureToStatic() {
		this.dynamicStructure = false;
	}
	
	private synchronized void allungaCoda() {
		Object[] tmp;
		if(this.incremento > 0) {
			tmp = new Object[this.coda.length + this.incremento];
		} else {
			tmp = new Object[this.coda.length * 2];
		}
		
		for(int i = 0; i < coda.length; i++) {
			tmp[i] = this.coda[i];
		}
				
		this.coda = tmp;
	}
	
	private synchronized void compattaCoda() {
		Object[] tmp;
		if(incremento < 10) {
			tmp = new Object[elementiInCoda + incremento];
		} else {
			tmp = new Object[elementiInCoda + 10];
		}
		
		for(int i=testa, j=0; i<coda.length && coda[i] != null; i++) {
			tmp[j++] = coda[i];
		}
		
		codaDellaCoda = elementiInCoda; // codaDellaCoda viene impostato al numero degli elementi in coda
		testa = 0; // riporto testa a 0 perchè il primo elemento da estrarre adesso è il primo nell'array
		
		coda = tmp;
	}
	
	public synchronized void add(T obj) {
		if(isFull() && dynamicStructure) {
			allungaCoda();
		}
		coda[codaDellaCoda++] = obj;
		elementiInCoda++;
		if(!isEmpty()) {
			notify();
		}
	}
	
	public synchronized T remove() {
		while(isEmpty()) {
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		T tmp = (T) coda[testa];
		coda[testa++] = null;
		elementiInCoda--;
		
		// se ci sono più di incremento posti liberi oppure se ci sono posti liberi pari alla capacità dimezzata compatto la coda
		int postiLiberi = coda.length - elementiInCoda;
		if(dynamicStructure && (postiLiberi > initialCapacity && ((incremento > 0 && postiLiberi > incremento) || (coda.length - elementiInCoda > coda.length/2 && incremento <= 0)))) {
			compattaCoda();
		}
		return tmp;
	}
	
	public synchronized boolean isEmpty() {
		return this.elementiInCoda==0;
	}
	
	public synchronized boolean isFull() {
		return this.codaDellaCoda == coda.length;
	}
	
	public synchronized int capacity(){
		return coda.length;
	}
	
	public synchronized int size() {
		return elementiInCoda;
	}
	
	@Override
	public synchronized String toString() {
		StringBuilder sb = new StringBuilder("[");
					
		for(int i=testa; i<coda.length && coda[i] != null; i++) {
			sb.append(coda[i] + ", ");
		}
		// Prima di toglere la sottostringa ", " controllo se ci sono elementi in coda sennò lancierebbe un'eccezione
		if(elementiInCoda > 0) {
			sb.delete(sb.length()-2, sb.length());
		}
		
		sb.append("]");
		return sb.toString();
	}
}
