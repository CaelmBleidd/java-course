package ru.ifmo.rain.menshutin.arrayset;

import java.util.AbstractList;
import java.util.List;

public class ReverseListView<E> extends AbstractList<E> {
    private List<E> view;
    private boolean isReversed;

    public ReverseListView(List<E> list) {
        if (list instanceof ReverseListView) {
            view = ((ReverseListView<E>) list).view;
            isReversed = !((ReverseListView<E>) list).isReversed;
        } else {
            view = list;
            isReversed = true;
        }
    }

    @Override
    public E get(int index) {
        return isReversed ? view.get(size() - 1 - index) : view.get(index);
    }

    @Override
    public int size() {
        return view.size();
    }
}
