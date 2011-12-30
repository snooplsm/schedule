package com.happytap.transit;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Queue<Item> implements Iterable<Item> {
    private Item[] q;            // queue elements
    private int N = 0;           
    private int first = 0;       
    private int last  = 0;       

    public Queue(int size) {
        q = (Item[]) new Object[size];
    }
    
    
    // Checks if the queue is empty
    public boolean isEmpty() { return N == 0;    }

    // Returns the number of elements in the queue
    public int size()        { return N;         }


    // add a new item to the queue
    public void enqueue(Item item) {
        q[last++] = item;                        
        if (last == q.length) last = 0;          // wrap-around
        N++;
    }

    // remove the least recently added item 
    public Item dequeue() {
        if (isEmpty()) throw new RuntimeException("Queue underflow");
        Item item = q[first];
        q[first] = null;                            // to avoid loitering
        N--;
        first++;
        if (first == q.length) first = 0;           // wrap-around
        return item;
    }

    public Iterator<Item> iterator() { return new QueueIterator(); }

    // an iterator; doesn't implement remove() since it's optional
    private class QueueIterator implements Iterator<Item> {
        private int i = 0;
        public boolean hasNext()  { return i < N;                               }
        public void remove()      { throw new UnsupportedOperationException();  }

        public Item next() {
            if (!hasNext()) throw new NoSuchElementException();
            Item item = q[(i + first) % q.length];
            i++;
            return item;
        }
    }
    
    public static void main(String...args) {
    	Queue q = new Queue(10);
    	for(int i = 1; i <=20;i++) {
    		q.enqueue(i);
    	}
    	while(!q.isEmpty()) {
    		System.out.println(q.dequeue());
    	}
    }
}

/* This is the end of the Queue definition  */
/********************************************/