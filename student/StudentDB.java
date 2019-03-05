package ru.ifmo.rain.menshutin.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentGroupQuery {

    private Stream<Map.Entry<String, List<Student>>> getGroupStream(Collection<Student> collection) {
        return collection
                .stream()
                .collect(Collectors.groupingBy(Student::getGroup))
                .entrySet()
                .stream();
    }

    private List<Group> getGroupsBy(Collection<Student> collection, Comparator<Student> comparator) {
        return getGroupStream(collection)
                .map(mapEntry -> new Group(mapEntry.getKey(), sortStudentsBy(mapEntry.getValue(), comparator)))
                .sorted(Comparator.comparing(Group::getName))
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> collection) {
        return getGroupsBy(collection, COMPARATOR_BY_NAME);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> collection) {
        return getGroupsBy(collection, Student::compareTo);
    }

    private String getLargestGroupBy(Collection<Student> collection, Comparator<Group> comparator) {
        return getGroupStream(collection)
                .map(entry -> new Group(entry.getKey(), entry.getValue()))
                .max(comparator)
                .map(Group::getName)
                .orElse("");
    }

    @Override
    public String getLargestGroup(Collection<Student> collection) {
        return getLargestGroupBy(collection,
                                 Comparator
                                         .<Group, Integer>comparing(group -> group.getStudents().size())
                                         .thenComparing(Comparator.comparing(Group::getName).reversed()));
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> collection) {
        return getLargestGroupBy(collection,
                                 Comparator
                                         .<Group, Integer>comparing(group -> getDistinctFirstNames(group.getStudents()).size())
                                         .thenComparing(Comparator.comparing(Group::getName).reversed()));
    }

    private List<String> getListOfSomething(List<Student> list, Function<Student, String> mapFunction) {
        return list
                .stream()
                .map(mapFunction)
                .collect(Collectors.toList());

    }

    @Override
    public List<String> getFirstNames(List<Student> list) {
        return getListOfSomething(list, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> list) {
        return getListOfSomething(list, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> list) {
        return getListOfSomething(list, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> list) {
        return getListOfSomething(list, this::getFullName);

    }

    private String getFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> list) {
        return list
                .stream()
                .map(Student::getFirstName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> list) {
        return list
                .stream()
                .min(Student::compareTo)
                .map(Student::getFirstName)
                .orElse("");
    }

    private List<Student> sortStudentsBy(Collection<Student> collection, Comparator<Student> comparator) {
        return collection
                .stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> collection) {
        return sortStudentsBy(collection, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> collection) {
        return sortStudentsBy(collection, COMPARATOR_BY_NAME);
    }

    private final static Comparator<Student> COMPARATOR_BY_NAME = Comparator.comparing(Student::getLastName)
                                                                            .thenComparing(Student::getFirstName)
                                                                            .thenComparingInt(Student::getId);

    private List<Student> findStudentBy(Collection<Student> collection,
                                        Function<Student, String> mapFunction,
                                        String name) {
        return collection
                .stream()
                .filter(student -> mapFunction.apply(student).equals(name))
                .sorted(COMPARATOR_BY_NAME)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> collection, String s) {
        return findStudentBy(collection, Student::getFirstName, s);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> collection, String s) {
        return findStudentBy(collection, Student::getLastName, s);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> collection, String s) {
        return findStudentBy(collection, Student::getGroup, s);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> collection, String s) {
        return collection
                .stream()
                .filter(student -> student.getGroup().equals(s))
                .collect(Collectors.toMap(Student::getLastName,
                                          Student::getFirstName,
                                          BinaryOperator.minBy(String::compareTo)));
    }

}
