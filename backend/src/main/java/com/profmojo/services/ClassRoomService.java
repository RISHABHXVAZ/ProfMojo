package com.profmojo.services;

import com.profmojo.models.ClassRoom;
import com.profmojo.models.Professor;

import java.util.List;

public interface ClassRoomService {

    ClassRoom createClass(String className, Professor professor);

    List<ClassRoom> getMyClasses(Professor professor);
}
