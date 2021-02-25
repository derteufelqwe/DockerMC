package de.derteufelqwe.bungeeplugin.permissions;

import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

public class TimeoutList<E> implements List<E> {

    private List<Element<E>> storage = new ArrayList<>();

    private void clearList() {
        long time = System.currentTimeMillis();
        this.storage.removeIf(e -> time >= e.timeout && e.timeout >= 0);
    }
    
    
    @Override
    public int size() {
        this.clearList();
        return this.storage.size();
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        return this.indexOf(o) >= 0;
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        this.clearList();
        return this.storage.stream().map(e -> e.data).iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        this.clearList();

        return this.storage.stream().map(e -> e.data).toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public boolean add(E e) {
        return this.storage.add(new Element<>(e, -1));
    }

    public boolean add(E e, long timeout) {
        return this.storage.add(new Element<>(e, timeout));
    }

    @Override
    public boolean remove(Object o) {
        for (int i = this.size(); i >= 0; i++) {
            Element<E> obj = this.storage.get(i);
            if (o == null && obj.data == null) {
                this.storage.remove(obj);
                return true;

            } else if (obj.data.equals(o)) {
                this.storage.remove(obj);
                return true;
            }
        }

        return false;
    }

    @Override
    public E remove(int index) {
        this.clearList();
        Element<E> element = this.storage.remove(index);

        if (element == null)
            return null;
        return element.data;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        return this.storage.addAll(c.stream().map(e -> new Element<E>(e, -1)).collect(Collectors.toList()));
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends E> c) {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public void clear() {
        this.storage.clear();
    }

    @Override
    public E get(int index) {
        this.clearList();
        Element<E> elem = this.storage.get(index);

        if (elem == null)
            return null;

        return elem.data;
    }

    @Override
    public E set(int index, E element) {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public void add(int index, E element) {
        throw new RuntimeException("Not supported.");
    }


    @Override
    public int indexOf(Object o) {
        for (int i = 0; i < this.size(); i++) {
            Element<E> elem = this.storage.get(i);

            if (elem.data == null && o == null)
                return i;

            else if (elem.data == null)
                return -1;

            else if (elem.data.equals(o))
                return i;
        }

        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new RuntimeException("Not supported.");
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator() {
        throw new RuntimeException("Not supported");
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator(int index) {
        throw new RuntimeException("Not supported");
    }

    @NotNull
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        this.clearList();

        TimeoutList<E> newList = new TimeoutList<>();

        for (Element<E> elem : this.storage.subList(fromIndex, toIndex)) {
            newList.add(elem.data, elem.timeout);
        }

        return newList;
    }

    @AllArgsConstructor
    private static class Element<E> {
        public E data;
        public long timeout;
    }
    
}
